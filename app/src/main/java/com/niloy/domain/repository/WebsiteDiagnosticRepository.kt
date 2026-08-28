package com.niloy.domain.repository

import com.niloy.data.local.entity.DomainRuleEntity
import com.niloy.data.local.entity.WebsiteClassificationEntity
import com.niloy.data.local.entity.WebsiteEventEntity
import com.niloy.domain.model.*
import kotlinx.coroutines.flow.Flow

interface WebsiteDiagnosticRepository {

    // Status & Configuration
    fun isWebsiteDiagnosticEnabled(): Flow<Boolean>
    suspend fun setWebsiteDiagnosticEnabled(enabled: Boolean)

    // Event Ingestion (Debounced & Safe)
    suspend fun recordDomainVisit(domain: String, browserPackage: String = "Browser")
    suspend fun recordDomainVisits(domains: List<String>, browserPackage: String = "Browser")

    // Classifications & Lookups
    fun getAllClassifications(): Flow<List<WebsiteClassificationEntity>>
    suspend fun getClassification(domain: String): DomainClassification
    suspend fun saveClassification(
        domain: String,
        category: String,
        qualityRating: WebsiteQualityRating,
        isUserOverride: Boolean = true
    )
    suspend fun deleteClassification(domain: String)

    // Summary Analytics
    suspend fun getSummary(startTimeMillis: Long, endTimeMillis: Long): WebsiteDiagnosticSummary

    // Rules Management
    fun getAllRules(): Flow<List<DomainRuleEntity>>
    suspend fun addRule(pattern: String, ruleType: DomainRuleType, category: String, rating: WebsiteQualityRating)
    suspend fun deleteRule(id: Long)
    suspend fun toggleRule(id: Long, isEnabled: Boolean)
}
