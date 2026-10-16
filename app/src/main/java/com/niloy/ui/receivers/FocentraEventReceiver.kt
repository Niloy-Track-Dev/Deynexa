package com.niloy.ui.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.niloy.DaynexaApplication
import com.niloy.data.local.entity.FocentraStudySessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocentraEventReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "FocentraEventReceiver"
        private const val ACTION_SESSION_COMPLETED = "com.focentra.ACTION_SESSION_COMPLETED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SESSION_COMPLETED) {
            val app = context.applicationContext as? DaynexaApplication ?: return
            val manager = app.focentraIntegrationManager

            val sessionId = intent.getStringExtra("sessionId") ?: return
            val subject = intent.getStringExtra("subject") ?: "Study Session"
            val topic = intent.getStringExtra("topic") ?: ""
            val startTime = intent.getLongExtra("startTime", System.currentTimeMillis())
            val endTime = intent.getLongExtra("endTime", System.currentTimeMillis())
            val duration = intent.getLongExtra("duration", (endTime - startTime) / 60000L)
            val completionStatus = intent.getStringExtra("completionStatus") ?: "COMPLETED"
            val focusScore = intent.getIntExtra("focusScore", 0)
            val schemaVersion = intent.getIntExtra("schemaVersion", 1)

            val entity = FocentraStudySessionEntity(
                sessionId = sessionId,
                subject = subject,
                topic = topic,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                completionStatus = completionStatus,
                focusScore = focusScore,
                schemaVersion = schemaVersion
            )

            Log.d(TAG, "Received Focentra session completed: $sessionId")

            CoroutineScope(Dispatchers.IO).launch {
                manager.insertOrUpdateSession(entity)
            }
        }
    }
}
