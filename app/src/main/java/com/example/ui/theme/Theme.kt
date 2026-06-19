package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GoldLight,
    onPrimary = ObsidianDark,
    primaryContainer = GoldDark,
    onPrimaryContainer = PearlWhite,
    secondary = LapisLight,
    onSecondary = PearlWhite,
    secondaryContainer = SandDark,
    onSecondaryContainer = SandLight,
    tertiary = EmeraldGreen,
    onTertiary = PearlWhite,
    background = ObsidianDark,
    onBackground = SandLight,
    surface = SandDark,
    onSurface = SandLight,
    error = CurseRed,
    onError = PearlWhite
)

private val LightColorScheme = lightColorScheme(
    primary = GoldMedium,
    onPrimary = ObsidianDark,
    primaryContainer = GoldLight,
    onPrimaryContainer = ObsidianDark,
    secondary = LapisBlue,
    onSecondary = PearlWhite,
    secondaryContainer = SandLight,
    onSecondaryContainer = ObsidianDark,
    tertiary = EmeraldGreen,
    onTertiary = PearlWhite,
    background = SandLight,
    onBackground = ObsidianDark,
    surface = SandLight,
    onSurface = ObsidianDark,
    error = CurseRed,
    onError = PearlWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We force the dark tomb look for atmospheric ancient explorer feeling
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
