package com.niloy.domain.model

enum class WebsiteQualityRating(val label: String, val score: Int) {
    VERY_GOOD("Very Good", 2),
    GOOD("Good", 1),
    NEUTRAL("Neutral", 0),
    NOT_GOOD("Not Good", -1),
    BAD("Bad", -2),
    VERY_BAD("Very Bad", -3),
    UNRATED("Unrated", 0);

    companion object {
        fun fromName(name: String): WebsiteQualityRating {
            return values().find { it.name.equals(name, ignoreCase = true) } ?: UNRATED
        }
    }
}

enum class WebsiteProductivityType(val label: String) {
    PRODUCTIVE("Productive"),
    NON_PRODUCTIVE("Non-Productive"),
    NEUTRAL("Neutral")
}

object WebsiteCategories {
    const val EDUCATION = "Education"
    const val PRODUCTIVITY = "Productivity"
    const val SOCIAL_MEDIA = "Social Media"
    const val ENTERTAINMENT = "Entertainment"
    const val NEWS = "News"
    const val SHOPPING = "Shopping"
    const val GAMING = "Gaming"
    const val TECHNOLOGY = "Technology"
    const val SEARCH = "Search"
    const val COMMUNICATION = "Communication"
    const val ADULT = "Adult"
    const val GAMBLING = "Gambling"
    const val OTHER = "Other"
    const val UNKNOWN = "Unknown"

    val ALL_CATEGORIES = listOf(
        EDUCATION, PRODUCTIVITY, TECHNOLOGY, SEARCH,
        COMMUNICATION, NEWS, SOCIAL_MEDIA, ENTERTAINMENT,
        SHOPPING, GAMING, ADULT, GAMBLING, OTHER, UNKNOWN
    )
}

enum class DomainRuleType {
    ALLOW,
    BLOCK,
    CLASSIFY
}

data class DomainRule(
    val id: Long = 0,
    val domainPattern: String,
    val ruleType: DomainRuleType = DomainRuleType.CLASSIFY,
    val category: String = WebsiteCategories.UNKNOWN,
    val qualityRating: WebsiteQualityRating = WebsiteQualityRating.UNRATED,
    val isEnabled: Boolean = true
)

data class DomainClassification(
    val domain: String,
    val category: String,
    val qualityRating: WebsiteQualityRating,
    val productivityType: WebsiteProductivityType,
    val isUserOverride: Boolean = false,
    val visitCount: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val firstDetected: Long = 0L,
    val lastDetected: Long = 0L
)

data class WebsiteActivityEvent(
    val id: Long = 0,
    val domain: String,
    val timestamp: Long,
    val estimatedDurationMillis: Long = 30_000L,
    val browserPackage: String = "Browser",
    val productivityType: WebsiteProductivityType = WebsiteProductivityType.NEUTRAL
)

data class WebsiteDailyPoint(
    val dateLabel: String,
    val fullDate: String,
    val productiveMillis: Long,
    val nonProductiveMillis: Long,
    val neutralMillis: Long,
    val totalMillis: Long,
    val totalVisits: Int
)

data class CategoryUsageBreakdown(
    val category: String,
    val count: Int,
    val durationMillis: Long,
    val percentage: Float
)

data class WebsiteDiagnosticSummary(
    val startDateLabel: String,
    val endDateLabel: String,
    val totalVisits: Int,
    val totalDurationMillis: Long,
    val productiveTimeMillis: Long,
    val nonProductiveTimeMillis: Long,
    val neutralTimeMillis: Long,
    val productivityRate: Float,
    val mostVisitedDomain: DomainClassification?,
    val topDomains: List<DomainClassification>,
    val dailyTrend: List<WebsiteDailyPoint>,
    val categoryBreakdown: List<CategoryUsageBreakdown>,
    val productiveDomainsCount: Int,
    val nonProductiveDomainsCount: Int,
    val unknownDomainsCount: Int
)

