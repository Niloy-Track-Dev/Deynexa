package com.niloy.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niloy.domain.model.*
import com.niloy.ui.util.TimeUtils
import java.time.DayOfWeek
import java.time.LocalDate
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
    val context = LocalContext.current

    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var templateNameInput by remember { mutableStateOf("") }
    var showTemplatesBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    // Series Edit Mode Dialog
    if (uiState.showEditModeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissEditModeDialog() },
            icon = { Icon(Icons.Outlined.Update, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Edit Recurring Routine", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "How would you like to apply these changes to the recurring series?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.setEditMode(RecurrenceEditMode.ENTIRE_SERIES) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entire Series (All Occurrences)")
                    }
                    Button(
                        onClick = { viewModel.setEditMode(RecurrenceEditMode.THIS_AND_FUTURE) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("This & Future Occurrences")
                    }
                    OutlinedButton(
                        onClick = { viewModel.setEditMode(RecurrenceEditMode.THIS_OCCURRENCE) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("This Occurrence Only")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEditModeDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save as Template Dialog
    if (showSaveTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            icon = { Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Save Routine as Template", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Give this template a name for quick one-tap routine creation in the future:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = templateNameInput,
                        onValueChange = { templateNameInput = it },
                        label = { Text("Template Name") },
                        placeholder = { Text("e.g., Deep Work Block, Gym Workout") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveAsTemplate(templateNameInput)
                        showSaveTemplateDialog = false
                    },
                    enabled = templateNameInput.isNotBlank() || uiState.name.isNotBlank()
                ) {
                    Text("Save Template")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                    if (uiState.templates.isNotEmpty()) {
                        IconButton(onClick = { showTemplatesBottomSheet = true }) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = "Use Template", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Button(
                        onClick = { viewModel.onSaveClicked() },
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
            // Next Occurrence & Live Summary Banner
            uiState.nextOccurrence?.let { nextOcc ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.EventRepeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Next Scheduled Run",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = nextOcc.formattedLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Conflict Warning Banner
            if (uiState.conflicts.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Schedule Overlap Detected (${uiState.conflicts.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        uiState.conflicts.forEach { conflict ->
                            Text(
                                text = "• ${conflict.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Task Name and Description Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                        placeholder = { Text("e.g., Morning Workout, Client Standup") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("task_name_input")
                    )

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = { Text("Description & Notes (Optional)") },
                        placeholder = { Text("Add targets, instructions, or meeting links") },
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                            text = "No categories available. Go to Categories to create one.",
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

            // Time & Timing Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                                text = "No fixed time window",
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
                            // Start Time Picker Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val initialHour = uiState.startTime / 60
                                        val initialMinute = uiState.startTime % 60
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                viewModel.updateStartTime(hour * 60 + minute)
                                            },
                                            initialHour,
                                            initialMinute,
                                            uiState.is24Hour
                                        ).show()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Start Time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = TimeUtils.formatMinutes(uiState.startTime.toLong(), uiState.is24Hour),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // End Time Picker Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val initialHour = uiState.endTime / 60
                                        val initialMinute = uiState.endTime % 60
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                viewModel.updateEndTime(hour * 60 + minute)
                                            },
                                            initialHour,
                                            initialMinute,
                                            uiState.is24Hour
                                        ).show()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "End Time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = TimeUtils.formatMinutes(uiState.endTime.toLong(), uiState.is24Hour),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Advanced Recurrence Schedule Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                                text = if (uiState.isRecurring) "Automated recurrence engine" else "One-time task only",
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

                        // Recurrence Type Selection Chips
                        Text(
                            text = "Recurrence Frequency",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val recurrenceTypes = listOf(
                            RecurrenceType.DAILY to "Every Day",
                            RecurrenceType.SPECIFIC_DAYS to "Selected Days",
                            RecurrenceType.WEEKDAYS to "Weekdays (Mon-Fri)",
                            RecurrenceType.WEEKENDS to "Weekends",
                            RecurrenceType.EVERY_X_DAYS to "Every X Days",
                            RecurrenceType.EVERY_X_WEEKS to "Every X Weeks",
                            RecurrenceType.MONTHLY to "Monthly",
                            RecurrenceType.SPECIFIC_DAY_OF_MONTH to "Day of Month",
                            RecurrenceType.YEARLY to "Yearly"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recurrenceTypes.forEach { (type, label) ->
                                val isSelected = uiState.recurrenceType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateRecurrenceType(type) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Specific Day Selection (for SPECIFIC_DAYS or EVERY_X_WEEKS)
                        if (uiState.recurrenceType == RecurrenceType.SPECIFIC_DAYS || uiState.recurrenceType == RecurrenceType.EVERY_X_WEEKS) {
                            Text(
                                text = "Select Days of Week",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DayOfWeek.values().forEach { day ->
                                    val isSelected = uiState.recurringDays.contains(day)
                                    val shortName = day.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .clickable { viewModel.toggleRecurringDay(day) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shortName,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Interval Stepper (for EVERY_X_DAYS or EVERY_X_WEEKS)
                        if (uiState.recurrenceType == RecurrenceType.EVERY_X_DAYS || uiState.recurrenceType == RecurrenceType.EVERY_X_WEEKS) {
                            val unitLabel = if (uiState.recurrenceType == RecurrenceType.EVERY_X_DAYS) "day(s)" else "week(s)"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Repeat Every",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateRecurrenceInterval(uiState.recurrenceInterval - 1) },
                                        enabled = uiState.recurrenceInterval > 1
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = "${uiState.recurrenceInterval} $unitLabel",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    IconButton(
                                        onClick = { viewModel.updateRecurrenceInterval(uiState.recurrenceInterval + 1) }
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                    }
                                }
                            }
                        }

                        // Day of Month Stepper (for SPECIFIC_DAY_OF_MONTH)
                        if (uiState.recurrenceType == RecurrenceType.SPECIFIC_DAY_OF_MONTH) {
                            val currentDayOfMonth = uiState.recurrenceDayOfMonth ?: LocalDate.now().dayOfMonth
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Day of Month",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateRecurrenceDayOfMonth(maxOf(1, currentDayOfMonth - 1)) },
                                        enabled = currentDayOfMonth > 1
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = "Day $currentDayOfMonth",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    IconButton(
                                        onClick = { viewModel.updateRecurrenceDayOfMonth(minOf(31, currentDayOfMonth + 1)) },
                                        enabled = currentDayOfMonth < 31
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                    }
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Recurrence End Condition
                        Text(
                            text = "End Recurrence",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.recurrenceEndType == RecurrenceEndType.NEVER,
                                onClick = { viewModel.updateRecurrenceEndType(RecurrenceEndType.NEVER) },
                                label = { Text("Never") }
                            )
                            FilterChip(
                                selected = uiState.recurrenceEndType == RecurrenceEndType.ON_DATE,
                                onClick = {
                                    val now = LocalDate.now().plusMonths(1)
                                    viewModel.updateRecurrenceEndType(RecurrenceEndType.ON_DATE)
                                    viewModel.updateRecurrenceEndDate(now.toString())
                                },
                                label = { Text("On Date") }
                            )
                            FilterChip(
                                selected = uiState.recurrenceEndType == RecurrenceEndType.AFTER_OCCURRENCES,
                                onClick = {
                                    viewModel.updateRecurrenceEndType(RecurrenceEndType.AFTER_OCCURRENCES)
                                    viewModel.updateRecurrenceCount(10)
                                },
                                label = { Text("After Count") }
                            )
                        }

                        if (uiState.recurrenceEndType == RecurrenceEndType.ON_DATE) {
                            val currentEnd = uiState.recurrenceEndDate ?: LocalDate.now().plusMonths(1).toString()
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val local = try { LocalDate.parse(currentEnd) } catch (e: Exception) { LocalDate.now() }
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val sel = LocalDate.of(y, m + 1, d)
                                                viewModel.updateRecurrenceEndDate(sel.toString())
                                            },
                                            local.year,
                                            local.monthValue - 1,
                                            local.dayOfMonth
                                        ).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ends on Date", style = MaterialTheme.typography.bodyMedium)
                                    Text(currentEnd, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else if (uiState.recurrenceEndType == RecurrenceEndType.AFTER_OCCURRENCES) {
                            val count = uiState.recurrenceCount ?: 10
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Stop after occurrences", style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateRecurrenceCount(maxOf(1, count - 1)) },
                                        enabled = count > 1
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                    }
                                    Text("$count runs", fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { viewModel.updateRecurrenceCount(count + 1) }
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Reminders Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                        Column {
                            Text(
                                text = "Reminder Notification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Alert before routine starts",
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
                            text = "Notification Offset",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val offsets = listOf(
                                0 to "At start",
                                5 to "5 min before",
                                10 to "10 min before",
                                15 to "15 min before",
                                30 to "30 min before"
                            )
                            offsets.forEach { (mins, label) ->
                                FilterChip(
                                    selected = uiState.reminderOffsetMinutes == mins,
                                    onClick = { viewModel.updateReminderOffsetMinutes(mins) },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Save As Template Action
            OutlinedButton(
                onClick = {
                    templateNameInput = uiState.name
                    showSaveTemplateDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isTemplateSaved) "Template Saved ✓" else "Save as Routine Template")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Templates Bottom Sheet
    if (showTemplatesBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTemplatesBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Apply Routine Template",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select a pre-saved routine template to quickly autofill time, recurrence, and category:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                uiState.templates.forEach { tmpl ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.applyTemplate(tmpl)
                                showTemplatesBottomSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(tmpl.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${tmpl.recurrenceType.name.lowercase().replaceFirstChar { it.uppercase() }} • ${if (tmpl.isAllDay) "All-Day" else "${tmpl.defaultDurationMinutes} min"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
