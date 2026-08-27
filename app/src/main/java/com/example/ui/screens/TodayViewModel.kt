package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Category
import com.example.domain.model.Task
import com.example.domain.model.TaskOccurrence
import com.example.domain.model.TaskState
import com.example.domain.repository.TaskRepository
import com.example.domain.service.SchedulingService
import com.example.domain.service.TaskWithOccurrence
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val isLoading: Boolean = true
)

private data class RepoData(
    val tasks: List<Task>,
    val occurrences: List<TaskOccurrence>,
    val categories: List<Category>
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

    init {
        viewModelScope.launch {
            val format = repository.getSetting("time_format") ?: "24H"
            _is24Hour.value = format == "24H"
        }
    }

    private val repoDataFlow: Flow<RepoData> = combine(
        repository.getTasks(),
        repository.getAllOccurrences(),
        repository.getCategories()
    ) { tasks, occurrences, categories ->
        RepoData(tasks, occurrences, categories)
    }

    val uiState: StateFlow<TodayUiState> = combine(
        _selectedDate,
        _selectedCategoryId,
        _is24Hour,
        repoDataFlow
    ) { date, categoryIdFilter, is24H, repoData ->
        val dateStr = date.toString()
        val occurrencesForDate = repoData.occurrences.filter { it.date == dateStr }
        val filteredForDate = schedulingService.getTasksForDate(repoData.tasks, occurrencesForDate, date)
        
        val completedCount = filteredForDate.count { it.occurrence.state == TaskState.COMPLETED }
        val pendingCount = filteredForDate.count { it.occurrence.state == TaskState.PENDING }
        val skippedCount = filteredForDate.count { it.occurrence.state == TaskState.SKIPPED }
        val totalCount = filteredForDate.size
        val percentage = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

        val displayedTasks = if (categoryIdFilter != null) {
            filteredForDate.filter { it.task.categoryId == categoryIdFilter }
        } else {
            filteredForDate
        }.sortedWith(
            compareBy<TaskWithOccurrence> { it.task.isAllDay }
                .thenBy { it.task.startTime ?: Long.MAX_VALUE }
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
            repository.saveOccurrence(
                TaskOccurrence(
                    taskId = taskId,
                    date = date,
                    state = newState
                )
            )
        }
    }

    fun skipTask(taskId: Long, date: String) {
        viewModelScope.launch {
            repository.saveOccurrence(
                TaskOccurrence(
                    taskId = taskId,
                    date = date,
                    state = TaskState.SKIPPED
                )
            )
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
