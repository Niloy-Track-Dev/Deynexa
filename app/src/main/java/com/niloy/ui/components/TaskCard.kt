package com.niloy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niloy.domain.model.Category
import com.niloy.domain.model.RecurrenceType
import com.niloy.domain.model.TaskState
import com.niloy.domain.service.TaskWithOccurrence
import androidx.compose.foundation.isSystemInDarkTheme
import com.niloy.ui.theme.StateCompleted
import com.niloy.ui.theme.StateSkipped
import com.niloy.ui.util.TimeUtils
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TaskCard(
    item: TaskWithOccurrence,
    category: Category? = null,
    is24Hour: Boolean = true,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    onReschedule: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isCompleted = item.occurrence.state == TaskState.COMPLETED
    val isSkipped = item.occurrence.state == TaskState.SKIPPED
    val isRescheduled = item.occurrence.rescheduledStartTime != null || item.occurrence.rescheduledDate != null || item.occurrence.isException

    val cardAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0.8f else if (isSkipped) 0.6f else 1f,
        animationSpec = spring(),
        label = "task_alpha"
    )

    val checkBorderColor by animateColorAsState(
        targetValue = if (isCompleted) StateCompleted 
            else if (isSkipped) StateSkipped.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(),
        label = "check_border"
    )

    val isDark = isSystemInDarkTheme()
    val categoryColor = category?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable { onToggle() }
            .testTag("task_item_${item.task.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isCompleted) StateCompleted.copy(alpha = 0.2f)
            else if (isSkipped) StateSkipped.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        tonalElevation = if (isCompleted || isSkipped) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Check Button with minimum 48dp touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(26.dp),
                    shape = CircleShape,
                    color = if (isCompleted) StateCompleted else Color.Transparent,
                    border = BorderStroke(
                        if (isCompleted) 0.dp else 1.5.dp,
                        checkBorderColor
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (isSkipped) {
                            Icon(
                                imageVector = Icons.Outlined.SkipNext,
                                contentDescription = "Skipped",
                                tint = StateSkipped,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Task info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.task.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            else if (isSkipped) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isSkipped) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StateSkipped.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Skipped",
                                style = MaterialTheme.typography.labelSmall,
                                color = StateSkipped,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isRescheduled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Adjusted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (item.task.description.isNotBlank() || !item.occurrence.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val displayText = if (!item.occurrence.notes.isNullOrBlank()) {
                        "Note: ${item.occurrence.notes}"
                    } else {
                        item.task.description
                    }
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Time & Category row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val timeString = TimeUtils.formatTimeRange(
                        startTime = item.effectiveStartTime,
                        endTime = item.effectiveEndTime,
                        isAllDay = item.task.isAllDay,
                        is24Hour = is24Hour
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = if (isRescheduled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isRescheduled) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRescheduled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    if (category != null) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = categoryColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor)
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = categoryColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Options menu with minimum 48dp touch target
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Routine") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    if (onReschedule != null) {
                        DropdownMenuItem(
                            text = { Text("Reschedule / Adjust") },
                            leadingIcon = { Icon(Icons.Outlined.Update, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onReschedule()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (isSkipped) "Unskip" else "Skip Today") },
                        leadingIcon = {
                            Icon(
                                if (isSkipped) Icons.Outlined.Redo else Icons.Outlined.SkipNext,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showMenu = false
                            onSkip()
                        }
                    )
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatRecurrenceShort(task: com.niloy.domain.model.Task): String {
    return when (task.recurrenceType) {
        RecurrenceType.DAILY -> if (task.recurrenceInterval > 1) "Every ${task.recurrenceInterval}d" else "Daily"
        RecurrenceType.WEEKDAYS -> "Weekdays"
        RecurrenceType.WEEKENDS -> "Weekends"
        RecurrenceType.SPECIFIC_DAYS -> {
            if (task.recurringDays.size == 7) "Daily"
            else if (task.recurringDays.size <= 3) {
                task.recurringDays.joinToString(",") { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
            } else {
                "${task.recurringDays.size}d/wk"
            }
        }
        RecurrenceType.EVERY_X_DAYS -> "Every ${task.recurrenceInterval}d"
        RecurrenceType.EVERY_X_WEEKS -> "Every ${task.recurrenceInterval}w"
        RecurrenceType.MONTHLY -> "Monthly"
        RecurrenceType.SPECIFIC_DAY_OF_MONTH -> "Day ${task.recurrenceDayOfMonth ?: 1}"
        RecurrenceType.YEARLY -> "Yearly"
        RecurrenceType.CUSTOM -> "Custom"
        RecurrenceType.NONE -> "One-time"
    }
}
