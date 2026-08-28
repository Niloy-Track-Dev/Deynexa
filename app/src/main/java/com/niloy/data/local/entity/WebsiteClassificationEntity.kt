package com.niloy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "website_classifications")
data class WebsiteClassificationEntity(
    @PrimaryKey
    val domain: String,
    val category: String,
    val qualityRating: String,
    val customProductivityType: String,
    val isUserOverride: Boolean = false,
    val visitCount: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val firstDetected: Long = 0L,
    val lastDetected: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
