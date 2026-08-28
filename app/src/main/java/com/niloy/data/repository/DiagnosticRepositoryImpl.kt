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
    private val classificationDao: AppClassificationDao
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

        val appUsageList = mutableListOf<AppUsageInfo>()

        for ((pkgName, totalMillis) in packageTimeMap) {
            if (totalMillis < 1000) continue // Skip under 1 second

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
                    totalTimeInForegroundMillis = totalMillis,
                    categories = categories,
                    qualityRating = rating,
                    productivityType = prodType,
                    lastTimeUsed = packageLastUsedMap[pkgName] ?: 0L
                )
            )
        }

        appUsageList.sortByDescending { it.totalTimeInForegroundMillis }

        var totalMillis = 0L
        var productiveMillis = 0L
        var nonProductiveMillis = 0L
        var neutralMillis = 0L

        for (app in appUsageList) {
            totalMillis += app.totalTimeInForegroundMillis
            when (app.productivityType) {
                ProductivityType.PRODUCTIVE -> productiveMillis += app.totalTimeInForegroundMillis
                ProductivityType.NON_PRODUCTIVE -> nonProductiveMillis += app.totalTimeInForegroundMillis
                ProductivityType.NEUTRAL -> neutralMillis += app.totalTimeInForegroundMillis
            }
        }

        val rate = if (totalMillis > 0) {
            (productiveMillis.toFloat() / totalMillis.toFloat()) * 100f
        } else 0f

        val startFormatter = DateTimeFormatter.ofPattern("d MMM")
        val startDate = Instant.ofEpochMilli(startTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = Instant.ofEpochMilli(endTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()

        // Daily trend calculation
        val dailyTrend = calculateDailyTrend(usageStatsManager, startTimeMillis, endTimeMillis)

        // Find most productive day
        val mostProductiveDay = dailyTrend.maxByOrNull { it.productiveMillis }?.dateLabel ?: "N/A"

        DiagnosticSummary(
            startDateLabel = startDate.format(startFormatter),
            endDateLabel = endDate.format(startFormatter),
            totalUsageTimeMillis = totalMillis,
            productiveTimeMillis = productiveMillis,
            nonProductiveTimeMillis = nonProductiveMillis,
            neutralTimeMillis = neutralMillis,
            mostUsedApp = appUsageList.firstOrNull(),
            appsCount = appUsageList.size,
            productivityRate = rate,
            mostProductiveDayName = mostProductiveDay,
            topApps = appUsageList,
            dailyTrend = dailyTrend
        )
    }

    private suspend fun calculateDailyTrend(
        usageStatsManager: UsageStatsManager,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<DailyUsagePoint> {
        val list = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTimeMillis,
            endTimeMillis
        ) ?: emptyList()

        val dayMap = mutableMapOf<String, MutableMap<String, Long>>()
        val dayLabelMap = mutableMapOf<String, String>()

        val dayFormatter = DateTimeFormatter.ofPattern("EEE")
        val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        for (stat in list) {
            if (stat.totalTimeInForeground > 0) {
                val localDate = Instant.ofEpochMilli(stat.firstTimeStamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val isoDate = localDate.format(isoFormatter)
                val dayLabel = localDate.format(dayFormatter)

                dayLabelMap[isoDate] = dayLabel
                val mapForDay = dayMap.getOrPut(isoDate) { mutableMapOf() }
                val current = mapForDay.getOrDefault(stat.packageName, 0L)
                mapForDay[stat.packageName] = current + stat.totalTimeInForeground
            }
        }

        val sortedDates = dayMap.keys.sorted()
        val points = mutableListOf<DailyUsagePoint>()

        for (isoDate in sortedDates) {
            val appsForDay = dayMap[isoDate] ?: continue
            var dayProd = 0L
            var dayNonProd = 0L
            var dayNeutral = 0L

            for ((pkgName, time) in appsForDay) {
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
                    dateLabel = dayLabelMap[isoDate] ?: isoDate,
                    fullDate = isoDate,
                    productiveMillis = dayProd,
                    nonProductiveMillis = dayNonProd,
                    neutralMillis = dayNeutral,
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
            productivityRate = 0f,
            mostProductiveDayName = "N/A",
            topApps = emptyList(),
            dailyTrend = emptyList()
        )
    }
}
