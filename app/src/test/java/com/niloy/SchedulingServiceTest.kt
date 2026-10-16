package com.niloy

import com.niloy.domain.model.*
import com.niloy.domain.service.SchedulingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class SchedulingServiceTest {

    private val schedulingService = SchedulingService()

    @Test
    fun testDailyRecurrence() {
        val task = Task(
            id = 1L,
            name = "Morning Stretch",
            categoryId = 1L,
            recurrenceType = RecurrenceType.DAILY,
            startDate = "2026-08-01"
        )

        val date1 = LocalDate.of(2026, 8, 20)
        val date2 = LocalDate.of(2026, 8, 21)

        val result1 = schedulingService.getTasksForDate(listOf(task), emptyList(), date1)
        val result2 = schedulingService.getTasksForDate(listOf(task), emptyList(), date2)

        assertEquals(1, result1.size)
        assertEquals(1, result2.size)
    }

    @Test
    fun testEveryXDaysRecurrence() {
        val task = Task(
            id = 2L,
            name = "Water Plants",
            categoryId = 1L,
            recurrenceType = RecurrenceType.EVERY_X_DAYS,
            recurrenceInterval = 3,
            startDate = "2026-08-01"
        )

        // Day 0: Aug 1 (match)
        // Day 3: Aug 4 (match)
        // Day 4: Aug 5 (no match)
        val aug1 = LocalDate.of(2026, 8, 1)
        val aug4 = LocalDate.of(2026, 8, 4)
        val aug5 = LocalDate.of(2026, 8, 5)

        assertEquals(1, schedulingService.getTasksForDate(listOf(task), emptyList(), aug1).size)
        assertEquals(1, schedulingService.getTasksForDate(listOf(task), emptyList(), aug4).size)
        assertEquals(0, schedulingService.getTasksForDate(listOf(task), emptyList(), aug5).size)
    }

    @Test
    fun testWeekdaysAndWeekendsRecurrence() {
        val weekdayTask = Task(
            id = 3L,
            name = "Standup Meeting",
            categoryId = 1L,
            recurrenceType = RecurrenceType.WEEKDAYS,
            startDate = "2026-08-01"
        )

        val weekendTask = Task(
            id = 4L,
            name = "Weekend Hike",
            categoryId = 1L,
            recurrenceType = RecurrenceType.WEEKENDS,
            startDate = "2026-08-01"
        )

        val customWeekend = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)

        // Sunday (Aug 23, 2026) -> working day with custom weekend
        val sunday = LocalDate.of(2026, 8, 23)
        // Friday (Aug 28, 2026) -> weekend with custom weekend
        val friday = LocalDate.of(2026, 8, 28)

        // On Sunday: weekdayTask matches, weekendTask does not
        assertEquals(1, schedulingService.getTasksForDate(listOf(weekdayTask), emptyList(), sunday, customWeekend).size)
        assertEquals(0, schedulingService.getTasksForDate(listOf(weekendTask), emptyList(), sunday, customWeekend).size)

        // On Friday: weekdayTask does not match, weekendTask matches
        assertEquals(0, schedulingService.getTasksForDate(listOf(weekdayTask), emptyList(), friday, customWeekend).size)
        assertEquals(1, schedulingService.getTasksForDate(listOf(weekendTask), emptyList(), friday, customWeekend).size)
    }

    @Test
    fun testSpecificDayOfMonthRecurrence() {
        val task = Task(
            id = 5L,
            name = "Monthly Rent & Bills",
            categoryId = 1L,
            recurrenceType = RecurrenceType.SPECIFIC_DAY_OF_MONTH,
            recurrenceDayOfMonth = 15,
            startDate = "2026-01-01"
        )

        val matchDate = LocalDate.of(2026, 8, 15)
        val nonMatchDate = LocalDate.of(2026, 8, 16)

        assertEquals(1, schedulingService.getTasksForDate(listOf(task), emptyList(), matchDate).size)
        assertEquals(0, schedulingService.getTasksForDate(listOf(task), emptyList(), nonMatchDate).size)
    }

    @Test
    fun testYearlyRecurrence() {
        val task = Task(
            id = 6L,
            name = "Annual Review",
            categoryId = 1L,
            recurrenceType = RecurrenceType.YEARLY,
            recurrenceMonthOfYear = 10,
            recurrenceDayOfMonth = 25,
            startDate = "2025-01-01"
        )

        val match2026 = LocalDate.of(2026, 10, 25)
        val match2027 = LocalDate.of(2027, 10, 25)
        val nonMatch = LocalDate.of(2026, 10, 26)

        assertEquals(1, schedulingService.getTasksForDate(listOf(task), emptyList(), match2026).size)
        assertEquals(1, schedulingService.getTasksForDate(listOf(task), emptyList(), match2027).size)
        assertEquals(0, schedulingService.getTasksForDate(listOf(task), emptyList(), nonMatch).size)
    }

    @Test
    fun testRecurrenceEndDate() {
        val task = Task(
            id = 7L,
            name = "Sprint Focus",
            categoryId = 1L,
            recurrenceType = RecurrenceType.DAILY,
            recurrenceEndType = RecurrenceEndType.ON_DATE,
            recurrenceEndDate = "2026-08-20",
            startDate = "2026-08-01"
        )

        val beforeEnd = LocalDate.of(2026, 8, 20)
        val afterEnd = LocalDate.of(2026, 8, 21)

        assertEquals(1, schedulingService.getTasksForDate(listOf(task), emptyList(), beforeEnd).size)
        assertEquals(0, schedulingService.getTasksForDate(listOf(task), emptyList(), afterEnd).size)
    }

    @Test
    fun testReschedulingException() {
        val task = Task(
            id = 8L,
            name = "Deep Work",
            categoryId = 1L,
            recurrenceType = RecurrenceType.DAILY,
            startTime = 540, // 09:00 AM
            endTime = 600,   // 10:00 AM
            startDate = "2026-08-01"
        )

        // Reschedule Aug 24 instance to 14:00 (840)
        val occurrence = TaskOccurrence(
            taskId = 8L,
            date = "2026-08-24",
            state = TaskState.PENDING,
            rescheduledStartTime = 840,
            rescheduledEndTime = 900,
            isException = true
        )

        val aug24 = LocalDate.of(2026, 8, 24)
        val results = schedulingService.getTasksForDate(listOf(task), listOf(occurrence), aug24)

        assertEquals(1, results.size)
        assertEquals(840L, results[0].effectiveStartTime)
        assertEquals(900L, results[0].effectiveEndTime)
    }

    @Test
    fun testConflictDetection() {
        val task1 = Task(
            id = 9L,
            name = "Client Call",
            categoryId = 1L,
            startTime = 600, // 10:00 AM
            endTime = 660,   // 11:00 AM
            startDate = "2026-08-24"
        )
        val task2 = Task(
            id = 10L,
            name = "Design Review",
            categoryId = 1L,
            startTime = 630, // 10:30 AM
            endTime = 720,   // 12:00 PM
            startDate = "2026-08-24"
        )

        val aug24 = LocalDate.of(2026, 8, 24)
        val existing = schedulingService.getTasksForDate(listOf(task1), emptyList(), aug24)

        val conflicts = schedulingService.findConflicts(
            existingTasks = existing,
            targetTask = task2,
            targetStartTime = 630,
            targetEndTime = 720
        )

        assertEquals(1, conflicts.size)
        assertEquals(30, conflicts[0].overlapMinutes)
    }
}
