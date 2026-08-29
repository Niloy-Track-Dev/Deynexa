package com.niloy.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niloy.domain.model.*
import com.niloy.domain.repository.TaskRepository
import com.niloy.domain.service.SchedulingService
import com.niloy.domain.service.TaskWithOccurrence
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

data class TodayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksWithOccurrences: List<TaskWithOccurrence> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val skippedCount: Int = 0,
    val completionPercentage: Float = 0f,
    val is24Hour: Boolean = true,
    val weekendDays: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
    val productivityInsights: ProductivityInsights = ProductivityInsights(),
    val isLoading: Boolean = true
)

private data class RepoData(
    val tasks: List<Task>,
    val occurrences: List<TaskOccurrence>,
    val categories: List<Category>,
    val weekendDays: Set<DayOfWeek>
)

class TodayViewModel(
    private val repository: TaskRepository,
    private val schedulingService: SchedulingService
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

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

    private val repoDataFlow: Flow<RepoData> = combine(
        repository.getTasks(),
        repository.getAllOccurrences(),
        repository.getCategories(),
        _weekendDays
    ) { tasks, occurrences, categories, weekendDays ->
        RepoData(tasks, occurrences, categories, weekendDays)
    }

    val uiState: StateFlow<TodayUiState> = combine(
        _selectedDate,
        _selectedCategoryId,
        _is24Hour,
        repoDataFlow
    ) { date, categoryIdFilter, is24H, repoData ->
        val filteredForDate = schedulingService.getTasksForDate(
            tasks = repoData.tasks,
            occurrences = repoData.occurrences,
            date = date,
            customWeekend = repoData.weekendDays
        )
        
        val completedCount = filteredForDate.count { it.occurrence.state == TaskState.COMPLETED }
        val pendingCount = filteredForDate.count { it.occurrence.state == TaskState.PENDING }
        val skippedCount = filteredForDate.count { it.occurrence.state == TaskState.SKIPPED }
        val totalCount = filteredForDate.size
        val percentage = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

        val displayedTasks = if (categoryIdFilter != null) {
            filteredForDate.filter { it.task.categoryId == categoryIdFilter }
        } else {
            filteredForDate
        }

        val insights = schedulingService.calculateProductivityInsights(
            tasks = repoData.tasks,
            allOccurrences = repoData.occurrences,
            customWeekend = repoData.weekendDays
        )

        TodayUiState(
            selectedDate = date,
            tasksWithOccurrences = displayedTasks,
            categories = repoData.categories,
            selectedCategoryId = categoryIdFilter,
            totalCount = totalCount,
            completedCount = completedCount,
            pendingCount = pendingCount,
            skippedCount = skippedCount,
            completionPercentage = percentage,
            is24Hour = is24H,
            weekendDays = repoData.weekendDays,
            productivityInsights = insights,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun jumpToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun selectCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
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

    fun toggleSkipTask(taskId: Long, date: String, currentState: TaskState, reason: String? = null) {
        viewModelScope.launch {
            val newState = if (currentState == TaskState.SKIPPED) TaskState.PENDING else TaskState.SKIPPED
            val existing = repository.getAllOccurrences().first().find { it.taskId == taskId && it.date == date }
            val updated = (existing ?: TaskOccurrence(taskId = taskId, date = date)).copy(
                state = newState,
                notes = if (newState == TaskState.SKIPPED) reason else existing?.notes,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveOccurrence(updated)
        }
    }

    fun rescheduleOccurrence(
        taskId: Long,
        originalDate: String,
        newStartTime: Long?,
        newEndTime: Long?,
        newDate: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val existing = repository.getAllOccurrences().first().find { it.taskId == taskId && it.date == originalDate }
            val updated = (existing ?: TaskOccurrence(taskId = taskId, date = originalDate)).copy(
                rescheduledStartTime = newStartTime,
                rescheduledEndTime = newEndTime,
                rescheduledDate = newDate,
                isException = true,
                notes = notes,
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
            return TodayViewModel(repository, schedulingService) as T
        }
    }
}
