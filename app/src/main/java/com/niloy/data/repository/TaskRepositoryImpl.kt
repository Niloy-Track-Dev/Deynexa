package com.niloy.data.repository

import com.niloy.data.local.dao.CategoryDao
import com.niloy.data.local.dao.SettingDao
import com.niloy.data.local.dao.TaskDao
import com.niloy.data.local.dao.TaskOccurrenceDao
import com.niloy.data.local.entity.CategoryEntity
import com.niloy.data.local.entity.SettingEntity
import com.niloy.data.local.entity.TaskEntity
import com.niloy.data.local.entity.TaskOccurrenceEntity
import com.niloy.domain.model.Category
import com.niloy.domain.model.Task
import com.niloy.domain.model.TaskOccurrence
import com.niloy.domain.model.TaskState
import com.niloy.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

class TaskRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao,
    private val occurrenceDao: TaskOccurrenceDao,
    private val settingDao: SettingDao
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

    private fun TaskEntity.toDomain() = Task(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        isRecurring = isRecurring,
        recurringDays = if (recurringDays.isBlank()) {
            emptySet()
        } else {
            recurringDays.split(",")
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
        },
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
        updatedAt = updatedAt
    )

    private fun TaskOccurrence.toEntity() = TaskOccurrenceEntity(
        taskId = taskId,
        date = date,
        state = state.name,
        updatedAt = updatedAt
    )
}
