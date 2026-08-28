package com.niloy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "website_events",
    indices = [
        Index("timestamp"),
        Index("domain")
    ]
)
data class WebsiteEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val timestamp: Long,
    val estimatedDurationMillis: Long = 30_000L,
    val browserPackage: String = "Browser",
    val productivityType: String = "NEUTRAL"
)
