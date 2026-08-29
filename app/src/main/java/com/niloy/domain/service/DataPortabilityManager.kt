package com.niloy.domain.service

import androidx.room.withTransaction
import com.niloy.data.local.AppDatabase
import com.niloy.data.local.entity.*
import com.niloy.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek

class DataPortabilityManager(
    private val database: AppDatabase,
    private val backupService: BackupService
) {

    suspend fun generateFullBackup(): BackupData = withContext(Dispatchers.IO) {
        val categories = database.categoryDao().getAllCategoriesOnce().map { it.toDomain() }
        val tasks = database.taskDao().getAllTasksOnce().map { it.toDomain() }
        val occurrences = database.taskOccurrenceDao().getAllOccurrencesOnce().map { it.toDomain() }
        val templates = database.taskTemplateDao().getAllTemplatesOnce().map { it.toDomain() }
        val goals = database.goalDao().getAllGoalsOnce().map { it.toDomain() }
        val settings = database.settingDao().getAllSettingsOnce().associate { it.key to it.value }
        
        val appCategories = database.appCategoryDao().getAllCategoriesOneShot().map {
            AppCategoryBackup(
                id = it.id,
                name = it.name,
                isProductive = it.isProductive,
                createdAt = it.createdAt
            )
        }

        val appClassifications = database.appClassificationDao().getAllClassificationsOnce().map {
            AppClassificationBackup(
                packageName = it.packageName,
                appName = it.appName,
                categories = it.categories,
                qualityRating = it.qualityRating,
                customProductivityType = it.customProductivityType,
                updatedAt = it.updatedAt
            )
        }

        val focentraSessions = database.focentraStudySessionDao().getAllSessions().map {
            FocentraSessionBackup(
                sessionId = it.sessionId,
                subject = it.subject,
                topic = it.topic,
                startTime = it.startTime,
                endTime = it.endTime,
                duration = it.duration,
                completionStatus = it.completionStatus,
                focusScore = it.focusScore,
                schemaVersion = it.schemaVersion,
                importedAt = it.importedAt,
                source = "Focentra",
                origin = "Focentra Integration API v1",
                storageOwner = "Daynexa imported local copy"
            )
        }

        BackupData(
            backupVersion = 4,
            appVersion = "0.8.0",
            createdAt = System.currentTimeMillis(),
            backupType = "FULL_BACKUP",
            source = "Daynexa",
            format = "daynexa-backup",
            categories = categories,
            tasks = tasks,
            occurrences = occurrences,
            settings = settings,
            appCategories = appCategories,
            appClassifications = appClassifications,
            focentraSessions = focentraSessions,
            taskTemplates = templates,
            goals = goals
        )
    }

    suspend fun generateFocentraExportBundle(): FocentraExportBundle = withContext(Dispatchers.IO) {
        val sessions = database.focentraStudySessionDao().getAllSessions().map {
            FocentraSessionBackup(
                sessionId = it.sessionId,
                subject = it.subject,
                topic = it.topic,
                startTime = it.startTime,
                endTime = it.endTime,
                duration = it.duration,
                completionStatus = it.completionStatus,
                focusScore = it.focusScore,
                schemaVersion = it.schemaVersion,
                importedAt = it.importedAt,
                source = "Focentra",
                origin = "Focentra Integration API v1",
                storageOwner = "Daynexa imported local copy"
            )
        }

        FocentraExportBundle(
            exportVersion = 1,
            appVersion = "0.7.0",
            createdAt = System.currentTimeMillis(),
            source = "Focentra",
            origin = "Focentra Integration API v1",
            storageOwner = "Daynexa imported local copy",
            totalSessions = sessions.size,
            sessions = sessions
        )
    }

    suspend fun restoreFullBackup(data: BackupData, mode: ImportMode): Boolean = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                if (mode == ImportMode.REPLACE) {
                    database.taskDao().deleteAll()
                    database.taskOccurrenceDao().deleteAll()
                    database.taskTemplateDao().deleteAll()
                    database.goalDao().deleteAll()
                    database.categoryDao().deleteAll()
                    database.appCategoryDao().deleteAll()
                    database.appClassificationDao().deleteAll()
                    database.focentraStudySessionDao().deleteAllSessions()

                    if (data.categories.isNotEmpty()) {
                        database.categoryDao().insertAll(data.categories.map { it.toEntity() })
                    }
                    if (data.tasks.isNotEmpty()) {
                        database.taskDao().insertAll(data.tasks.map { it.toEntity() })
                    }
                    if (data.occurrences.isNotEmpty()) {
                        database.taskOccurrenceDao().insertAll(data.occurrences.map { it.toEntity() })
                    }
                    data.taskTemplates?.let { list ->
                        if (list.isNotEmpty()) {
                            database.taskTemplateDao().insertAll(list.map { it.toEntity() })
                        }
                    }
                    data.goals?.let { list ->
                        if (list.isNotEmpty()) {
                            database.goalDao().insertAll(list.map { it.toEntity() })
                        }
                    }
                    data.appCategories?.let { list ->
                        if (list.isNotEmpty()) {
                            database.appCategoryDao().insertAll(list.map {
                                AppCategoryEntity(id = it.id, name = it.name, isProductive = it.isProductive, createdAt = it.createdAt)
                            })
                        }
                    }
                    data.appClassifications?.let { list ->
                        if (list.isNotEmpty()) {
                            database.appClassificationDao().insertAll(list.map {
                                AppClassificationEntity(
                                    packageName = it.packageName,
                                    appName = it.appName,
                                    categories = it.categories,
                                    qualityRating = it.qualityRating,
                                    customProductivityType = it.customProductivityType,
                                    updatedAt = it.updatedAt
                                )
                            })
                        }
                    }
                    data.focentraSessions?.let { list ->
                        if (list.isNotEmpty()) {
                            database.focentraStudySessionDao().insertSessions(list.map {
                                FocentraStudySessionEntity(
                                    sessionId = it.sessionId,
                                    subject = it.subject,
                                    topic = it.topic,
                                    startTime = it.startTime,
                                    endTime = it.endTime,
                                    duration = it.duration,
                                    completionStatus = it.completionStatus,
                                    focusScore = it.focusScore,
                                    schemaVersion = it.schemaVersion,
                                    importedAt = it.importedAt
                                )
                            })
                        }
                    }
                    data.settings.forEach { (k, v) ->
                        database.settingDao().insert(SettingEntity(k, v))
                    }
                } else {
                    // MERGE Mode
                    // 1. Categories
                    val existingCategories = database.categoryDao().getAllCategoriesOnce()
                    val categoryNameMap = existingCategories.associateBy { it.name.lowercase().trim() }
                    val categoryIdRemap = mutableMapOf<Long, Long>()

                    data.categories.forEach { incomingCat ->
                        val existing = categoryNameMap[incomingCat.name.lowercase().trim()]
                        if (existing != null) {
                            categoryIdRemap[incomingCat.id] = existing.id
                        } else {
                            val newId = database.categoryDao().insert(incomingCat.toEntity().copy(id = 0))
                            categoryIdRemap[incomingCat.id] = newId
                        }
                    }

                    // 2. Tasks
                    val existingTasks = database.taskDao().getAllTasksOnce()
                    val existingTaskKeyMap = existingTasks.associateBy { "${it.name.lowercase().trim()}_${it.categoryId}" }
                    val taskIdRemap = mutableMapOf<Long, Long>()

                    data.tasks.forEach { incomingTask ->
                        val remappedCatId = categoryIdRemap[incomingTask.categoryId] ?: incomingTask.categoryId
                        val key = "${incomingTask.name.lowercase().trim()}_$remappedCatId"
                        val existing = existingTaskKeyMap[key]
                        if (existing != null) {
                            taskIdRemap[incomingTask.id] = existing.id
                            if (incomingTask.updatedAt > existing.updatedAt) {
                                database.taskDao().update(
                                    incomingTask.toEntity().copy(id = existing.id, categoryId = remappedCatId)
                                )
                            }
                        } else {
                            val newId = database.taskDao().insert(
                                incomingTask.toEntity().copy(id = 0, categoryId = remappedCatId)
                            )
                            taskIdRemap[incomingTask.id] = newId
                        }
                    }

                    // 3. Occurrences
                    data.occurrences.forEach { incomingOcc ->
                        val mappedTaskId = taskIdRemap[incomingOcc.taskId] ?: incomingOcc.taskId
                        val existingOcc = database.taskOccurrenceDao().getByTaskIdAndDate(mappedTaskId, incomingOcc.date)
                        if (existingOcc != null) {
                            if (incomingOcc.updatedAt >= existingOcc.updatedAt) {
                                database.taskOccurrenceDao().update(
                                    incomingOcc.toEntity().copy(id = existingOcc.id, taskId = mappedTaskId)
                                )
                            }
                        } else {
                            database.taskOccurrenceDao().insert(
                                incomingOcc.toEntity().copy(id = 0, taskId = mappedTaskId)
                            )
                        }
                    }

                    // 4. Task Templates
                    data.taskTemplates?.forEach { incomingTmpl ->
                        val remappedCatId = categoryIdRemap[incomingTmpl.categoryId] ?: incomingTmpl.categoryId
                        database.taskTemplateDao().insert(
                            incomingTmpl.toEntity().copy(id = 0, categoryId = remappedCatId)
                        )
                    }

                    // 5. Goals
                    data.goals?.forEach { incomingGoal ->
                        val remappedCatId = incomingGoal.categoryId?.let { categoryIdRemap[it] ?: it }
                        database.goalDao().insert(
                            incomingGoal.toEntity().copy(id = 0, categoryId = remappedCatId)
                        )
                    }

                    // 6. App Categories
                    data.appCategories?.forEach { incomingAppCat ->
                        val existing = database.appCategoryDao().getByName(incomingAppCat.name)
                        if (existing == null) {
                            database.appCategoryDao().insert(
                                AppCategoryEntity(
                                    id = 0,
                                    name = incomingAppCat.name,
                                    isProductive = incomingAppCat.isProductive,
                                    createdAt = incomingAppCat.createdAt
                                )
                            )
                        }
                    }

                    // 7. App Classifications
                    data.appClassifications?.forEach { incomingClass ->
                        database.appClassificationDao().insertOrUpdate(
                            AppClassificationEntity(
                                packageName = incomingClass.packageName,
                                appName = incomingClass.appName,
                                categories = incomingClass.categories,
                                qualityRating = incomingClass.qualityRating,
                                customProductivityType = incomingClass.customProductivityType,
                                updatedAt = incomingClass.updatedAt
                            )
                        )
                    }

                    // 8. Focentra Sessions (Deduplicated by sessionId)
                    data.focentraSessions?.forEach { incomingSession ->
                        database.focentraStudySessionDao().insertSession(
                            FocentraStudySessionEntity(
                                sessionId = incomingSession.sessionId,
                                subject = incomingSession.subject,
                                topic = incomingSession.topic,
                                startTime = incomingSession.startTime,
                                endTime = incomingSession.endTime,
                                duration = incomingSession.duration,
                                completionStatus = incomingSession.completionStatus,
                                focusScore = incomingSession.focusScore,
                                schemaVersion = incomingSession.schemaVersion,
                                importedAt = incomingSession.importedAt
                            )
                        )
                    }

                    // 9. Settings
                    data.settings.forEach { (k, v) ->
                        val existing = database.settingDao().getByKey(k)
                        if (existing == null) {
                            database.settingDao().insert(SettingEntity(k, v))
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreFocentraBackup(bundle: FocentraExportBundle, mode: ImportMode): Boolean = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                if (mode == ImportMode.REPLACE) {
                    database.focentraStudySessionDao().deleteAllSessions()
                }
                bundle.sessions.forEach { session ->
                    database.focentraStudySessionDao().insertSession(
                        FocentraStudySessionEntity(
                            sessionId = session.sessionId,
                            subject = session.subject,
                            topic = session.topic,
                            startTime = session.startTime,
                            endTime = session.endTime,
                            duration = session.duration,
                            completionStatus = session.completionStatus,
                            focusScore = session.focusScore,
                            schemaVersion = session.schemaVersion,
                            importedAt = session.importedAt
                        )
                    )
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parseRecurringDays(str: String): Set<DayOfWeek> {
        if (str.isBlank()) return emptySet()
        return str.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull {
                try { DayOfWeek.valueOf(it) } catch (e: Exception) { null }
            }
            .toSet()
    }

    // Mappers
    private fun CategoryEntity.toDomain() = Category(id = id, name = name, icon = icon, color = color)
    private fun Category.toEntity() = CategoryEntity(id = id, name = name, icon = icon, color = color)

    private fun TaskEntity.toDomain() = Task(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        isRecurring = isRecurring,
        recurringDays = parseRecurringDays(recurringDays),
        recurrenceType = try { RecurrenceType.valueOf(recurrenceType) } catch (e: Exception) { RecurrenceType.DAILY },
        recurrenceInterval = recurrenceInterval,
        recurrenceDayOfMonth = recurrenceDayOfMonth,
        recurrenceMonthOfYear = recurrenceMonthOfYear,
        recurrenceEndType = try { RecurrenceEndType.valueOf(recurrenceEndType) } catch (e: Exception) { RecurrenceEndType.NEVER },
        recurrenceEndDate = recurrenceEndDate,
        recurrenceCount = recurrenceCount,
        seriesId = seriesId,
        startDate = startDate,
        isEnabled = isEnabled,
        reminderEnabled = reminderEnabled,
        reminderOffsetMinutes = reminderOffsetMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Task.toEntity() = TaskEntity(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        isRecurring = isRecurring,
        recurringDays = recurringDays.joinToString(",") { it.name },
        recurrenceType = recurrenceType.name,
        recurrenceInterval = recurrenceInterval,
        recurrenceDayOfMonth = recurrenceDayOfMonth,
        recurrenceMonthOfYear = recurrenceMonthOfYear,
        recurrenceEndType = recurrenceEndType.name,
        recurrenceEndDate = recurrenceEndDate,
        recurrenceCount = recurrenceCount,
        seriesId = seriesId,
        startDate = startDate,
        isEnabled = isEnabled,
        reminderEnabled = reminderEnabled,
        reminderOffsetMinutes = reminderOffsetMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun TaskOccurrenceEntity.toDomain() = TaskOccurrence(
        taskId = taskId,
        date = date,
        state = try { TaskState.valueOf(state) } catch (e: Exception) { TaskState.PENDING },
        rescheduledStartTime = rescheduledStartTime,
        rescheduledEndTime = rescheduledEndTime,
        rescheduledDate = rescheduledDate,
        isException = isException,
        notes = notes,
        updatedAt = updatedAt
    )

    private fun TaskOccurrence.toEntity() = TaskOccurrenceEntity(
        taskId = taskId,
        date = date,
        state = state.name,
        rescheduledStartTime = rescheduledStartTime,
        rescheduledEndTime = rescheduledEndTime,
        rescheduledDate = rescheduledDate,
        isException = isException,
        notes = notes,
        updatedAt = updatedAt
    )

    private fun TaskTemplateEntity.toDomain() = TaskTemplate(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        defaultDurationMinutes = defaultDurationMinutes,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        recurrenceType = try { RecurrenceType.valueOf(recurrenceType) } catch (e: Exception) { RecurrenceType.DAILY },
        recurringDays = parseRecurringDays(recurringDays),
        recurrenceInterval = recurrenceInterval,
        reminderEnabled = reminderEnabled,
        reminderOffsetMinutes = reminderOffsetMinutes,
        createdAt = createdAt
    )

    private fun TaskTemplate.toEntity() = TaskTemplateEntity(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        defaultDurationMinutes = defaultDurationMinutes,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        recurrenceType = recurrenceType.name,
        recurringDays = recurringDays.joinToString(",") { it.name },
        recurrenceInterval = recurrenceInterval,
        reminderEnabled = reminderEnabled,
        reminderOffsetMinutes = reminderOffsetMinutes,
        createdAt = createdAt
    )

    private fun GoalEntity.toDomain() = Goal(
        id = id,
        title = title,
        targetType = try { GoalType.valueOf(targetType) } catch (e: Exception) { GoalType.TASKS_COMPLETED },
        targetPeriod = try { GoalPeriod.valueOf(targetPeriod) } catch (e: Exception) { GoalPeriod.DAILY },
        targetValue = targetValue,
        unit = unit,
        categoryId = categoryId,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastCompletedPeriod = lastCompletedPeriod,
        isActive = isActive,
        createdAt = createdAt
    )

    private fun Goal.toEntity() = GoalEntity(
        id = id,
        title = title,
        targetType = targetType.name,
        targetPeriod = targetPeriod.name,
        targetValue = targetValue,
        unit = unit,
        categoryId = categoryId,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastCompletedPeriod = lastCompletedPeriod,
        isActive = isActive,
        createdAt = createdAt
    )
}
