package com.niloy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focentra_study_sessions")
data class FocentraStudySessionEntity(
    @PrimaryKey val sessionId: String,
    val subject: String,
    val topic: String = "",
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val completionStatus: String,
    val focusScore: Int = 0,
    val schemaVersion: Int = 1,
    val importedAt: Long = System.currentTimeMillis()
)

