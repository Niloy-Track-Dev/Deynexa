package com.niloy.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niloy.data.local.entity.DomainRuleEntity
import com.niloy.domain.model.*
import com.niloy.domain.repository.DiagnosticRepository
import com.niloy.domain.repository.WebsiteDiagnosticRepository
import com.niloy.domain.service.WebsiteDiagnosticVpnService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DiagnosticUiState(
    val selectedDiagnosticSection: Int = 0, // 0: Apps & Usage, 1: Website Diagnostics
    val isPermissionGranted: Boolean = false,
    val isVpnRunning: Boolean = false,
    val selectedPeriodTab: Int = 1, // 0: Today, 1: Weekly, 2: Monthly, 3: Custom
    val customStartDate: LocalDate = LocalDate.now().minusDays(7),
    val customEndDate: LocalDate = LocalDate.now(),
    val summary: DiagnosticSummary? = null,
    val websiteSummary: WebsiteDiagnosticSummary? = null,
    val rules: List<DomainRuleEntity> = emptyList(),
    val isLoading: Boolean = false,
    val selectedAppDetail: AppUsageInfo? = null,
    val selectedDomainDetail: DomainClassification? = null,
    val showRulesDialog: Boolean = false,
    val domainSearchQuery: String = "",
    val selectedCategoryFilter: String? = null
)

class DiagnosticViewModel(
    private val diagnosticRepository: DiagnosticRepository,
    private val websiteDiagnosticRepository: WebsiteDiagnosticRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticUiState())
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoadData()
        observeVpnStatus()
        observeRules()
    }

    private fun observeVpnStatus() {
        viewModelScope.launch {
            WebsiteDiagnosticVpnService.isRunning.collect { running ->
                _uiState.update { it.copy(isVpnRunning = running) }
            }
        }
    }

    private fun observeRules() {
        viewModelScope.launch {
            websiteDiagnosticRepository.getAllRules().collect { rulesList ->
                _uiState.update { it.copy(rules = rulesList) }
            }
        }
    }

    fun setDiagnosticSection(section: Int) {
        _uiState.update { it.copy(selectedDiagnosticSection = section) }
        loadSummaryForCurrentPeriod()
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

    fun setDomainSearchQuery(query: String) {
        _uiState.update { it.copy(domainSearchQuery = query) }
    }

    fun setCategoryFilter(category: String?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun selectAppDetail(app: AppUsageInfo?) {
        _uiState.update { it.copy(selectedAppDetail = app) }
    }

    fun selectDomainDetail(domain: DomainClassification?) {
        _uiState.update { it.copy(selectedDomainDetail = domain) }
    }

    fun setShowRulesDialog(show: Boolean) {
        _uiState.update { it.copy(showRulesDialog = show) }
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

    fun updateWebsiteClassification(
        domain: String,
        category: String,
        qualityRating: WebsiteQualityRating
    ) {
        viewModelScope.launch {
            websiteDiagnosticRepository.saveClassification(domain, category, qualityRating, isUserOverride = true)
            loadSummaryForCurrentPeriod()
            _uiState.value.selectedDomainDetail?.let { current ->
                if (current.domain == domain) {
                    val prod = KnownDomainsDictionary.determineProductivity(qualityRating, category)
                    _uiState.update {
                        it.copy(
                            selectedDomainDetail = current.copy(
                                category = category,
                                qualityRating = qualityRating,
                                productivityType = prod,
                                isUserOverride = true
                            )
                        )
                    }
                }
            }
        }
    }

    fun addDomainRule(
        pattern: String,
        ruleType: DomainRuleType,
        category: String,
        rating: WebsiteQualityRating
    ) {
        viewModelScope.launch {
            websiteDiagnosticRepository.addRule(pattern, ruleType, category, rating)
            loadSummaryForCurrentPeriod()
        }
    }

    fun deleteDomainRule(id: Long) {
        viewModelScope.launch {
            websiteDiagnosticRepository.deleteRule(id)
            loadSummaryForCurrentPeriod()
        }
    }

    fun toggleDomainRule(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            websiteDiagnosticRepository.toggleRule(id, isEnabled)
            loadSummaryForCurrentPeriod()
        }
    }

    // Helper to simulate/test domain activity
    fun simulateSampleWebsiteVisits() {
        viewModelScope.launch {
            val sampleDomains = listOf(
                "github.com",
                "wikipedia.org",
                "coursera.org",
                "youtube.com",
                "stackoverflow.com",
                "notion.so",
                "reddit.com",
                "news.ycombinator.com"
            )
            websiteDiagnosticRepository.recordDomainVisits(sampleDomains)
            loadSummaryForCurrentPeriod()
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
                val websiteSummary = websiteDiagnosticRepository.getSummary(startMillis, endMillis)

                _uiState.update {
                    it.copy(
                        summary = appSummary,
                        websiteSummary = websiteSummary,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    class Factory(
        private val diagnosticRepository: DiagnosticRepository,
        private val websiteDiagnosticRepository: WebsiteDiagnosticRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiagnosticViewModel(diagnosticRepository, websiteDiagnosticRepository) as T
        }
    }
}
