package com.niloy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TodayProgressCard(
    totalCount: Int,
    completedCount: Int,
    pendingCount: Int,
    skippedCount: Int,
    completionPercentage: Float,
    currentStreak: Int = 0,
    totalFocusMinutes: Long = 0L,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = completionPercentage,
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )

    val statusText = when {
        completionPercentage <= 0f -> "Ready to get started"
        completionPercentage >= 1f -> "Awesome! All tasks completed!"
        else -> "Making steady progress!"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F8FC) // Clean, soft light bluish/grey card background matching image
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Row: Title, Subtitle, and Percentage Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B) // Premium dark charcoal
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B) // Slate grey description
                    )
                }

                // Percentage Pill (Soft Blue Background + Dark Blue Text)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE8F0FE))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(completionPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A73E8)
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color(0xFF1A73E8),
                trackColor = Color(0xFFE2E8F0),
                strokeCap = StrokeCap.Round
            )

            // State Pills (White Status Boxes in a Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Completed Pill (White Background + Green Indicator)
                StatePill(
                    count = completedCount,
                    label = "Completed",
                    dotColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )

                // Pending Pill (White Background + Grey Indicator)
                StatePill(
                    count = pendingCount,
                    label = "Pending",
                    dotColor = Color(0xFF94A3B8),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatePill(
    count: Int,
    label: String,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White, // pure white as requested
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)), // clean subtle border
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Small Colored Indicator Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            // Count and Label
            Column {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
