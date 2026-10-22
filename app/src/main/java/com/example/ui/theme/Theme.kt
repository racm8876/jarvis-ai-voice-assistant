package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF002026),
    primaryContainer = JarvisCyanDim,
    onPrimaryContainer = Color(0xFFE0F7FA),

    secondary = JarvisGold,
    onSecondary = Color(0xFF2E1C00),
    secondaryContainer = JarvisAmber,
    onSecondaryContainer = Color(0xFFFFF8E1),

    tertiary = JarvisLime,
    onTertiary = Color(0xFF0E2800),

    error = JarvisRed,
    onError = Color.White,

    background = JarvisBackground,
    onBackground = JarvisTextPrimary,

    surface = JarvisSurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = JarvisTextSecondary,

    outline = JarvisBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}
