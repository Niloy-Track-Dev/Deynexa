package com.niloy.data.local

import androidx.room.TypeConverter
import com.niloy.domain.model.TaskState
import java.time.DayOfWeek

class Converters {
    @TypeConverter
    fun fromTaskState(state: TaskState): String = state.name

    @TypeConverter
    fun toTaskState(value: String): TaskState = TaskState.valueOf(value)

    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String = days.joinToString(",") { it.name }

    @TypeConverter
    fun toDayOfWeekSet(value: String): Set<DayOfWeek> {
        if (value.isEmpty()) return emptySet()
        return value.split(",").map { DayOfWeek.valueOf(it) }.toSet()
    }
}
