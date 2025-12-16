package com.hrms.jeejateamozy.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp

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