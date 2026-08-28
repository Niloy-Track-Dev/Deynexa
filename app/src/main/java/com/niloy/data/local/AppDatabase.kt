package com.niloy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.niloy.data.local.dao.AppClassificationDao
import com.niloy.data.local.dao.CategoryDao
import com.niloy.data.local.dao.SettingDao
import com.niloy.data.local.dao.TaskDao
import com.niloy.data.local.dao.TaskOccurrenceDao
import com.niloy.data.local.entity.AppClassificationEntity
import com.niloy.data.local.entity.CategoryEntity
import com.niloy.data.local.entity.SettingEntity
import com.niloy.data.local.entity.TaskEntity
import com.niloy.data.local.entity.TaskOccurrenceEntity

@Database(
    entities = [
        CategoryEntity::class,
        TaskEntity::class,
        TaskOccurrenceEntity::class,
        SettingEntity::class,
        AppClassificationEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun taskOccurrenceDao(): TaskOccurrenceDao
    abstract fun settingDao(): SettingDao
    abstract fun appClassificationDao(): AppClassificationDao
}

