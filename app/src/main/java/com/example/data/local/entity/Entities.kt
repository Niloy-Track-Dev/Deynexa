package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.TaskState
import java.time.DayOfWeek

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Int
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val categoryId: Long,
    val icon: String,
    val startTime: Long?,
    val endTime: Long?,
    val isAllDay: Boolean,
    val isEnabled: Boolean,
    val isRecurring: Boolean,
    val recurringDays: String, // Comma separated days e.g. "MONDAY,TUESDAY"
    val color: Int?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "task_occurrences",
    primaryKeys = ["taskId", "date"]
)
data class TaskOccurrenceEntity(
    val taskId: Long,
    val date: String, // yyyy-MM-dd
    val state: TaskState,
    val updatedAt: Long
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
