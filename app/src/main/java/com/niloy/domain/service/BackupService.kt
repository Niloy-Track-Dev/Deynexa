package com.niloy.domain.service

import com.niloy.domain.model.Category
import com.niloy.domain.model.Goal
import com.niloy.domain.model.Task
import com.niloy.domain.model.TaskOccurrence
import com.niloy.domain.model.TaskTemplate
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONObject
import java.security.MessageDigest

data class AppClassificationBackup(
    val packageName: String,
    val appName: String,
    val categories: String,
    val qualityRating: String,
    val customProductivityType: String = "NEUTRAL",
    val updatedAt: Long = System.currentTimeMillis()
)

data class AppCategoryBackup(
    val id: Long = 0,
    val name: String,
    val isProductive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class FocentraSessionBackup(
    val sessionId: String,
    val subject: String,
    val topic: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val completionStatus: String,
    val focusScore: Int,
    val schemaVersion: Int = 1,
    val importedAt: Long = System.currentTimeMillis(),
    val source: String = "Focentra",
    val origin: String = "Focentra Integration API v1",
    val storageOwner: String = "Daynexa imported local copy"
)

data class FocentraExportBundle(
    val exportVersion: Int = 1,
    val appVersion: String = "0.8.0",
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "Focentra",
    val origin: String = "Focentra Integration API v1",
    val storageOwner: String = "Daynexa imported local copy",
    val totalSessions: Int = 0,
    val sessions: List<FocentraSessionBackup> = emptyList()
)

data class BackupData(
    val backupVersion: Int = 4,
    val appVersion: String = "0.8.0",
    val createdAt: Long = System.currentTimeMillis(),
    val backupType: String = "FULL_BACKUP",
    val source: String = "Daynexa",
    val format: String = "daynexa-backup",
    val categories: List<Category> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val occurrences: List<TaskOccurrence> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
    val appCategories: List<AppCategoryBackup>? = null,
    val appClassifications: List<AppClassificationBackup>? = null,
    val focentraSessions: List<FocentraSessionBackup>? = null,
    val taskTemplates: List<TaskTemplate>? = null,
    val goals: List<Goal>? = null,
    val checksum: String? = null
)

data class BackupPreviewInfo(
    val backupType: String,
    val appVersion: String,
    val createdAt: Long,
    val tasksCount: Int,
    val categoriesCount: Int,
    val occurrencesCount: Int,
    val taskTemplatesCount: Int,
    val goalsCount: Int,
    val focentraSessionsCount: Int,
    val appClassificationsCount: Int,
    val appCategoriesCount: Int,
    val settingsCount: Int
)

sealed class BackupValidationResult {
    data class ValidFull(val data: BackupData, val preview: BackupPreviewInfo) : BackupValidationResult()
    data class ValidFocentra(val bundle: FocentraExportBundle, val preview: BackupPreviewInfo) : BackupValidationResult()
    data class Invalid(val errorReason: String) : BackupValidationResult()
}

enum class ImportMode {
    REPLACE,
    MERGE
}

class BackupService {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val fullBackupAdapter = moshi.adapter(BackupData::class.java).indent("  ")
    private val focentraBundleAdapter = moshi.adapter(FocentraExportBundle::class.java).indent("  ")

    fun calculateSha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun exportBackup(data: BackupData): String {
        val dataWithChecksum = if (data.checksum.isNullOrBlank()) {
            val contentDigest = "${data.categories.size}-${data.tasks.size}-${data.occurrences.size}-${data.createdAt}"
            data.copy(checksum = calculateSha256(contentDigest))
        } else {
            data
        }
        return fullBackupAdapter.toJson(dataWithChecksum)
    }

    fun exportFocentraBundle(bundle: FocentraExportBundle): String {
        return focentraBundleAdapter.toJson(bundle)
    }

    fun parseBackup(json: String): BackupData? {
        return try {
            fullBackupAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun parseFocentraBundle(json: String): FocentraExportBundle? {
        return try {
            focentraBundleAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun validateBackup(json: String): BackupValidationResult {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) {
            return BackupValidationResult.Invalid("The provided backup file is empty.")
        }

        // Basic JSON sanity check
        val jsonObject = try {
            JSONObject(trimmed)
        } catch (e: Exception) {
            return BackupValidationResult.Invalid("Invalid JSON syntax: ${e.localizedMessage}")
        }

        // Check if it's a dedicated Focentra Export Bundle
        val hasSourceFocentra = jsonObject.optString("source") == "Focentra" || jsonObject.has("sessions")
        if (hasSourceFocentra && !jsonObject.has("tasks") && !jsonObject.has("categories")) {
            val bundle = parseFocentraBundle(trimmed)
                ?: return BackupValidationResult.Invalid("Failed to parse Focentra export bundle structure.")

            val preview = BackupPreviewInfo(
                backupType = "Focentra Focus Export",
                appVersion = bundle.appVersion,
                createdAt = bundle.createdAt,
                tasksCount = 0,
                categoriesCount = 0,
                occurrencesCount = 0,
                taskTemplatesCount = 0,
                goalsCount = 0,
                focentraSessionsCount = bundle.sessions.size,
                appClassificationsCount = 0,
                appCategoriesCount = 0,
                settingsCount = 0
            )
            return BackupValidationResult.ValidFocentra(bundle, preview)
        }

        // Otherwise check as Full Backup (or legacy backup)
        val fullData = parseBackup(trimmed)
        if (fullData != null) {
            val preview = BackupPreviewInfo(
                backupType = "Unified Daynexa Backup",
                appVersion = fullData.appVersion,
                createdAt = fullData.createdAt,
                tasksCount = fullData.tasks.size,
                categoriesCount = fullData.categories.size,
                occurrencesCount = fullData.occurrences.size,
                taskTemplatesCount = fullData.taskTemplates?.size ?: 0,
                goalsCount = fullData.goals?.size ?: 0,
                focentraSessionsCount = fullData.focentraSessions?.size ?: 0,
                appClassificationsCount = fullData.appClassifications?.size ?: 0,
                appCategoriesCount = fullData.appCategories?.size ?: 0,
                settingsCount = fullData.settings.size
            )
            return BackupValidationResult.ValidFull(fullData, preview)
        }

        return BackupValidationResult.Invalid("Unrecognized backup schema. Please ensure the file is a valid Daynexa backup (.daynexa or .json).")
    }
}
