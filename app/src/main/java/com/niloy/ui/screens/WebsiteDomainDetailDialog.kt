package com.niloy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niloy.domain.model.DomainClassification
import com.niloy.domain.model.WebsiteCategories
import com.niloy.domain.model.WebsiteQualityRating
import com.niloy.ui.theme.StateCompleted
import com.niloy.ui.theme.StatePending
import com.niloy.ui.theme.StateSkipped

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteDomainDetailDialog(
    domainClassification: DomainClassification,
    onDismiss: () -> Unit,
    onSave: (domain: String, category: String, rating: WebsiteQualityRating) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(domainClassification.category) }
    var selectedRating by remember { mutableStateOf(domainClassification.qualityRating) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = domainClassification.domain,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Visits: ${domainClassification.visitCount} • Privacy Safe",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quality Rating Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quality Rating",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            WebsiteQualityRating.VERY_GOOD to StateCompleted,
                            WebsiteQualityRating.GOOD to StateCompleted.copy(alpha = 0.8f),
                            WebsiteQualityRating.NEUTRAL to MaterialTheme.colorScheme.outline,
                            WebsiteQualityRating.NOT_GOOD to StatePending,
                            WebsiteQualityRating.BAD to StateSkipped,
                            WebsiteQualityRating.VERY_BAD to StateSkipped.copy(alpha = 0.8f)
                        ).forEach { (rating, color) ->
                            val isSelected = selectedRating == rating
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedRating = rating },
                                label = {
                                    Text(
                                        text = rating.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) BorderStroke(1.5.dp, color) else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.15f),
                                    selectedLabelColor = color
                                )
                            )
                        }
                    }
                }

                // Category Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Classification Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WebsiteCategories.ALL_CATEGORIES.forEach { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = {
                                    Text(
                                        text = category,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Privacy Note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = StateCompleted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "This classification is stored 100% on-device and applies instantly to your productivity scores.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(domainClassification.domain, selectedCategory, selectedRating)
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Classification")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
