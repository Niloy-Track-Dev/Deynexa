package com.niloy.ui.theme

import android.os.Build
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimaryDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = AccentPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
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

private val LightColorScheme = lightColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = AccentPrimaryContainer,
    onPrimaryContainer = Color(0xFF1E40AF),
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

private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node {
        return object : Modifier.Node() {}
    }
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}

@Composable
fun DaynexaTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false, // Set to false to preserve strict cohesive design system
    content: @Composable () -> Unit,
) {
    val colorScheme = LightColorScheme

    CompositionLocalProvider(
        LocalIndication provides NoIndication
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
