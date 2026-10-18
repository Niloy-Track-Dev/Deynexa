package com.niloy.ui.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object TimeUtils {

    fun formatTime(minutes: Long?, is24Hour: Boolean = false): String {
        if (minutes == null) return ""
        val normalized = ((minutes % 1440) + 1440) % 1440
        val hours = (normalized / 60).toInt()
        val mins = (normalized % 60).toInt()

        val period = if (hours < 12) "AM" else "PM"
        val displayHour = when (hours) {
            0 -> 12
            in 1..12 -> hours
            else -> hours - 12
        }
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, mins, period)
    }

    fun formatMinutes(minutes: Long, is24Hour: Boolean = false): String = formatTime(minutes, false)
    fun formatMinutes(minutes: Int, is24Hour: Boolean = false): String = formatTime(minutes.toLong(), false)

    fun formatTimeRange(startTime: Long?, endTime: Long?, isAllDay: Boolean, is24Hour: Boolean = false): String {
        if (isAllDay) return "All Day"
        if (startTime == null && endTime == null) return "All Day"
        if (startTime != null && endTime != null) {
            if (startTime == endTime) {
                return formatTime(startTime, false)
            }

            val startNorm = ((startTime % 1440) + 1440) % 1440
            val endNorm = ((endTime % 1440) + 1440) % 1440

            val startHours = (startNorm / 60).toInt()
            val startMins = (startNorm % 60).toInt()
            val startPeriod = if (startHours < 12) "AM" else "PM"
            val startDispHour = when (startHours) {
                0 -> 12
                in 1..12 -> startHours
                else -> startHours - 12
            }

            val endHours = (endNorm / 60).toInt()
            val endMins = (endNorm % 60).toInt()
            val endPeriod = if (endHours < 12) "AM" else "PM"
            val endDispHour = when (endHours) {
                0 -> 12
                in 1..12 -> endHours
                else -> endHours - 12
            }

            return String.format(
                Locale.getDefault(),
                "%02d:%02d %s – %02d:%02d %s",
                startDispHour,
                startMins,
                startPeriod,
                endDispHour,
                endMins,
                endPeriod
            )
        }
        return formatTime(startTime ?: endTime, false)
    }

    fun getDayGreeting(): String {
        val hour = java.time.LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    fun formatDateHeader(date: LocalDate): String {
        val today = LocalDate.now()
        val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val day = date.dayOfMonth

        return when {
            date == today -> "Today, $month $day"
            date == today.plusDays(1) -> "Tomorrow, $month $day"
            date == today.minusDays(1) -> "Yesterday, $month $day"
            else -> "$dayOfWeek, $month $day"
        }
    }
}
