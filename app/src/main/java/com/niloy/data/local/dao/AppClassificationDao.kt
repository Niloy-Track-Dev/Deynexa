package com.niloy.data.local.dao

import androidx.room.*
import com.niloy.data.local.entity.AppClassificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppClassificationDao {
    @Query("SELECT * FROM app_classifications")
    fun getAllClassifications(): Flow<List<AppClassificationEntity>>

    @Query("SELECT * FROM app_classifications")
    suspend fun getAllClassificationsOnce(): List<AppClassificationEntity>

    @Query("SELECT * FROM app_classifications WHERE packageName = :packageName")
    suspend fun getClassification(packageName: String): AppClassificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: AppClassificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AppClassificationEntity>)

    @Query("DELETE FROM app_classifications")
    suspend fun deleteAll()
}
