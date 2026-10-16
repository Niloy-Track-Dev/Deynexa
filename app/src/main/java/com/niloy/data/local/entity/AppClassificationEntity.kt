package com.niloy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_classifications")
data class AppClassificationEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val categories: String, // Comma separated list of category names e.g. "Education, Productivity"
    val qualityRating: String, // VERY_GOOD, GOOD, NEUTRAL, NOT_GOOD, BAD, VERY_BAD, UNRATED
    val customProductivityType: String = "NEUTRAL", // PRODUCTIVE, NON_PRODUCTIVE, NEUTRAL
    val updatedAt: Long = System.currentTimeMillis()
)
