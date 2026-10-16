package com.niloy.domain.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.niloy.DaynexaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        Log.d("BootCompletedReceiver", "Device reboot detected: rescheduling all task reminders")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? DaynexaApplication
                if (app != null) {
                    val tasks = app.repository.getTasks().first()
                    val categories = app.repository.getCategories().first()
                    val scheduler = TaskReminderScheduler(context)
                    scheduler.rescheduleAll(tasks, categories)
                    Log.d("BootCompletedReceiver", "Successfully restored reminders for ${tasks.count { it.reminderEnabled }} routines")
                }
            } catch (e: Exception) {
                Log.e("BootCompletedReceiver", "Failed to reschedule reminders on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
