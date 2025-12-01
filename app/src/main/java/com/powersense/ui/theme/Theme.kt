package com.powersense.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.powersense.viewmodels.ThemeOption

private val LightColorScheme = lightColorScheme(
    primary = PowerSensePurple,
    secondary = PowerSenseGreen,
    tertiary = PowerSenseOrange,

    background = CozyBackground,
    surface = CozySurface,
    surfaceVariant = CozySurfaceVariant,

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,

    onBackground = CozyPrimaryText,
    onSurface = CozyPrimaryText,
    onSurfaceVariant = CozySecondaryText,

    outline = CozyOutline,
    outlineVariant = CozyOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = PowerSensePurple,
    secondary = PowerSenseGreen,
    tertiary = PowerSenseOrange,

    // UPDATED: A slightly lighter "Greyish" background (not total black)
    background = Color(0xFF202124),

    // UPDATED: Surface matches background for seamless headers in dark mode
    // or slightly lighter for cards. Let's keep surface consistent.
    surface = Color(0xFF303134),

    surfaceVariant = DarkCardSurface,

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,

    onBackground = DarkPrimaryText,
    onSurface = DarkPrimaryText,
    onSurfaceVariant = DarkSecondaryText,

    outline = DarkOutline,
    outlineVariant = DarkOutline
)

@Composable
fun PowerSenseTheme(
    dynamicColor: Boolean = false,
    themeOption: ThemeOption = ThemeOption.System,
    content: @Composable () -> Unit
) {
    val darkTheme = when(themeOption) {
        ThemeOption.Light -> false
        ThemeOption.Dark -> true
        ThemeOption.System -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            if (darkTheme) {
                // Dark Mode: Use the NEW Greyish Background for Status Bar
                // This ensures the status bar matches the app background
                window.statusBarColor = Color(0xFF202124).toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            } else {
                // Light Mode: Dark Mocha Header
                val aestheticDarkHeader = Color(0xFF4E463F)
                window.statusBarColor = aestheticDarkHeader.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}