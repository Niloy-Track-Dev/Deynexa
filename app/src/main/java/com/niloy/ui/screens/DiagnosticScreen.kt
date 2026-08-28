package com.niloy.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.VpnService
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.niloy.domain.model.*
import com.niloy.domain.service.WebsiteDiagnosticVpnService
import com.niloy.ui.theme.AccentPrimary
import com.niloy.ui.theme.StateCompleted
import com.niloy.ui.theme.StatePending
import com.niloy.ui.theme.StateSkipped
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    viewModel: DiagnosticViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToClassifications: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // VPN Preparation Launcher
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            WebsiteDiagnosticVpnService.start(context)
        }
    }

    // Auto refresh when returning from Android System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndLoadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showClearDataConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.selectedDiagnosticSection == 0) "App Diagnostics" else "Website Diagnostics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.selectedDiagnosticSection == 0) {
                        IconButton(onClick = onNavigateToClassifications) {
                            Icon(Icons.Outlined.Category, contentDescription = "Classifications")
                        }
                    } else {
                        IconButton(onClick = { viewModel.setShowRulesDialog(true) }) {
                            Icon(Icons.Outlined.Rule, contentDescription = "Domain Rules")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Section Tabs: Apps vs Websites
                TabRow(
                    selectedTabIndex = uiState.selectedDiagnosticSection,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = uiState.selectedDiagnosticSection == 0,
                        onClick = { viewModel.setDiagnosticSection(0) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Apps & Usage", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = uiState.selectedDiagnosticSection == 1,
                        onClick = { viewModel.setDiagnosticSection(1) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Websites", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                if (uiState.selectedDiagnosticSection == 0) {
                    // Apps & Usage Diagnostics Section
                    if (!uiState.isPermissionGranted) {
                        PermissionExplanationView(
                            onGrantPermission = {
                                try {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                        )
                    } else if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        AppDiagnosticsContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            onSelectStartDate = { showStartDatePicker = true },
                            onSelectEndDate = { showEndDatePicker = true }
                        )
                    }
                } else {
                    // Website Diagnostics Section
                    WebsiteDiagnosticsContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        isVpnRunning = uiState.isVpnRunning,
                        onToggleVpn = {
                            if (uiState.isVpnRunning) {
                                WebsiteDiagnosticVpnService.stop(context)
                            } else {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnLauncher.launch(prepareIntent)
                                } else {
                                    WebsiteDiagnosticVpnService.start(context)
                                }
                            }
                        },
                        onSelectStartDate = { showStartDatePicker = true },
                        onSelectEndDate = { showEndDatePicker = true },
                        onRequestClearData = { showClearDataConfirmDialog = true }
                    )
                }
            }
        }
    }

    // App Detail Dialog
    uiState.selectedAppDetail?.let { app ->
        AppDetailDialog(
            appInfo = app,
            onDismiss = { viewModel.selectAppDetail(null) },
            onSaveClassification = { pkg, name, cats, rating ->
                viewModel.updateAppClassification(pkg, name, cats, rating)
            }
        )
    }

    // Website Domain Detail Dialog
    uiState.selectedDomainDetail?.let { domain ->
        WebsiteDomainDetailDialog(
            domainClassification = domain,
            onDismiss = { viewModel.selectDomainDetail(null) },
            onSave = { d, cat, rating ->
                viewModel.updateWebsiteClassification(d, cat, rating)
            }
        )
    }

    // Domain Rules Dialog
    if (uiState.showRulesDialog) {
        WebsiteRulesDialog(
            rules = uiState.rules,
            onDismiss = { viewModel.setShowRulesDialog(false) },
            onAddRule = { pattern, type, cat, rating ->
                viewModel.addDomainRule(pattern, type, cat, rating)
            },
            onDeleteRule = { viewModel.deleteDomainRule(it) },
            onToggleRule = { id, enabled -> viewModel.toggleDomainRule(id, enabled) }
        )
    }

    // Clear Website Data Confirmation Dialog
    if (showClearDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirmDialog = false },
            icon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear Website Diagnostics") },
            text = { Text("Choose whether to clear today's website records or reset all recorded website diagnostic data.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllWebsiteData()
                        showClearDataConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.clearTodayWebsiteData()
                        showClearDataConfirmDialog = false
                    }
                ) {
                    Text("Clear Today Only")
                }
            }
        )
    }

    // Start Date Picker Dialog
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.customStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.setCustomDateRange(date, uiState.customEndDate)
                    }
                    showStartDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // End Date Picker Dialog
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.customEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.setCustomDateRange(uiState.customStartDate, date)
                    }
                    showEndDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ----------------------------------------------------
