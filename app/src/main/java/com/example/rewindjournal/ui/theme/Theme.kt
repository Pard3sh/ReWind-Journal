package com.example.rewindjournal.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = ReWindDarkPurple,
    onPrimary = Color.White,
    primaryContainer = ReWindDarkPurple,
    onPrimaryContainer = Color.White,
    secondary = ReWindLightPurple,
    onSecondary = ReWindDarkPurple,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ReWindDarkPurple,
    onPrimary = Color.White,
    primaryContainer = ReWindLightPurple,
    onPrimaryContainer = ReWindDarkPurple,
    secondary = ReWindLightPurple,
    onSecondary = ReWindDarkPurple,
    secondaryContainer = ReWindLightPurple,
    onSecondaryContainer = ReWindDarkPurple,
    background = ReWindBackground,
    surface = ReWindSurface,
    surfaceVariant = ReWindLightPurple,
    onSurfaceVariant = Color(0xFF49454F),
    onTertiary = Color.White,
    onBackground = ReWindTextDark,
    onSurface = ReWindTextDark,
)

@Composable
fun RewindJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to strictly follow the Figma design
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
