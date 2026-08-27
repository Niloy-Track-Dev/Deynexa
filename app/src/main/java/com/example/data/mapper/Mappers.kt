package com.example.data.mapper

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskOccurrenceEntity
import com.example.domain.model.Category
import com.example.domain.model.Task
import com.example.domain.model.TaskOccurrence
import java.time.DayOfWeek

fun CategoryEntity.toDomain(): Category = Category(id, name, icon, color)
fun Category.toEntity(): CategoryEntity = CategoryEntity(id, name, icon, color)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    icon = icon,
    startTime = startTime,
    endTime = endTime,
    isAllDay = isAllDay,
    isEnabled = isEnabled,
    isRecurring = isRecurring,
    recurringDays = if (recurringDays.isEmpty()) emptySet() else recurringDays.split(",").map { DayOfWeek.valueOf(it) }.toSet(),
    color = color,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    icon = icon,
    startTime = startTime,
    endTime = endTime,
    isAllDay = isAllDay,
    isEnabled = isEnabled,
    isRecurring = isRecurring,
    recurringDays = recurringDays.joinToString(",") { it.name },
    color = color,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TaskOccurrenceEntity.toDomain(): TaskOccurrence = TaskOccurrence(taskId, date, state, updatedAt)
fun TaskOccurrence.toEntity(): TaskOccurrenceEntity = TaskOccurrenceEntity(taskId, date, state, updatedAt)
