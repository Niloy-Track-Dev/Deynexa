package com.niloy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.niloy.data.local.dao.AppClassificationDao
import com.niloy.data.local.dao.CategoryDao
import com.niloy.data.local.dao.DomainRuleDao
import com.niloy.data.local.dao.FocentraStudySessionDao
import com.niloy.data.local.dao.SettingDao
import com.niloy.data.local.dao.TaskDao
import com.niloy.data.local.dao.TaskOccurrenceDao
import com.niloy.data.local.dao.WebsiteDiagnosticDao
import com.niloy.data.local.entity.AppClassificationEntity
import com.niloy.data.local.entity.CategoryEntity
import com.niloy.data.local.entity.DomainRuleEntity
import com.niloy.data.local.entity.FocentraStudySessionEntity
import com.niloy.data.local.entity.SettingEntity
import com.niloy.data.local.entity.TaskEntity
import com.niloy.data.local.entity.TaskOccurrenceEntity
import com.niloy.data.local.entity.WebsiteClassificationEntity
import com.niloy.data.local.entity.WebsiteEventEntity

@Database(
    entities = [
        CategoryEntity::class,
        TaskEntity::class,
        TaskOccurrenceEntity::class,
        SettingEntity::class,
        AppClassificationEntity::class,
        WebsiteClassificationEntity::class,
        WebsiteEventEntity::class,
        DomainRuleEntity::class,
        FocentraStudySessionEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun taskOccurrenceDao(): TaskOccurrenceDao
    abstract fun settingDao(): SettingDao
    abstract fun appClassificationDao(): AppClassificationDao
    abstract fun websiteDiagnosticDao(): WebsiteDiagnosticDao
    abstract fun domainRuleDao(): DomainRuleDao
    abstract fun focentraStudySessionDao(): FocentraStudySessionDao
}

