package com.niloy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.niloy.data.local.entity.TaskOccurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskOccurrenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(occurrence: TaskOccurrenceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(occurrences: List<TaskOccurrenceEntity>)

    @Update
    suspend fun update(occurrence: TaskOccurrenceEntity)

    @Delete
    suspend fun delete(occurrence: TaskOccurrenceEntity)

    @Query("DELETE FROM task_occurrences WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM task_occurrences WHERE taskId = :taskId AND date = :date LIMIT 1")
    suspend fun getByTaskIdAndDate(taskId: Long, date: String): TaskOccurrenceEntity?

    @Query("SELECT * FROM task_occurrences WHERE date = :date ORDER BY taskId ASC")
    suspend fun getOccurrencesByDate(date: String): List<TaskOccurrenceEntity>

    @Query("SELECT * FROM task_occurrences WHERE taskId = :taskId ORDER BY date DESC")
    fun getOccurrencesByTaskId(taskId: Long): Flow<List<TaskOccurrenceEntity>>

    @Query("SELECT * FROM task_occurrences ORDER BY date DESC")
    fun getAllOccurrences(): Flow<List<TaskOccurrenceEntity>>

    @Query("SELECT * FROM task_occurrences ORDER BY date DESC")
    suspend fun getAllOccurrencesOnce(): List<TaskOccurrenceEntity>

    @Query("DELETE FROM task_occurrences WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Long)

    @Query("SELECT * FROM task_occurrences WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getOccurrencesByDateRange(startDate: String, endDate: String): List<TaskOccurrenceEntity>
}
