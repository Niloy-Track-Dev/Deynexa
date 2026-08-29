package com.niloy.data.repository

import com.niloy.data.local.dao.CategoryDao
import com.niloy.data.local.dao.SettingDao
import com.niloy.data.local.dao.TaskDao
import com.niloy.data.local.dao.TaskOccurrenceDao
import com.niloy.data.local.dao.TaskTemplateDao
import com.niloy.data.local.entity.CategoryEntity
import com.niloy.data.local.entity.SettingEntity
import com.niloy.data.local.entity.TaskEntity
import com.niloy.data.local.entity.TaskOccurrenceEntity
import com.niloy.data.local.entity.TaskTemplateEntity
import com.niloy.domain.model.*
import com.niloy.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

class TaskRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao,
    private val occurrenceDao: TaskOccurrenceDao,
    private val settingDao: SettingDao,
    private val taskTemplateDao: TaskTemplateDao? = null
) : TaskRepository {

    override fun getCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveCategory(category: Category): Long {
        return categoryDao.insert(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category.toEntity())
    }

    override suspend fun saveCategories(categories: List<Category>) {
        categoryDao.insertAll(categories.map { it.toEntity() })
    }

    override fun getTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getTaskById(id: Long): Task? {
        return taskDao.getById(id)?.toDomain()
    }

    override suspend fun saveTask(task: Task): Long {
        return taskDao.insert(task.toEntity())
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.delete(task.toEntity())
    }

    override suspend fun saveTasks(tasks: List<Task>) {
        taskDao.insertAll(tasks.map { it.toEntity() })
    }

    override fun getAllOccurrences(): Flow<List<TaskOccurrence>> =
        occurrenceDao.getAllOccurrences().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveOccurrence(occurrence: TaskOccurrence): Long {
        return occurrenceDao.insert(occurrence.toEntity())
    }

    override suspend fun deleteOccurrence(occurrence: TaskOccurrence) {
        occurrenceDao.delete(occurrence.toEntity())
    }

    override suspend fun saveOccurrences(occurrences: List<TaskOccurrence>) {
        occurrenceDao.insertAll(occurrences.map { it.toEntity() })
    }

    // Task Template operations
    override fun getTaskTemplates(): Flow<List<TaskTemplate>> =
        (taskTemplateDao?.getAllTemplates() ?: kotlinx.coroutines.flow.flowOf(emptyList())).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getTaskTemplateById(id: Long): TaskTemplate? {
        return taskTemplateDao?.getById(id)?.toDomain()
    }

    override suspend fun saveTaskTemplate(template: TaskTemplate): Long {
        return taskTemplateDao?.insert(template.toEntity()) ?: 0L
    }

    override suspend fun deleteTaskTemplate(template: TaskTemplate) {
        taskTemplateDao?.delete(template.toEntity())
    }

    override suspend fun saveTaskTemplates(templates: List<TaskTemplate>) {
        taskTemplateDao?.insertAll(templates.map { it.toEntity() })
    }

    override suspend fun getSetting(key: String): String? {
        return settingDao.getByKey(key)?.value
    }

    override suspend fun saveSetting(key: String, value: String) {
        settingDao.insert(SettingEntity(key, value))
    }

    // Mappers
    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        icon = icon,
        color = color
    )

    private fun Category.toEntity() = CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        color = color
    )

    private fun parseRecurringDays(str: String): Set<DayOfWeek> {
        if (str.isBlank()) return emptySet()
        return str.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull {
                try {
                    DayOfWeek.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }
            .toSet()
    }

    private fun TaskEntity.toDomain() = Task(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        isRecurring = isRecurring,
        recurringDays = parseRecurringDays(recurringDays),
        recurrenceType = try { RecurrenceType.valueOf(recurrenceType) } catch (e: Exception) { RecurrenceType.DAILY },
        recurrenceInterval = recurrenceInterval,
        recurrenceDayOfMonth = recurrenceDayOfMonth,
        recurrenceMonthOfYear = recurrenceMonthOfYear,
        recurrenceEndType = try { RecurrenceEndType.valueOf(recurrenceEndType) } catch (e: Exception) { RecurrenceEndType.NEVER },
        recurrenceEndDate = recurrenceEndDate,
        recurrenceCount = recurrenceCount,
        seriesId = seriesId,
        startDate = startDate,
        isEnabled = isEnabled,
        reminderEnabled = reminderEnabled,
        reminderOffsetMinutes = reminderOffsetMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Task.toEntity() = TaskEntity(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        isRecurring = isRecurring,
        recurringDays = recurringDays.joinToString(",") { it.name },
        recurrenceType = recurrenceType.name,
        recurrenceInterval = recurrenceInterval,
        recurrenceDayOfMonth = recurrenceDayOfMonth,
        recurrenceMonthOfYear = recurrenceMonthOfYear,
        recurrenceEndType = recurrenceEndType.name,
        recurrenceEndDate = recurrenceEndDate,
        recurrenceCount = recurrenceCount,
        seriesId = seriesId,
        startDate = startDate,
        isEnabled = isEnabled,
        reminderEnabled = reminderEnabled,
        reminderOffsetMinutes = reminderOffsetMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun TaskOccurrenceEntity.toDomain() = TaskOccurrence(
        taskId = taskId,
        date = date,
        state = try { TaskState.valueOf(state) } catch (e: Exception) { TaskState.PENDING },
        rescheduledStartTime = rescheduledStartTime,
        rescheduledEndTime = rescheduledEndTime,
        rescheduledDate = rescheduledDate,
        isException = isException,
        notes = notes,
        updatedAt = updatedAt
    )

    private fun TaskOccurrence.toEntity() = TaskOccurrenceEntity(
        taskId = taskId,
        date = date,
        state = state.name,
        rescheduledStartTime = rescheduledStartTime,
        rescheduledEndTime = rescheduledEndTime,
        rescheduledDate = rescheduledDate,
        isException = isException,
        notes = notes,
        updatedAt = updatedAt
    )

    private fun TaskTemplateEntity.toDomain() = TaskTemplate(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        defaultDurationMinutes = defaultDurationMinutes,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        recurrenceType = try { RecurrenceType.valueOf(recurrenceType) } catch (e: Exception) { RecurrenceType.DAILY },
        recurringDays = parseRecurringDays(recurringDays),
        recurrenceInterval = recurrenceInterval,
        reminderEnabled = reminderEnabled,
        reminderOffsetMinutes = reminderOffsetMinutes,
        createdAt = createdAt
    )

    private fun TaskTemplate.toEntity() = TaskTemplateEntity(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        defaultDurationMinutes = defaultDurationMinutes,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        recurrenceType = recurrenceType.name,
        recurringDays = recurringDays.joinToString(",") { it.name },
        recurrenceInterval = recurrenceInterval,
        reminderEnabled = reminderEnabled,
        reminderOffsetMinutes = reminderOffsetMinutes,
        createdAt = createdAt
    )
}

