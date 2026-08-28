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

data class BackupData(
    val version: Int = 2,
    val categories: List<Category>,
    val tasks: List<Task>,
    val occurrences: List<TaskOccurrence>,
    val settings: Map<String, String>,
    val appClassifications: List<AppClassificationBackup>? = null
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
