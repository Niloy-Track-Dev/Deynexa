package com.example.domain.service

import com.example.domain.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SchedulingService {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getTasksForDate(
        tasks: List<Task>,
        occurrences: List<TaskOccurrence>,
        date: LocalDate
    ): List<TaskWithOccurrence> {
        val dateString = date.format(dateFormatter)
        val dayOfWeek = date.dayOfWeek

        return tasks.filter { task ->
            if (!task.isEnabled) return@filter false
            
            if (task.isRecurring) {
                task.recurringDays.contains(dayOfWeek)
            } else {
                // For non-recurring tasks, we assume they appear on the day they were created
                // or we could have a specific 'date' field for them. 
                // For this MVP, let's assume if it's not recurring it's a "one-off" task.
                val createdDate = LocalDate.ofEpochDay(task.createdAt / (24 * 60 * 60 * 1000))
                createdDate == date
            }
        }.map { task ->
            val occurrence = occurrences.find { it.taskId == task.id && it.date == dateString }
            TaskWithOccurrence(task, occurrence ?: TaskOccurrence(task.id, dateString))
        }
    }
}

data class TaskWithOccurrence(
    val task: Task,
    val occurrence: TaskOccurrence
)
