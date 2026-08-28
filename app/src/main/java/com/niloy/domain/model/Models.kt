package com.niloy.domain.model

import java.time.DayOfWeek

enum class TaskState {
    PENDING,
    COMPLETED,
    SKIPPED
}

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String, // Material Icon name or resource name
    val color: Int,
    val isProductive: Boolean = true
)

data class Task(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val categoryId: Long,
    val icon: String = "",
    val startTime: Long? = null, // Minutes since midnight
    val endTime: Long? = null, // Minutes since midnight
    val isAllDay: Boolean = false,
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = true,
    val recurringDays: Set<DayOfWeek> = emptySet(),
    val color: Int? = null,
    val notes: String = "",
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int? = null, // null: none, 0: at start, 5: 5 min before, 10: 10 min before, 15: 15 min before, 30: 30 min before
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TaskOccurrence(
    val taskId: Long,
    val date: String, // ISO date string yyyy-MM-dd
    val state: TaskState = TaskState.PENDING,
    val updatedAt: Long = System.currentTimeMillis()
)

data class TaskWithCategory(
    val task: Task,
    val category: Category?
)
