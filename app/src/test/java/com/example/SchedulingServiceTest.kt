package com.example

import com.example.domain.model.Task
import com.example.domain.model.TaskOccurrence
import com.example.domain.model.TaskState
import com.example.domain.service.SchedulingService
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class SchedulingServiceTest {

    private val schedulingService = SchedulingService()

    @Test
    fun testRecurringTaskMatchesDate() {
        val task = Task(
            id = 1L,
            name = "Morning Workout",
            categoryId = 1L,
            isRecurring = true,
            recurringDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            createdAt = System.currentTimeMillis()
        )

        // Monday
        val monday = LocalDate.of(2026, 8, 24) // Monday
        val resultMonday = schedulingService.getTasksForDate(listOf(task), emptyList(), monday)
        assertEquals(1, resultMonday.size)
        assertEquals(TaskState.PENDING, resultMonday[0].occurrence.state)

        // Tuesday
        val tuesday = LocalDate.of(2026, 8, 25) // Tuesday
        val resultTuesday = schedulingService.getTasksForDate(listOf(task), emptyList(), tuesday)
        assertEquals(0, resultTuesday.size)
    }

    @Test
    fun testTaskWithOccurrenceState() {
        val task = Task(
            id = 2L,
            name = "Read Book",
            categoryId = 1L,
            isRecurring = true,
            recurringDays = setOf(DayOfWeek.MONDAY),
            createdAt = System.currentTimeMillis()
        )
        val occurrence = TaskOccurrence(
            taskId = 2L,
            date = "2026-08-24",
            state = TaskState.COMPLETED
        )

        val monday = LocalDate.of(2026, 8, 24)
        val result = schedulingService.getTasksForDate(listOf(task), listOf(occurrence), monday)
        assertEquals(1, result.size)
        assertEquals(TaskState.COMPLETED, result[0].occurrence.state)
    }
}
