package com.niloy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetType: String, // TASKS_COMPLETED, FOCUS_MINUTES, HIGH_PRIORITY_TASKS
    val targetPeriod: String, // DAILY, WEEKLY, MONTHLY
    val targetValue: Int,
    val unit: String = "tasks",
    val categoryId: Long? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedPeriod: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
