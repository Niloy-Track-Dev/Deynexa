package com.niloy.data.repository

import android.content.Context
import com.niloy.data.local.dao.DomainRuleDao
import com.niloy.data.local.dao.SettingDao
import com.niloy.data.local.dao.WebsiteDiagnosticDao
import com.niloy.data.local.entity.DomainRuleEntity
import com.niloy.data.local.entity.SettingEntity
import com.niloy.data.local.entity.WebsiteClassificationEntity
import com.niloy.data.local.entity.WebsiteEventEntity
import com.niloy.domain.model.*
import com.niloy.domain.repository.WebsiteDiagnosticRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

class WebsiteDiagnosticRepositoryImpl(
    private val context: Context,
    private val websiteDao: WebsiteDiagnosticDao,
    private val domainRuleDao: DomainRuleDao,
    private val settingDao: SettingDao
) : WebsiteDiagnosticRepository {

    companion object {
        private const val SETTING_WEBSITE_DIAGNOSTIC_ENABLED = "setting_website_diagnostic_enabled"
        private const val DEBOUNCE_WINDOW_MILLIS = 20_000L // 20 seconds
        private const val DEFAULT_VISIT_DURATION_MILLIS = 30_000L // 30 seconds
    }

    // In-memory cache of last recorded timestamp per domain for debouncing
    private val lastVisitTimeMap = ConcurrentHashMap<String, Long>()

    override fun isWebsiteDiagnosticEnabled(): Flow<Boolean> {
        return settingDao.getAllSettings().map { settings ->
            settings.find { it.key == SETTING_WEBSITE_DIAGNOSTIC_ENABLED }?.value?.toBoolean() ?: false
        }
    }

    override suspend fun setWebsiteDiagnosticEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            settingDao.insert(
                SettingEntity(
                    key = SETTING_WEBSITE_DIAGNOSTIC_ENABLED,
                    value = enabled.toString()
                )
            )
        }
    }

    override suspend fun recordDomainVisit(domain: String, browserPackage: String) {
        withContext(Dispatchers.IO) {
            val cleanDomain = KnownDomainsDictionary.normalizeDomain(domain)
            if (!isValidDomain(cleanDomain)) return@withContext

            val now = System.currentTimeMillis()
            val lastRecorded = lastVisitTimeMap[cleanDomain] ?: 0L

            // Debounce check
            if (now - lastRecorded < DEBOUNCE_WINDOW_MILLIS) {
                return@withContext
            }
            lastVisitTimeMap[cleanDomain] = now

            val classification = resolveClassification(cleanDomain)
            val event = WebsiteEventEntity(
                domain = cleanDomain,
                timestamp = now,
                estimatedDurationMillis = DEFAULT_VISIT_DURATION_MILLIS,
                browserPackage = browserPackage,
                productivityType = classification.productivityType.name
            )
            websiteDao.insertEvent(event)

            // Update aggregated classification statistics
            val existing = websiteDao.getClassification(cleanDomain)
            val firstDet = if (existing != null && existing.firstDetected > 0L) existing.firstDetected else now
            val visitCnt = (existing?.visitCount ?: 0L) + 1L
            val totalDur = (existing?.totalDurationMillis ?: 0L) + DEFAULT_VISIT_DURATION_MILLIS

            val updatedEntity = WebsiteClassificationEntity(
                domain = cleanDomain,
                category = classification.category,
                qualityRating = classification.qualityRating.name,
                customProductivityType = classification.productivityType.name,
                isUserOverride = classification.isUserOverride,
                visitCount = visitCnt,
                totalDurationMillis = totalDur,
                firstDetected = firstDet,
                lastDetected = now,
                updatedAt = now
            )
            websiteDao.insertOrUpdateClassification(updatedEntity)
        }
    }

    override suspend fun recordDomainVisits(domains: List<String>, browserPackage: String) {
        for (domain in domains) {
            recordDomainVisit(domain, browserPackage)
        }
    }

    override fun getAllClassifications(): Flow<List<WebsiteClassificationEntity>> {
        return websiteDao.getAllClassifications()
    }

    override suspend fun getClassification(domain: String): DomainClassification = withContext(Dispatchers.IO) {
        val cleanDomain = KnownDomainsDictionary.normalizeDomain(domain)
        resolveClassification(cleanDomain)
    }

    override suspend fun saveClassification(
        domain: String,
        category: String,
        qualityRating: WebsiteQualityRating,
        isUserOverride: Boolean
    ) {
        withContext(Dispatchers.IO) {
            val cleanDomain = KnownDomainsDictionary.normalizeDomain(domain)
            val prodType = KnownDomainsDictionary.determineProductivity(qualityRating, category)
            val existing = websiteDao.getClassification(cleanDomain)
            val now = System.currentTimeMillis()

            val entity = WebsiteClassificationEntity(
                domain = cleanDomain,
                category = category,
                qualityRating = qualityRating.name,
                customProductivityType = prodType.name,
                isUserOverride = isUserOverride,
                visitCount = existing?.visitCount ?: 1L,
                totalDurationMillis = existing?.totalDurationMillis ?: DEFAULT_VISIT_DURATION_MILLIS,
                firstDetected = existing?.firstDetected ?: now,
                lastDetected = now,
                updatedAt = now
            )
            websiteDao.insertOrUpdateClassification(entity)
        }
    }

    override suspend fun deleteClassification(domain: String) {
        withContext(Dispatchers.IO) {
            val cleanDomain = KnownDomainsDictionary.normalizeDomain(domain)
            websiteDao.deleteClassification(cleanDomain)
        }
    }

    override suspend fun getSummary(startTimeMillis: Long, endTimeMillis: Long): WebsiteDiagnosticSummary = withContext(Dispatchers.IO) {
        val events = websiteDao.getEventsBetween(startTimeMillis, endTimeMillis)

        val startFormatter = DateTimeFormatter.ofPattern("d MMM")
        val startDate = Instant.ofEpochMilli(startTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = Instant.ofEpochMilli(endTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()

        if (events.isEmpty()) {
            return@withContext WebsiteDiagnosticSummary(
                startDateLabel = startDate.format(startFormatter),
                endDateLabel = endDate.format(startFormatter),
                totalVisits = 0,
                totalDurationMillis = 0L,
                productiveTimeMillis = 0L,
                nonProductiveTimeMillis = 0L,
                neutralTimeMillis = 0L,
                productivityRate = 0f,
                mostVisitedDomain = null,
                topDomains = emptyList(),
                dailyTrend = emptyList(),
                categoryBreakdown = emptyList(),
                productiveDomainsCount = 0,
                nonProductiveDomainsCount = 0,
                unknownDomainsCount = 0
            )
        }

        // Aggregate by domain
        val domainVisitsMap = mutableMapOf<String, Int>()
        val domainDurationMap = mutableMapOf<String, Long>()
        val domainFirstSeen = mutableMapOf<String, Long>()
        val domainLastSeen = mutableMapOf<String, Long>()

        var totalVisits = 0
        var totalDuration = 0L
        var productiveDuration = 0L
        var nonProductiveDuration = 0L
        var neutralDuration = 0L

        for (event in events) {
            totalVisits++
            totalDuration += event.estimatedDurationMillis
            val d = event.domain
            domainVisitsMap[d] = domainVisitsMap.getOrDefault(d, 0) + 1
            domainDurationMap[d] = domainDurationMap.getOrDefault(d, 0L) + event.estimatedDurationMillis

            val currentFirst = domainFirstSeen[d]
            if (currentFirst == null || event.timestamp < currentFirst) {
                domainFirstSeen[d] = event.timestamp
            }
            val currentLast = domainLastSeen[d]
            if (currentLast == null || event.timestamp > currentLast) {
                domainLastSeen[d] = event.timestamp
            }
        }

        val domainClassificationList = mutableListOf<DomainClassification>()
        var prodCount = 0
        var nonProdCount = 0
        var unknownCount = 0

        val categoryDurationMap = mutableMapOf<String, Long>()
        val categoryCountMap = mutableMapOf<String, Int>()

        for ((domain, visits) in domainVisitsMap) {
            val classification = resolveClassification(domain)
            val duration = domainDurationMap.getOrDefault(domain, 0L)

            when (classification.productivityType) {
                WebsiteProductivityType.PRODUCTIVE -> {
                    prodCount++
                    productiveDuration += duration
                }
                WebsiteProductivityType.NON_PRODUCTIVE -> {
                    nonProdCount++
                    nonProductiveDuration += duration
                }
                WebsiteProductivityType.NEUTRAL -> {
                    neutralDuration += duration
                }
            }

            if (classification.category == WebsiteCategories.UNKNOWN) {
                unknownCount++
            }

            categoryDurationMap[classification.category] = categoryDurationMap.getOrDefault(classification.category, 0L) + duration
            categoryCountMap[classification.category] = categoryCountMap.getOrDefault(classification.category, 0) + visits

            domainClassificationList.add(
                classification.copy(
                    visitCount = visits.toLong(),
                    totalDurationMillis = duration,
                    firstDetected = domainFirstSeen[domain] ?: 0L,
                    lastDetected = domainLastSeen[domain] ?: 0L
                )
            )
        }

        domainClassificationList.sortByDescending { it.visitCount }

        val productivityRate = if (totalDuration > 0) {
            (productiveDuration.toFloat() / totalDuration.toFloat()) * 100f
        } else 0f

        // Daily trend
        val dailyTrend = calculateDailyTrend(events)

        // Category breakdown
        val categoryBreakdown = categoryDurationMap.map { (cat, duration) ->
            CategoryUsageBreakdown(
                category = cat,
                count = categoryCountMap.getOrDefault(cat, 0),
                durationMillis = duration,
                percentage = if (totalDuration > 0) (duration.toFloat() / totalDuration.toFloat()) * 100f else 0f
            )
        }.sortedByDescending { it.durationMillis }

        WebsiteDiagnosticSummary(
            startDateLabel = startDate.format(startFormatter),
            endDateLabel = endDate.format(startFormatter),
            totalVisits = totalVisits,
            totalDurationMillis = totalDuration,
            productiveTimeMillis = productiveDuration,
            nonProductiveTimeMillis = nonProductiveDuration,
            neutralTimeMillis = neutralDuration,
            productivityRate = productivityRate,
            mostVisitedDomain = domainClassificationList.firstOrNull(),
            topDomains = domainClassificationList,
            dailyTrend = dailyTrend,
            categoryBreakdown = categoryBreakdown,
            productiveDomainsCount = prodCount,
            nonProductiveDomainsCount = nonProdCount,
            unknownDomainsCount = unknownCount
        )
    }

    private suspend fun calculateDailyTrend(events: List<WebsiteEventEntity>): List<WebsiteDailyPoint> {
        val dayFormatter = DateTimeFormatter.ofPattern("EEE")
        val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val dayEventsMap = mutableMapOf<String, MutableList<WebsiteEventEntity>>()
        val dayLabelMap = mutableMapOf<String, String>()

        for (event in events) {
            val localDate = Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            val iso = localDate.format(isoFormatter)
            val label = localDate.format(dayFormatter)
            dayLabelMap[iso] = label
            val list = dayEventsMap.getOrPut(iso) { mutableListOf() }
            list.add(event)
        }

        val sortedDates = dayEventsMap.keys.sorted()
        val result = mutableListOf<WebsiteDailyPoint>()

        for (iso in sortedDates) {
            val dayEvents = dayEventsMap[iso] ?: continue
            var prod = 0L
            var nonProd = 0L
            var neutral = 0L
            var total = 0L

            for (e in dayEvents) {
                total += e.estimatedDurationMillis
                when (e.productivityType) {
                    WebsiteProductivityType.PRODUCTIVE.name -> prod += e.estimatedDurationMillis
                    WebsiteProductivityType.NON_PRODUCTIVE.name -> nonProd += e.estimatedDurationMillis
                    else -> neutral += e.estimatedDurationMillis
                }
            }

            result.add(
                WebsiteDailyPoint(
                    dateLabel = dayLabelMap[iso] ?: iso,
                    fullDate = iso,
                    productiveMillis = prod,
                    nonProductiveMillis = nonProd,
                    neutralMillis = neutral,
                    totalMillis = total,
                    totalVisits = dayEvents.size
                )
            )
        }

        return result
    }

    override fun getAllRules(): Flow<List<DomainRuleEntity>> {
        return domainRuleDao.getAllRules()
    }

    override suspend fun addRule(
        pattern: String,
        ruleType: DomainRuleType,
        category: String,
        rating: WebsiteQualityRating
    ) {
        withContext(Dispatchers.IO) {
            val cleanPattern = KnownDomainsDictionary.normalizeDomain(pattern)
            val entity = DomainRuleEntity(
                domainPattern = cleanPattern,
                ruleType = ruleType.name,
                category = category,
                qualityRating = rating.name,
                isEnabled = true
            )
            domainRuleDao.insertRule(entity)
        }
    }

    override suspend fun deleteRule(id: Long) {
        withContext(Dispatchers.IO) {
            domainRuleDao.deleteRuleById(id)
        }
    }

    override suspend fun toggleRule(id: Long, isEnabled: Boolean) {
        withContext(Dispatchers.IO) {
            val rules = domainRuleDao.getActiveRules()
            rules.find { it.id == id }?.let {
                domainRuleDao.updateRule(it.copy(isEnabled = isEnabled))
            }
        }
    }

    override suspend fun clearTodayWebsiteData() {
        withContext(Dispatchers.IO) {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = System.currentTimeMillis()
            websiteDao.deleteEventsBetween(startOfDay, endOfDay)
            lastVisitTimeMap.clear()
        }
    }

    override suspend fun clearAllWebsiteData() {
        withContext(Dispatchers.IO) {
            websiteDao.deleteAllEvents()
            websiteDao.deleteAllClassifications()
            lastVisitTimeMap.clear()
        }
    }

    private suspend fun resolveClassification(domain: String): DomainClassification {
        // 1. Check custom user override in classification table
        val saved = websiteDao.getClassification(domain)
        if (saved != null && (saved.isUserOverride || saved.category != WebsiteCategories.UNKNOWN)) {
            val rating = WebsiteQualityRating.fromName(saved.qualityRating)
            val prodType = KnownDomainsDictionary.determineProductivity(rating, saved.category)
            return DomainClassification(
                domain = domain,
                category = saved.category,
                qualityRating = rating,
                productivityType = prodType,
                isUserOverride = saved.isUserOverride,
                visitCount = saved.visitCount,
                totalDurationMillis = saved.totalDurationMillis,
                firstDetected = saved.firstDetected,
                lastDetected = saved.lastDetected
            )
        }

        // 2. Check custom active domain rules
        val activeRules = domainRuleDao.getActiveRules()
        for (rule in activeRules) {
            if (domain.equals(rule.domainPattern, ignoreCase = true) || domain.endsWith(".${rule.domainPattern}")) {
                val rating = WebsiteQualityRating.fromName(rule.qualityRating)
                val prodType = KnownDomainsDictionary.determineProductivity(rating, rule.category)
                return DomainClassification(
                    domain = domain,
                    category = rule.category,
                    qualityRating = rating,
                    productivityType = prodType,
                    isUserOverride = true
                )
            }
        }

        // 3. Check Known dictionary
        val known = KnownDomainsDictionary.getCategoryAndRating(domain)
        if (known != null) {
            val (cat, rating) = known
            val prodType = KnownDomainsDictionary.determineProductivity(rating, cat)
            return DomainClassification(
                domain = domain,
                category = cat,
                qualityRating = rating,
                productivityType = prodType,
                isUserOverride = false
            )
        }

        // 4. Default / Unknown
        return DomainClassification(
            domain = domain,
            category = WebsiteCategories.UNKNOWN,
            qualityRating = WebsiteQualityRating.UNRATED,
            productivityType = WebsiteProductivityType.NEUTRAL,
            isUserOverride = false
        )
    }

    private fun isValidDomain(domain: String): Boolean {
        if (domain.isBlank() || domain.length < 3) return false
        if (domain.contains(" ")) return false
        if (!domain.contains(".")) return false
        if (domain.endsWith(".arpa") || domain.endsWith(".local") || domain.endsWith(".internal")) return false
        if (domain.matches(Regex("^[0-9.]+$"))) return false // Skip raw IP addresses
        if (domain.matches(Regex("^[0-9a-fA-F:]+$"))) return false // Skip raw IPv6
        return true
    }
}
