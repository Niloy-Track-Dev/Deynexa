package com.niloy.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niloy.domain.model.*
import com.niloy.domain.repository.TaskRepository
import com.niloy.domain.service.NextOccurrenceInfo
import com.niloy.domain.service.SchedulingService
import com.niloy.domain.service.TaskReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

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
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val recurrenceInterval: Int = 1,
    val recurrenceDayOfMonth: Int? = null,
    val recurrenceMonthOfYear: Int? = null,
    val recurrenceEndType: RecurrenceEndType = RecurrenceEndType.NEVER,
    val recurrenceEndDate: String? = null,
    val recurrenceCount: Int? = null,
    val startDate: String = LocalDate.now().toString(),
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int = 0,
    val categories: List<Category> = emptyList(),
    val templates: List<TaskTemplate> = emptyList(),
    val conflicts: List<ScheduleConflict> = emptyList(),
    val nextOccurrence: NextOccurrenceInfo? = null,
    val editMode: RecurrenceEditMode = RecurrenceEditMode.ENTIRE_SERIES,
    val showEditModeDialog: Boolean = false,
    val isSaved: Boolean = false,
    val isTemplateSaved: Boolean = false
)

class TaskDetailViewModel(
    private val repository: TaskRepository,
    private val reminderScheduler: TaskReminderScheduler?,
    private val schedulingService: SchedulingService,
    private val taskId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var allExistingTasks: List<Task> = emptyList()
    private var allOccurrences: List<TaskOccurrence> = emptyList()

    init {
        viewModelScope.launch {
            val categories = repository.getCategories().first()
            val templates = repository.getTaskTemplates().first()
            allExistingTasks = repository.getTasks().first()
            allOccurrences = repository.getAllOccurrences().first()

            _uiState.update {
                it.copy(
                    categories = categories,
                    templates = templates,
                    categoryId = categories.firstOrNull()?.id ?: 0
                )
            }

            if (taskId != null && taskId > 0) {
                repository.getTaskById(taskId)?.let { task ->
                    _uiState.update {
                        it.copy(
                            id = task.id,
                            name = task.name,
                            description = task.description,
                            categoryId = task.categoryId,
                            startTime = task.startTime?.toInt() ?: 420,
                            endTime = task.endTime?.toInt() ?: 480,
                            isAllDay = task.isAllDay,
                            isRecurring = task.isRecurring,
                            recurringDays = task.recurringDays,
                            recurrenceType = task.recurrenceType,
                            recurrenceInterval = task.recurrenceInterval,
                            recurrenceDayOfMonth = task.recurrenceDayOfMonth,
                            recurrenceMonthOfYear = task.recurrenceMonthOfYear,
                            recurrenceEndType = task.recurrenceEndType,
                            recurrenceEndDate = task.recurrenceEndDate,
                            recurrenceCount = task.recurrenceCount,
                            startDate = if (task.startDate.isNotBlank()) task.startDate else LocalDate.now().toString(),
                            reminderEnabled = task.reminderEnabled,
                            reminderOffsetMinutes = task.reminderOffsetMinutes ?: 0
                        )
                    }
                }
            }
            recomputePreviewAndConflicts()
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateCategory(categoryId: Long) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun updateStartTime(minutes: Int) {
        _uiState.update {
            val newEnd = if (minutes >= it.endTime) minutes + 30 else it.endTime
            it.copy(startTime = minutes, endTime = newEnd)
        }
        recomputePreviewAndConflicts()
    }

    fun updateEndTime(minutes: Int) {
        _uiState.update { it.copy(endTime = minutes) }
        recomputePreviewAndConflicts()
    }

    fun updateIsAllDay(isAllDay: Boolean) {
        _uiState.update { it.copy(isAllDay = isAllDay) }
        recomputePreviewAndConflicts()
    }

    fun updateIsRecurring(isRecurring: Boolean) {
        _uiState.update { it.copy(isRecurring = isRecurring) }
        recomputePreviewAndConflicts()
    }

    fun updateRecurrenceType(type: RecurrenceType) {
        _uiState.update { it.copy(recurrenceType = type) }
        recomputePreviewAndConflicts()
    }

    fun updateRecurrenceInterval(interval: Int) {
        _uiState.update { it.copy(recurrenceInterval = maxOf(1, interval)) }
        recomputePreviewAndConflicts()
    }

    fun updateRecurrenceDayOfMonth(day: Int?) {
        _uiState.update { it.copy(recurrenceDayOfMonth = day) }
        recomputePreviewAndConflicts()
    }

    fun updateRecurrenceMonthOfYear(month: Int?) {
        _uiState.update { it.copy(recurrenceMonthOfYear = month) }
        recomputePreviewAndConflicts()
    }

    fun updateRecurrenceEndType(endType: RecurrenceEndType) {
        _uiState.update { it.copy(recurrenceEndType = endType) }
        recomputePreviewAndConflicts()
    }

    fun updateRecurrenceEndDate(dateStr: String?) {
        _uiState.update { it.copy(recurrenceEndDate = dateStr) }
        recomputePreviewAndConflicts()
    }

    fun updateRecurrenceCount(count: Int?) {
        _uiState.update { it.copy(recurrenceCount = count) }
        recomputePreviewAndConflicts()
    }

    fun updateStartDate(dateStr: String) {
        _uiState.update { it.copy(startDate = dateStr) }
        recomputePreviewAndConflicts()
    }

    fun updateReminderEnabled(enabled: Boolean) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
    }

    fun updateReminderOffsetMinutes(offset: Int) {
        _uiState.update { it.copy(reminderOffsetMinutes = offset) }
    }

    fun toggleRecurringDay(day: DayOfWeek) {
        _uiState.update { state ->
            val newDays = if (state.recurringDays.contains(day)) {
                if (state.recurringDays.size > 1) state.recurringDays - day else state.recurringDays
            } else {
                state.recurringDays + day
            }
            state.copy(recurringDays = newDays)
        }
        recomputePreviewAndConflicts()
    }

    fun setEditMode(mode: RecurrenceEditMode) {
        _uiState.update { it.copy(editMode = mode, showEditModeDialog = false) }
        performSaveWithMode(mode)
    }

    fun applyTemplate(template: TaskTemplate) {
        _uiState.update {
            it.copy(
                name = template.name,
                description = template.description,
                categoryId = template.categoryId,
                startTime = template.startTime?.toInt() ?: it.startTime,
                endTime = template.endTime?.toInt() ?: (it.startTime + template.defaultDurationMinutes),
                isAllDay = template.isAllDay,
                isRecurring = template.recurrenceType != RecurrenceType.NONE,
                recurrenceType = template.recurrenceType,
                recurringDays = template.recurringDays.ifEmpty { it.recurringDays },
                recurrenceInterval = template.recurrenceInterval,
                reminderEnabled = template.reminderEnabled,
                reminderOffsetMinutes = template.reminderOffsetMinutes
            )
        }
        recomputePreviewAndConflicts()
    }

    fun saveAsTemplate(templateName: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val duration = if (!state.isAllDay && state.endTime > state.startTime) (state.endTime - state.startTime) else 45
            val template = TaskTemplate(
                name = templateName.ifBlank { state.name },
                description = state.description,
                categoryId = state.categoryId,
                defaultDurationMinutes = duration,
                startTime = if (state.isAllDay) null else state.startTime.toLong(),
                endTime = if (state.isAllDay) null else state.endTime.toLong(),
                isAllDay = state.isAllDay,
                recurrenceType = state.recurrenceType,
                recurringDays = state.recurringDays,
                recurrenceInterval = state.recurrenceInterval,
                reminderEnabled = state.reminderEnabled,
                reminderOffsetMinutes = state.reminderOffsetMinutes
            )
            repository.saveTaskTemplate(template)
            val updated = repository.getTaskTemplates().first()
            _uiState.update { it.copy(templates = updated, isTemplateSaved = true) }
        }
    }

    fun onSaveClicked() {
        val state = _uiState.value
        if (state.id > 0 && state.isRecurring) {
            // Ask user which occurrences to apply changes to
            _uiState.update { it.copy(showEditModeDialog = true) }
        } else {
            performSaveWithMode(RecurrenceEditMode.ENTIRE_SERIES)
        }
    }

    fun dismissEditModeDialog() {
        _uiState.update { it.copy(showEditModeDialog = false) }
    }

    private fun performSaveWithMode(mode: RecurrenceEditMode) {
        viewModelScope.launch {
            val state = _uiState.value
            val today = LocalDate.now()

            val baseTask = Task(
                id = state.id,
                name = state.name,
                description = state.description,
                categoryId = state.categoryId,
                startTime = if (state.isAllDay) null else state.startTime.toLong(),
                endTime = if (state.isAllDay) null else state.endTime.toLong(),
                isAllDay = state.isAllDay,
                isRecurring = state.isRecurring,
                recurringDays = state.recurringDays,
                recurrenceType = if (state.isRecurring) state.recurrenceType else RecurrenceType.NONE,
                recurrenceInterval = state.recurrenceInterval,
                recurrenceDayOfMonth = state.recurrenceDayOfMonth,
                recurrenceMonthOfYear = state.recurrenceMonthOfYear,
                recurrenceEndType = state.recurrenceEndType,
                recurrenceEndDate = state.recurrenceEndDate,
                recurrenceCount = state.recurrenceCount,
                startDate = state.startDate,
                reminderEnabled = state.reminderEnabled,
                reminderOffsetMinutes = if (state.reminderEnabled) state.reminderOffsetMinutes else null
            )

            when (mode) {
                RecurrenceEditMode.THIS_OCCURRENCE -> {
                    // Save an exception occurrence for today with custom timing and details
                    val occurrence = TaskOccurrence(
                        taskId = state.id,
                        date = today.toString(),
                        state = TaskState.PENDING,
                        rescheduledStartTime = if (state.isAllDay) null else state.startTime.toLong(),
                        rescheduledEndTime = if (state.isAllDay) null else state.endTime.toLong(),
                        isException = true,
                        notes = state.description
                    )
                    repository.saveOccurrence(occurrence)
                }

                RecurrenceEditMode.THIS_AND_FUTURE -> {
                    // Split series:
                    // 1. Terminate original task series as of yesterday
                    val original = repository.getTaskById(state.id)
                    if (original != null) {
                        val yesterday = today.minusDays(1)
                        val updatedOriginal = original.copy(
                            recurrenceEndType = RecurrenceEndType.ON_DATE,
                            recurrenceEndDate = yesterday.toString()
                        )
                        repository.saveTask(updatedOriginal)
                    }

                    // 2. Create new task starting today
                    val newTask = baseTask.copy(
                        id = 0L,
                        startDate = today.toString()
                    )
                    val savedId = repository.saveTask(newTask)
                    val finalTask = newTask.copy(id = savedId)

                    val catName = state.categories.find { it.id == state.categoryId }?.name ?: ""
                    reminderScheduler?.scheduleReminder(finalTask, catName)
                }

                RecurrenceEditMode.ENTIRE_SERIES -> {
                    val savedId = repository.saveTask(baseTask)
                    val finalTask = if (state.id == 0L) baseTask.copy(id = savedId) else baseTask

                    val catName = state.categories.find { it.id == state.categoryId }?.name ?: ""
                    reminderScheduler?.scheduleReminder(finalTask, catName)
                }
            }

            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun recomputePreviewAndConflicts() {
        val state = _uiState.value
        val dummyTask = Task(
            id = state.id,
            name = state.name.ifBlank { "New Routine" },
            categoryId = state.categoryId,
            startTime = if (state.isAllDay) null else state.startTime.toLong(),
            endTime = if (state.isAllDay) null else state.endTime.toLong(),
            isAllDay = state.isAllDay,
            isRecurring = state.isRecurring,
            recurringDays = state.recurringDays,
            recurrenceType = if (state.isRecurring) state.recurrenceType else RecurrenceType.NONE,
            recurrenceInterval = state.recurrenceInterval,
            recurrenceDayOfMonth = state.recurrenceDayOfMonth,
            recurrenceMonthOfYear = state.recurrenceMonthOfYear,
            recurrenceEndType = state.recurrenceEndType,
            recurrenceEndDate = state.recurrenceEndDate,
            recurrenceCount = state.recurrenceCount,
            startDate = state.startDate
        )

        val nextOcc = schedulingService.calculateNextOccurrence(dummyTask)

        val conflicts = if (!state.isAllDay && nextOcc != null) {
            val dateTasks = schedulingService.getTasksForDate(allExistingTasks, allOccurrences, nextOcc.date)
            schedulingService.findConflicts(
                existingTasks = dateTasks,
                targetTask = dummyTask,
                targetStartTime = state.startTime.toLong(),
                targetEndTime = state.endTime.toLong(),
                isAllDay = state.isAllDay
            )
        } else {
            emptyList()
        }

        _uiState.update {
            it.copy(
                nextOccurrence = nextOcc,
                conflicts = conflicts
            )
        }
    }

    class Factory(
        private val repository: TaskRepository,
        private val reminderScheduler: TaskReminderScheduler?,
        private val schedulingService: SchedulingService,
        private val taskId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskDetailViewModel(repository, reminderScheduler, schedulingService, taskId) as T
        }
    }
}
