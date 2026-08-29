package com.niloy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_templates")
data class TaskTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val categoryId: Long,
    val priority: String = "MEDIUM",
    val defaultDurationMinutes: Int = 45,
    val startTime: Long? = 420,
    val endTime: Long? = 465,
    val isAllDay: Boolean = false,
    val recurrenceType: String = "DAILY",
    val recurringDays: String = "",
    val recurrenceInterval: Int = 1,
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
