package com.niloy.data.mapper

import com.niloy.data.local.entity.CategoryEntity
import com.niloy.data.local.entity.TaskEntity
import com.niloy.data.local.entity.TaskOccurrenceEntity
import com.niloy.domain.model.Category
import com.niloy.domain.model.Task
import com.niloy.domain.model.TaskOccurrence
import com.niloy.domain.model.TaskState
import java.time.DayOfWeek

fun CategoryEntity.toDomain(): Category = Category(id, name, icon, color)
fun Category.toEntity(): CategoryEntity = CategoryEntity(id, name, icon, color)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    startTime = startTime,
    endTime = endTime,
    isAllDay = isAllDay,
    isEnabled = isEnabled,
    isRecurring = isRecurring,
    recurringDays = if (recurringDays.isEmpty()) emptySet() else recurringDays.split(",").map { DayOfWeek.valueOf(it) }.toSet(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    startTime = startTime,
    endTime = endTime,
    isAllDay = isAllDay,
    isEnabled = isEnabled,
    isRecurring = isRecurring,
    recurringDays = recurringDays.joinToString(",") { it.name },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TaskOccurrenceEntity.toDomain(): TaskOccurrence = TaskOccurrence(taskId, date, TaskState.valueOf(state), updatedAt)
fun TaskOccurrence.toEntity(): TaskOccurrenceEntity = TaskOccurrenceEntity(taskId = taskId, date = date, state = state.name, updatedAt = updatedAt)
