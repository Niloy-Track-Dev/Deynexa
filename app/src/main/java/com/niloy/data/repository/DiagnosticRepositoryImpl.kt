package com.niloy.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.niloy.data.local.dao.AppClassificationDao
import com.niloy.data.local.entity.AppClassificationEntity
import com.niloy.domain.model.*
import com.niloy.domain.repository.DiagnosticRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DiagnosticRepositoryImpl(
    private val context: Context,
    private val classificationDao: AppClassificationDao,
    private val focentraDao: com.niloy.data.local.dao.FocentraStudySessionDao
) : DiagnosticRepository {

    override fun isUsagePermissionGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun getAllClassifications(): Flow<List<AppClassificationEntity>> {
        return classificationDao.getAllClassifications()
    }

    override suspend fun saveClassification(
        packageName: String,
        appName: String,
        categories: List<String>,
        qualityRating: AppQualityRating
    ) {
        withContext(Dispatchers.IO) {
            val categoriesString = if (categories.isEmpty()) AppCategories.UNCLASSIFIED else categories.joinToString(",")
            val prodType = determineProductivityType(qualityRating, categories)
            val entity = AppClassificationEntity(
                packageName = packageName,
                appName = appName,
                categories = categoriesString,
                qualityRating = qualityRating.name,
                customProductivityType = prodType.name,
                updatedAt = System.currentTimeMillis()
            )
            classificationDao.insertOrUpdate(entity)
        }
    }

    override suspend fun clearDiagnosticData() {
        withContext(Dispatchers.IO) {
            classificationDao.deleteAll()
        }
    }

    override suspend fun getInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedPackages = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList()
        }

        val classificationsMap = try {
            val list = mutableMapOf<String, AppClassificationEntity>()
            classificationDao.getAllClassifications()
            // We read one-shot by querying DB directly
            list
        } catch (e: Exception) {
            emptyMap<String, AppClassificationEntity>()
        }

        val result = mutableListOf<InstalledAppInfo>()
        for (app in installedPackages) {
            // Filter out system apps without launch intent if desired, or show launcher apps
            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null || (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                val appName = pm.getApplicationLabel(app).toString()
                val pkgName = app.packageName
                val saved = classificationDao.getClassification(pkgName)

                val categories = if (saved != null && saved.categories.isNotBlank()) {
                    saved.categories.split(",").map { it.trim() }
                } else {
                    getDefaultCategories(pkgName)
                }

                val rating = if (saved != null) {
                    AppQualityRating.fromName(saved.qualityRating)
                } else {
                    AppQualityRating.UNRATED
                }

                val prodType = determineProductivityType(rating, categories)

                result.add(
                    InstalledAppInfo(
                        packageName = pkgName,
                        appName = appName,
                        categories = categories,
                        qualityRating = rating,
                        productivityType = prodType
                    )
                )
            }
        }
        result.sortedBy { it.appName.lowercase() }
    }

    override suspend fun getUsageSummary(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): DiagnosticSummary = withContext(Dispatchers.IO) {
        if (!isUsagePermissionGranted()) {
            return@withContext createEmptySummary(startTimeMillis, endTimeMillis)
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext createEmptySummary(startTimeMillis, endTimeMillis)

        val pm = context.packageManager

        val statsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTimeMillis,
            endTimeMillis
        ) ?: emptyList()

        // Aggregate by package name
        val packageTimeMap = mutableMapOf<String, Long>()
        val packageLastUsedMap = mutableMapOf<String, Long>()

        for (stat in statsList) {
            if (stat.totalTimeInForeground > 0) {
                val current = packageTimeMap.getOrDefault(stat.packageName, 0L)
                packageTimeMap[stat.packageName] = current + stat.totalTimeInForeground
                val lastUsed = packageLastUsedMap.getOrDefault(stat.packageName, 0L)
                if (stat.lastTimeUsed > lastUsed) {
                    packageLastUsedMap[stat.packageName] = stat.lastTimeUsed
                }
            }
        }

        val focentraSessions = focentraDao.getSessionsBetween(startTimeMillis, endTimeMillis)
        val focentraTimeMillis = focentraSessions.sumOf { it.duration * 60000L }

        val appUsageList = mutableListOf<AppUsageInfo>()

        for ((pkgName, totalMillis) in packageTimeMap) {
            if (totalMillis < 1000) continue // Skip under 1 second

            // Prevent double counting the Focentra app itself if it appears in usage stats
            // We'll prioritize the "verified" Focentra sessions
            val adjustedMillis = if (pkgName == "com.focentra") {
                // If Focentra app usage is recorded, we subtract it here because we'll add the verified sessions separately
                // or we just let it be and make sure verified sessions are added on top if they represent "additional" time.
                // However, usually the Focentra app IS the study timer.
                // Priority: Use the verified sessions for "Focentra Focus Time".
                // Total usage should be max(AppUsageStats, verified sessions) roughly?
                // Actually, let's just keep app usage as is, but mark Focentra app as productive.
                totalMillis
            } else {
                totalMillis
            }

            val appName = try {
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkgName.substringAfterLast(".")
            }

            val saved = classificationDao.getClassification(pkgName)

            val categories = if (saved != null && saved.categories.isNotBlank()) {
                saved.categories.split(",").map { it.trim() }
            } else {
                getDefaultCategories(pkgName)
            }

            val rating = if (saved != null) {
                AppQualityRating.fromName(saved.qualityRating)
            } else {
                AppQualityRating.UNRATED
            }

            val prodType = determineProductivityType(rating, categories)

            appUsageList.add(
                AppUsageInfo(
                    packageName = pkgName,
                    appName = appName,
                    totalTimeInForegroundMillis = adjustedMillis,
                    categories = categories,
                    qualityRating = rating,
                    productivityType = prodType,
                    lastTimeUsed = packageLastUsedMap[pkgName] ?: 0L
                )
            )
        }

        appUsageList.sortByDescending { it.totalTimeInForegroundMillis }

        var productiveMillis = focentraTimeMillis // Start with Focentra time
        var totalMillis = focentraTimeMillis // Start with Focentra time
        var nonProductiveMillis = 0L
        var neutralMillis = 0L
        var productiveCount = 0
        var nonProductiveCount = 0

        val categoryDurationMap = mutableMapOf<String, Long>()
        if (focentraTimeMillis > 0) {
            categoryDurationMap[AppCategories.STUDY_TIMER] = focentraTimeMillis
        }
        val categoryCountMap = mutableMapOf<String, Int>()

        for (app in appUsageList) {
            // Priority/Deduplication Logic:
            // If the app is Focentra itself, we've already accounted for its core value via verified sessions.
            // If the app was used during Focentra sessions, it's tricky.
            // Simplified: Add Focentra sessions as base. For other apps, if they are "productive", add them.
            // If they are "non-productive", add them. 
            // To avoid double-counting the Focentra app itself:
            if (app.packageName == "com.focentra") {
                // Already added verified sessions. If app usage > verified sessions, maybe add the delta?
                val delta = (app.totalTimeInForegroundMillis - focentraTimeMillis).coerceAtLeast(0L)
                // However, usually verified sessions are more accurate for study time.
                // Let's just not add the Focentra app usage if we have verified sessions.
                continue 
            }

            totalMillis += app.totalTimeInForegroundMillis
            when (app.productivityType) {
                ProductivityType.PRODUCTIVE -> {
                    productiveMillis += app.totalTimeInForegroundMillis
                    productiveCount++
                }
                ProductivityType.NON_PRODUCTIVE -> {
                    nonProductiveMillis += app.totalTimeInForegroundMillis
                    nonProductiveCount++
                }
                ProductivityType.NEUTRAL -> neutralMillis += app.totalTimeInForegroundMillis
            }

            for (cat in app.categories) {
                categoryDurationMap[cat] = categoryDurationMap.getOrDefault(cat, 0L) + app.totalTimeInForegroundMillis
                categoryCountMap[cat] = categoryCountMap.getOrDefault(cat, 0) + 1
            }
        }

        val enrichedAppsList = appUsageList.map { app ->
            val pct = if (totalMillis > 0) (app.totalTimeInForegroundMillis.toFloat() / totalMillis.toFloat()) * 100f else 0f
            app.copy(percentageOfTotal = pct)
        }

        val rate = if (totalMillis > 0) {
            (productiveMillis.toFloat() / totalMillis.toFloat()) * 100f
        } else 0f

        val startFormatter = DateTimeFormatter.ofPattern("d MMM")
        val startDate = Instant.ofEpochMilli(startTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = Instant.ofEpochMilli(endTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()

        // Daily trend calculation for the last 7 days or selected range
        val dailyTrend = calculateDailyTrend(usageStatsManager, startDate, endDate)

        // Find most productive day
        val mostProductiveDay = dailyTrend.maxByOrNull { it.productiveMillis }?.dateLabel ?: "N/A"

        val categoryBreakdown: List<CategoryUsageBreakdown> = categoryDurationMap.map { (cat, duration) ->
            CategoryUsageBreakdown(
                category = cat,
                count = categoryCountMap.getOrDefault(cat, 0),
                durationMillis = duration,
                percentage = if (totalMillis > 0) (duration.toFloat() / totalMillis.toFloat()) * 100f else 0f
            )
        }.sortedByDescending { it.durationMillis }

        DiagnosticSummary(
            startDateLabel = startDate.format(startFormatter),
            endDateLabel = endDate.format(startFormatter),
            totalUsageTimeMillis = totalMillis,
            productiveTimeMillis = productiveMillis,
            nonProductiveTimeMillis = nonProductiveMillis,
            neutralTimeMillis = neutralMillis,
            focentraFocusMillis = focentraTimeMillis,
            mostUsedApp = enrichedAppsList.firstOrNull(),
            appsCount = enrichedAppsList.size,
            productiveAppsCount = productiveCount,
            nonProductiveAppsCount = nonProductiveCount,
            productivityRate = rate,
            mostProductiveDayName = mostProductiveDay,
            topApps = enrichedAppsList,
            dailyTrend = dailyTrend,
            categoryBreakdown = categoryBreakdown
        )
    }

    private suspend fun calculateDailyTrend(
        usageStatsManager: UsageStatsManager,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyUsagePoint> {
        val trendDates = if (startDate == endDate || java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) < 6) {
            (0..6).map { endDate.minusDays(6L - it) }
        } else {
            generateSequence(startDate) { it.plusDays(1) }
                .takeWhile { !it.isAfter(endDate) }
                .toList()
        }

        val trendStartMillis = trendDates.first().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val trendEndMillis = trendDates.last().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        val list = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            trendStartMillis,
            trendEndMillis
        ) ?: emptyList()

        val dayMap = mutableMapOf<String, MutableMap<String, Long>>()
        val dayFormatter = DateTimeFormatter.ofPattern("EEE")
        val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        for (stat in list) {
            if (stat.totalTimeInForeground > 0) {
                val localDate = Instant.ofEpochMilli(stat.firstTimeStamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val isoDate = localDate.format(isoFormatter)

                val mapForDay = dayMap.getOrPut(isoDate) { mutableMapOf() }
                val current = mapForDay.getOrDefault(stat.packageName, 0L)
                mapForDay[stat.packageName] = current + stat.totalTimeInForeground
            }
        }

        val points = mutableListOf<DailyUsagePoint>()

        val focentraSessions = focentraDao.getSessionsBetween(trendStartMillis, trendEndMillis)
        val focentraDayMap = mutableMapOf<String, Long>()
        for (session in focentraSessions) {
            val isoDate = Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).toLocalDate().format(isoFormatter)
            focentraDayMap[isoDate] = focentraDayMap.getOrDefault(isoDate, 0L) + (session.duration * 60000L)
        }

        for (targetDate in trendDates) {
            val isoDate = targetDate.format(isoFormatter)
            val dayLabel = targetDate.format(dayFormatter)
            val appsForDay = dayMap[isoDate] ?: emptyMap()
            val focentraTime = focentraDayMap[isoDate] ?: 0L

            var dayProd = focentraTime
            var dayNonProd = 0L
            var dayNeutral = 0L

            for ((pkgName, time) in appsForDay) {
                if (pkgName == "com.focentra") continue // Already added verified sessions

                val saved = classificationDao.getClassification(pkgName)
                val categories = if (saved != null && saved.categories.isNotBlank()) {
                    saved.categories.split(",").map { it.trim() }
                } else {
                    getDefaultCategories(pkgName)
                }
                val rating = if (saved != null) {
                    AppQualityRating.fromName(saved.qualityRating)
                } else {
                    AppQualityRating.UNRATED
                }
                val prodType = determineProductivityType(rating, categories)

                when (prodType) {
                    ProductivityType.PRODUCTIVE -> dayProd += time
                    ProductivityType.NON_PRODUCTIVE -> dayNonProd += time
                    ProductivityType.NEUTRAL -> dayNeutral += time
                }
            }

            val dayTotal = dayProd + dayNonProd + dayNeutral
            points.add(
                DailyUsagePoint(
                    dateLabel = dayLabel,
                    fullDate = isoDate,
                    productiveMillis = dayProd,
                    nonProductiveMillis = dayNonProd,
                    neutralMillis = dayNeutral,
                    focentraFocusMillis = focentraTime,
                    totalMillis = dayTotal
                )
            )
        }

        return points
    }

    private fun getDefaultCategories(packageName: String): List<String> {
        val lower = packageName.lowercase()
        return when {
            lower.contains("youtube") -> listOf(AppCategories.ENTERTAINMENT, AppCategories.EDUCATION)
            lower.contains("duolingo") || lower.contains("coursera") || lower.contains("udemy") || lower.contains("khan") || lower.contains("anki") -> listOf(AppCategories.EDUCATION)
            lower.contains("chrome") || lower.contains("firefox") || lower.contains("browser") || lower.contains("opera") || lower.contains("edge") -> listOf(AppCategories.BROWSER)
            lower.contains("whatsapp") || lower.contains("telegram") || lower.contains("messenger") || lower.contains("signal") -> listOf(AppCategories.COMMUNICATION)
            lower.contains("facebook") || lower.contains("instagram") || lower.contains("twitter") || lower.contains("tiktok") || lower.contains("reddit") || lower.contains("snapchat") || lower.contains("x.android") -> listOf(AppCategories.SOCIAL_MEDIA)
            lower.contains("game") || lower.contains("pubg") || lower.contains("roblox") || lower.contains("clash") -> listOf(AppCategories.GAMES)
            lower.contains("calculator") || lower.contains("clock") || lower.contains("settings") || lower.contains("camera") || lower.contains("gallery") -> listOf(AppCategories.UTILITIES)
            lower.contains("notion") || lower.contains("slack") || lower.contains("task") || lower.contains("todo") || lower.contains("daynexa") || lower.contains("keep") || lower.contains("calendar") -> listOf(AppCategories.PRODUCTIVITY)
            else -> listOf(AppCategories.UNCLASSIFIED)
        }
    }

    private fun determineProductivityType(qualityRating: AppQualityRating, categories: List<String>): ProductivityType {
        return when {
            qualityRating == AppQualityRating.VERY_GOOD || qualityRating == AppQualityRating.GOOD -> ProductivityType.PRODUCTIVE
            qualityRating == AppQualityRating.BAD || qualityRating == AppQualityRating.VERY_BAD || qualityRating == AppQualityRating.NOT_GOOD -> ProductivityType.NON_PRODUCTIVE
            categories.contains(AppCategories.EDUCATION) || categories.contains(AppCategories.STUDY_TIMER) || categories.contains(AppCategories.PRODUCTIVITY) -> ProductivityType.PRODUCTIVE
            categories.contains(AppCategories.SOCIAL_MEDIA) || categories.contains(AppCategories.ENTERTAINMENT) || categories.contains(AppCategories.GAMES) -> ProductivityType.NON_PRODUCTIVE
            else -> ProductivityType.NEUTRAL
        }
    }

    private fun createEmptySummary(startTimeMillis: Long, endTimeMillis: Long): DiagnosticSummary {
        val startFormatter = DateTimeFormatter.ofPattern("d MMM")
        val startDate = Instant.ofEpochMilli(startTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = Instant.ofEpochMilli(endTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()

        return DiagnosticSummary(
            startDateLabel = startDate.format(startFormatter),
            endDateLabel = endDate.format(startFormatter),
            totalUsageTimeMillis = 0L,
            productiveTimeMillis = 0L,
            nonProductiveTimeMillis = 0L,
            neutralTimeMillis = 0L,
            mostUsedApp = null,
            appsCount = 0,
            productiveAppsCount = 0,
            nonProductiveAppsCount = 0,
            productivityRate = 0f,
            mostProductiveDayName = "N/A",
            topApps = emptyList<AppUsageInfo>(),
            dailyTrend = emptyList<DailyUsagePoint>(),
            categoryBreakdown = emptyList<CategoryUsageBreakdown>()
        )
    }
}
