package com.example.ui.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object TimeUtils {

    fun formatTime(minutes: Long?, is24Hour: Boolean = true): String {
        if (minutes == null) return ""
        val normalized = (minutes % 1440).toInt()
        val hours = normalized / 60
        val mins = normalized % 60

        return if (is24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", hours, mins)
        } else {
            val period = if (hours < 12) "AM" else "PM"
            val displayHour = when (hours) {
                0 -> 12
                in 1..12 -> hours
                else -> hours - 12
            }
            String.format(Locale.getDefault(), "%d:%02d %s", displayHour, mins, period)
        }
    }

    fun formatTimeRange(startTime: Long?, endTime: Long?, isAllDay: Boolean, is24Hour: Boolean = true): String {
        if (isAllDay) return "All Day"
        if (startTime == null && endTime == null) return "All Day"
        if (startTime != null && endTime != null) {
            if (startTime == endTime) {
                return formatTime(startTime, is24Hour)
            }
            return "${formatTime(startTime, is24Hour)} – ${formatTime(endTime, is24Hour)}"
        }
        return formatTime(startTime ?: endTime, is24Hour)
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
