package com.niloy.domain.service

import com.niloy.domain.model.Category
import com.niloy.domain.model.Task
import com.niloy.domain.model.TaskOccurrence
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class AppClassificationBackup(
    val packageName: String,
    val appName: String,
    val categories: String,
    val qualityRating: String,
    val customProductivityType: String = "NEUTRAL"
)

data class WebsiteClassificationBackup(
    val domain: String,
    val category: String,
    val qualityRating: String,
    val customProductivityType: String,
    val isUserOverride: Boolean = false
)

data class DomainRuleBackup(
    val domainPattern: String,
    val ruleType: String,
    val category: String,
    val qualityRating: String,
    val isEnabled: Boolean = true
)

data class BackupData(
    val version: Int = 4,
    val categories: List<Category>,
    val tasks: List<Task>,
    val occurrences: List<TaskOccurrence>,
    val settings: Map<String, String>,
    val appClassifications: List<AppClassificationBackup>? = null,
    val websiteClassifications: List<WebsiteClassificationBackup>? = null,
    val domainRules: List<DomainRuleBackup>? = null
)

class BackupService {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val adapter = moshi.adapter(BackupData::class.java)

    fun exportBackup(data: BackupData): String {
        return adapter.toJson(data)
    }

    fun importBackup(json: String): BackupData? {
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
