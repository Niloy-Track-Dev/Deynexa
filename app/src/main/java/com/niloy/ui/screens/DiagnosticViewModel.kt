package com.niloy.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niloy.domain.model.*
import com.niloy.domain.repository.DiagnosticRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DiagnosticUiState(
    val isPermissionGranted: Boolean = false,
    val selectedPeriodTab: Int = 1, // 0: Today, 1: Weekly, 2: Monthly, 3: Custom
    val customStartDate: LocalDate = LocalDate.now().minusDays(7),
    val customEndDate: LocalDate = LocalDate.now(),
    val summary: DiagnosticSummary? = null,
    val isLoading: Boolean = false,
    val selectedAppDetail: AppUsageInfo? = null,
    val selectedCategoryFilter: String? = null,
    val appCategories: List<AppCategory> = emptyList()
)

class DiagnosticViewModel(
    private val diagnosticRepository: DiagnosticRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticUiState())
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    init {
        observeCategories()
        checkPermissionAndLoadData()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            diagnosticRepository.getAppCategories().collect { cats ->
                _uiState.update { it.copy(appCategories = cats) }
            }
        }
    }

    fun saveAppCategory(name: String, isProductive: Boolean) {
        viewModelScope.launch {
            diagnosticRepository.saveAppCategory(com.niloy.domain.model.AppCategory(name = name, isProductive = isProductive))
        }
    }

    fun checkPermissionAndLoadData() {
        val granted = diagnosticRepository.isUsagePermissionGranted()
        _uiState.update { it.copy(isPermissionGranted = granted) }
        if (granted) {
            loadSummaryForCurrentPeriod()
        }
    }

    fun setPeriodTab(tab: Int) {
        _uiState.update { it.copy(selectedPeriodTab = tab) }
        loadSummaryForCurrentPeriod()
    }

    fun setCustomDateRange(start: LocalDate, end: LocalDate) {
        val validEnd = if (end.isBefore(start)) start else end
        _uiState.update {
            it.copy(
                customStartDate = start,
                customEndDate = validEnd,
                selectedPeriodTab = 3
            )
        }
        loadSummaryForCurrentPeriod()
    }

    fun setCategoryFilter(category: String?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun selectAppDetail(app: AppUsageInfo?) {
        _uiState.update { it.copy(selectedAppDetail = app) }
    }

    fun updateAppClassification(
        packageName: String,
        appName: String,
        categories: List<String>,
        qualityRating: AppQualityRating
    ) {
        viewModelScope.launch {
            diagnosticRepository.saveClassification(packageName, appName, categories, qualityRating)
            loadSummaryForCurrentPeriod()
            _uiState.value.selectedAppDetail?.let { current ->
                if (current.packageName == packageName) {
                    val updatedProdType = when {
                        qualityRating == AppQualityRating.VERY_GOOD || qualityRating == AppQualityRating.GOOD -> ProductivityType.PRODUCTIVE
                        qualityRating == AppQualityRating.BAD || qualityRating == AppQualityRating.VERY_BAD || qualityRating == AppQualityRating.NOT_GOOD -> ProductivityType.NON_PRODUCTIVE
                        categories.contains(AppCategories.EDUCATION) || categories.contains(AppCategories.STUDY_TIMER) || categories.contains(AppCategories.PRODUCTIVITY) -> ProductivityType.PRODUCTIVE
                        categories.contains(AppCategories.SOCIAL_MEDIA) || categories.contains(AppCategories.ENTERTAINMENT) || categories.contains(AppCategories.GAMES) -> ProductivityType.NON_PRODUCTIVE
                        else -> ProductivityType.NEUTRAL
                    }
                    _uiState.update {
                        it.copy(
                            selectedAppDetail = current.copy(
                                categories = categories,
                                qualityRating = qualityRating,
                                productivityType = updatedProdType
                            )
                        )
                    }
                }
            }
        }
    }

    fun loadSummaryForCurrentPeriod() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val (startMillis, endMillis) = when (_uiState.value.selectedPeriodTab) {
                    0 -> {
                        // Today
                        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val end = System.currentTimeMillis()
                        Pair(start, end)
                    }
                    1 -> {
                        // Weekly (last 7 days)
                        val start = LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val end = System.currentTimeMillis()
                        Pair(start, end)
                    }
                    2 -> {
                        // Monthly (current month start)
                        val start = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val end = System.currentTimeMillis()
                        Pair(start, end)
                    }
                    else -> {
                        // Custom range
                        val start = _uiState.value.customStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val end = _uiState.value.customEndDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
                        Pair(start, end)
                    }
                }

                val appSummary = diagnosticRepository.getUsageSummary(startMillis, endMillis)

                _uiState.update {
                    it.copy(
                        summary = appSummary,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    class Factory(
        private val diagnosticRepository: DiagnosticRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiagnosticViewModel(diagnosticRepository) as T
        }
    }
}

