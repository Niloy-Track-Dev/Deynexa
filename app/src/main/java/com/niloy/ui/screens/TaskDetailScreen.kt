package com.niloy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niloy.domain.model.Category
import com.niloy.ui.components.IconHelper
import com.niloy.ui.util.TimeUtils
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    viewModel: TaskDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.id == 0L) "New Routine" else "Edit Routine",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveTask() },
                        enabled = uiState.name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("save_task_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Name and Description Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.updateName(it) },
                        label = { Text("Routine Name") },
                        placeholder = { Text("e.g., Morning Meditation, Code Review") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("task_name_input")
                    )

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = { Text("Description (Optional)") },
                        placeholder = { Text("Add notes, guidelines, or targets") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Category Selection Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (uiState.categories.isEmpty()) {
                        Text(
                            text = "No categories available. Please create one in Categories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.categories.forEach { category ->
                                val isSelected = uiState.categoryId == category.id
                                val catColor = Color(category.color)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateCategory(category.id) },
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
                }
            }

            // Schedule & Time Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Time & Timing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "All-Day Routine",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "No specific time window required",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isAllDay,
                            onCheckedChange = { viewModel.updateIsAllDay(it) }
                        )
                    }

                    if (!uiState.isAllDay) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TimeSelectButton(
                                label = "Start Time",
                                minutes = uiState.startTime,
                                modifier = Modifier.weight(1f),
                                onClick = { showStartTimePicker = true }
                            )
                            TimeSelectButton(
                                label = "End Time",
                                minutes = uiState.endTime,
                                modifier = Modifier.weight(1f),
                                onClick = { showEndTimePicker = true }
                            )
                        }
                    }
                }
            }

            // Recurring Schedule Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Repeat Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.isRecurring) "Repeats on selected days of the week" else "One-time task today only",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isRecurring,
                            onCheckedChange = { viewModel.updateIsRecurring(it) }
                        )
                    }

                    if (uiState.isRecurring) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Quick Select Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isEveryday = uiState.recurringDays.size == 7
                            val isWeekdays = uiState.recurringDays == setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                            val isWeekends = uiState.recurringDays == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

                            AssistChip(
                                onClick = {
                                    DayOfWeek.values().forEach { day ->
                                        if (!uiState.recurringDays.contains(day)) viewModel.toggleRecurringDay(day)
                                    }
                                },
                                label = { Text("Every Day", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isEveryday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            )

                            AssistChip(
                                onClick = {
                                    DayOfWeek.values().forEach { day ->
                                        val shouldHave = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
                                        if (shouldHave && !uiState.recurringDays.contains(day)) viewModel.toggleRecurringDay(day)
                                        if (!shouldHave && uiState.recurringDays.contains(day)) viewModel.toggleRecurringDay(day)
                                    }
                                },
                                label = { Text("Weekdays", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isWeekdays) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            )

                            AssistChip(
                                onClick = {
                                    DayOfWeek.values().forEach { day ->
                                        val shouldHave = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
                                        if (shouldHave && !uiState.recurringDays.contains(day)) viewModel.toggleRecurringDay(day)
                                        if (!shouldHave && uiState.recurringDays.contains(day)) viewModel.toggleRecurringDay(day)
                                    }
                                },
                                label = { Text("Weekends", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isWeekends) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            )
                        }

                        // Day of week circular badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY,
                                DayOfWeek.SATURDAY,
                                DayOfWeek.SUNDAY
                            ).forEach { day ->
                                val isSelected = uiState.recurringDays.contains(day)
                                DaySelectionChip(
                                    day = day,
                                    isSelected = isSelected,
                                    onClick = { viewModel.toggleRecurringDay(day) }
                                )
                            }
                        }
                    }
                }
            }

            // Smart Reminders Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart Reminder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.reminderEnabled) "Notify on device before routine starts" else "No reminder alerts configured",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.reminderEnabled,
                            onCheckedChange = { viewModel.updateReminderEnabled(it) }
                        )
                    }

                    if (uiState.reminderEnabled) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Text(
                            text = "Remind Me:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0 to "At Start",
                                5 to "5 min before",
                                10 to "10 min before",
                                15 to "15 min before",
                                30 to "30 min before"
                            ).forEach { (mins, label) ->
                                val isSelected = uiState.reminderOffsetMinutes == mins
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateReminderOffsetMinutes(mins) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showStartTimePicker) {
        SimpleTimePickerDialog(
            initialMinutes = uiState.startTime,
            title = "Select Start Time",
            onDismiss = { showStartTimePicker = false },
            onConfirm = { minutes ->
                viewModel.updateStartTime(minutes)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        SimpleTimePickerDialog(
            initialMinutes = uiState.endTime,
            title = "Select End Time",
            onDismiss = { showEndTimePicker = false },
            onConfirm = { minutes ->
                viewModel.updateEndTime(minutes)
                showEndTimePicker = false
            }
        )
    }
}

@Composable
private fun TimeSelectButton(
    label: String,
    minutes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = TimeUtils.formatTime(minutes.toLong()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DaySelectionChip(
    day: DayOfWeek,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = day.getDisplayName(TextStyle.NARROW, Locale.getDefault())
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTimePickerDialog(
    initialMinutes: Int,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val initialHour = initialMinutes / 60
    val initialMinute = initialMinutes % 60
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totalMins = timePickerState.hour * 60 + timePickerState.minute
                    onConfirm(totalMins)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
