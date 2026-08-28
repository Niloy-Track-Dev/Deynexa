package com.niloy.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niloy.domain.model.AppQualityRating
import com.niloy.domain.model.InstalledAppInfo
import com.niloy.domain.repository.DiagnosticRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppClassificationUiState(
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val appCategories: List<com.niloy.domain.model.AppCategory> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedAppToEdit: InstalledAppInfo? = null
)

class AppClassificationViewModel(
    private val diagnosticRepository: DiagnosticRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppClassificationUiState())
    val uiState: StateFlow<AppClassificationUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
        observeCategories()
    }

    private fun observeCategories() {
        diagnosticRepository.getAppCategories().onEach { categories ->
            _uiState.update { it.copy(appCategories = categories) }
        }.launchIn(viewModelScope)
    }

    fun saveAppCategory(name: String, isProductive: Boolean) {
        viewModelScope.launch {
            diagnosticRepository.saveAppCategory(com.niloy.domain.model.AppCategory(name = name, isProductive = isProductive))
        }
    }

    fun deleteAppCategory(category: com.niloy.domain.model.AppCategory) {
        viewModelScope.launch {
            diagnosticRepository.deleteAppCategory(category)
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val apps = diagnosticRepository.getInstalledApps()
                _uiState.value = _uiState.value.copy(
                    installedApps = apps,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectAppToEdit(app: InstalledAppInfo?) {
        _uiState.value = _uiState.value.copy(selectedAppToEdit = app)
    }

    fun saveAppClassification(
        packageName: String,
        appName: String,
        categories: List<String>,
        qualityRating: AppQualityRating
    ) {
        viewModelScope.launch {
            diagnosticRepository.saveClassification(packageName, appName, categories, qualityRating)
            loadInstalledApps()
            _uiState.value = _uiState.value.copy(selectedAppToEdit = null)
        }
    }

    class Factory(
        private val diagnosticRepository: DiagnosticRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppClassificationViewModel(diagnosticRepository) as T
        }
    }
}
