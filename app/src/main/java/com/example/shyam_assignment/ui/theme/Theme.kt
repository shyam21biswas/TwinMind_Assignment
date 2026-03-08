package com.example.shyam_assignment.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material 3 light color scheme using the TwinMind palette.
 * Maps our custom colors to Material 3's color roles.
 */
private val TwinMindColorScheme = lightColorScheme(
    primary = TwinPrimary,               // Deep teal — buttons, accents, links
    onPrimary = TwinOnPrimary,           // White text on teal
    secondary = TwinSecondary,           // Orange — secondary actions
    onSecondary = TwinOnPrimary,         // White text on orange
    background = TwinBackground,         // Warm off-white page background
    onBackground = TwinTextPrimary,      // Dark navy text on background
    surface = TwinSurface,               // Pure white card surfaces
    onSurface = TwinTextPrimary,         // Dark navy text on cards
    surfaceVariant = TwinSurfaceVariant, // Subtle warm gray
    onSurfaceVariant = TwinTextSecondary,// Muted blue-gray text
    error = TwinError,                   // Red for errors
    onError = TwinOnPrimary,             // White text on red
    outline = TwinOutline                // Light gray borders
)

/**
 * Applies the TwinMind light theme to the entire app.
 * Sets status bar and navigation bar colors to match the warm off-white background.
 * Uses dark (black) status bar icons since the background is light.
 */
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
            // Light theme → dark status bar icons
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = TwinMindColorScheme,
        typography = Typography,
        content = content
    )
}