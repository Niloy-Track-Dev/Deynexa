package com.niloy.ui.screens

import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.niloy.ui.theme.AccentPrimary
import com.niloy.ui.theme.StateCompleted
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Usage Diagnostics",
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
                    IconButton(onClick = onNavigateToClassifications) {
                        Icon(Icons.Outlined.Category, contentDescription = "Classifications")
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
            }
        }
    }

    // App Detail Dialog
    uiState.selectedAppDetail?.let { app ->
        val dynamicCategoryNames = (uiState.appCategories.map { it.name } + AppCategories.ALL_CATEGORIES + app.categories).distinct()
        AppDetailDialog(
            appInfo = app,
            availableCategories = dynamicCategoryNames,
            onDismiss = { viewModel.selectAppDetail(null) },
            onSaveClassification = { pkg, name, cats, rating ->
                viewModel.updateAppClassification(pkg, name, cats, rating)
            },
            onAddCustomCategory = { newCatName ->
                viewModel.saveAppCategory(newCatName, isProductive = true)
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
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiagnosticKpiCard(
                            title = "Focentra Focus",
                            value = formatDurationMillis(summary.focentraFocusMillis),
                            subtitle = "Verified sessions",
                            icon = Icons.Outlined.Psychology,
                            iconColor = AccentPrimary,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Screen Time Trend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Compact Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = StateCompleted, label = "Productive")
                    LegendItem(color = StateSkipped, label = "Distracting")
                }
            }

            AppUsageBarChart(points = points)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun AppUsageBarChart(points: List<DailyUsagePoint>) {
    val displayPoints = if (points.size > 7) points.takeLast(7) else points
    val maxMillis = (displayPoints.maxOfOrNull { it.totalMillis } ?: 1L).coerceAtLeast(60000L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        displayPoints.forEach { point ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Top duration label
                Text(
                    text = if (point.totalMillis > 0) formatShortDuration(point.totalMillis) else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                val totalRatio = (point.totalMillis.toFloat() / maxMillis.toFloat()).coerceIn(0f, 1f)
                val barHeight = (80.dp * totalRatio).coerceAtLeast(if (point.totalMillis > 0) 8.dp else 4.dp)

                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (point.totalMillis > 0) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (point.neutralMillis > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(point.neutralMillis.toFloat().coerceAtLeast(0.001f))
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                )
                            }
                            if (point.nonProductiveMillis > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(point.nonProductiveMillis.toFloat().coerceAtLeast(0.001f))
                                        .background(StateSkipped)
                                )
                            }
                            if (point.productiveMillis > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(point.productiveMillis.toFloat().coerceAtLeast(0.001f))
                                        .background(StateCompleted)
                                )
                            }
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

private fun formatShortDuration(millis: Long): String {
    if (millis <= 0L) return "0m"
    val totalMinutes = millis / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
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
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onSaveClassification: (String, String, List<String>, AppQualityRating) -> Unit,
    onAddCustomCategory: (String) -> Unit
) {
    var selectedCategories by remember { mutableStateOf(appInfo.categories.toSet()) }
    var selectedRating by remember { mutableStateOf(appInfo.qualityRating) }
    var showAddCategoryInput by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIconImage(packageName = appInfo.packageName, modifier = Modifier.size(36.dp))
                Text(appInfo.appName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Package: ${appInfo.packageName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Productivity Rating", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppQualityRating.entries.forEach { rating ->
                            FilterChip(
                                selected = selectedRating == rating,
                                onClick = { selectedRating = rating },
                                label = { Text(rating.label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Categories", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { showAddCategoryInput = !showAddCategoryInput },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = if (showAddCategoryInput) Icons.Outlined.Close else Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showAddCategoryInput) "Cancel" else "Add Custom", fontSize = 12.sp)
                        }
                    }

                    if (showAddCategoryInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                placeholder = { Text("Category name...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    val trimmed = newCategoryName.trim()
                                    if (trimmed.isNotBlank()) {
                                        onAddCustomCategory(trimmed)
                                        selectedCategories = selectedCategories + trimmed
                                        newCategoryName = ""
                                        showAddCategoryInput = false
                                    }
                                },
                                enabled = newCategoryName.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Add", fontSize = 12.sp)
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableCategories.forEach { category ->
                            FilterChip(
                                selected = selectedCategories.contains(category),
                                onClick = {
                                    selectedCategories = if (selectedCategories.contains(category)) {
                                        selectedCategories - category
                                    } else {
                                        selectedCategories + category
                                    }
                                },
                                label = { Text(category, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveClassification(appInfo.packageName, appInfo.appName, selectedCategories.toList(), selectedRating)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Changes")
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
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        color = Color.Transparent
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Usage Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Daynexa needs 'Usage Access' permission to diagnostic which apps you use most. This data stays 100% on your device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onGrantPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Grant Usage Access", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatDurationMillis(millis: Long): String {
    val totalMinutes = millis / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
