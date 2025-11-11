package com.hrms.jeejateamozy.feature.home.presentation.utils

import android.util.Log

private const val TAG = "HomeUtils"

/**
 * Calculate elapsed seconds from last check-in time
 * Format expected: "yyyy-MM-dd HH:mm:ss"
 */
fun calculateElapsedSeconds(lastCheckInTime: String?): Int {
    if (lastCheckInTime.isNullOrBlank()) return 0

    try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val checkInDate = formatter.parse(lastCheckInTime) ?: return 0
        val now = java.util.Date()
        val elapsedMillis = now.time - checkInDate.time
        val elapsedSeconds = (elapsedMillis / 1000).toInt()
        return if (elapsedSeconds >= 0) elapsedSeconds else 0
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing last_check_in_time: $lastCheckInTime", e)
        return 0
    }
}

/**
 * Format elapsed seconds to HH:MM:SS format
 */
fun formatElapsedTime(elapsedSeconds: Int): String {
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}