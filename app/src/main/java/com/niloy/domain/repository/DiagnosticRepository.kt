package com.niloy.domain.repository

import com.niloy.data.local.entity.AppClassificationEntity
import com.niloy.domain.model.AppQualityRating
import com.niloy.domain.model.DiagnosticSummary
import com.niloy.domain.model.InstalledAppInfo
import kotlinx.coroutines.flow.Flow

interface DiagnosticRepository {
    fun isUsagePermissionGranted(): Boolean
    suspend fun getUsageSummary(startTimeMillis: Long, endTimeMillis: Long): DiagnosticSummary
    fun getAllClassifications(): Flow<List<AppClassificationEntity>>
    suspend fun saveClassification(
        packageName: String,
        appName: String,
        categories: List<String>,
        qualityRating: AppQualityRating
    )
    suspend fun clearDiagnosticData()
    suspend fun getInstalledApps(): List<InstalledAppInfo>
    
    // App Categories
    fun getAppCategories(): Flow<List<com.niloy.domain.model.AppCategory>>
    suspend fun saveAppCategory(category: com.niloy.domain.model.AppCategory)
    suspend fun deleteAppCategory(category: com.niloy.domain.model.AppCategory)
}
