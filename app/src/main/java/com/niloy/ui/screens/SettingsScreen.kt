package com.niloy.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niloy.ui.theme.AccentPrimary
import com.niloy.ui.theme.StateCompleted
import com.niloy.ui.theme.StateSkipped

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToDiagnostic: () -> Unit = {},
    onNavigateToClassifications: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var importInputText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.importMessage) {
        uiState.importMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearImportMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Privacy Guarantee Section (At the top)
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = StateCompleted.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, StateCompleted.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(StateCompleted.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = StateCompleted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "100% Offline & Private",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Daynexa operates entirely on your device. No analytics trackers, no account requirements, and zero remote servers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Preferences Section
            item {
                SettingsSection(title = "PREFERENCES") {
                    // Time Format
                    SettingsRowItem(
                        icon = Icons.Outlined.Schedule,
                        title = "Time Format",
                        subtitle = if (uiState.timeFormat == "24H") "24-Hour (14:30)" else "12-Hour (2:30 PM)",
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = uiState.timeFormat == "24H",
                                    onClick = { viewModel.updateTimeFormat("24H") },
                                    label = { Text("24h") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = uiState.timeFormat == "12H",
                                    onClick = { viewModel.updateTimeFormat("12H") },
                                    label = { Text("12h") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Theme
                    SettingsRowItem(
                        icon = Icons.Outlined.DarkMode,
                        title = "Appearance Theme",
                        subtitle = when (uiState.theme) {
                            "DARK" -> "Dark Mode"
                            "LIGHT" -> "Light Mode"
                            else -> "System Default"
                        },
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = uiState.theme == "SYSTEM",
                                    onClick = { viewModel.updateTheme("SYSTEM") },
                                    label = { Text("Auto") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = uiState.theme == "LIGHT",
                                    onClick = { viewModel.updateTheme("LIGHT") },
                                    label = { Text("Light") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = uiState.theme == "DARK",
                                    onClick = { viewModel.updateTheme("DARK") },
                                    label = { Text("Dark") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Week Start
                    SettingsRowItem(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "First Day of Week",
                        subtitle = if (uiState.weekStart == "MONDAY") "Monday" else "Sunday",
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = uiState.weekStart == "MONDAY",
                                    onClick = { viewModel.updateWeekStart("MONDAY") },
                                    label = { Text("Mon") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = uiState.weekStart == "SUNDAY",
                                    onClick = { viewModel.updateWeekStart("SUNDAY") },
                                    label = { Text("Sun") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Notifications & Smart Reminders Section
            item {
                SettingsSection(title = "NOTIFICATIONS & REMINDERS") {
                    SettingsRowItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Routine Notifications",
                        subtitle = if (uiState.notificationsEnabled) "Smart on-device reminder alerts enabled" else "All reminder alerts muted",
                        trailing = {
                            Switch(
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
                            )
                        }
                    )

                    if (uiState.notificationsEnabled) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsRowItem(
                            icon = Icons.Outlined.Timer,
                            title = "Default Reminder Timing",
                            subtitle = when (uiState.defaultReminderOffset) {
                                5 -> "5 minutes before routine"
                                10 -> "10 minutes before routine"
                                15 -> "15 minutes before routine"
                                30 -> "30 minutes before routine"
                                else -> "At start time"
                            },
                            trailing = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = uiState.defaultReminderOffset == 0,
                                        onClick = { viewModel.updateDefaultReminderOffset(0) },
                                        label = { Text("0m") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    FilterChip(
                                        selected = uiState.defaultReminderOffset == 5,
                                        onClick = { viewModel.updateDefaultReminderOffset(5) },
                                        label = { Text("5m") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    FilterChip(
                                        selected = uiState.defaultReminderOffset == 10,
                                        onClick = { viewModel.updateDefaultReminderOffset(10) },
                                        label = { Text("10m") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    FilterChip(
                                        selected = uiState.defaultReminderOffset == 15,
                                        onClick = { viewModel.updateDefaultReminderOffset(15) },
                                        label = { Text("15m") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        )

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsActionItem(
                            icon = Icons.Outlined.NotificationAdd,
                            title = "Send Test Routine Alert",
                            subtitle = "Verify on-device notification sound and banner",
                            onClick = {
                                try {
                                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                    com.niloy.domain.service.TaskReminderScheduler.createNotificationChannel(context)
                                    val notification = androidx.core.app.NotificationCompat.Builder(context, com.niloy.domain.service.TaskReminderScheduler.CHANNEL_ID)
                                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                                        .setContentTitle("⏰ Daynexa Smart Reminder")
                                        .setContentText("Your scheduled routine reminder test is working perfectly!")
                                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                                        .setAutoCancel(true)
                                        .build()
                                    notificationManager.notify(99999, notification)
                                    Toast.makeText(context, "Test notification triggered!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Notification check: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // Data Management Section
            item {
                SettingsSection(title = "DATA & BACKUP") {
                    SettingsActionItem(
                        icon = Icons.Outlined.FileUpload,
                        title = "Export Local Backup",
                        subtitle = "Export all routines, categories, and logs as a JSON file",
                        onClick = {
                            viewModel.generateBackup()
                            showExportDialog = true
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingsActionItem(
                        icon = Icons.Outlined.FileDownload,
                        title = "Import Backup",
                        subtitle = "Restore routines and data from a Daynexa JSON backup",
                        onClick = {
                            importInputText = ""
                            showImportDialog = true
                        }
                    )
                }
            }

            // Diagnostic & App Usage Section
            item {
                SettingsSection(title = "APP USAGE DIAGNOSTICS") {
                    SettingsActionItem(
                        icon = Icons.Outlined.Insights,
                        title = "Diagnostic Dashboard",
                        subtitle = "View focus time, app quality ratings, and usage stats",
                        onClick = onNavigateToDiagnostic
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingsActionItem(
                        icon = Icons.Outlined.Category,
                        title = "Manage App Classifications",
                        subtitle = "Categorize installed apps and set quality ratings",
                        onClick = onNavigateToClassifications
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingsActionItem(
                        icon = Icons.Outlined.Security,
                        title = "Usage Access Permission",
                        subtitle = "Open Android settings to grant or check usage permission",
                        onClick = {
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            // System & Release Section
            item {
                SettingsSection(title = "SYSTEM & RELEASES") {
                    SettingsActionItem(
                        icon = Icons.Outlined.SystemUpdate,
                        title = "Download Latest Version",
                        subtitle = "Check for updates and download APK on GitHub",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Niloy-Track-Dev/Deynexa/releases"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open release link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingsActionItem(
                        icon = Icons.Outlined.Code,
                        title = "Open Source Repository",
                        subtitle = "View source code, report bugs, and contribute on GitHub",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Niloy-Track-Dev/Deynexa"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open repository link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Danger Zone Section
            item {
                SettingsSection(title = "DANGER ZONE") {
                    SettingsActionItem(
                        icon = Icons.Outlined.DeleteForever,
                        title = "Reset All Application Data",
                        subtitle = "Erase all routines, history, and restored backups",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showResetDialog = true }
                    )
                }
            }

            // App Identity Info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Daynexa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Build Better Days • v0.3.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog && uiState.backupJson != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Backup Generated") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your full routine data has been converted to JSON:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Text(
                            text = uiState.backupJson ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Daynexa Backup", uiState.backupJson)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Backup copied to clipboard", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste a valid Daynexa backup JSON below:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        placeholder = { Text("Paste JSON here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInputText.isNotBlank()) {
                            viewModel.importBackup(importInputText.trim())
                            showImportDialog = false
                        }
                    },
                    enabled = importInputText.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Data?") },
            text = { Text("This will permanently remove all tasks, history logs, and custom categories. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                        Toast.makeText(context, "All data has been reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 4.dp)
        )

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 6.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        trailing()
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = titleColor,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}
