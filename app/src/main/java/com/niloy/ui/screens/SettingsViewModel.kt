package com.niloy.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niloy.domain.model.Category
import com.niloy.domain.model.Task
import com.niloy.domain.model.TaskOccurrence
import com.niloy.domain.repository.TaskRepository
import com.niloy.domain.service.BackupData
import com.niloy.domain.service.BackupService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isOnboardingCompleted: Boolean = false,
    val theme: String = "SYSTEM",
    val timeFormat: String = "24H",
    val weekStart: String = "MONDAY",
    val notificationsEnabled: Boolean = true,
    val defaultReminderOffset: Int = 0,
    val isLoading: Boolean = true,
    val backupJson: String? = null,
    val importMessage: String? = null,
    val isExportSuccess: Boolean = false
)

class SettingsViewModel(
    private val repository: TaskRepository,
    private val backupService: BackupService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val onboarding = repository.getSetting("onboarding_completed")?.toBoolean() ?: false
                val theme = repository.getSetting("theme") ?: "SYSTEM"
                val timeFormat = repository.getSetting("time_format") ?: "24H"
                val weekStart = repository.getSetting("week_start") ?: "MONDAY"
                val notificationsEnabled = repository.getSetting("notifications_enabled")?.toBoolean() ?: true
                val defaultReminderOffset = repository.getSetting("default_reminder_offset")?.toIntOrNull() ?: 0
                
                _uiState.update { it.copy(
                    isOnboardingCompleted = onboarding,
                    theme = theme,
                    timeFormat = timeFormat,
                    weekStart = weekStart,
                    notificationsEnabled = notificationsEnabled,
                    defaultReminderOffset = defaultReminderOffset,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.saveSetting("onboarding_completed", "true")
            _uiState.update { it.copy(isOnboardingCompleted = true) }
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            repository.saveSetting("theme", theme)
            _uiState.update { it.copy(theme = theme) }
        }
    }

    fun updateTimeFormat(format: String) {
        viewModelScope.launch {
            repository.saveSetting("time_format", format)
            _uiState.update { it.copy(timeFormat = format) }
        }
    }

    fun updateWeekStart(startDay: String) {
        viewModelScope.launch {
            repository.saveSetting("week_start", startDay)
            _uiState.update { it.copy(weekStart = startDay) }
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("notifications_enabled", enabled.toString())
            _uiState.update { it.copy(notificationsEnabled = enabled) }
        }
    }

    fun updateDefaultReminderOffset(offset: Int) {
        viewModelScope.launch {
            repository.saveSetting("default_reminder_offset", offset.toString())
            _uiState.update { it.copy(defaultReminderOffset = offset) }
        }
    }

    fun generateBackup() {
        viewModelScope.launch {
            val categories = repository.getCategories().first()
            val tasks = repository.getTasks().first()
            val occurrences = repository.getAllOccurrences().first()
            
            val backupData = BackupData(
                categories = categories,
                tasks = tasks,
                occurrences = occurrences,
                settings = mapOf(
                    "theme" to _uiState.value.theme,
                    "time_format" to _uiState.value.timeFormat,
                    "week_start" to _uiState.value.weekStart,
                    "notifications_enabled" to _uiState.value.notificationsEnabled.toString(),
                    "default_reminder_offset" to _uiState.value.defaultReminderOffset.toString()
                )
            )
            val json = backupService.exportBackup(backupData)
            _uiState.update { it.copy(backupJson = json, isExportSuccess = true) }
        }
    }

    fun importBackup(jsonString: String) {
        viewModelScope.launch {
            val backupData = backupService.importBackup(jsonString)
            if (backupData != null) {
                if (backupData.categories.isNotEmpty()) {
                    repository.saveCategories(backupData.categories)
                }
                if (backupData.tasks.isNotEmpty()) {
                    repository.saveTasks(backupData.tasks)
                }
                if (backupData.occurrences.isNotEmpty()) {
                    repository.saveOccurrences(backupData.occurrences)
                }
                backupData.settings.forEach { (k, v) ->
                    repository.saveSetting(k, v)
                }
                _uiState.update { it.copy(importMessage = "Data successfully imported!") }
            } else {
                _uiState.update { it.copy(importMessage = "Invalid backup format.") }
            }
        }
    }

    fun clearImportMessage() {
        _uiState.update { it.copy(importMessage = null, backupJson = null) }
    }

    fun resetAllData() {
        viewModelScope.launch {
            val tasks = repository.getTasks().first()
            tasks.forEach { repository.deleteTask(it) }
            val categories = repository.getCategories().first()
            categories.forEach { repository.deleteCategory(it) }
            
            // Re-seed default clean categories
            repository.saveCategory(Category(name = "Health", icon = "fitness", color = 0xFF10B981.toInt()))
            repository.saveCategory(Category(name = "Deep Work", icon = "code", color = 0xFF3B82F6.toInt()))
            repository.saveCategory(Category(name = "Personal", icon = "person", color = 0xFF8B5CF6.toInt()))
        }
    }

    class Factory(
        private val repository: TaskRepository,
        private val backupService: BackupService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, backupService) as T
        }
    }
}
