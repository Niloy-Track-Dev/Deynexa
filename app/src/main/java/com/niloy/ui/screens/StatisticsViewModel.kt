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

    val uiState: StateFlow<StatisticsUiState> = combine(
        repository.getTasks(),
        repository.getAllOccurrences(),
        repository.getCategories()
    ) { tasks, allOccurrences, categories ->
        if (tasks.isEmpty()) {
            return@combine StatisticsUiState(isLoading = false)
        }

        val today = LocalDate.now()
        val occurrencesMap = allOccurrences.groupBy { it.date }

        // Helper to compute tasks for any date
        fun getTasksFor(date: LocalDate): List<com.niloy.domain.service.TaskWithOccurrence> {
            val dateStr = date.toString()
            val occs = occurrencesMap[dateStr] ?: emptyList()
            return schedulingService.getTasksForDate(tasks, occs, date)
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

        // Calculate Streak (consecutive days with completed >= 1 and completionRate >= 0.5)
        var streak = 0
        var checkDate = today
        // If today has tasks but none completed yet, check starting yesterday for streak
        if (todayTasks.isNotEmpty() && todayCompleted == 0) {
            checkDate = today.minusDays(1)
        }
        while (true) {
            val dTasks = getTasksFor(checkDate)
            if (dTasks.isEmpty()) {
                // If no tasks scheduled on weekend or break, don't break streak if checked within past 30 days
                val isWeekend = checkDate.dayOfWeek == DayOfWeek.SATURDAY || checkDate.dayOfWeek == DayOfWeek.SUNDAY
                if (isWeekend) {
                    checkDate = checkDate.minusDays(1)
                    if (checkDate.isBefore(today.minusDays(60))) break
                    continue
                } else {
                    break
                }
            }
            val dComp = dTasks.count { it.occurrence.state == TaskState.COMPLETED }
            if (dComp > 0 && (dComp.toFloat() / dTasks.size) >= 0.5f) {
                streak++
                checkDate = checkDate.minusDays(1)
                if (checkDate.isBefore(today.minusDays(365))) break
            } else {
                break
            }
        }

        // Best Day of Week Calculation (Monday to Sunday)
        val dayOfWeekStats = DayOfWeek.values().map { day ->
            var dayOccurrencesTotal = 0
            var dayOccurrencesComp = 0
            (0..60).forEach { offset ->
                val d = today.minusDays(offset.toLong())
                if (d.dayOfWeek == day) {
                    val dTasks = getTasksFor(d)
                    dayOccurrencesTotal += dTasks.size
                    dayOccurrencesComp += dTasks.count { it.occurrence.state == TaskState.COMPLETED }
                }
            }
            val rate = if (dayOccurrencesTotal == 0) 0f else dayOccurrencesComp.toFloat() / dayOccurrencesTotal
            day to rate
        }
        val bestDayEntry = dayOfWeekStats.maxByOrNull { it.second }
        val bestDayName = if (bestDayEntry != null && bestDayEntry.second > 0f) {
            bestDayEntry.first.getDisplayName(TextStyle.FULL, Locale.getDefault())
        } else {
            "Every Day"
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

        StatisticsUiState(
            totalCompletedAllTime = allTimeCompleted,
            totalPendingAllTime = allTimePending,
            totalSkippedAllTime = allTimeSkipped,
            currentStreak = streak,
            bestStreak = maxOf(streak, 7),
            bestDayName = bestDayName,
            overallCompletionRate = monthRate,
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
