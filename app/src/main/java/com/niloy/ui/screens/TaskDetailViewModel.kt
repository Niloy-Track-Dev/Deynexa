package com.niloy.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niloy.domain.model.Category
import com.niloy.domain.model.Task
import com.niloy.domain.repository.TaskRepository
import com.niloy.domain.service.TaskReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class TaskDetailUiState(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val categoryId: Long = 0,
    val startTime: Int = 420, // 07:00
    val endTime: Int = 480, // 08:00
    val isAllDay: Boolean = false,
    val isRecurring: Boolean = true,
    val recurringDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int = 0, // 0: at start, 5: 5 min before, 10: 10 min before, 15: 15 min before, 30: 30 min before
    val categories: List<Category> = emptyList(),
    val isSaved: Boolean = false
)

class TaskDetailViewModel(
    private val repository: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler?,
    private val taskId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = repository.getCategories().first()
            _uiState.update { it.copy(categories = categories, categoryId = categories.firstOrNull()?.id ?: 0) }
            
            if (taskId != null) {
                repository.getTaskById(taskId)?.let { task ->
                    _uiState.update { it.copy(
                        id = task.id,
                        name = task.name,
                        description = task.description,
                        categoryId = task.categoryId,
                        startTime = task.startTime?.toInt() ?: 420,
                        endTime = task.endTime?.toInt() ?: 480,
                        isAllDay = task.isAllDay,
                        isRecurring = task.isRecurring,
                        recurringDays = task.recurringDays,
                        reminderEnabled = task.reminderEnabled,
                        reminderOffsetMinutes = task.reminderOffsetMinutes ?: 0
                    ) }
                }
            }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateDescription(description: String) = _uiState.update { it.copy(description = description) }
    fun updateCategory(categoryId: Long) = _uiState.update { it.copy(categoryId = categoryId) }
    fun updateStartTime(minutes: Int) = _uiState.update { it.copy(startTime = minutes) }
    fun updateEndTime(minutes: Int) = _uiState.update { it.copy(endTime = minutes) }
    fun updateIsAllDay(isAllDay: Boolean) = _uiState.update { it.copy(isAllDay = isAllDay) }
    fun updateIsRecurring(isRecurring: Boolean) = _uiState.update { it.copy(isRecurring = isRecurring) }
    fun updateReminderEnabled(enabled: Boolean) = _uiState.update { it.copy(reminderEnabled = enabled) }
    fun updateReminderOffsetMinutes(offset: Int) = _uiState.update { it.copy(reminderOffsetMinutes = offset) }
    
    fun toggleRecurringDay(day: DayOfWeek) {
        _uiState.update { state ->
            val newDays = if (state.recurringDays.contains(day)) {
                state.recurringDays - day
            } else {
                state.recurringDays + day
            }
            state.copy(recurringDays = newDays)
        }
    }

    fun saveTask() {
        viewModelScope.launch {
            val state = _uiState.value
            val task = Task(
                id = state.id,
                name = state.name,
                description = state.description,
                categoryId = state.categoryId,
                startTime = state.startTime.toLong(),
                endTime = state.endTime.toLong(),
                isAllDay = state.isAllDay,
                isRecurring = state.isRecurring,
                recurringDays = state.recurringDays,
                reminderEnabled = state.reminderEnabled,
                reminderOffsetMinutes = if (state.reminderEnabled) state.reminderOffsetMinutes else null
            )
            val savedId = repository.saveTask(task)
            val finalTask = if (state.id == 0L) task.copy(id = savedId) else task
            
            // Schedule reminder via AlarmManager
            val catName = state.categories.find { it.id == state.categoryId }?.name ?: ""
            reminderScheduler?.scheduleReminder(finalTask, catName)

            _uiState.update { it.copy(isSaved = true) }
        }
    }

    class Factory(
        private val repository: TaskRepository,
        private val reminderScheduler: TaskReminderScheduler?,
        private val taskId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskDetailViewModel(repository, reminderScheduler, taskId) as T
        }
    }
}
