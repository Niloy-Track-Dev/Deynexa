package com.niloy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niloy.domain.model.TaskState
import com.niloy.ui.components.EmptyStateView
import com.niloy.ui.components.TaskCard
import com.niloy.ui.theme.StateCompleted
import com.niloy.ui.theme.StatePending
import com.niloy.ui.theme.StateSkipped
import com.niloy.ui.util.TimeUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onEditTask: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val monthTitleFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val isCurrentMonth = uiState.currentMonth == YearMonth.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentMonth.format(monthTitleFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous Month")
                    }
                    if (!isCurrentMonth || uiState.selectedDate != LocalDate.now()) {
                        IconButton(onClick = { viewModel.jumpToToday() }) {
                            Icon(Icons.Outlined.Today, contentDescription = "Jump to Today", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Next Month")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Calendar Month Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Weekday labels (Mon -> Sun)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY,
                                DayOfWeek.SATURDAY,
                                DayOfWeek.SUNDAY
                            ).forEach { dayOfWeek ->
                                Text(
                                    text = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Divider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )

                        // Month Grid Days
                        MonthCalendarGrid(
                            currentMonth = uiState.currentMonth,
                            selectedDate = uiState.selectedDate,
                            productivityMap = uiState.monthProductivity,
                            onSelectDate = { viewModel.selectDate(it) }
                        )
                    }
                }
            }

            // Selected Day Summary Card
            item {
                SelectedDateSummaryCard(
                    date = uiState.selectedDate,
                    stats = uiState.stats
                )
            }

            // Tasks List for selected date
            if (uiState.tasksWithOccurrences.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.EventBusy,
                        title = "No routines scheduled",
                        subtitle = "There are no tasks or habits scheduled for ${TimeUtils.formatDateHeader(uiState.selectedDate)}.",
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                item {
                    Text(
                        text = "SCHEDULE FOR THIS DAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                items(
                    items = uiState.tasksWithOccurrences,
                    key = { "cal_task_${it.task.id}_${it.occurrence.date}" }
                ) { item ->
                    val category = uiState.categories.find { it.id == item.task.categoryId }
                    TaskCard(
                        item = item,
                        category = category,
                        is24Hour = uiState.is24Hour,
                        onToggle = {
                            viewModel.toggleTaskCompletion(
                                item.task.id,
                                item.occurrence.date,
                                item.occurrence.state
                            )
                        },
                        onSkip = {
                            viewModel.toggleSkipTask(
                                item.task.id,
                                item.occurrence.date,
                                item.occurrence.state
                            )
                        },
                        onEdit = {
                            onEditTask(item.task.id)
                        },
                        onDelete = {
                            viewModel.deleteTask(item.task)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    productivityMap: Map<LocalDate, DayProductivity>,
    onSelectDate: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val today = LocalDate.now()

    // 1 (Monday) to 7 (Sunday)
    val startDayOffset = firstDayOfMonth.dayOfWeek.value - 1

    val totalCells = ((startDayOffset + daysInMonth + 6) / 7) * 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until totalCells / 7) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startDayOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayNumber)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val productivity = productivityMap[date]

                        CalendarDayCell(
                            dayNumber = dayNumber,
                            isSelected = isSelected,
                            isToday = isToday,
                            productivity = productivity,
                            onClick = { onSelectDate(date) }
                        )
                    } else {
                        // Empty placeholder cell
                        Spacer(modifier = Modifier.size(38.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    productivity: DayProductivity?,
    onClick: () -> Unit
) {
    val cellBgColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.surfaceVariant
            else -> Color.Transparent
        },
        label = "cell_bg"
    )

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(cellBgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                fontSize = 13.sp
            )

            // Productivity indicator dot
            if (productivity != null && productivity.totalCount > 0) {
                val dotColor = when {
                    isSelected -> Color.White.copy(alpha = 0.9f)
                    productivity.completionRate >= 1f -> StateCompleted
                    productivity.completedCount > 0 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SelectedDateSummaryCard(
    date: LocalDate,
    stats: CalendarStats
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = TimeUtils.formatDateHeader(date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (stats.total == 0) "No tasks scheduled" else "${stats.completed} of ${stats.total} completed (${(stats.completionRate * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallStatBadge(label = "Done", count = stats.completed, color = StateCompleted)
                SmallStatBadge(label = "Pending", count = stats.pending, color = StatePending)
                if (stats.skipped > 0) {
                    SmallStatBadge(label = "Skipped", count = stats.skipped, color = StateSkipped)
                }
            }
        }
    }
}

@Composable
private fun SmallStatBadge(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
