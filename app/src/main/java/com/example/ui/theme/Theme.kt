package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TvDarkColorScheme = darkColorScheme(
    primary = TvPrimary,
    onPrimary = Color(0xFF041424),
    secondary = TvSecondary,
    onSecondary = Color(0xFF041424),
    tertiary = TvTertiary,
    background = TvBackground,
    onBackground = TvOnBackground,
    surface = TvSurface,
    onSurface = TvOnSurface,
    surfaceVariant = TvSurfaceVariant,
    outline = TvOutline
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    // Android TV uses deep dark theme consistently for optimum viewing on TV displays
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        typography = Typography,
        content = content
    )
}
