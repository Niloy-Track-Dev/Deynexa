package com.niloy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niloy.domain.model.AppCategories
import com.niloy.domain.model.AppQualityRating
import com.niloy.domain.model.InstalledAppInfo
import com.niloy.domain.model.ProductivityType
import com.niloy.ui.theme.StateCompleted
import com.niloy.ui.theme.StateSkipped

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppClassificationScreen(
    viewModel: AppClassificationViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCategoryManagement by remember { mutableStateOf(false) }

    val filteredApps = remember(uiState.installedApps, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.installedApps
        } else {
            val query = uiState.searchQuery.trim().lowercase()
            uiState.installedApps.filter {
                it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Classifications",
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
                    IconButton(onClick = { showCategoryManagement = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Manage Categories")
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
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search app by name or package...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps) { app ->
                        InstalledAppRowItem(
                            app = app,
                            onClick = { viewModel.selectAppToEdit(app) }
                        )
                    }
                }
            }
        }
    }

    // Edit Dialog
    uiState.selectedAppToEdit?.let { app ->
        EditAppClassificationDialog(
            app = app,
            availableCategories = uiState.appCategories,
            onDismiss = { viewModel.selectAppToEdit(null) },
            onSave = { pkg, name, categories, rating ->
                viewModel.saveAppClassification(pkg, name, categories, rating)
            },
            onAddCategory = { name, isProd -> viewModel.saveAppCategory(name, isProd) }
        )
    }

    if (showCategoryManagement) {
        ManageAppCategoriesDialog(
            categories = uiState.appCategories,
            onDismiss = { showCategoryManagement = false },
            onAddCategory = { name, isProd -> viewModel.saveAppCategory(name, isProd) },
            onDeleteCategory = { viewModel.deleteAppCategory(it) }
        )
    }
}

@Composable
private fun InstalledAppRowItem(
    app: InstalledAppInfo,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIconImage(packageName = app.packageName, modifier = Modifier.size(40.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgeColor = when (app.productivityType) {
                        ProductivityType.PRODUCTIVE -> StateCompleted
                        ProductivityType.NON_PRODUCTIVE -> StateSkipped
                        ProductivityType.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = app.productivityType.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (app.categories.isNotEmpty()) {
                        Text(
                            text = "• ${app.categories.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (app.qualityRating != AppQualityRating.UNRATED) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = app.qualityRating.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditAppClassificationDialog(
    app: InstalledAppInfo,
    availableCategories: List<com.niloy.domain.model.AppCategory>,
    onDismiss: () -> Unit,
    onSave: (String, String, List<String>, AppQualityRating) -> Unit,
    onAddCategory: (String, Boolean) -> Unit
) {
    var selectedCategories by remember { mutableStateOf(app.categories.toSet()) }
    var selectedRating by remember { mutableStateOf(app.qualityRating) }
    var showQuickAddCategory by remember { mutableStateOf(false) }
    var newQuickCatName by remember { mutableStateOf("") }
    var isNewCatProd by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppIconImage(packageName = app.packageName, modifier = Modifier.size(36.dp))
                Column {
                    Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories (Multiple Allowed)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TextButton(
                        onClick = { showQuickAddCategory = !showQuickAddCategory },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(if (showQuickAddCategory) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(if (showQuickAddCategory) "Close" else "New", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (showQuickAddCategory) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newQuickCatName,
                                onValueChange = { newQuickCatName = it },
                                placeholder = { Text("New category name...", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = isNewCatProd,
                                    onClick = { isNewCatProd = true },
                                    label = { Text("Productive", style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = !isNewCatProd,
                                    onClick = { isNewCatProd = false },
                                    label = { Text("Distracting", style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        if (newQuickCatName.isNotBlank()) {
                                            onAddCategory(newQuickCatName.trim(), isNewCatProd)
                                            selectedCategories = selectedCategories + newQuickCatName.trim()
                                            newQuickCatName = ""
                                            showQuickAddCategory = false
                                        }
                                    },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    enabled = newQuickCatName.isNotBlank()
                                ) {
                                    Text("Add", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableCategories.forEach { category ->
                        val isSelected = selectedCategories.contains(category.name)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategories = if (isSelected) {
                                    selectedCategories - category.name
                                } else {
                                    selectedCategories + category.name
                                }
                            },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(category.name, style = MaterialTheme.typography.labelSmall)
                                    if (!category.isProductive) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(MaterialTheme.colorScheme.error, CircleShape)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        app.packageName,
                        app.appName,
                        selectedCategories.toList(),
                        selectedRating
                    )
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save")
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
private fun ManageAppCategoriesDialog(
    categories: List<com.niloy.domain.model.AppCategory>,
    onDismiss: () -> Unit,
    onAddCategory: (String, Boolean) -> Unit,
    onDeleteCategory: (com.niloy.domain.model.AppCategory) -> Unit
) {
    var newCatName by remember { mutableStateOf("") }
    var isProd by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Categories", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                // Add New Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add New Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        placeholder = { Text("Category name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isProd,
                            onClick = { isProd = true },
                            label = { Text("Productive") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = !isProd,
                            onClick = { isProd = false },
                            label = { Text("Distracting") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                if (newCatName.isNotBlank()) {
                                    onAddCategory(newCatName.trim(), isProd)
                                    newCatName = ""
                                }
                            },
                            enabled = newCatName.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // List Section
                Text("Existing Categories", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (cat.isProductive) "Productive" else "Distracting",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cat.isProductive) StateCompleted else MaterialTheme.colorScheme.error
                                    )
                                }
                                IconButton(onClick = { onDeleteCategory(cat) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
