package com.hrms.jeejateamozy.core.ui

import android.app.Activity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat

/**
 * Utility functions for handling system insets consistently across the app.
 *
 * This ensures proper handling for both:
 * - 3-button navigation (~56dp)
 * - Gesture navigation (~48dp)
 * - Devices with no navigation bar (some tablets)
 */
object InsetUtils {

    /**
     * Get the current navigation bar height as Dp
     * Use this when you need the actual height value
     */
    @Composable
    fun getNavigationBarHeight(): Dp {
        return WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }
}

/**
 * Extension modifier to add navigation bar padding to any composable.
 * Equivalent to Modifier.navigationBarsPadding() but more explicit.
 *
 * Usage:
 * Column(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .safeNavigationBarPadding()
 * )
 */
fun Modifier.safeNavigationBarPadding(): Modifier = composed {
    this.navigationBarsPadding()
}

/**
 * Sets light status bar icons (white) for screens with dark top bars.
 * Automatically restores dark icons (for light backgrounds) when the composable leaves composition.
 *
 * Use this in screens that have a dark-colored TopAppBar (e.g., primary/indigo background)
 * so that status bar icons remain visible on Android 15+ where the bar is always transparent.
 */
@Composable
fun LightStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = false  // white icons
        onDispose {
            controller.isAppearanceLightStatusBars = true  // dark icons (default for light theme)
        }
    }
}