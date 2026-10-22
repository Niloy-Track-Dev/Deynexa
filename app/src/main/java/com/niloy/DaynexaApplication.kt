package com.niloy

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.niloy.data.local.AppDatabase
import com.niloy.data.repository.DiagnosticRepositoryImpl
import com.niloy.data.repository.TaskRepositoryImpl
import com.niloy.domain.repository.DiagnosticRepository
import com.niloy.domain.repository.TaskRepository
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
    lateinit var focentraIntegrationManager: FocentraIntegrationManager
    lateinit var schedulingService: SchedulingService
    lateinit var backupService: BackupService
    lateinit var dataPortabilityManager: com.niloy.domain.service.DataPortabilityManager
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
            // Deprecated
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

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `website_classifications`")
            db.execSQL("DROP TABLE IF EXISTS `website_events`")
            db.execSQL("DROP TABLE IF EXISTS `domain_rules`")
            db.execSQL("ALTER TABLE `categories` ADD COLUMN `isProductive` INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `app_categories` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `isProductive` INTEGER NOT NULL DEFAULT 1,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceType` TEXT NOT NULL DEFAULT 'DAILY'")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceInterval` INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceDayOfMonth` INTEGER")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceMonthOfYear` INTEGER")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceEndType` TEXT NOT NULL DEFAULT 'NEVER'")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceEndDate` TEXT")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceCount` INTEGER")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `seriesId` INTEGER")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `startDate` TEXT NOT NULL DEFAULT ''")

            db.execSQL("ALTER TABLE `task_occurrences` ADD COLUMN `rescheduledStartTime` INTEGER")
            db.execSQL("ALTER TABLE `task_occurrences` ADD COLUMN `rescheduledEndTime` INTEGER")
            db.execSQL("ALTER TABLE `task_occurrences` ADD COLUMN `rescheduledDate` TEXT")
            db.execSQL("ALTER TABLE `task_occurrences` ADD COLUMN `isException` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `task_occurrences` ADD COLUMN `notes` TEXT")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `task_templates` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT NOT NULL DEFAULT '',
                    `categoryId` INTEGER NOT NULL,
                    `defaultDurationMinutes` INTEGER NOT NULL DEFAULT 45,
                    `startTime` INTEGER,
                    `endTime` INTEGER,
                    `isAllDay` INTEGER NOT NULL DEFAULT 0,
                    `recurrenceType` TEXT NOT NULL DEFAULT 'DAILY',
                    `recurringDays` TEXT NOT NULL DEFAULT '',
                    `recurrenceInterval` INTEGER NOT NULL DEFAULT 1,
                    `reminderEnabled` INTEGER NOT NULL DEFAULT 0,
                    `reminderOffsetMinutes` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'MEDIUM'")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `deadlineDate` TEXT")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `deadlineTime` INTEGER")
            db.execSQL("ALTER TABLE `task_templates` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'MEDIUM'")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `goals` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `targetType` TEXT NOT NULL,
                    `targetPeriod` TEXT NOT NULL,
                    `targetValue` INTEGER NOT NULL,
                    `unit` TEXT NOT NULL DEFAULT 'tasks',
                    `categoryId` INTEGER,
                    `currentStreak` INTEGER NOT NULL DEFAULT 0,
                    `longestStreak` INTEGER NOT NULL DEFAULT 0,
                    `lastCompletedPeriod` TEXT,
                    `isActive` INTEGER NOT NULL DEFAULT 1,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_categoryId` ON `tasks` (`categoryId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_occurrences_date_state` ON `task_occurrences` (`date`, `state`)")
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "daynexa.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .fallbackToDestructiveMigration()
            .build()

        repository = TaskRepositoryImpl(
            database.categoryDao(),
            database.taskDao(),
            database.taskOccurrenceDao(),
            database.settingDao(),
            database.taskTemplateDao(),
            database.goalDao()
        )

        diagnosticRepository = DiagnosticRepositoryImpl(
            applicationContext,
            database.appClassificationDao(),
            database.focentraStudySessionDao(),
            database.appCategoryDao()
        )

        focentraIntegrationManager = FocentraIntegrationManager(
            applicationContext,
            database.focentraStudySessionDao(),
            database.settingDao()
        )

        schedulingService = SchedulingService()
        backupService = BackupService()
        dataPortabilityManager = com.niloy.domain.service.DataPortabilityManager(database, backupService)
        reminderScheduler = TaskReminderScheduler(applicationContext)

        initDemoData()
    }

    private fun initDemoData() {
        kotlinx.coroutines.MainScope().launch {
            try {
                val categories = repository.getCategories().first()
                if (categories.isEmpty()) {
                    val studyId = repository.saveCategory(com.niloy.domain.model.Category(name = "Study", icon = "book", color = 0xFF3B82F6.toInt()))
                    val mealId = repository.saveCategory(com.niloy.domain.model.Category(name = "Meal", icon = "restaurant", color = 0xFFF59E0B.toInt()))
                    val personalCareId = repository.saveCategory(com.niloy.domain.model.Category(name = "Personal Care", icon = "person", color = 0xFF10B981.toInt()))
                    val prayerId = repository.saveCategory(com.niloy.domain.model.Category(name = "Prayer", icon = "favorite", color = 0xFF8B5CF6.toInt()))
                    val breakId = repository.saveCategory(com.niloy.domain.model.Category(name = "Break", icon = "timer", color = 0xFF06B6D4.toInt()))
                    val napId = repository.saveCategory(com.niloy.domain.model.Category(name = "Nap", icon = "bed", color = 0xFF6366F1.toInt()))
                    val othersId = repository.saveCategory(com.niloy.domain.model.Category(name = "Others", icon = "more_horiz", color = 0xFF14B8A6.toInt()))

                    // Daily "ঘুম থেকে উঠা" task at 07:00 AM
                    repository.saveTask(com.niloy.domain.model.Task(
                        name = "ঘুম থেকে উঠা",
                        categoryId = personalCareId,
                        startTime = 420L, // 07:00 AM
                        endTime = 420L,   // 07:00 AM
                        isRecurring = true,
                        recurringDays = java.time.DayOfWeek.values().toSet()
                    ))

                    val sat = java.time.DayOfWeek.SATURDAY
                    val sun = java.time.DayOfWeek.SUNDAY
                    val mon = java.time.DayOfWeek.MONDAY
                    val tue = java.time.DayOfWeek.TUESDAY
                    val wed = java.time.DayOfWeek.WEDNESDAY
                    val thu = java.time.DayOfWeek.THURSDAY
                    val fri = java.time.DayOfWeek.FRIDAY

                    data class SeedTask(val name: String, val categoryId: Long, val start: Long, val end: Long, val days: Set<java.time.DayOfWeek>)

                    val seedTasks = listOf(
                        // Saturday & Thursday
                        SeedTask("ঘুম থেকে উঠে রেডি হওয়া ও নাস্তা", personalCareId, 420L, 450L, setOf(sat, thu)),
                        SeedTask("কলেজ (যাওয়া-আসা ও ক্লাস)", othersId, 450L, 870L, setOf(sat, thu)),
                        SeedTask("Fresh হওয়া", personalCareId, 870L, 880L, setOf(sat, thu)),
                        SeedTask("দুপুরের ভাত", mealId, 880L, 900L, setOf(sat, thu)),
                        SeedTask("ঘুম", napId, 900L, 990L, setOf(sat, thu)),
                        SeedTask("প্রার্থনা", prayerId, 990L, 1020L, setOf(sat, thu)),
                        SeedTask("নাস্তা", mealId, 1020L, 1050L, setOf(sat, thu)),
                        SeedTask("Physics পড়া", studyId, 1050L, 1170L, setOf(sat, thu)),
                        SeedTask("Chemistry পড়া", studyId, 1170L, 1260L, setOf(sat, thu)),
                        SeedTask("রাতের ভাত", mealId, 1260L, 1280L, setOf(sat, thu)),
                        SeedTask("বিরতি", breakId, 1280L, 1290L, setOf(sat, thu)),
                        SeedTask("Bangla 1st পড়া", studyId, 1290L, 1380L, setOf(sat, thu)),
                        SeedTask("Chemistry পড়া", studyId, 1380L, 1480L, setOf(sat, thu)),

                        // Sunday & Friday
                        SeedTask("নাস্তা ও Fresh Air", mealId, 420L, 450L, setOf(sun, fri)),
                        SeedTask("হাঁটাহাঁটি", othersId, 450L, 460L, setOf(sun, fri)),
                        SeedTask("ওয়াশরুম ও স্নান", personalCareId, 460L, 490L, setOf(sun, fri)),
                        SeedTask("প্রার্থনা", prayerId, 490L, 520L, setOf(sun, fri)),
                        SeedTask("Higher Math পড়া", studyId, 520L, 670L, setOf(sun, fri)),
                        SeedTask("Biology পড়া", studyId, 670L, 750L, setOf(sun, fri)),
                        SeedTask("ছোট বিরতি", breakId, 750L, 760L, setOf(sun, fri)),
                        SeedTask("দুপুরের ভাত", mealId, 760L, 790L, setOf(sun, fri)),
                        SeedTask("কোচিংয়ের জন্য রেডি হওয়া", personalCareId, 790L, 800L, setOf(sun, fri)),
                        SeedTask("কোচিং (যাওয়া-আসা সহ)", othersId, 800L, 945L, setOf(sun, fri)),
                        SeedTask("মোবাইল", othersId, 945L, 975L, setOf(sun, fri)),
                        SeedTask("বিশ্রাম", othersId, 975L, 1005L, setOf(sun, fri)),
                        SeedTask("প্রার্থনা", prayerId, 1005L, 1025L, setOf(sun, fri)),
                        SeedTask("নাস্তা", mealId, 1025L, 1050L, setOf(sun, fri)),
                        SeedTask("Higher Math পড়া", studyId, 1050L, 1170L, setOf(sun, fri)),
                        SeedTask("English 2nd পড়া", studyId, 1170L, 1260L, setOf(sun, fri)),
                        SeedTask("রাতের ভাত", mealId, 1260L, 1280L, setOf(sun, fri)),
                        SeedTask("বিরতি", breakId, 1280L, 1290L, setOf(sun, fri)),
                        SeedTask("Biology পড়া", studyId, 1290L, 1410L, setOf(sun, fri)),

                        // Monday
                        SeedTask("নাস্তা ও Fresh Air", mealId, 420L, 450L, setOf(mon)),
                        SeedTask("হাঁটাহাঁটি", othersId, 450L, 460L, setOf(mon)),
                        SeedTask("ওয়াশরুম ও স্নান", personalCareId, 460L, 490L, setOf(mon)),
                        SeedTask("প্রার্থনা", prayerId, 490L, 520L, setOf(mon)),
                        SeedTask("Physics পড়া", studyId, 520L, 660L, setOf(mon)),
                        SeedTask("Biology পড়া", studyId, 660L, 750L, setOf(mon)),
                        SeedTask("ছোট বিরতি", breakId, 750L, 760L, setOf(mon)),
                        SeedTask("দুপুরের ভাত", mealId, 760L, 790L, setOf(mon)),
                        SeedTask("বিরতি", breakId, 790L, 800L, setOf(mon)),
                        SeedTask("ঘুম", napId, 800L, 890L, setOf(mon)),
                        SeedTask("ঘুম থেকে উঠে Fresh হওয়া", personalCareId, 890L, 900L, setOf(mon)),
                        SeedTask("English 1st পড়া", studyId, 900L, 975L, setOf(mon)),
                        SeedTask("বাজারে যাওয়ার প্রস্তুতি", othersId, 975L, 980L, setOf(mon)),
                        SeedTask("বাজার করা", othersId, 980L, 1005L, setOf(mon)),
                        SeedTask("Fresh হওয়া", personalCareId, 1005L, 1015L, setOf(mon)),
                        SeedTask("মোবাইল ও বিশ্রাম", othersId, 1015L, 1030L, setOf(mon)),
                        SeedTask("প্রার্থনা", prayerId, 1030L, 1040L, setOf(mon)),
                        SeedTask("নাস্তা", mealId, 1040L, 1050L, setOf(mon)),
                        SeedTask("Physics পড়া", studyId, 1050L, 1170L, setOf(mon)),
                        SeedTask("Biology পড়া", studyId, 1170L, 1260L, setOf(mon)),
                        SeedTask("রাতের ভাত", mealId, 1260L, 1280L, setOf(mon)),
                        SeedTask("বিরতি", breakId, 1280L, 1290L, setOf(mon)),
                        SeedTask("English 1st পড়া", studyId, 1290L, 1350L, setOf(mon)),
                        SeedTask("Physics পড়া", studyId, 1350L, 1410L, setOf(mon)),

                        // Tuesday
                        SeedTask("নাস্তা ও Fresh Air", mealId, 420L, 450L, setOf(tue)),
                        SeedTask("হাঁটাহাঁটি", othersId, 450L, 460L, setOf(tue)),
                        SeedTask("ওয়াশরুম ও স্নান", personalCareId, 460L, 490L, setOf(tue)),
                        SeedTask("প্রার্থনা", prayerId, 490L, 520L, setOf(tue)),
                        SeedTask("Physics পড়া", studyId, 520L, 660L, setOf(tue)),
                        SeedTask("Higher Math পড়া", studyId, 660L, 750L, setOf(tue)),
                        SeedTask("ছোট বিরতি", breakId, 750L, 760L, setOf(tue)),
                        SeedTask("দুপুরের ভাত", mealId, 760L, 790L, setOf(tue)),
                        SeedTask("বিরতি", breakId, 790L, 800L, setOf(tue)),
                        SeedTask("ঘুম", napId, 800L, 890L, setOf(tue)),
                        SeedTask("ঘুম থেকে উঠে Fresh হওয়া", personalCareId, 890L, 900L, setOf(tue)),
                        SeedTask("English 1st পড়া", studyId, 900L, 1000L, setOf(tue)),
                        SeedTask("প্রার্থনা", prayerId, 1000L, 1025L, setOf(tue)),
                        SeedTask("নাস্তা", mealId, 1025L, 1050L, setOf(tue)),
                        SeedTask("Higher Math পড়া", studyId, 1050L, 1170L, setOf(tue)),
                        SeedTask("Physics পড়া", studyId, 1170L, 1260L, setOf(tue)),
                        SeedTask("রাতের ভাত", mealId, 1260L, 1280L, setOf(tue)),
                        SeedTask("বিরতি", breakId, 1280L, 1290L, setOf(tue)),
                        SeedTask("Physics পড়া", studyId, 1290L, 1350L, setOf(tue)),
                        SeedTask("English 1st পড়া", studyId, 1350L, 1410L, setOf(tue)),

                        // Wednesday
                        SeedTask("নাস্তা ও Fresh Air", mealId, 420L, 450L, setOf(wed)),
                        SeedTask("হাঁটাহাঁটি", othersId, 450L, 460L, setOf(wed)),
                        SeedTask("ওয়াশরুম ও স্নান", personalCareId, 460L, 490L, setOf(wed)),
                        SeedTask("প্রার্থনা", prayerId, 490L, 520L, setOf(wed)),
                        SeedTask("Chemistry পড়া", studyId, 520L, 650L, setOf(wed)),
                        SeedTask("Biology পড়া", studyId, 650L, 750L, setOf(wed)),
                        SeedTask("ছোট বিরতি", breakId, 750L, 760L, setOf(wed)),
                        SeedTask("দুপুরের ভাত", mealId, 760L, 790L, setOf(wed)),
                        SeedTask("কোচিংয়ের জন্য রেডি হওয়া", personalCareId, 790L, 800L, setOf(wed)),
                        SeedTask("কোচিং (যাওয়া-আসা সহ)", othersId, 800L, 945L, setOf(wed)),
                        SeedTask("মোবাইল ও বিশ্রাম", othersId, 945L, 990L, setOf(wed)),
                        SeedTask("প্রার্থনা", prayerId, 990L, 1020L, setOf(wed)),
                        SeedTask("নাস্তা", mealId, 1020L, 1050L, setOf(wed)),
                        SeedTask("দোকানে গিয়ে বাজার করা", othersId, 1050L, 1120L, setOf(wed)),
                        SeedTask("Chemistry পড়া", studyId, 1120L, 1210L, setOf(wed)),
                        SeedTask("ICT পড়া", studyId, 1210L, 1260L, setOf(wed)),
                        SeedTask("রাতের ভাত", mealId, 1260L, 1280L, setOf(wed)),
                        SeedTask("বিরতি", breakId, 1280L, 1290L, setOf(wed)),
                        SeedTask("Biology পড়া", studyId, 1290L, 1350L, setOf(wed)),
                        SeedTask("ICT পড়া", studyId, 1350L, 1410L, setOf(wed))
                    )

                    seedTasks.forEach { task ->
                        repository.saveTask(com.niloy.domain.model.Task(
                            name = task.name,
                            categoryId = task.categoryId,
                            startTime = task.start,
                            endTime = task.end,
                            isRecurring = true,
                            recurringDays = task.days
                        ))
                    }
                }
            } catch (e: Exception) {
                // Ignore seed errors on launch
            }
        }
    }
}
