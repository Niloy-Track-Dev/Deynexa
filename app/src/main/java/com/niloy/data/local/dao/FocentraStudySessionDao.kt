package com.niloy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.niloy.data.local.entity.FocentraStudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocentraStudySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<FocentraStudySessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocentraStudySessionEntity): Long

    @Query("SELECT * FROM focentra_study_sessions WHERE startTime >= :startTime AND endTime <= :endTime ORDER BY startTime DESC")
    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<FocentraStudySessionEntity>

    @Query("SELECT * FROM focentra_study_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<FocentraStudySessionEntity>

    @Query("SELECT * FROM focentra_study_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): FocentraStudySessionEntity?

    @Query("DELETE FROM focentra_study_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT * FROM focentra_study_sessions WHERE startTime >= :startTime AND endTime <= :endTime ORDER BY startTime DESC")
    fun observeSessionsBetween(startTime: Long, endTime: Long): Flow<List<FocentraStudySessionEntity>>

    @Query("SELECT * FROM focentra_study_sessions ORDER BY startTime DESC")
    fun observeAllSessions(): Flow<List<FocentraStudySessionEntity>>
}
