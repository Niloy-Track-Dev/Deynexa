package com.niloy.data.local.dao

import androidx.room.*
import com.niloy.data.local.entity.TaskTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TaskTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<TaskTemplateEntity>)

    @Update
    suspend fun update(template: TaskTemplateEntity)

    @Delete
    suspend fun delete(template: TaskTemplateEntity)

    @Query("DELETE FROM task_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM task_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<TaskTemplateEntity>>

    @Query("SELECT * FROM task_templates ORDER BY name ASC")
    suspend fun getAllTemplatesOnce(): List<TaskTemplateEntity>

    @Query("SELECT * FROM task_templates WHERE id = :id")
    suspend fun getById(id: Long): TaskTemplateEntity?

    @Query("DELETE FROM task_templates")
    suspend fun deleteAll()
}
