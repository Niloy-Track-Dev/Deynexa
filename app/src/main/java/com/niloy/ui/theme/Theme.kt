package com.niloy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class ColorPresetColors(
    val primaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val primaryDark: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color
)

val ColorPresets = mapOf(
    "COBALT" to ColorPresetColors(
        primaryLight = Color(0xFF3B82F6),
        primaryContainerLight = Color(0xFFDBEAFE),
        onPrimaryContainerLight = Color(0xFF1E40AF),
        primaryDark = Color(0xFF60A5FA),
        primaryContainerDark = Color(0xFF1E3A8A),
        onPrimaryContainerDark = Color(0xFFDBEAFE)
    ),
    "EMERALD" to ColorPresetColors(
        primaryLight = Color(0xFF10B981),
        primaryContainerLight = Color(0xFFD1FAE5),
        onPrimaryContainerLight = Color(0xFF065F46),
        primaryDark = Color(0xFF34D399),
        primaryContainerDark = Color(0xFF064E3B),
        onPrimaryContainerDark = Color(0xFFD1FAE5)
    ),
    "AMETHYST" to ColorPresetColors(
        primaryLight = Color(0xFF8B5CF6),
        primaryContainerLight = Color(0xFFEDE9FE),
        onPrimaryContainerLight = Color(0xFF5B21B6),
        primaryDark = Color(0xFFA78BFA),
        primaryContainerDark = Color(0xFF4C1D95),
        onPrimaryContainerDark = Color(0xFFEDE9FE)
    ),
    "AMBER" to ColorPresetColors(
        primaryLight = Color(0xFFF59E0B),
        primaryContainerLight = Color(0xFFFEF3C7),
        onPrimaryContainerLight = Color(0xFF92400E),
        primaryDark = Color(0xFFFBBF24),
        primaryContainerDark = Color(0xFF78350F),
        onPrimaryContainerDark = Color(0xFFFEF3C7)
    ),
    "ROSE" to ColorPresetColors(
        primaryLight = Color(0xFFF43F5E),
        primaryContainerLight = Color(0xFFFFE4E6),
        onPrimaryContainerLight = Color(0xFF9F1239),
        primaryDark = Color(0xFFFB7185),
        primaryContainerDark = Color(0xFF881337),
        onPrimaryContainerDark = Color(0xFFFFE4E6)
    )
)

private fun getDarkColorScheme(preset: String) = darkColorScheme(
    primary = ColorPresets[preset]?.primaryDark ?: AccentPrimaryDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = ColorPresets[preset]?.primaryContainerDark ?: AccentPrimaryContainerDark,
    onPrimaryContainer = ColorPresets[preset]?.onPrimaryContainerDark ?: Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = StateCompleted,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = StateSkipped,
    onError = Color.White
)

private fun getLightColorScheme(preset: String) = lightColorScheme(
    primary = ColorPresets[preset]?.primaryLight ?: AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = ColorPresets[preset]?.primaryContainerLight ?: AccentPrimaryContainer,
    onPrimaryContainer = ColorPresets[preset]?.onPrimaryContainerLight ?: Color(0xFF1E40AF),
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = StateCompleted,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = StateSkipped,
    onError = Color.White
)

@Composable
fun DaynexaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorPreset: String = "COBALT",
    dynamicColor: Boolean = false, // Set to false to preserve strict cohesive design system
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getDarkColorScheme(colorPreset)
        else -> getLightColorScheme(colorPreset)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
