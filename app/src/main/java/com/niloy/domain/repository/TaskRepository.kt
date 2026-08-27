package com.niloy.domain.repository

import com.niloy.domain.model.Category
import com.niloy.domain.model.Task
import com.niloy.domain.model.TaskOccurrence
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    // Category operations
    fun getCategories(): Flow<List<Category>>
    suspend fun saveCategory(category: Category): Long
    suspend fun deleteCategory(category: Category)
    suspend fun saveCategories(categories: List<Category>)

    // Task operations
    fun getTasks(): Flow<List<Task>>
    suspend fun getTaskById(id: Long): Task?
    suspend fun saveTask(task: Task): Long
    suspend fun deleteTask(task: Task)
    suspend fun saveTasks(tasks: List<Task>)

    // Task Occurrence operations
    fun getAllOccurrences(): Flow<List<TaskOccurrence>>
    suspend fun saveOccurrence(occurrence: TaskOccurrence): Long
    suspend fun deleteOccurrence(occurrence: TaskOccurrence)
    suspend fun saveOccurrences(occurrences: List<TaskOccurrence>)

    // Settings operations
    suspend fun getSetting(key: String): String?
    suspend fun saveSetting(key: String, value: String)
}
