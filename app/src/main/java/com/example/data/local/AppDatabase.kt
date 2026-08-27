package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.SettingDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TaskOccurrenceDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskOccurrenceEntity

@Database(
    entities = [
        CategoryEntity::class,
        TaskEntity::class,
        TaskOccurrenceEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun taskOccurrenceDao(): TaskOccurrenceDao
    abstract fun settingDao(): SettingDao
}
