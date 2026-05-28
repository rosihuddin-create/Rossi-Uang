package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    secondary = IndigoSecondary,
    tertiary = IndigoTertiary,
    background = DarkBg,
    surface = DarkSurface,
    onBackground = TextLight,
    onSurface = TextLight,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextLight,
    error = AccentRed
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    secondary = IndigoSecondary,
    tertiary = IndigoTertiary,
    background = LightBg,
    surface = LightSurface,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextDark,
    error = AccentRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to preserve our custom brand identity perfectly
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