// APP DIAGNOSTICS CONTENT
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDiagnosticsContent(
    uiState: DiagnosticUiState,
    viewModel: DiagnosticViewModel,
    onSelectStartDate: () -> Unit,
    onSelectEndDate: () -> Unit
) {
    val summary = uiState.summary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period Selector
        item {
            PeriodSelectorRow(
                selectedPeriodTab = uiState.selectedPeriodTab,
                onSelectPeriod = { viewModel.setPeriodTab(it) }
            )
        }

        // Custom Date Range Pickers (Visible if Custom selected)
        if (uiState.selectedPeriodTab == 3) {
            item {
                CustomDateRangeCard(
                    startDate = uiState.customStartDate,
                    endDate = uiState.customEndDate,
                    onSelectStartDate = onSelectStartDate,
                    onSelectEndDate = onSelectEndDate
                )
            }
        }

        if (summary == null || summary.totalUsageTimeMillis == 0L) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.HourglassEmpty,
                    title = "No app usage data recorded",
                    subtitle = "App usage data for this period is unavailable or zero."
                )
            }
        } else {
            // Diagnostic KPI Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiagnosticKpiCard(
                            title = "Total Screen Time",
                            value = formatDurationMillis(summary.totalUsageTimeMillis),
                            subtitle = "${summary.startDateLabel} - ${summary.endDateLabel}",
                            icon = Icons.Outlined.Timer,
                            iconColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticKpiCard(
                            title = "Productive Time",
                            value = formatDurationMillis(summary.productiveTimeMillis),
                            subtitle = "${summary.productiveAppsCount} apps",
                            icon = Icons.Outlined.CheckCircle,
                            iconColor = StateCompleted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiagnosticKpiCard(
                            title = "Productivity Ratio",
                            value = "${summary.productivityRate.toInt()}%",
                            subtitle = if (summary.productivityRate >= 50) "Optimal" else "Needs Improvement",
                            icon = Icons.Outlined.TrendingUp,
                            iconColor = if (summary.productivityRate >= 50) StateCompleted else StateSkipped,
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticKpiCard(
                            title = "Non-Productive",
                            value = formatDurationMillis(summary.nonProductiveTimeMillis),
                            subtitle = "${summary.nonProductiveAppsCount} apps",
                            icon = Icons.Outlined.Cancel,
                            iconColor = StateSkipped,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Most Used App Highlight
            summary.mostUsedApp?.let { topApp ->
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AppIconImage(packageName = topApp.packageName, modifier = Modifier.size(48.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Most Used App",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = topApp.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${formatDurationMillis(topApp.totalTimeInForegroundMillis)} (${topApp.percentageOfTotal.toInt()}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Daily Usage Trend Chart
            if (summary.dailyTrend.isNotEmpty()) {
                item {
                    UsageTrendCard(points = summary.dailyTrend)
                }
            }

            // Category Breakdown Card
            if (summary.categoryBreakdown.isNotEmpty()) {
                item {
                    CategoryBreakdownCard(breakdown = summary.categoryBreakdown)
                }
            }

            // App Usage List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Usage Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.topApps.size} apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // App Usage Items
            items(summary.topApps, key = { it.packageName }) { app ->
                AppUsageListItem(
                    app = app,
                    onClick = { viewModel.selectAppDetail(app) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// WEBSITE DIAGNOSTICS CONTENT
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebsiteDiagnosticsContent(
    uiState: DiagnosticUiState,
    viewModel: DiagnosticViewModel,
    isVpnRunning: Boolean,
    onToggleVpn: () -> Unit,
    onSelectStartDate: () -> Unit,
    onSelectEndDate: () -> Unit,
    onRequestClearData: () -> Unit
) {
    val summary = uiState.websiteSummary
    val filteredDomains = remember(summary, uiState.domainSearchQuery, uiState.selectedCategoryFilter) {
        if (summary == null) emptyList()
        else {
            summary.topDomains.filter { domain ->
                val matchesSearch = uiState.domainSearchQuery.isBlank() || domain.domain.contains(uiState.domainSearchQuery.trim(), ignoreCase = true)
                val matchesCategory = uiState.selectedCategoryFilter == null || domain.category == uiState.selectedCategoryFilter
                matchesSearch && matchesCategory
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // VPN Diagnostic Control Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isVpnRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (isVpnRunning) StateCompleted.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isVpnRunning) StateCompleted.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isVpnRunning) Icons.Outlined.Shield else Icons.Outlined.VpnLock,
                                        contentDescription = null,
                                        tint = if (isVpnRunning) StateCompleted else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (isVpnRunning) "Diagnostics Active" else "Diagnostics Inactive",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isVpnRunning) "Monitoring visited domains locally" else "Tap below to enable local domain capture",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = onToggleVpn,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isVpnRunning) StateSkipped.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary,
                                contentColor = if (isVpnRunning) StateSkipped else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(if (isVpnRunning) "Stop" else "Start", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Privacy Shield Notice
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = StateCompleted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Privacy First: Daynexa records only domain names on-device. No HTTPS inspection, passwords, or cloud syncing.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quick Actions (Rules, Sample Simulate, Clear)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setShowRulesDialog(true) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Outlined.Rule, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rules", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.simulateSampleWebsiteVisits() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Outlined.Science, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sample Data", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = onRequestClearData,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = "Clear Data",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Period Selector
        item {
            PeriodSelectorRow(
                selectedPeriodTab = uiState.selectedPeriodTab,
                onSelectPeriod = { viewModel.setPeriodTab(it) }
            )
        }

        // Custom Date Range Pickers
        if (uiState.selectedPeriodTab == 3) {
            item {
                CustomDateRangeCard(
                    startDate = uiState.customStartDate,
                    endDate = uiState.customEndDate,
                    onSelectStartDate = onSelectStartDate,
                    onSelectEndDate = onSelectEndDate
                )
            }
        }

        if (summary == null || summary.totalVisits == 0) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.Language,
                    title = "No website diagnostic activity",
                    subtitle = "Start the Website Diagnostics service or tap 'Sample Data' above to preview metrics."
                )
            }
        } else {
            // Diagnostic KPI Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiagnosticKpiCard(
                            title = "Total Visits",
                            value = "${summary.totalVisits}",
                            subtitle = formatDurationMillis(summary.totalDurationMillis) + " estimated",
                            icon = Icons.Outlined.Visibility,
                            iconColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticKpiCard(
                            title = "Productive Web",
                            value = "${summary.productiveDomainsCount} domains",
                            subtitle = formatDurationMillis(summary.productiveTimeMillis),
                            icon = Icons.Outlined.CheckCircle,
                            iconColor = StateCompleted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiagnosticKpiCard(
                            title = "Productivity Ratio",
                            value = "${summary.productivityRate.toInt()}%",
                            subtitle = if (summary.productivityRate >= 50) "Good Ratio" else "Review Domains",
                            icon = Icons.Outlined.TrendingUp,
                            iconColor = if (summary.productivityRate >= 50) StateCompleted else StateSkipped,
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticKpiCard(
                            title = "Distracting Web",
                            value = "${summary.nonProductiveDomainsCount} domains",
                            subtitle = formatDurationMillis(summary.nonProductiveTimeMillis),
                            icon = Icons.Outlined.Cancel,
                            iconColor = StateSkipped,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Most Visited Domain Highlight
            summary.mostVisitedDomain?.let { topDomain ->
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectDomainDetail(topDomain) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Most Visited Domain",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = topDomain.domain,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${topDomain.visitCount} visits • ${topDomain.category} (${topDomain.qualityRating.label})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Daily Website Trend Chart
            if (summary.dailyTrend.isNotEmpty()) {
                item {
                    WebsiteTrendCard(points = summary.dailyTrend)
                }
            }

            // Category Breakdown Card
            if (summary.categoryBreakdown.isNotEmpty()) {
                item {
                    WebsiteCategoryBreakdownCard(breakdown = summary.categoryBreakdown)
                }
            }

            // Visited Domains Search and Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visited Domains",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredDomains.size} listed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = uiState.domainSearchQuery,
                        onValueChange = { viewModel.setDomainSearchQuery(it) },
                        placeholder = { Text("Search domain (e.g. github, reddit)...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.domainSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setDomainSearchQuery("") }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = uiState.selectedCategoryFilter == null,
                            onClick = { viewModel.setCategoryFilter(null) },
                            label = { Text("All") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        WebsiteCategories.ALL_CATEGORIES.forEach { category ->
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == category,
                                onClick = {
                                    if (uiState.selectedCategoryFilter == category) {
                                        viewModel.setCategoryFilter(null)
                                    } else {
                                        viewModel.setCategoryFilter(category)
                                    }
                                },
                                label = { Text(category, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Visited Domain List Items
            items(filteredDomains, key = { it.domain }) { domain ->
                WebsiteDomainListItem(
                    domain = domain,
                    onClick = { viewModel.selectDomainDetail(domain) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// SHARED REUSABLE COMPONENTS
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelectorRow(
    selectedPeriodTab: Int,
    onSelectPeriod: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selectedPeriodTab == 0,
            onClick = { onSelectPeriod(0) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
            modifier = Modifier.weight(1f)
        ) {
            Text("Today", style = MaterialTheme.typography.labelSmall)
        }
        SegmentedButton(
            selected = selectedPeriodTab == 1,
            onClick = { onSelectPeriod(1) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4),
            modifier = Modifier.weight(1f)
        ) {
            Text("Weekly", style = MaterialTheme.typography.labelSmall)
        }
        SegmentedButton(
            selected = selectedPeriodTab == 2,
            onClick = { onSelectPeriod(2) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4),
            modifier = Modifier.weight(1f)
        ) {
            Text("Monthly", style = MaterialTheme.typography.labelSmall)
        }
        SegmentedButton(
            selected = selectedPeriodTab == 3,
            onClick = { onSelectPeriod(3) },
            shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4),
            modifier = Modifier.weight(1f)
        ) {
            Text("Custom", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CustomDateRangeCard(
    startDate: LocalDate,
    endDate: LocalDate,
    onSelectStartDate: () -> Unit,
    onSelectEndDate: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onSelectStartDate,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("From: $startDate", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))

            OutlinedButton(
                onClick = onSelectEndDate,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("To: $endDate", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DiagnosticKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun UsageTrendCard(points: List<DailyUsagePoint>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Daily Screen Time Trend",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            AppUsageBarChart(points = points)
        }
    }
}

@Composable
private fun WebsiteTrendCard(points: List<WebsiteDailyPoint>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Daily Website Activity Trend",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            WebsiteUsageBarChart(points = points)
        }
    }
}

@Composable
private fun CategoryBreakdownCard(breakdown: List<CategoryUsageBreakdown>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Usage by App Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            breakdown.take(5).forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("${item.percentage.toInt()}% • ${formatDurationMillis(item.durationMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WebsiteCategoryBreakdownCard(breakdown: List<CategoryUsageBreakdown>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Usage by Website Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            breakdown.take(5).forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("${item.percentage.toInt()}% • ${item.count} visits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUsageListItem(
    app: AppUsageInfo,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIconImage(packageName = app.packageName, modifier = Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgeColor = when (app.productivityType) {
                        ProductivityType.PRODUCTIVE -> StateCompleted
                        ProductivityType.NON_PRODUCTIVE -> StateSkipped
                        ProductivityType.NEUTRAL -> MaterialTheme.colorScheme.outline
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = app.qualityRating.label,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = app.categories.firstOrNull() ?: "Uncategorized",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatDurationMillis(app.totalTimeInForegroundMillis), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("${app.percentageOfTotal.toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun WebsiteDomainListItem(
    domain: DomainClassification,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = domain.domain,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgeColor = when (domain.productivityType) {
                        WebsiteProductivityType.PRODUCTIVE -> StateCompleted
                        WebsiteProductivityType.NON_PRODUCTIVE -> StateSkipped
                        WebsiteProductivityType.NEUTRAL -> MaterialTheme.colorScheme.outline
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = domain.qualityRating.label,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = domain.category,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${domain.visitCount} visits", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(formatDurationMillis(domain.totalDurationMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AppUsageBarChart(points: List<DailyUsagePoint>) {
    val maxMillis = (points.maxOfOrNull { it.totalMillis } ?: 1L).coerceAtLeast(1L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        points.takeLast(7).forEach { point ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                val totalRatio = (point.totalMillis.toFloat() / maxMillis.toFloat()).coerceIn(0.1f, 1f)
                val chartHeight = 90.dp * totalRatio

                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(chartHeight)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                        if (point.productiveMillis > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction = (point.productiveMillis.toFloat() / point.totalMillis.toFloat()))
                                    .background(StateCompleted)
                            )
                        }
                        if (point.nonProductiveMillis > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction = (point.nonProductiveMillis.toFloat() / point.totalMillis.toFloat()))
                                    .background(StateSkipped)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = point.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WebsiteUsageBarChart(points: List<WebsiteDailyPoint>) {
    val maxMillis = (points.maxOfOrNull { it.totalMillis } ?: 1L).coerceAtLeast(1L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        points.takeLast(7).forEach { point ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                val totalRatio = (point.totalMillis.toFloat() / maxMillis.toFloat()).coerceIn(0.1f, 1f)
                val chartHeight = 90.dp * totalRatio

                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(chartHeight)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                        if (point.productiveMillis > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction = (point.productiveMillis.toFloat() / point.totalMillis.toFloat()))
                                    .background(StateCompleted)
                            )
                        }
                        if (point.nonProductiveMillis > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction = (point.nonProductiveMillis.toFloat() / point.totalMillis.toFloat()))
                                    .background(StateSkipped)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = point.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AppIconImage(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(packageName) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(packageName) {
        try {
            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            bitmap = drawable.toBitmap(96, 96).asImageBitmap()
        } catch (e: Exception) {
            bitmap = null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppDetailDialog(
    appInfo: AppUsageInfo,
    onDismiss: () -> Unit,
    onSaveClassification: (String, String, List<String>, AppQualityRating) -> Unit
) {
    var selectedCategories by remember { mutableStateOf(appInfo.categories.toSet()) }
    var selectedRating by remember { mutableStateOf(appInfo.qualityRating) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppIconImage(packageName = appInfo.packageName, modifier = Modifier.size(36.dp))
                Column {
                    Text(appInfo.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        formatDurationMillis(appInfo.totalTimeInForegroundMillis) + " active time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "App Quality Rating",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppQualityRating.values().filter { it != AppQualityRating.UNRATED }.forEach { rating ->
                        FilterChip(
                            selected = selectedRating == rating,
                            onClick = { selectedRating = rating },
                            label = { Text(rating.label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Text(
                    text = "Categories (Multiple Allowed)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppCategories.ALL_CATEGORIES.forEach { category ->
                        val isSelected = selectedCategories.contains(category)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategories = if (isSelected) {
                                    selectedCategories - category
                                } else {
                                    selectedCategories + category
                                }
                            },
                            label = { Text(category, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveClassification(
                        appInfo.packageName,
                        appInfo.appName,
                        selectedCategories.toList(),
                        selectedRating
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Classification")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PermissionExplanationView(onGrantPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Usage Access Required",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "To measure your daily productivity, categorize digital time, and correlate habits with planned tasks, Daynexa requires Android Usage Access permission.\n\nAll diagnostic calculations are computed 100% on-device and never leave your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onGrantPermission,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grant Usage Access", fontWeight = FontWeight.Bold)
        }
    }
}

fun formatDurationMillis(millis: Long): String {
    if (millis <= 0) return "0m"
    val totalMinutes = millis / 1000 / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
