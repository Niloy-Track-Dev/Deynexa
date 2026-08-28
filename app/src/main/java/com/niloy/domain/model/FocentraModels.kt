package com.niloy.domain.model

data class FocentraStudySession(
    val sessionId: String,
    val subject: String,
    val topic: String = "",
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val completionStatus: String,
    val focusScore: Int = 0,
    val schemaVersion: Int = 1
)

data class FocentraIntegrationStatus(
    val isInstalled: Boolean,
    val isConnected: Boolean,
    val hasConsent: Boolean,
    val schemaVersion: Int,
    val totalImportedSessions: Int,
    val lastSyncTimestamp: Long
)

