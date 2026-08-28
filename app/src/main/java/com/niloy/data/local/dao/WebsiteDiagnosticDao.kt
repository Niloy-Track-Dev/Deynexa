package com.niloy.data.local.dao

import androidx.room.*
import com.niloy.data.local.entity.WebsiteClassificationEntity
import com.niloy.data.local.entity.WebsiteEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebsiteDiagnosticDao {

    // Website Classifications
    @Query("SELECT * FROM website_classifications ORDER BY visitCount DESC, domain ASC")
    fun getAllClassifications(): Flow<List<WebsiteClassificationEntity>>

    @Query("SELECT * FROM website_classifications WHERE domain = :domain LIMIT 1")
    suspend fun getClassification(domain: String): WebsiteClassificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateClassification(entity: WebsiteClassificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateClassifications(entities: List<WebsiteClassificationEntity>)

    @Query("DELETE FROM website_classifications WHERE domain = :domain")
    suspend fun deleteClassification(domain: String)

    @Query("DELETE FROM website_classifications")
    suspend fun deleteAllClassifications()

    // Website Events / Activity Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: WebsiteEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<WebsiteEventEntity>)

    @Query("SELECT * FROM website_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getEventsBetween(startTime: Long, endTime: Long): List<WebsiteEventEntity>

    @Query("SELECT * FROM website_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 100): Flow<List<WebsiteEventEntity>>

    @Query("SELECT COUNT(*) FROM website_events")
    suspend fun getTotalEventsCount(): Int

    @Query("DELETE FROM website_events WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun deleteEventsBetween(startTime: Long, endTime: Long)

    @Query("DELETE FROM website_events")
    suspend fun deleteAllEvents()
}
