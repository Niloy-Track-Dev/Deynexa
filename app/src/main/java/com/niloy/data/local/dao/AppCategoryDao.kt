package com.niloy.data.local.dao

import androidx.room.*
import com.niloy.data.local.entity.AppCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryDao {
    @Query("SELECT * FROM app_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<AppCategoryEntity>>

    @Query("SELECT * FROM app_categories")
    suspend fun getAllCategoriesOneShot(): List<AppCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: AppCategoryEntity): Long

    @Update
    suspend fun update(category: AppCategoryEntity)

    @Delete
    suspend fun delete(category: AppCategoryEntity)

    @Query("SELECT * FROM app_categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): AppCategoryEntity?
}
