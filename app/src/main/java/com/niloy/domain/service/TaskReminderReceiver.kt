package com.niloy.domain.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.niloy.DaynexaApplication
import com.niloy.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != TaskReminderScheduler.ACTION_TASK_REMINDER) return

        val taskId = intent.getLongExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1L)
        val taskName = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_NAME) ?: "Scheduled Routine"
        val categoryName = intent.getStringExtra(TaskReminderScheduler.EXTRA_CATEGORY_NAME) ?: ""
        val startTime = intent.getLongExtra(TaskReminderScheduler.EXTRA_START_TIME, -1L)
        val offsetMinutes = intent.getIntExtra(TaskReminderScheduler.EXTRA_OFFSET_MINUTES, 0)
        val dayOfWeekValue = intent.getIntExtra(TaskReminderScheduler.EXTRA_DAY_OF_WEEK, 0)

        Log.d("TaskReminderReceiver", "Received alarm for task: $taskName ($taskId)")

        // Format time string
        val timeString = if (startTime >= 0) {
            val hours = (startTime / 60).toInt()
            val mins = (startTime % 60).toInt()
            val period = if (hours >= 12) "PM" else "AM"
            val displayHour = when {
                hours == 0 -> 12
                hours > 12 -> hours - 12
                else -> hours
            }
            String.format("%d:%02d %s", displayHour, mins, period)
        } else {
            "Scheduled Time"
        }

        val contentMessage = when {
            offsetMinutes > 0 -> "Starting in $offsetMinutes minutes (at $timeString)"
            else -> "Routine starts now ($timeString)"
        }

        val finalCategoryText = if (categoryName.isNotBlank()) " • $categoryName" else ""

        // PendingIntent to launch Daynexa
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_task_id", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (taskId % 100000).toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        TaskReminderScheduler.createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, TaskReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $taskName$finalCategoryText")
            .setContentText(contentMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentMessage\nStay consistent with your daily routine!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((taskId % 100000).toInt(), notification)

        // Reschedule recurring alarm for next week in background
        if (dayOfWeekValue > 0 && taskId > 0) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? DaynexaApplication
                    val task = app?.repository?.getTaskById(taskId)
                    if (task != null && task.reminderEnabled && task.isEnabled) {
                        val categories = app.repository.getCategories()
                        val scheduler = TaskReminderScheduler(context)
                        scheduler.scheduleReminder(task, categoryName)
                    }
                } catch (e: Exception) {
                    Log.e("TaskReminderReceiver", "Error re-scheduling recurring reminder", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
