package com.niloy.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

object IconHelper {
    val availableIcons = listOf(
        "favorite" to Icons.Default.Favorite,
        "work" to Icons.Default.Work,
        "person" to Icons.Default.Person,
        "fitness" to Icons.Default.FitnessCenter,
        "book" to Icons.Default.MenuBook,
        "code" to Icons.Default.Code,
        "home" to Icons.Default.Home,
        "star" to Icons.Default.Star,
        "brush" to Icons.Default.Brush,
        "schedule" to Icons.Default.Schedule,
        "local_cafe" to Icons.Default.LocalCafe,
        "directions_run" to Icons.Default.DirectionsRun,
        "spa" to Icons.Default.Spa,
        "psychology" to Icons.Default.Psychology,
        "school" to Icons.Default.School,
        "bed" to Icons.Default.Bed
    )

    fun getIcon(name: String): ImageVector {
        return availableIcons.find { it.first.equals(name, ignoreCase = true) }?.second
            ?: when (name.lowercase()) {
                "health", "fitness_center", "gym" -> Icons.Default.FitnessCenter
                "work", "job", "business" -> Icons.Default.Work
                "personal", "user", "self" -> Icons.Default.Person
                "study", "education", "school" -> Icons.Default.School
                "meditation", "mindfulness", "spa" -> Icons.Default.Spa
                "coffee", "drink" -> Icons.Default.LocalCafe
                "walk", "run", "running" -> Icons.Default.DirectionsRun
                "sleep", "rest" -> Icons.Default.Bed
                "reading", "books" -> Icons.Default.MenuBook
                "art", "design" -> Icons.Default.Brush
                else -> Icons.Default.CheckCircle
            }
    }
}
