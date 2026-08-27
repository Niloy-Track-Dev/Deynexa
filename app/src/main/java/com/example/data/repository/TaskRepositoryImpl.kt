package com.example.data.repository

import com.example.data.local.dao.*
import com.example.data.local.entity.SettingEntity
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.*
import com.example.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao,
    private val occurrenceDao: TaskOccurrenceDao,
    private val settingDao: SettingDao
) : TaskRepository {

    override fun getCategories(): Flow<List<Category>> =
        categoryDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun saveCategory(category: Category): Long =
        categoryDao.insert(category.toEntity())

    override suspend fun saveCategories(categories: List<Category>) =
        categoryDao.insertAll(categories.map { it.toEntity() })

    override suspend fun deleteCategory(category: Category) =
        categoryDao.delete(category.toEntity())

    override fun getTasks(): Flow<List<Task>> =
        taskDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getTasksWithCategories(): Flow<List<TaskWithCategory>> {
        return combine(getTasks(), getCategories()) { tasks, categories ->
            tasks.map { task ->
                TaskWithCategory(task, categories.find { it.id == task.categoryId })
            }
        }
    }

    override suspend fun getTaskById(id: Long): Task? =
        taskDao.getById(id)?.toDomain()

    override suspend fun saveTask(task: Task): Long =
        taskDao.insert(task.toEntity())

    override suspend fun saveTasks(tasks: List<Task>) =
        taskDao.insertAll(tasks.map { it.toEntity() })

    override suspend fun deleteTask(task: Task) =
        taskDao.delete(task.toEntity())

    override fun getAllOccurrences(): Flow<List<TaskOccurrence>> =
        occurrenceDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getOccurrencesByDate(date: String): Flow<List<TaskOccurrence>> =
        occurrenceDao.getByDate(date).map { list -> list.map { it.toDomain() } }

    override fun getOccurrencesByDateRange(startDate: String, endDate: String): Flow<List<TaskOccurrence>> =
        occurrenceDao.getByDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }

    override suspend fun saveOccurrence(occurrence: TaskOccurrence) =
        occurrenceDao.insert(occurrence.toEntity())

    override suspend fun saveOccurrences(occurrences: List<TaskOccurrence>) =
        occurrenceDao.insertAll(occurrences.map { it.toEntity() })

    override suspend fun getSetting(key: String): String? =
        settingDao.getByKey(key)?.value

    override suspend fun saveSetting(key: String, value: String) =
        settingDao.insert(SettingEntity(key, value))
}
