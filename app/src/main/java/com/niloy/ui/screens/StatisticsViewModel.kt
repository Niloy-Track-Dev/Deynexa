package com.niloy.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niloy.domain.model.Category
import com.niloy.domain.model.Task
import com.niloy.domain.model.TaskOccurrence
import com.niloy.domain.model.TaskState
import com.niloy.domain.repository.TaskRepository
import com.niloy.domain.service.SchedulingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class DailyChartPoint(
    val dayLabel: String,
    val date: LocalDate,
    val totalCount: Int,
    val completedCount: Int,
    val completionRate: Float
)

data class HeatmapCell(
    val date: LocalDate,
    val totalTasks: Int,
    val completedTasks: Int,
    val intensity: Int // 0: None, 1: Low (1-49%), 2: Medium (50-79%), 3: High (80-100%)
)

data class CategoryStat(
    val category: Category,
    val totalCount: Int,
    val completedCount: Int,
    val completionRate: Float
)

data class PeriodStats(
    val total: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    val skipped: Int = 0,
    val completionRate: Float = 0f
)

data class StatisticsUiState(
    val totalCompletedAllTime: Int = 0,
    val totalPendingAllTime: Int = 0,
    val totalSkippedAllTime: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val bestDayName: String = "—",
    val overallCompletionRate: Float = 0f,
    val productivityScore: Int = 85,
    val productivityRating: String = "High",
    val weeklyComparisonDelta: Int = 0,
    val todayStats: PeriodStats = PeriodStats(),
    val weekStats: PeriodStats = PeriodStats(),
    val monthStats: PeriodStats = PeriodStats(),
    val last7DaysPoints: List<DailyChartPoint> = emptyList(),
    val heatmapCells: List<HeatmapCell> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val isLoading: Boolean = true
)

