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
import com.powersense.viewmodels.ThemeOption // <-- IMPORT

// Updated light color scheme using your wireframe colors
private val LightColorScheme = lightColorScheme(
    primary = PowerSensePurple,
    secondary = PowerSenseGreen,
    tertiary = Pink40,
    background = LightGreyBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DarkText,
    onSurface = DarkText,
)

// --- ADD A DARK COLOR SCHEME ---
private val DarkColorScheme = darkColorScheme(
    primary = PowerSensePurple,
    secondary = PowerSenseGreen,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F), // Dark background
    surface = Color(0xFF2C2A2F),    // Dark surface
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE6E1E5), // Light text
    onSurface = Color(0xFFE6E1E5)  // Light text
)


@Composable
fun PowerSenseTheme(
    dynamicColor: Boolean = false,
    themeOption: ThemeOption = ThemeOption.System, // <-- PARAMETER
    content: @Composable () -> Unit
) {
    // DETERMINE darkTheme from themeOption
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
            window.statusBarColor = colorScheme.background.toArgb() // Set status bar color
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}