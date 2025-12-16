package com.hrms.jeejateamozy.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Simple wrapper to ensure back gesture works on any screen.
 *
 * Add this at the TOP of any screen that has onNavigateBack or onBack callback:
 *
 * @Composable
 * fun MyScreen(
 *     onNavigateBack: () -> Unit
 * ) {
 *     // ✅ Add this line at the top
 *     HandleBackGesture(onBack = onNavigateBack)
 *
 *     Scaffold(...) { }
 * }
 */
@Composable
fun HandleBackGesture(onBack: () -> Unit) {
    BackHandler(enabled = true) {
        onBack()
    }
}