package com.niloy.domain.model

enum class AppQualityRating(val label: String, val score: Int) {
    VERY_GOOD("Very Good", 2),
    GOOD("Good", 1),
    NEUTRAL("Neutral", 0),
    NOT_GOOD("Not Good", -1),
    BAD("Bad", -2),
    VERY_BAD("Very Bad", -3),
    UNRATED("Unrated", 0);

    companion object {
        fun fromName(name: String): AppQualityRating {
            return values().find { it.name.equals(name, ignoreCase = true) } ?: UNRATED
        }
    }
}

enum class ProductivityType(val label: String) {
    PRODUCTIVE("Productive"),
    NON_PRODUCTIVE("Non-Productive"),
    NEUTRAL("Neutral")
}

object AppCategories {
    const val SOCIAL_MEDIA = "Social Media"
    const val EDUCATION = "Education"
    const val STUDY_TIMER = "Study Timer"
    const val PRODUCTIVITY = "Productivity"
    const val ENTERTAINMENT = "Entertainment"
    const val COMMUNICATION = "Communication"
    const val BROWSER = "Browser"
    const val GAMES = "Games"
    const val UTILITIES = "Utilities"
    const val OTHER = "Other"
    const val UNCLASSIFIED = "Unclassified"

    val ALL_CATEGORIES = listOf(
        PRODUCTIVITY, EDUCATION, STUDY_TIMER, BROWSER,
        COMMUNICATION, SOCIAL_MEDIA, ENTERTAINMENT, GAMES, UTILITIES, OTHER
    )
}

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForegroundMillis: Long,
    val categories: List<String>,
    val qualityRating: AppQualityRating,
    val productivityType: ProductivityType,
    val percentageOfTotal: Float = 0f,
    val lastTimeUsed: Long = 0L
)

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val categories: List<String>,
    val qualityRating: AppQualityRating,
    val productivityType: ProductivityType
)

data class DailyUsagePoint(
    val dateLabel: String, // e.g., "Mon", "15 Aug"
    val fullDate: String, // ISO date string yyyy-MM-dd
    val productiveMillis: Long,
    val nonProductiveMillis: Long,
    val neutralMillis: Long,
    val focentraFocusMillis: Long = 0L,
    val totalMillis: Long
)

data class CategoryUsageBreakdown(
    val category: String,
    val durationMillis: Long,
    val percentage: Float,
    val count: Int = 0
)

data class AppCategory(
    val id: Long = 0,
    val name: String,
    val isProductive: Boolean = true
)

data class DiagnosticSummary(
    val startDateLabel: String,
    val endDateLabel: String,
    val totalUsageTimeMillis: Long,
    val productiveTimeMillis: Long,
    val nonProductiveTimeMillis: Long,
    val neutralTimeMillis: Long,
    val focentraFocusMillis: Long = 0L,
    val mostUsedApp: AppUsageInfo?,
    val appsCount: Int,
    val productiveAppsCount: Int = 0,
    val nonProductiveAppsCount: Int = 0,
    val productivityRate: Float, // percentage 0..100
    val mostProductiveDayName: String,
    val topApps: List<AppUsageInfo>,
    val dailyTrend: List<DailyUsagePoint>,
    val categoryBreakdown: List<CategoryUsageBreakdown> = emptyList()
)
