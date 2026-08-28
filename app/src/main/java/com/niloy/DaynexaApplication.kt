package com.niloy

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.niloy.data.local.AppDatabase
import com.niloy.data.repository.DiagnosticRepositoryImpl
import com.niloy.data.repository.TaskRepositoryImpl
import com.niloy.data.repository.WebsiteDiagnosticRepositoryImpl
import com.niloy.domain.repository.DiagnosticRepository
import com.niloy.domain.repository.TaskRepository
import com.niloy.domain.repository.WebsiteDiagnosticRepository
import com.niloy.domain.service.BackupService
import com.niloy.domain.service.FocentraIntegrationManager
import com.niloy.domain.service.SchedulingService
import com.niloy.domain.service.TaskReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DaynexaApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: TaskRepository
    lateinit var diagnosticRepository: DiagnosticRepository
    lateinit var websiteDiagnosticRepository: WebsiteDiagnosticRepository
    lateinit var focentraIntegrationManager: FocentraIntegrationManager
    lateinit var schedulingService: SchedulingService
    lateinit var backupService: BackupService
    lateinit var reminderScheduler: TaskReminderScheduler

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `app_classifications` (
                    `packageName` TEXT NOT NULL,
                    `appName` TEXT NOT NULL,
                    `categories` TEXT NOT NULL,
                    `qualityRating` TEXT NOT NULL,
                    `customProductivityType` TEXT NOT NULL DEFAULT 'NEUTRAL',
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`packageName`)
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderEnabled` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderOffsetMinutes` INTEGER")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `website_classifications` (
                    `domain` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `qualityRating` TEXT NOT NULL,
                    `customProductivityType` TEXT NOT NULL,
                    `isUserOverride` INTEGER NOT NULL DEFAULT 0,
                    `visitCount` INTEGER NOT NULL DEFAULT 0,
                    `totalDurationMillis` INTEGER NOT NULL DEFAULT 0,
                    `firstDetected` INTEGER NOT NULL DEFAULT 0,
                    `lastDetected` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`domain`)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `website_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `domain` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `estimatedDurationMillis` INTEGER NOT NULL DEFAULT 30000,
                    `browserPackage` TEXT NOT NULL DEFAULT 'Browser',
                    `productivityType` TEXT NOT NULL DEFAULT 'NEUTRAL'
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_website_events_timestamp` ON `website_events` (`timestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_website_events_domain` ON `website_events` (`domain`)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `domain_rules` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `domainPattern` TEXT NOT NULL,
                    `ruleType` TEXT NOT NULL DEFAULT 'CLASSIFY',
                    `category` TEXT NOT NULL DEFAULT 'Unknown',
                    `qualityRating` TEXT NOT NULL DEFAULT 'UNRATED',
                    `isEnabled` INTEGER NOT NULL DEFAULT 1,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `focentra_study_sessions` (
                    `sessionId` TEXT NOT NULL,
                    `subject` TEXT NOT NULL,
                    `topic` TEXT NOT NULL,
                    `startTime` INTEGER NOT NULL,
                    `endTime` INTEGER NOT NULL,
                    `duration` INTEGER NOT NULL,
                    `completionStatus` TEXT NOT NULL,
                    `focusScore` INTEGER NOT NULL,
                    `schemaVersion` INTEGER NOT NULL DEFAULT 1,
                    `importedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`sessionId`)
                )
            """.trimIndent())
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "daynexa.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()

        repository = TaskRepositoryImpl(
            database.categoryDao(),
            database.taskDao(),
            database.taskOccurrenceDao(),
            database.settingDao()
        )
        
        diagnosticRepository = DiagnosticRepositoryImpl(
            applicationContext,
            database.appClassificationDao(),
            database.focentraStudySessionDao()
        )

        websiteDiagnosticRepository = WebsiteDiagnosticRepositoryImpl(
            applicationContext,
            database.websiteDiagnosticDao(),
            database.domainRuleDao(),
            database.settingDao()
        )

        focentraIntegrationManager = FocentraIntegrationManager(
            applicationContext,
            database.focentraStudySessionDao(),
            database.settingDao()
        )
        
        schedulingService = SchedulingService()
        backupService = BackupService()
        reminderScheduler = TaskReminderScheduler(applicationContext)
        
        initDemoData()
    }

    private fun initDemoData() {
        kotlinx.coroutines.MainScope().launch {
            try {
                val categories = repository.getCategories().first()
                if (categories.isEmpty()) {
                    val healthId = repository.saveCategory(com.niloy.domain.model.Category(name = "Health", icon = "favorite", color = 0xFFE91E63.toInt()))
                    val workId = repository.saveCategory(com.niloy.domain.model.Category(name = "Work", icon = "work", color = 0xFF2196F3.toInt()))
                    val personalId = repository.saveCategory(com.niloy.domain.model.Category(name = "Personal", icon = "person", color = 0xFF4CAF50.toInt()))

                    repository.saveTask(com.niloy.domain.model.Task(
                        name = "Wake Up",
                        categoryId = personalId,
                        startTime = 420, // 07:00
                        endTime = 420,
                        isRecurring = true,
                        recurringDays = java.time.DayOfWeek.values().toSet()
                    ))
                    repository.saveTask(com.niloy.domain.model.Task(
                        name = "Exercise",
                        categoryId = healthId,
                        startTime = 420, // 07:00
                        endTime = 465, // 07:45
                        isRecurring = true,
                        recurringDays = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY)
                    ))
                }
            } catch (e: Exception) {
                // Ignore seed errors on launch
            }
        }
    }
}