object KnownDomainsDictionary {
    private val DICTIONARY = mapOf(
        // Technology & Productivity
        "github.com" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.VERY_GOOD),
        "gitlab.com" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.VERY_GOOD),
        "stackoverflow.com" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.VERY_GOOD),
        "stackexchange.com" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.VERY_GOOD),
        "developer.android.com" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.VERY_GOOD),
        "kotlinlang.org" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.VERY_GOOD),
        "notion.so" to Pair(WebsiteCategories.PRODUCTIVITY, WebsiteQualityRating.VERY_GOOD),
        "figma.com" to Pair(WebsiteCategories.PRODUCTIVITY, WebsiteQualityRating.VERY_GOOD),
        "slack.com" to Pair(WebsiteCategories.COMMUNICATION, WebsiteQualityRating.GOOD),
        "trello.com" to Pair(WebsiteCategories.PRODUCTIVITY, WebsiteQualityRating.GOOD),
        "linear.app" to Pair(WebsiteCategories.PRODUCTIVITY, WebsiteQualityRating.VERY_GOOD),
        "jira.com" to Pair(WebsiteCategories.PRODUCTIVITY, WebsiteQualityRating.GOOD),
        "atlassian.com" to Pair(WebsiteCategories.PRODUCTIVITY, WebsiteQualityRating.GOOD),
        "chatgpt.com" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.GOOD),
        "openai.com" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.GOOD),
        "claude.ai" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.GOOD),
        "deepmind.google" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.VERY_GOOD),
        "docs.google.com" to Pair(WebsiteCategories.PRODUCTIVITY, WebsiteQualityRating.VERY_GOOD),
        "drive.google.com" to Pair(WebsiteCategories.PRODUCTIVITY, WebsiteQualityRating.GOOD),

        // Education
        "wikipedia.org" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "khanacademy.org" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "coursera.org" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "edx.org" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "udemy.org" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.GOOD),
        "udemy.com" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.GOOD),
        "duolingo.com" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "ankiweb.net" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "mit.edu" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "stanford.edu" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "harvard.edu" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "geeksforgeeks.org" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.GOOD),
        "leetcode.com" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "hackerrank.com" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.GOOD),
        "w3schools.com" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.GOOD),
        "developer.mozilla.org" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "arxiv.org" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),
        "scholar.google.com" to Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD),

        // Search & Reference
        "google.com" to Pair(WebsiteCategories.SEARCH, WebsiteQualityRating.NEUTRAL),
        "bing.com" to Pair(WebsiteCategories.SEARCH, WebsiteQualityRating.NEUTRAL),
        "duckduckgo.com" to Pair(WebsiteCategories.SEARCH, WebsiteQualityRating.GOOD),
        "ecosia.org" to Pair(WebsiteCategories.SEARCH, WebsiteQualityRating.GOOD),
        "brave.com" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.GOOD),

        // News & Reading
        "bbc.com" to Pair(WebsiteCategories.NEWS, WebsiteQualityRating.NEUTRAL),
        "reuters.com" to Pair(WebsiteCategories.NEWS, WebsiteQualityRating.GOOD),
        "theguardian.com" to Pair(WebsiteCategories.NEWS, WebsiteQualityRating.NEUTRAL),
        "nytimes.com" to Pair(WebsiteCategories.NEWS, WebsiteQualityRating.NEUTRAL),
        "wsj.com" to Pair(WebsiteCategories.NEWS, WebsiteQualityRating.NEUTRAL),
        "medium.com" to Pair(WebsiteCategories.NEWS, WebsiteQualityRating.GOOD),
        "dev.to" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.VERY_GOOD),
        "news.ycombinator.com" to Pair(WebsiteCategories.TECHNOLOGY, WebsiteQualityRating.GOOD),
        "bloomberg.com" to Pair(WebsiteCategories.NEWS, WebsiteQualityRating.NEUTRAL),

        // Communication
        "mail.google.com" to Pair(WebsiteCategories.COMMUNICATION, WebsiteQualityRating.GOOD),
        "outlook.live.com" to Pair(WebsiteCategories.COMMUNICATION, WebsiteQualityRating.GOOD),
        "outlook.com" to Pair(WebsiteCategories.COMMUNICATION, WebsiteQualityRating.GOOD),
        "proton.me" to Pair(WebsiteCategories.COMMUNICATION, WebsiteQualityRating.GOOD),
        "whatsapp.com" to Pair(WebsiteCategories.COMMUNICATION, WebsiteQualityRating.NEUTRAL),
        "telegram.org" to Pair(WebsiteCategories.COMMUNICATION, WebsiteQualityRating.NEUTRAL),
        "web.whatsapp.com" to Pair(WebsiteCategories.COMMUNICATION, WebsiteQualityRating.NEUTRAL),
        "discord.com" to Pair(WebsiteCategories.COMMUNICATION, WebsiteQualityRating.NOT_GOOD),
        "zoom.us" to Pair(WebsiteCategories.COMMUNICATION, WebsiteQualityRating.GOOD),
        "meet.google.com" to Pair(WebsiteCategories.COMMUNICATION, WebsiteQualityRating.GOOD),

        // Social Media
        "facebook.com" to Pair(WebsiteCategories.SOCIAL_MEDIA, WebsiteQualityRating.BAD),
        "instagram.com" to Pair(WebsiteCategories.SOCIAL_MEDIA, WebsiteQualityRating.BAD),
        "twitter.com" to Pair(WebsiteCategories.SOCIAL_MEDIA, WebsiteQualityRating.NOT_GOOD),
        "x.com" to Pair(WebsiteCategories.SOCIAL_MEDIA, WebsiteQualityRating.NOT_GOOD),
        "tiktok.com" to Pair(WebsiteCategories.SOCIAL_MEDIA, WebsiteQualityRating.VERY_BAD),
        "reddit.com" to Pair(WebsiteCategories.SOCIAL_MEDIA, WebsiteQualityRating.NOT_GOOD),
        "linkedin.com" to Pair(WebsiteCategories.SOCIAL_MEDIA, WebsiteQualityRating.GOOD),
        "snapchat.com" to Pair(WebsiteCategories.SOCIAL_MEDIA, WebsiteQualityRating.BAD),
        "pinterest.com" to Pair(WebsiteCategories.SOCIAL_MEDIA, WebsiteQualityRating.NEUTRAL),
        "threads.net" to Pair(WebsiteCategories.SOCIAL_MEDIA, WebsiteQualityRating.BAD),

        // Entertainment & Media
        "youtube.com" to Pair(WebsiteCategories.ENTERTAINMENT, WebsiteQualityRating.NEUTRAL),
        "netflix.com" to Pair(WebsiteCategories.ENTERTAINMENT, WebsiteQualityRating.NOT_GOOD),
        "spotify.com" to Pair(WebsiteCategories.ENTERTAINMENT, WebsiteQualityRating.GOOD),
        "twitch.tv" to Pair(WebsiteCategories.ENTERTAINMENT, WebsiteQualityRating.BAD),
        "disneyplus.com" to Pair(WebsiteCategories.ENTERTAINMENT, WebsiteQualityRating.NOT_GOOD),
        "primevideo.com" to Pair(WebsiteCategories.ENTERTAINMENT, WebsiteQualityRating.NOT_GOOD),
        "soundcloud.com" to Pair(WebsiteCategories.ENTERTAINMENT, WebsiteQualityRating.NEUTRAL),

        // Shopping
        "amazon.com" to Pair(WebsiteCategories.SHOPPING, WebsiteQualityRating.NEUTRAL),
        "ebay.com" to Pair(WebsiteCategories.SHOPPING, WebsiteQualityRating.NEUTRAL),
        "aliexpress.com" to Pair(WebsiteCategories.SHOPPING, WebsiteQualityRating.NOT_GOOD),
        "walmart.com" to Pair(WebsiteCategories.SHOPPING, WebsiteQualityRating.NEUTRAL),

        // Gaming
        "steampowered.com" to Pair(WebsiteCategories.GAMING, WebsiteQualityRating.NOT_GOOD),
        "roblox.com" to Pair(WebsiteCategories.GAMING, WebsiteQualityRating.BAD),
        "epicgames.com" to Pair(WebsiteCategories.GAMING, WebsiteQualityRating.NOT_GOOD),
        "chess.com" to Pair(WebsiteCategories.GAMING, WebsiteQualityRating.GOOD),
        "lichess.org" to Pair(WebsiteCategories.GAMING, WebsiteQualityRating.GOOD)
    )

    fun getCategoryAndRating(domain: String): Pair<String, WebsiteQualityRating>? {
        val normalized = normalizeDomain(domain)
        // Direct match
        DICTIONARY[normalized]?.let { return it }

        // Suffix / Subdomain match (e.g. m.facebook.com -> facebook.com)
        for ((knownDomain, value) in DICTIONARY) {
            if (normalized.endsWith(".$knownDomain")) {
                return value
            }
        }

        // TLD heuristics for education and government
        if (normalized.endsWith(".edu") || normalized.endsWith(".ac.uk") || normalized.endsWith(".edu.bd")) {
            return Pair(WebsiteCategories.EDUCATION, WebsiteQualityRating.VERY_GOOD)
        }
        if (normalized.endsWith(".gov") || normalized.endsWith(".gov.bd") || normalized.endsWith(".gov.uk")) {
            return Pair(WebsiteCategories.PRODUCTIVITY, WebsiteQualityRating.GOOD)
        }

        return null
    }

    fun normalizeDomain(rawDomain: String): String {
        var clean = rawDomain.trim().lowercase()
        if (clean.startsWith("http://")) clean = clean.removePrefix("http://")
        if (clean.startsWith("https://")) clean = clean.removePrefix("https://")
        if (clean.contains("/")) clean = clean.substringBefore("/")
        if (clean.contains(":")) clean = clean.substringBefore(":")
        if (clean.startsWith("www.")) clean = clean.removePrefix("www.")
        return clean
    }

    fun determineProductivity(rating: WebsiteQualityRating, category: String): WebsiteProductivityType {
        return when {
            rating == WebsiteQualityRating.VERY_GOOD || rating == WebsiteQualityRating.GOOD -> WebsiteProductivityType.PRODUCTIVE
            rating == WebsiteQualityRating.BAD || rating == WebsiteQualityRating.VERY_BAD || rating == WebsiteQualityRating.NOT_GOOD -> WebsiteProductivityType.NON_PRODUCTIVE
            category == WebsiteCategories.EDUCATION || category == WebsiteCategories.PRODUCTIVITY || category == WebsiteCategories.TECHNOLOGY -> WebsiteProductivityType.PRODUCTIVE
            category == WebsiteCategories.SOCIAL_MEDIA || category == WebsiteCategories.ENTERTAINMENT || category == WebsiteCategories.GAMING || category == WebsiteCategories.ADULT || category == WebsiteCategories.GAMBLING -> WebsiteProductivityType.NON_PRODUCTIVE
            else -> WebsiteProductivityType.NEUTRAL
        }
    }
}
