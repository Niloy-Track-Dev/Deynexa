package com.niloy.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niloy.domain.model.Category
import com.niloy.domain.repository.TaskRepository
import com.niloy.domain.service.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import java.time.DayOfWeek

data class SettingsUiState(
    val isOnboardingCompleted: Boolean = false,
    val theme: String = "SYSTEM",
    val timeFormat: String = "24H",
    val weekStart: String = "MONDAY",
    val weekendDays: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
    val dayBoundary: String = "12:00 AM (Midnight)",
    val notificationsEnabled: Boolean = true,
    val defaultReminderOffset: Int = 0,
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportDialogType: String? = null, // "FULL" or "FOCENTRA"
    val generatedExportJson: String? = null,
    val validationResult: BackupValidationResult? = null,
    val importFeedbackMessage: String? = null,
    val isImportSuccess: Boolean = false,
    val focentraStatus: com.niloy.domain.model.FocentraIntegrationStatus? = null
)

class SettingsViewModel(
    private val repository: TaskRepository,
    private val backupService: BackupService,
    private val focentraIntegrationManager: com.niloy.domain.service.FocentraIntegrationManager,
    private val dataPortabilityManager: DataPortabilityManager
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
                val weekendStr = repository.getSetting("weekend_days") ?: "SATURDAY,SUNDAY"
                val weekendDays = if (weekendStr.isNotBlank()) {
                    weekendStr.split(",").mapNotNull {
                        try { DayOfWeek.valueOf(it.trim()) } catch (e: Exception) { null }
                    }.toSet()
                } else setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

                val notificationsEnabled = repository.getSetting("notifications_enabled")?.toBoolean() ?: true
                val defaultReminderOffset = repository.getSetting("default_reminder_offset")?.toIntOrNull() ?: 0
                
                _uiState.update { it.copy(
                    isOnboardingCompleted = onboarding,
                    theme = theme,
                    timeFormat = timeFormat,
                    weekStart = weekStart,
                    weekendDays = weekendDays,
                    notificationsEnabled = notificationsEnabled,
                    defaultReminderOffset = defaultReminderOffset,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        viewModelScope.launch {
            focentraIntegrationManager.integrationStatus.collect { status ->
                _uiState.update { it.copy(focentraStatus = status) }
            }
        }
    }

    fun connectFocentra(consentGiven: Boolean) {
        viewModelScope.launch {
            focentraIntegrationManager.setConsent(consentGiven)
            if (consentGiven) {
                focentraIntegrationManager.setConnected(true)
            }
        }
    }

    fun disconnectFocentra() {
        viewModelScope.launch {
            focentraIntegrationManager.setConnected(false)
        }
    }

    fun clearFocentraData() {
        viewModelScope.launch {
            focentraIntegrationManager.clearFocentraData()
        }
    }

    fun syncFocentra() {
        viewModelScope.launch {
            focentraIntegrationManager.syncSessions()
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

    fun toggleWeekendDay(day: DayOfWeek) {
        viewModelScope.launch {
            val current = _uiState.value.weekendDays
            val updated = if (current.contains(day)) {
                if (current.size > 1) current - day else current // Keep at least 1 weekend day
            } else {
                if (current.size >= 2) {
                    // Replace the first item to maintain max 2
                    current.drop(1).toSet() + day
                } else {
                    current + day
                }
            }
            val settingStr = updated.joinToString(",") { it.name }
            repository.saveSetting("weekend_days", settingStr)
            _uiState.update { it.copy(weekendDays = updated) }
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

    fun generateFullBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val backupData = dataPortabilityManager.generateFullBackup()
                val json = backupService.exportBackup(backupData)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportDialogType = "FULL",
                        generatedExportJson = json
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        importFeedbackMessage = "Export failed: ${e.localizedMessage}",
                        isImportSuccess = false
                    )
                }
            }
        }
    }

    fun generateFocentraExport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val bundle = dataPortabilityManager.generateFocentraExportBundle()
                val json = backupService.exportFocentraBundle(bundle)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportDialogType = "FOCENTRA",
                        generatedExportJson = json
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        importFeedbackMessage = "Export failed: ${e.localizedMessage}",
                        isImportSuccess = false
                    )
                }
            }
        }
    }

    fun validateImportJson(rawJson: String) {
        val result = backupService.validateBackup(rawJson)
        _uiState.update { it.copy(validationResult = result) }
    }

    fun executeImport(mode: ImportMode) {
        val validation = _uiState.value.validationResult ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            val success = when (validation) {
                is BackupValidationResult.ValidFull -> {
                    dataPortabilityManager.restoreFullBackup(validation.data, mode)
                }
                is BackupValidationResult.ValidFocentra -> {
                    dataPortabilityManager.restoreFocentraBackup(validation.bundle, mode)
                }
                is BackupValidationResult.Invalid -> false
            }

            _uiState.update {
                it.copy(
                    isImporting = false,
                    validationResult = null,
                    isImportSuccess = success,
                    importFeedbackMessage = if (success) {
                        if (mode == ImportMode.REPLACE) "Database replaced and restored successfully!"
                        else "Backup data merged seamlessly without loss!"
                    } else {
                        "Failed to restore backup data."
                    }
                )
            }
        }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(exportDialogType = null, generatedExportJson = null) }
    }

    fun dismissValidationDialog() {
        _uiState.update { it.copy(validationResult = null) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(importFeedbackMessage = null) }
    }

    fun resetAllData() {
        viewModelScope.launch {
            val tasks = repository.getTasks().first()
            tasks.forEach { repository.deleteTask(it) }
            val categories = repository.getCategories().first()
            categories.forEach { repository.deleteCategory(it) }
            focentraIntegrationManager.clearFocentraData()
            
            // Re-seed default clean categories
            repository.saveCategory(Category(name = "Health", icon = "fitness", color = 0xFF10B981.toInt()))
            repository.saveCategory(Category(name = "Deep Work", icon = "code", color = 0xFF3B82F6.toInt()))
            repository.saveCategory(Category(name = "Personal", icon = "person", color = 0xFF8B5CF6.toInt()))
        }
    }

    class Factory(
        private val repository: TaskRepository,
        private val backupService: BackupService,
        private val focentraIntegrationManager: com.niloy.domain.service.FocentraIntegrationManager,
        private val dataPortabilityManager: DataPortabilityManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, backupService, focentraIntegrationManager, dataPortabilityManager) as T
        }
    }
}
