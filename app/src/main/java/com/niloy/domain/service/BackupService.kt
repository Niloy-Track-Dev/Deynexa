package com.niloy.domain.service

import com.niloy.domain.model.Category
import com.niloy.domain.model.Task
import com.niloy.domain.model.TaskOccurrence
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class BackupData(
    val version: Int = 1,
    val categories: List<Category>,
    val tasks: List<Task>,
    val occurrences: List<TaskOccurrence>,
    val settings: Map<String, String>
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
