package com.example.shyam_assignment.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TwinMindColorScheme = darkColorScheme(
    primary = TwinPrimary,
    onPrimary = TwinOnPrimary,
    secondary = TwinSecondary,
    onSecondary = TwinOnPrimary,
    background = TwinBackground,
    onBackground = TwinTextPrimary,
    surface = TwinSurface,
    onSurface = TwinTextPrimary,
    surfaceVariant = TwinSurfaceVariant,
    onSurfaceVariant = TwinTextSecondary,
    error = TwinError,
    onError = TwinOnPrimary,
    outline = TwinOutline
)

@Suppress("DEPRECATION")
@Composable
fun TwinMindTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = TwinBackground.toArgb()
            window.navigationBarColor = TwinBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = TwinMindColorScheme,
        typography = Typography,
        content = content
    )
}