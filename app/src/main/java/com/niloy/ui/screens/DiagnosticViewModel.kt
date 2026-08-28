package com.niloy.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niloy.domain.model.AppQualityRating
import com.niloy.domain.model.AppUsageInfo
import com.niloy.domain.model.DiagnosticSummary
import com.niloy.domain.repository.DiagnosticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val selectedAppDetail: AppUsageInfo? = null
)

class DiagnosticViewModel(
    private val diagnosticRepository: DiagnosticRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticUiState())
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoadData()
    }

    fun checkPermissionAndLoadData() {
        val granted = diagnosticRepository.isUsagePermissionGranted()
        _uiState.value = _uiState.value.copy(isPermissionGranted = granted)
        if (granted) {
            loadSummaryForCurrentPeriod()
        }
    }

    fun setPeriodTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedPeriodTab = tab)
        loadSummaryForCurrentPeriod()
    }

    fun setCustomDateRange(start: LocalDate, end: LocalDate) {
        val validEnd = if (end.isBefore(start)) start else end
        _uiState.value = _uiState.value.copy(
            customStartDate = start,
            customEndDate = validEnd,
            selectedPeriodTab = 3
        )
        loadSummaryForCurrentPeriod()
    }

    fun selectAppDetail(app: AppUsageInfo?) {
        _uiState.value = _uiState.value.copy(selectedAppDetail = app)
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
            // update selected app detail if open
            _uiState.value.selectedAppDetail?.let { current ->
                if (current.packageName == packageName) {
                    val updatedProdType = when {
                        qualityRating == AppQualityRating.VERY_GOOD || qualityRating == AppQualityRating.GOOD -> com.niloy.domain.model.ProductivityType.PRODUCTIVE
                        qualityRating == AppQualityRating.BAD || qualityRating == AppQualityRating.VERY_BAD || qualityRating == AppQualityRating.NOT_GOOD -> com.niloy.domain.model.ProductivityType.NON_PRODUCTIVE
                        categories.contains(com.niloy.domain.model.AppCategories.EDUCATION) || categories.contains(com.niloy.domain.model.AppCategories.STUDY_TIMER) || categories.contains(com.niloy.domain.model.AppCategories.PRODUCTIVITY) -> com.niloy.domain.model.ProductivityType.PRODUCTIVE
                        categories.contains(com.niloy.domain.model.AppCategories.SOCIAL_MEDIA) || categories.contains(com.niloy.domain.model.AppCategories.ENTERTAINMENT) || categories.contains(com.niloy.domain.model.AppCategories.GAMES) -> com.niloy.domain.model.ProductivityType.NON_PRODUCTIVE
                        else -> com.niloy.domain.model.ProductivityType.NEUTRAL
                    }
                    _uiState.value = _uiState.value.copy(
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

    private fun loadSummaryForCurrentPeriod() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

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

                val summary = diagnosticRepository.getUsageSummary(startMillis, endMillis)
                _uiState.value = _uiState.value.copy(
                    summary = summary,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
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
