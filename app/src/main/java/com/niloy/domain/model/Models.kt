package com.niloy.domain.model

import java.time.DayOfWeek

enum class TaskState {
    PENDING,
    COMPLETED,
    SKIPPED
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class RecurrenceType {
    NONE,                   // One-time task
    DAILY,                  // Every Day
    SPECIFIC_DAYS,          // Selected Days of Week
    WEEKDAYS,               // Monday-Friday or Custom Working Days
    WEEKENDS,               // Custom Weekend Days (max 2)
    EVERY_X_DAYS,           // Every X Days (e.g., every 2 days)
    EVERY_X_WEEKS,          // Every X Weeks on selected days
    MONTHLY,                // Monthly on the same day
    SPECIFIC_DAY_OF_MONTH,  // Specific day of month (e.g. 1st, 15th, 28th)
    YEARLY,                 // Yearly on specific month & day
    CUSTOM                  // Custom interval
}

enum class RecurrenceEndType {
    NEVER,
    ON_DATE,
    AFTER_OCCURRENCES
}

enum class RecurrenceEditMode {
    THIS_OCCURRENCE,
    THIS_AND_FUTURE,
    ENTIRE_SERIES
}

enum class GoalType {
    TASKS_COMPLETED,
    FOCUS_MINUTES,
    HIGH_PRIORITY_TASKS
}

enum class GoalPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

data class Goal(
    val id: Long = 0,
    val title: String,
    val targetType: GoalType = GoalType.TASKS_COMPLETED,
    val targetPeriod: GoalPeriod = GoalPeriod.DAILY,
    val targetValue: Int = 5,
    val unit: String = "tasks",
    val categoryId: Long? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedPeriod: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class GoalProgress(
    val goal: Goal,
    val currentValue: Int,
    val targetValue: Int,
    val percentage: Float,
    val isCompleted: Boolean,
    val periodLabel: String
)

data class ScoreBreakdown(
    val totalScore: Int,
    val rating: String,
    val taskCompletionPoints: Int,
    val priorityPoints: Int,
    val focusPoints: Int,
    val streakPoints: Int,
    val penaltyPoints: Int,
    val explanation: String
)

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String, // Material Icon name or resource name
    val color: Int
)

data class Task(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val categoryId: Long,
    val icon: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val deadlineDate: String? = null, // yyyy-MM-dd
    val deadlineTime: Long? = null, // Minutes since midnight
    val startTime: Long? = null, // Minutes since midnight
    val endTime: Long? = null, // Minutes since midnight
    val isAllDay: Boolean = false,
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = true,
    val recurringDays: Set<DayOfWeek> = emptySet(),
    // Advanced Recurrence fields
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val recurrenceInterval: Int = 1,
    val recurrenceDayOfMonth: Int? = null,
    val recurrenceMonthOfYear: Int? = null,
    val recurrenceEndType: RecurrenceEndType = RecurrenceEndType.NEVER,
    val recurrenceEndDate: String? = null, // yyyy-MM-dd
    val recurrenceCount: Int? = null,
    val seriesId: Long? = null,
    val startDate: String = "", // yyyy-MM-dd
    // Additional fields
    val color: Int? = null,
    val notes: String = "",
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int? = null, // null: none, 0: at start, 5, 10, 15, 30 min before
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TaskOccurrence(
    val taskId: Long,
    val date: String, // ISO date string yyyy-MM-dd
    val state: TaskState = TaskState.PENDING,
    val rescheduledStartTime: Long? = null,
    val rescheduledEndTime: Long? = null,
    val rescheduledDate: String? = null,
    val isException: Boolean = false,
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class TaskTemplate(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val categoryId: Long,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val defaultDurationMinutes: Int = 45,
    val startTime: Long? = 420,
    val endTime: Long? = 465,
    val isAllDay: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val recurringDays: Set<DayOfWeek> = emptySet(),
    val recurrenceInterval: Int = 1,
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class TaskWithCategory(
    val task: Task,
    val category: Category?
)

data class ScheduleConflict(
    val conflictingTask: Task,
    val overlapMinutes: Int,
    val message: String
)

data class ProductivityInsights(
    val mostCompletedTaskName: String = "—",
    val mostCompletedTaskCount: Int = 0,
    val mostSkippedTaskName: String = "—",
    val mostSkippedTaskCount: Int = 0,
    val bestCompletionWeekday: String = "—",
    val bestCompletionWeekdayRate: Float = 0f,
    val lowestCompletionWeekday: String = "—",
    val lowestCompletionWeekdayRate: Float = 0f,
    val currentStreak: Int = 0,
    val longestRecurringStreak: Int = 0,
    val averageFocusSessionMinutes: Long = 0L,
    val longestFocusSessionMinutes: Long = 0L,
    val totalFocusMinutes: Long = 0L,
    val bestFocusDay: String = "—"
)

data class WeeklyReviewData(
    val weekStartDate: String,
    val weekEndDate: String,
    val totalScheduled: Int,
    val totalCompleted: Int,
    val totalSkipped: Int,
    val completionRate: Float,
    val focentraFocusMinutes: Long,
    val longestFocusSessionMinutes: Long,
    val bestFocusDay: String,
    val averageProductivityScore: Int,
    val bestDayName: String,
    val activeGoalsCount: Int,
    val completedGoalsCount: Int,
    val highlights: List<String>
)
