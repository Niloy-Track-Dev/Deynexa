package com.niloy.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niloy.domain.service.BackupValidationResult
import com.niloy.domain.service.ImportMode
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDiagnostic: () -> Unit,
    onNavigateToClassifications: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showResetDialog by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }
    var showImportInputDialog by remember { mutableStateOf(false) }
    var importInputText by remember { mutableStateOf("") }
    var selectedImportMode by remember { mutableStateOf(ImportMode.MERGE) }

    // File picker for import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    viewModel.validateImportJson(content)
                    showImportInputDialog = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Toast feedback for import / export
    LaunchedEffect(uiState.importFeedbackMessage) {
        uiState.importFeedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // 100% Offline & Private Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "100% Offline & Private",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Your data stays on your device. No cloud sync, no tracking, no telemetry. Ever.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Appearance Section
            item {
                SettingsSection(title = "APPEARANCE") {
                    SettingsRowItem(
                        icon = Icons.Outlined.Palette,
                        title = "App Theme",
                        subtitle = "Select between Light, Dark, or System mode",
                        trailing = {
                            SettingsChipRow(
                                options = listOf("Light" to "LIGHT", "Dark" to "DARK", "Auto" to "SYSTEM"),
                                selectedOption = uiState.theme,
                                onOptionSelected = { viewModel.updateTheme(it) }
                            )
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingsRowItem(
                        icon = Icons.Outlined.Schedule,
                        title = "Time Format",
                        subtitle = "Choose your preferred clock system",
                        trailing = {
                            SettingsChipRow(
                                options = listOf("12H" to "12H", "24H" to "24H"),
                                selectedOption = uiState.timeFormat,
                                onOptionSelected = { viewModel.updateTimeFormat(it) }
                            )
                        }
                    )
                }
            }

            // General Preferences Section
            item {
                SettingsSection(title = "GENERAL PREFERENCES") {
                    SettingsRowItem(
                        icon = Icons.Outlined.CalendarToday,
                        title = "Week Start Day",
                        subtitle = "Select which day starts your routine week",
                        trailing = {
                            SettingsChipRow(
                                options = listOf("Mon" to "MONDAY", "Sun" to "SUNDAY"),
                                selectedOption = uiState.weekStart,
                                onOptionSelected = { viewModel.updateWeekStart(it) }
                            )
                        }
                    )
                }
            }

            // Focentra Integration Section
            item {
                val focentraStatus = uiState.focentraStatus

                SettingsSection(title = "INTEGRATIONS • FOCENTRA") {
                    SettingsRowItem(
                        icon = Icons.Outlined.Hub,
                        title = "Focentra Study Focus",
                        subtitle = when {
                            focentraStatus == null -> "Checking installation..."
                            !focentraStatus.isInstalled -> "Focentra isn't installed on device"
                            focentraStatus.isConnected -> "Connected • ${focentraStatus.totalImportedSessions} sessions imported"
                            else -> "Not Connected (Optional)"
                        },
                        trailing = {
                            if (focentraStatus?.isInstalled == true) {
                                if (focentraStatus.isConnected) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { viewModel.syncFocentra() }) {
                                            Text("Sync")
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.disconnectFocentra() },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Disconnect")
                                        }
                                    }
                                } else {
                                    Button(onClick = { showConsentDialog = true }) {
                                        Text("Connect")
                                    }
                                }
                            } else {
                                TextButton(onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Niloy-Track-Dev"))
                                    context.startActivity(intent)
                                }) {
                                    Text("Get App")
                                }
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Default Reminder Timing",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = when (uiState.defaultReminderOffset) {
                                                5 -> "5 minutes before routine"
                                                10 -> "10 minutes before routine"
                                                15 -> "15 minutes before routine"
                                                30 -> "30 minutes before routine"
                                                else -> "At start time"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    0 to "At start",
                                    5 to "5m before",
                                    10 to "10m before",
                                    15 to "15m before"
                                ).forEach { (mins, label) ->
                                    FilterChip(
                                        selected = uiState.defaultReminderOffset == mins,
                                        onClick = { viewModel.updateDefaultReminderOffset(mins) },
                                        label = { Text(label, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsActionItem(
                            icon = Icons.Outlined.NotificationAdd,
                            title = "Send Test Routine Alert",
                            subtitle = "Verify on-device notification sound and banner",
                            onClick = {
                                try {
                                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                    com.niloy.domain.service.TaskReminderScheduler.createNotificationChannel(context)
                                    val notification = androidx.core.app.NotificationCompat.Builder(context, com.niloy.domain.service.TaskReminderScheduler.CHANNEL_ID)
                                        .setSmallIcon(com.niloy.R.drawable.ic_notification)
                                        .setContentTitle("Daynexa Smart Reminder")
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

            // Data & Portability Section
            item {
                SettingsSection(title = "DATA & PORTABILITY") {
                    SettingsActionItem(
                        icon = Icons.Outlined.FileUpload,
                        title = "Export Full Daynexa Backup",
                        subtitle = "Routines, categories, logs, classifications & local focus sessions",
                        onClick = {
                            viewModel.generateFullBackup()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingsActionItem(
                        icon = Icons.Outlined.Science,
                        title = "Export Focentra Study Focus Data",
                        subtitle = "Standalone export of imported Focentra session history",
                        onClick = {
                            viewModel.generateFocentraExport()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingsActionItem(
                        icon = Icons.Outlined.FileDownload,
                        title = "Import Backup / Restore Data",
                        subtitle = "Restore with validation & choose Replace or Merge mode",
                        onClick = {
                            importInputText = ""
                            showImportInputDialog = true
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingsActionItem(
                        icon = Icons.Outlined.Category,
                        title = "Manage App Classifications",
                        subtitle = "Categorize installed apps and set quality ratings",
                        onClick = onNavigateToClassifications
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

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
                        text = "Build Better Days • v0.6.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Focentra Consent Dialog
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("Connect Focentra Integration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Daynexa and Focentra communicate securely via local Android App-to-App IPC with zero cloud servers or telemetry.")
                    Text("• Received data: Completed study sessions, duration, date, category, and subject.")
                    Text("• Shared data: Optional daily productivity targets and completion context.")
                    Text("You can disconnect at any time without losing your task history.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showConsentDialog = false
                    viewModel.connectFocentra(true)
                }) {
                    Text("Agree & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Result Dialog
    if (uiState.generatedExportJson != null && uiState.exportDialogType != null) {
        val json = uiState.generatedExportJson ?: ""
        val title = if (uiState.exportDialogType == "FULL") "Full Daynexa Backup Ready" else "Focentra Focus Data Ready"

        AlertDialog(
            onDismissRequest = { viewModel.dismissExportDialog() },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your JSON export bundle is generated and ready to share or save:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        Text(
                            text = json,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, json)
                                putExtra(Intent.EXTRA_SUBJECT, title)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Backup JSON"))
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(title, json)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Backup copied to clipboard", Toast.LENGTH_SHORT).show()
                            viewModel.dismissExportDialog()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExportDialog() }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Input Dialog (Paste JSON or Pick File)
    if (showImportInputDialog) {
        AlertDialog(
            onDismissRequest = { showImportInputDialog = false },
            title = { Text("Import Backup / Restore", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select a JSON backup file from your storage or paste the JSON text below:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch("application/json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose JSON File From Storage")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text(" OR PASTE ", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }

                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        placeholder = { Text("Paste valid backup JSON here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInputText.isNotBlank()) {
                            viewModel.validateImportJson(importInputText.trim())
                            showImportInputDialog = false
                        }
                    },
                    enabled = importInputText.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Validate & Review")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Validation & Restore Confirmation Dialog
    uiState.validationResult?.let { validation ->
        when (validation) {
            is BackupValidationResult.Invalid -> {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissValidationDialog() },
                    title = { Text("Invalid Backup File", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("The selected backup could not be processed:")
                            Text(
                                text = validation.errorReason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.dismissValidationDialog() }) {
                            Text("OK")
                        }
                    }
                )
            }
            is BackupValidationResult.ValidFull, is BackupValidationResult.ValidFocentra -> {
                val preview = if (validation is BackupValidationResult.ValidFull) validation.preview else (validation as BackupValidationResult.ValidFocentra).preview
                val formattedDate = try {
                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    sdf.format(Date(preview.createdAt))
                } catch (e: Exception) {
                    "Unknown"
                }

                AlertDialog(
                    onDismissRequest = { viewModel.dismissValidationDialog() },
                    title = { Text("Backup Verification & Preview", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = preview.backupType,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Created: $formattedDate • App v${preview.appVersion}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text("Detected Items in Backup:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                if (preview.tasksCount > 0) Text("• ${preview.tasksCount} Routines / Tasks")
                                if (preview.categoriesCount > 0) Text("• ${preview.categoriesCount} Routine Categories")
                                if (preview.occurrencesCount > 0) Text("• ${preview.occurrencesCount} Routine Execution Logs")
                                if (preview.focentraSessionsCount > 0) Text("• ${preview.focentraSessionsCount} Focentra Focus Study Sessions")
                                if (preview.appClassificationsCount > 0) Text("• ${preview.appClassificationsCount} App Classifications")
                                if (preview.appCategoriesCount > 0) Text("• ${preview.appCategoriesCount} App Diagnostic Categories")
                                if (preview.settingsCount > 0) Text("• ${preview.settingsCount} Preference Settings")
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Text("Select Restoration Mode:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedImportMode == ImportMode.MERGE) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (selectedImportMode == ImportMode.MERGE) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedImportMode = ImportMode.MERGE }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedImportMode == ImportMode.MERGE,
                                        onClick = { selectedImportMode = ImportMode.MERGE }
                                    )
                                    Column {
                                        Text("Merge (Recommended)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Preserves existing data and adds/updates records without data loss.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedImportMode == ImportMode.REPLACE) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (selectedImportMode == ImportMode.REPLACE) MaterialTheme.colorScheme.error else Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedImportMode = ImportMode.REPLACE }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedImportMode == ImportMode.REPLACE,
                                        onClick = { selectedImportMode = ImportMode.REPLACE },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error)
                                    )
                                    Column {
                                        Text("Replace / Overwrite", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                        Text("Wipes local database and replaces with this backup file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.executeImport(selectedImportMode)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = if (selectedImportMode == ImportMode.REPLACE) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                        ) {
                            Text(if (selectedImportMode == ImportMode.REPLACE) "Replace & Restore" else "Merge & Restore")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissValidationDialog() }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Data?") },
            text = { Text("This will permanently remove all tasks, history logs, custom categories, and imported Focentra session history. This action cannot be undone.") },
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
private fun SettingsChipRow(
    options: List<Pair<String, String>>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        options.forEach { (label, value) ->
            val isSelected = selectedOption == value
            FilterChip(
                selected = isSelected,
                onClick = { onOptionSelected(value) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    borderWidth = 1.dp,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
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
