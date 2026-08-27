package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.StateCompleted
import com.example.ui.theme.StatePending
import com.example.ui.theme.StateSkipped
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedPeriodTab by remember { mutableStateOf(1) } // 0: Today, 1: Week, 2: Month

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.last7DaysPoints.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Insights,
                title = "No statistics available yet",
                subtitle = "Start scheduling and completing daily tasks to unlock rich analytics and consistency heatmaps.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top KPI Metric Cards Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                title = "Current Streak",
                                value = "${uiState.currentStreak} Days",
                                subtitle = "Best: ${uiState.bestStreak} days",
                                icon = Icons.Outlined.LocalFireDepartment,
                                accentColor = Color(0xFFF97316),
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Success Rate",
                                value = "${(uiState.overallCompletionRate * 100).toInt()}%",
                                subtitle = "This month's pace",
                                icon = Icons.Outlined.CheckCircle,
                                accentColor = StateCompleted,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                title = "Best Day",
                                value = uiState.bestDayName,
                                subtitle = "Peak consistency",
                                icon = Icons.Outlined.Star,
                                accentColor = AccentPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Completed",
                                value = "${uiState.totalCompletedAllTime}",
                                subtitle = "All-time completions",
                                icon = Icons.Outlined.Timeline,
                                accentColor = Color(0xFF8B5CF6),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Completion Overview Tabbed Card
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Completion Breakdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Segmented period selector
                                SingleChoiceSegmentedButtonRow {
                                    SegmentedButton(
                                        selected = selectedPeriodTab == 0,
                                        onClick = { selectedPeriodTab = 0 },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                                    ) {
                                        Text("Today", style = MaterialTheme.typography.labelSmall)
                                    }
                                    SegmentedButton(
                                        selected = selectedPeriodTab == 1,
                                        onClick = { selectedPeriodTab = 1 },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                                    ) {
                                        Text("Week", style = MaterialTheme.typography.labelSmall)
                                    }
                                    SegmentedButton(
                                        selected = selectedPeriodTab == 2,
                                        onClick = { selectedPeriodTab = 2 },
                                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                                    ) {
                                        Text("Month", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            val activeStats = when (selectedPeriodTab) {
                                0 -> uiState.todayStats
                                1 -> uiState.weekStats
                                else -> uiState.monthStats
                            }

                            // Progress row with percentage
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${(activeStats.completionRate * 100).toInt()}% Done",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeStats.completionRate >= 1f && activeStats.total > 0) StateCompleted else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${activeStats.completed} of ${activeStats.total} tasks",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { activeStats.completionRate },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = if (activeStats.completionRate >= 1f && activeStats.total > 0) StateCompleted else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatCounterBox(
                                    label = "Completed",
                                    count = activeStats.completed,
                                    color = StateCompleted,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCounterBox(
                                    label = "Pending",
                                    count = activeStats.pending,
                                    color = StatePending,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCounterBox(
                                    label = "Skipped",
                                    count = activeStats.skipped,
                                    color = StateSkipped,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 7-Day Performance Chart
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Last 7 Days Performance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            WeeklyBarChart(points = uiState.last7DaysPoints)
                        }
                    }
                }

                // Consistency Heatmap Card
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Routine Consistency Heatmap",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "14 Weeks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HeatmapGrid(cells = uiState.heatmapCells)

                            // Heatmap Legend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Less",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                listOf(0, 1, 2, 3).forEach { lvl ->
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(getHeatmapColor(lvl))
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "More",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Categories Breakdown
                if (uiState.categoryStats.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Category Completion",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                uiState.categoryStats.forEach { catStat ->
                                    CategoryProgressRow(stat = catStat)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatCounterBox(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(points: List<DailyChartPoint>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                // Percentage or count text on top
                Text(
                    text = if (point.totalCount > 0) "${point.completedCount}/${point.totalCount}" else "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Bar track and fill
                val fillFraction = if (point.totalCount == 0) 0.05f else maxOf(point.completionRate, 0.05f)
                val animatedFraction by animateFloatAsState(
                    targetValue = fillFraction,
                    animationSpec = tween(durationMillis = 600),
                    label = "bar_fill"
                )

                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(70.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedFraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                when {
                                    point.totalCount == 0 -> Color.Transparent
                                    point.completionRate >= 1f -> StateCompleted
                                    point.completedCount > 0 -> MaterialTheme.colorScheme.primary
                                    else -> Color.Transparent
                                }
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Day Label
                Text(
                    text = point.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun HeatmapGrid(cells: List<HeatmapCell>) {
    // 14 columns of 7 days
    val columns = cells.chunked(7)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        columns.forEach { columnDays ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                columnDays.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(getHeatmapColor(cell.intensity))
                    )
                }
            }
        }
    }
}

@Composable
private fun getHeatmapColor(intensity: Int): Color {
    return when (intensity) {
        0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        1 -> StateCompleted.copy(alpha = 0.35f)
        2 -> StateCompleted.copy(alpha = 0.65f)
        3 -> StateCompleted
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
private fun CategoryProgressRow(stat: CategoryStat) {
    val categoryColor = Color(stat.category.color)
    val animatedProgress by animateFloatAsState(
        targetValue = stat.completionRate,
        animationSpec = tween(durationMillis = 500),
        label = "cat_progress"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                )
                Text(
                    text = stat.category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${(stat.completionRate * 100).toInt()}% (${stat.completedCount}/${stat.totalCount})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = categoryColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
