package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.Category
import com.example.domain.model.TaskState
import com.example.domain.service.TaskWithOccurrence
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TaskCard
import com.example.ui.components.TodayProgressCard
import com.example.ui.util.TimeUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isToday = uiState.selectedDate == LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = TimeUtils.getDayGreeting(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = TimeUtils.formatDateHeader(uiState.selectedDate),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.previousDay() }) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous Day",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (!isToday) {
                        TextButton(
                            onClick = { viewModel.jumpToToday() },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.nextDay() }) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "Next Day",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Task",
                    modifier = Modifier.size(24.dp)
                )
            }
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress Card
                item {
                    TodayProgressCard(
                        totalCount = uiState.totalCount,
                        completedCount = uiState.completedCount,
                        pendingCount = uiState.pendingCount,
                        skippedCount = uiState.skippedCount,
                        completionPercentage = uiState.completionPercentage
                    )
                }

                // Category Filter Chips (if multiple categories exist)
                if (uiState.categories.isNotEmpty()) {
                    item {
                        CategoryFilterRow(
                            categories = uiState.categories,
                            selectedCategoryId = uiState.selectedCategoryId,
                            onSelectCategory = { viewModel.selectCategoryFilter(it) }
                        )
                    }
                }

                if (uiState.tasksWithOccurrences.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.Event,
                            title = if (uiState.selectedCategoryId != null) "No tasks in this category" else "No routines scheduled",
                            subtitle = if (isToday) "Plan your day by adding recurring routines or one-time tasks." else "No tasks scheduled for ${TimeUtils.formatDateHeader(uiState.selectedDate)}.",
                            actionLabel = if (isToday) "Add New Task" else null,
                            onActionClick = onAddTask,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                } else {
                    val activeTasks = uiState.tasksWithOccurrences.filter { it.occurrence.state != TaskState.COMPLETED }
                    val completedTasks = uiState.tasksWithOccurrences.filter { it.occurrence.state == TaskState.COMPLETED }

                    if (activeTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "SCHEDULE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }

                        items(
                            items = activeTasks,
                            key = { "task_${it.task.id}_${it.occurrence.date}" }
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
                                    viewModel.skipTask(item.task.id, item.occurrence.date)
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

                    if (completedTasks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "COMPLETED (${completedTasks.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            )
                        }

                        items(
                            items = completedTasks,
                            key = { "completed_${it.task.id}_${it.occurrence.date}" }
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
                                    viewModel.skipTask(item.task.id, item.occurrence.date)
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
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelectCategory: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategoryId == null,
            onClick = { onSelectCategory(null) },
            label = {
                Text(
                    text = "All",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedCategoryId == null) FontWeight.SemiBold else FontWeight.Normal
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        categories.forEach { category ->
            val isSelected = selectedCategoryId == category.id
            val catColor = Color(category.color)
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(category.id) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(catColor)
                        )
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = catColor.copy(alpha = 0.15f),
                    selectedLabelColor = catColor
                )
            )
        }
    }
}
