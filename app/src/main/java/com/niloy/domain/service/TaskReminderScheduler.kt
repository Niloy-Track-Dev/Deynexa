package com.niloy.domain.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.niloy.MainActivity
import com.niloy.domain.model.Category
import com.niloy.domain.model.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class TaskReminderScheduler(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "daynexa_task_reminders"
        const val CHANNEL_NAME = "Routine & Habit Reminders"
        const val CHANNEL_DESC = "Smart alerts for your scheduled daily routines and habits"

        const val ACTION_TASK_REMINDER = "com.niloy.daynexa.ACTION_TASK_REMINDER"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
        const val EXTRA_CATEGORY_NAME = "extra_category_name"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"
        const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel(context)
    }

    fun scheduleReminder(task: Task, categoryName: String = "") {
        // First cancel any existing alarms for this task
        cancelReminder(task.id)

        if (!task.isEnabled || !task.reminderEnabled || task.startTime == null) {
            return
        }

        val offsetMinutes = task.reminderOffsetMinutes ?: 0
        val taskStartMinutes = task.startTime
        val startHours = (taskStartMinutes / 60).toInt().coerceIn(0, 23)
        val startMins = (taskStartMinutes % 60).toInt().coerceIn(0, 59)
        val taskLocalTime = LocalTime.of(startHours, startMins)
        val reminderLocalTime = taskLocalTime.minusMinutes(offsetMinutes.toLong())

        val now = LocalDateTime.now()

        if (task.isRecurring && task.recurringDays.isNotEmpty()) {
            for (day in task.recurringDays) {
                val nextDateTime = calculateNextOccurrence(day, reminderLocalTime, now)
                val triggerEpochMillis = nextDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val requestCode = generateRequestCode(task.id, day.value)

                setAlarm(
                    triggerEpochMillis = triggerEpochMillis,
                    requestCode = requestCode,
                    task = task,
                    categoryName = categoryName,
                    dayValue = day.value
                )
            }
        } else {
            // One-time or default schedule
            var targetDateTime = LocalDateTime.of(LocalDate.now(), reminderLocalTime)
            if (targetDateTime.isBefore(now) || targetDateTime.isEqual(now)) {
                targetDateTime = targetDateTime.plusDays(1)
            }
            val triggerEpochMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val requestCode = generateRequestCode(task.id, 0)

            setAlarm(
                triggerEpochMillis = triggerEpochMillis,
                requestCode = requestCode,
                task = task,
                categoryName = categoryName,
                dayValue = 0
            )
        }
    }

    private fun calculateNextOccurrence(targetDay: DayOfWeek, reminderTime: LocalTime, now: LocalDateTime): LocalDateTime {
        var date = now.toLocalDate()
        val currentDay = date.dayOfWeek

        var daysToAdd = (targetDay.value - currentDay.value)
        if (daysToAdd < 0) {
            daysToAdd += 7
        } else if (daysToAdd == 0) {
            // Today is target day, check if reminder time is in future
            val todayReminderDateTime = LocalDateTime.of(date, reminderTime)
            if (!todayReminderDateTime.isAfter(now)) {
                daysToAdd = 7 // Move to next week
            }
        }

        val targetDate = date.plusDays(daysToAdd.toLong())
        return LocalDateTime.of(targetDate, reminderTime)
    }

    private fun setAlarm(
        triggerEpochMillis: Long,
        requestCode: Int,
        task: Task,
        categoryName: String,
        dayValue: Int
    ) {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_NAME, task.name)
            putExtra(EXTRA_CATEGORY_NAME, categoryName)
            putExtra(EXTRA_START_TIME, task.startTime ?: 0L)
            putExtra(EXTRA_OFFSET_MINUTES, task.reminderOffsetMinutes ?: 0)
            putExtra(EXTRA_DAY_OF_WEEK, dayValue)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            }
            Log.d("TaskReminderScheduler", "Alarm scheduled for task ${task.name} at epoch $triggerEpochMillis (requestCode: $requestCode)")
        } catch (e: SecurityException) {
            Log.e("TaskReminderScheduler", "SecurityException while setting exact alarm, fallback to inexact", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e("TaskReminderScheduler", "Error setting alarm for task ${task.name}", e)
        }
    }

    fun cancelReminder(taskId: Long) {
        // Cancel single requestCode 0 and recurring requestCodes 1..7
        for (dayVal in 0..7) {
            val requestCode = generateRequestCode(taskId, dayVal)
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                action = ACTION_TASK_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun rescheduleAll(tasks: List<Task>, categories: List<Category>) {
        val categoryMap = categories.associateBy { it.id }
        for (task in tasks) {
            if (task.reminderEnabled && task.isEnabled && task.startTime != null) {
                val catName = categoryMap[task.categoryId]?.name ?: ""
                scheduleReminder(task, catName)
            } else {
                cancelReminder(task.id)
            }
        }
    }

    private fun generateRequestCode(taskId: Long, dayValue: Int): Int {
        // Deterministic integer request code
        return ((taskId % 100000).toInt() * 10) + (dayValue % 10)
    }
}
