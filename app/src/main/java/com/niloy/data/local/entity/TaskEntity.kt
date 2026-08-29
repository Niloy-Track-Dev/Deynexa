package com.niloy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val categoryId: Long,
    val priority: String = "MEDIUM",
    val deadlineDate: String? = null,
    val deadlineTime: Long? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val isAllDay: Boolean = false,
    val isRecurring: Boolean = false,
    val recurringDays: String = "", // Serialized as comma-separated string
    val recurrenceType: String = "DAILY",
    val recurrenceInterval: Int = 1,
    val recurrenceDayOfMonth: Int? = null,
    val recurrenceMonthOfYear: Int? = null,
    val recurrenceEndType: String = "NEVER",
    val recurrenceEndDate: String? = null,
    val recurrenceCount: Int? = null,
    val seriesId: Long? = null,
    val startDate: String = "",
    val isEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
