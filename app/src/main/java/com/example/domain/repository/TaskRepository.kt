package com.example.domain.repository

import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getCategories(): Flow<List<Category>>
    suspend fun saveCategory(category: Category): Long
    suspend fun saveCategories(categories: List<Category>)
    suspend fun deleteCategory(category: Category)

    fun getTasks(): Flow<List<Task>>
    fun getTasksWithCategories(): Flow<List<TaskWithCategory>>
    suspend fun getTaskById(id: Long): Task?
    suspend fun saveTask(task: Task): Long
    suspend fun saveTasks(tasks: List<Task>)
    suspend fun deleteTask(task: Task)

    fun getAllOccurrences(): Flow<List<TaskOccurrence>>
    fun getOccurrencesByDate(date: String): Flow<List<TaskOccurrence>>
    fun getOccurrencesByDateRange(startDate: String, endDate: String): Flow<List<TaskOccurrence>>
    suspend fun saveOccurrence(occurrence: TaskOccurrence)
    suspend fun saveOccurrences(occurrences: List<TaskOccurrence>)

    suspend fun getSetting(key: String): String?
    suspend fun saveSetting(key: String, value: String)
}