class StatisticsViewModel(
    private val repository: TaskRepository,
    private val schedulingService: SchedulingService
) : ViewModel() {

    private val _weekendDays = MutableStateFlow(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))

    init {
        viewModelScope.launch {
            val weekendStr = repository.getSetting("weekend_days") ?: "SATURDAY,SUNDAY"
            if (weekendStr.isNotBlank()) {
                val parsed = weekendStr.split(",").mapNotNull {
                    try { DayOfWeek.valueOf(it.trim()) } catch (e: Exception) { null }
                }.toSet()
                if (parsed.isNotEmpty()) {
                    _weekendDays.value = parsed
                }
            }
        }
    }

    val uiState: StateFlow<StatisticsUiState> = combine(
        repository.getTasks(),
        repository.getAllOccurrences(),
        repository.getCategories(),
        _weekendDays
    ) { tasks, allOccurrences, categories, weekendDays ->
        if (tasks.isEmpty()) {
            return@combine StatisticsUiState(isLoading = false)
        }

        val today = LocalDate.now()

        // Helper to compute tasks for any date
        fun getTasksFor(date: LocalDate): List<com.niloy.domain.service.TaskWithOccurrence> {
            return schedulingService.getTasksForDate(
                tasks = tasks,
                occurrences = allOccurrences,
                date = date,
                customWeekend = weekendDays
            )
        }

        // Today Stats
        val todayTasks = getTasksFor(today)
        val todayCompleted = todayTasks.count { it.occurrence.state == TaskState.COMPLETED }
        val todayPending = todayTasks.count { it.occurrence.state == TaskState.PENDING }
        val todaySkipped = todayTasks.count { it.occurrence.state == TaskState.SKIPPED }
        val todayRate = if (todayTasks.isEmpty()) 0f else todayCompleted.toFloat() / todayTasks.size

        // Last 7 days points
        val last7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val last7DaysPoints = last7Days.map { date ->
            val dayTasks = getTasksFor(date)
            val comp = dayTasks.count { it.occurrence.state == TaskState.COMPLETED }
            val rate = if (dayTasks.isEmpty()) 0f else comp.toFloat() / dayTasks.size
            DailyChartPoint(
                dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                date = date,
                totalCount = dayTasks.size,
                completedCount = comp,
                completionRate = rate
            )
        }

        // This Week Stats
        val startOfWeek = today.with(DayOfWeek.MONDAY)
        val thisWeekDays = generateSequence(startOfWeek) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .toList()
        var weekTotal = 0
        var weekCompleted = 0
        var weekPending = 0
        var weekSkipped = 0
        thisWeekDays.forEach { d ->
            val dTasks = getTasksFor(d)
            weekTotal += dTasks.size
            weekCompleted += dTasks.count { it.occurrence.state == TaskState.COMPLETED }
            weekPending += dTasks.count { it.occurrence.state == TaskState.PENDING }
            weekSkipped += dTasks.count { it.occurrence.state == TaskState.SKIPPED }
        }
        val weekRate = if (weekTotal == 0) 0f else weekCompleted.toFloat() / weekTotal

        // This Month Stats
        val startOfMonth = today.withDayOfMonth(1)
        val thisMonthDays = generateSequence(startOfMonth) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .toList()
        var monthTotal = 0
        var monthCompleted = 0
        var monthPending = 0
        var monthSkipped = 0
        thisMonthDays.forEach { d ->
            val dTasks = getTasksFor(d)
            monthTotal += dTasks.size
            monthCompleted += dTasks.count { it.occurrence.state == TaskState.COMPLETED }
            monthPending += dTasks.count { it.occurrence.state == TaskState.PENDING }
            monthSkipped += dTasks.count { it.occurrence.state == TaskState.SKIPPED }
        }
        val monthRate = if (monthTotal == 0) 0f else monthCompleted.toFloat() / monthTotal

        // Heatmap Cells: last 14 weeks (98 days)
        val heatmapDaysCount = 14 * 7
        val heatmapStartDate = today.minusDays((heatmapDaysCount - 1).toLong())
        val heatmapCells = (0 until heatmapDaysCount).map { offset ->
            val date = heatmapStartDate.plusDays(offset.toLong())
            val dayTasks = getTasksFor(date)
            val comp = dayTasks.count { it.occurrence.state == TaskState.COMPLETED }
            val rate = if (dayTasks.isEmpty()) 0f else comp.toFloat() / dayTasks.size
            val intensity = when {
                dayTasks.isEmpty() -> 0
                comp == 0 -> 0
                rate < 0.5f -> 1
                rate < 0.8f -> 2
                else -> 3
            }
            HeatmapCell(
                date = date,
                totalTasks = dayTasks.size,
                completedTasks = comp,
                intensity = intensity
            )
        }

        // Productivity Insights Engine
        val insights = schedulingService.calculateProductivityInsights(
            tasks = tasks,
            allOccurrences = allOccurrences,
            customWeekend = weekendDays
        )

        // Best Day of Week
        val bestDayName = insights.bestCompletionWeekday.ifBlank { "Every Day" }

        val productivityScore = (monthRate * 100).toInt().coerceIn(0, 100)
        val productivityRating = when {
            productivityScore >= 80 -> "High"
            productivityScore >= 50 -> "Medium"
            else -> "Growing"
        }

        // Category Stats
        val categoryStatsList = categories.map { cat ->
            val catTasks = tasks.filter { it.categoryId == cat.id }
            var catTotalOccurrences = 0
            var catCompOccurrences = 0
            (0..30).forEach { offset ->
                val d = today.minusDays(offset.toLong())
                val dTasks = getTasksFor(d).filter { it.task.categoryId == cat.id }
                catTotalOccurrences += dTasks.size
                catCompOccurrences += dTasks.count { it.occurrence.state == TaskState.COMPLETED }
            }
            val rate = if (catTotalOccurrences == 0) 0f else catCompOccurrences.toFloat() / catTotalOccurrences
            CategoryStat(
                category = cat,
                totalCount = catTotalOccurrences,
                completedCount = catCompOccurrences,
                completionRate = rate
            )
        }

        val allTimeCompleted = allOccurrences.count { it.state == TaskState.COMPLETED }
        val allTimeSkipped = allOccurrences.count { it.state == TaskState.SKIPPED }
        val allTimePending = allOccurrences.count { it.state == TaskState.PENDING }

        // Previous week stats for comparison delta
        val prevWeekStart = startOfWeek.minusDays(7)
        val prevWeekDays = (0..6).map { prevWeekStart.plusDays(it.toLong()) }
        var prevWeekTotal = 0
        var prevWeekCompleted = 0
        prevWeekDays.forEach { d ->
            val dTasks = getTasksFor(d)
            prevWeekTotal += dTasks.size
            prevWeekCompleted += dTasks.count { it.occurrence.state == TaskState.COMPLETED }
        }
        val prevWeekRate = if (prevWeekTotal == 0) 0f else prevWeekCompleted.toFloat() / prevWeekTotal
        val weeklyDelta = ((weekRate - prevWeekRate) * 100).toInt()

        StatisticsUiState(
            totalCompletedAllTime = allTimeCompleted,
            totalPendingAllTime = allTimePending,
            totalSkippedAllTime = allTimeSkipped,
            currentStreak = insights.currentStreak,
            bestStreak = insights.longestRecurringStreak,
            bestDayName = bestDayName,
            overallCompletionRate = monthRate,
            productivityScore = productivityScore,
            productivityRating = productivityRating,
            weeklyComparisonDelta = weeklyDelta,
            todayStats = PeriodStats(todayTasks.size, todayCompleted, todayPending, todaySkipped, todayRate),
            weekStats = PeriodStats(weekTotal, weekCompleted, weekPending, weekSkipped, weekRate),
            monthStats = PeriodStats(monthTotal, monthCompleted, monthPending, monthSkipped, monthRate),
            last7DaysPoints = last7DaysPoints,
            heatmapCells = heatmapCells,
            categoryStats = categoryStatsList,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )

    class Factory(
        private val repository: TaskRepository,
        private val schedulingService: SchedulingService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticsViewModel(repository, schedulingService) as T
        }
    }
}
