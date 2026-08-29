package com.niloy.domain.repository

import com.niloy.domain.model.Category
import com.niloy.domain.model.Goal
import com.niloy.domain.model.Task
import com.niloy.domain.model.TaskOccurrence
import com.niloy.domain.model.TaskTemplate
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

    // Task Template operations (v0.7.0)
    fun getTaskTemplates(): Flow<List<TaskTemplate>>
    suspend fun getTaskTemplateById(id: Long): TaskTemplate?
    suspend fun saveTaskTemplate(template: TaskTemplate): Long
    suspend fun deleteTaskTemplate(template: TaskTemplate)
    suspend fun saveTaskTemplates(templates: List<TaskTemplate>)

    // Goal operations (v0.8.0)
    fun getGoals(): Flow<List<Goal>>
    fun getActiveGoals(): Flow<List<Goal>>
    suspend fun getGoalById(id: Long): Goal?
    suspend fun saveGoal(goal: Goal): Long
    suspend fun deleteGoal(goal: Goal)
    suspend fun saveGoals(goals: List<Goal>)

    // Settings operations
    suspend fun getSetting(key: String): String?
    suspend fun saveSetting(key: String, value: String)
}

