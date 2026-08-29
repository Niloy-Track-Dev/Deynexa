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
import com.niloy.domain.service.TaskWithOccurrence
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class DayProductivity(
    val date: LocalDate,
    val totalCount: Int,
    val completedCount: Int,
    val completionRate: Float
)

data class CalendarStats(
    val total: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    val skipped: Int = 0,
    val completionRate: Float = 0f
)

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksWithOccurrences: List<TaskWithOccurrence> = emptyList(),
    val categories: List<Category> = emptyList(),
    val monthProductivity: Map<LocalDate, DayProductivity> = emptyMap(),
    val stats: CalendarStats = CalendarStats(),
    val is24Hour: Boolean = true,
    val isLoading: Boolean = true
)

private data class CalendarRepoData(
    val tasks: List<Task>,
    val occurrences: List<TaskOccurrence>,
    val categories: List<Category>,
    val weekendDays: Set<DayOfWeek>
)

class CalendarViewModel(
    private val repository: TaskRepository,
    private val schedulingService: SchedulingService
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth = _currentMonth.asStateFlow()

    private val _is24Hour = MutableStateFlow(true)
    private val _weekendDays = MutableStateFlow(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))

    init {
        viewModelScope.launch {
            val format = repository.getSetting("time_format") ?: "24H"
            _is24Hour.value = format == "24H"

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

    private val repoDataFlow: Flow<CalendarRepoData> = combine(
        repository.getTasks(),
        repository.getAllOccurrences(),
        repository.getCategories(),
        _weekendDays
    ) { tasks, occurrences, categories, weekendDays ->
        CalendarRepoData(tasks, occurrences, categories, weekendDays)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _selectedDate,
        _is24Hour,
        repoDataFlow
    ) { month, selectedDate, is24H, repoData ->
        val tasksForSelectedDate = schedulingService.getTasksForDate(
            tasks = repoData.tasks,
            occurrences = repoData.occurrences,
            date = selectedDate,
            customWeekend = repoData.weekendDays
        )

        val completed = tasksForSelectedDate.count { it.occurrence.state == TaskState.COMPLETED }
        val pending = tasksForSelectedDate.count { it.occurrence.state == TaskState.PENDING }
        val skipped = tasksForSelectedDate.count { it.occurrence.state == TaskState.SKIPPED }
        val rate = if (tasksForSelectedDate.isEmpty()) 0f else completed.toFloat() / tasksForSelectedDate.size

        // Calculate productivity for each day in current month
        val productivityMap = mutableMapOf<LocalDate, DayProductivity>()
        val startOfMonth = month.atDay(1)
        val endOfMonth = month.atEndOfMonth()

        var iterDate = startOfMonth
        while (!iterDate.isAfter(endOfMonth)) {
            val dayTasks = schedulingService.getTasksForDate(
                tasks = repoData.tasks,
                occurrences = repoData.occurrences,
                date = iterDate,
                customWeekend = repoData.weekendDays
            )
            val dayCompleted = dayTasks.count { it.occurrence.state == TaskState.COMPLETED }
            val dayRate = if (dayTasks.isEmpty()) 0f else dayCompleted.toFloat() / dayTasks.size
            productivityMap[iterDate] = DayProductivity(
                date = iterDate,
                totalCount = dayTasks.size,
                completedCount = dayCompleted,
                completionRate = dayRate
            )
            iterDate = iterDate.plusDays(1)
        }

        CalendarUiState(
            currentMonth = month,
            selectedDate = selectedDate,
            tasksWithOccurrences = tasksForSelectedDate,
            categories = repoData.categories,
            monthProductivity = productivityMap,
            stats = CalendarStats(
                total = tasksForSelectedDate.size,
                completed = completed,
                pending = pending,
                skipped = skipped,
                completionRate = rate
            ),
            is24Hour = is24H,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _currentMonth.value) {
            _currentMonth.value = YearMonth.from(date)
        }
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun jumpToToday() {
        val today = LocalDate.now()
        _selectedDate.value = today
        _currentMonth.value = YearMonth.now()
    }

    fun toggleTaskCompletion(taskId: Long, date: String, currentState: TaskState) {
        viewModelScope.launch {
            val newState = if (currentState == TaskState.COMPLETED) TaskState.PENDING else TaskState.COMPLETED
            val existing = repository.getAllOccurrences().first().find { it.taskId == taskId && it.date == date }
            val updated = (existing ?: TaskOccurrence(taskId = taskId, date = date)).copy(
                state = newState,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveOccurrence(updated)
        }
    }

    fun toggleSkipTask(taskId: Long, date: String, currentState: TaskState) {
        viewModelScope.launch {
            val newState = if (currentState == TaskState.SKIPPED) TaskState.PENDING else TaskState.SKIPPED
            val existing = repository.getAllOccurrences().first().find { it.taskId == taskId && it.date == date }
            val updated = (existing ?: TaskOccurrence(taskId = taskId, date = date)).copy(
                state = newState,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveOccurrence(updated)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    class Factory(
        private val repository: TaskRepository,
        private val schedulingService: SchedulingService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalendarViewModel(repository, schedulingService) as T
        }
    }
}
