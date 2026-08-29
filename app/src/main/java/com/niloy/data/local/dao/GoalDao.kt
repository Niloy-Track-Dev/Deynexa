package com.niloy.data.local.dao

import androidx.room.*
import com.niloy.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<GoalEntity>)

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY createdAt ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY createdAt ASC")
    suspend fun getAllGoalsOnce(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY createdAt ASC")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
