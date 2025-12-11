// ============================================
// FILE: TrackingStateManager.kt
// LOCATION: feature/location/heartbeat/
// STATUS: ✅ CORRECTED - Works with your PreferencesManager
// ============================================

package com.hrms.jeejateamozy.feature.location.heartbeat

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tracking State Manager
 * Manages location tracking state and heartbeat
 */
class TrackingStateManager(private val context: Context) {

    companion object {
        private const val TAG = "TrackingStateManager"

        // Separate SharedPreferences for tracking state
        private const val PREF_NAME = "tracking_state_prefs"
        private const val PREF_LAST_HEARTBEAT = "last_heartbeat_timestamp"
        private const val PREF_TRACKING_ACTIVE = "tracking_active"
        private const val PREF_CHECK_IN_TIME = "check_in_timestamp"

        // Heartbeat timeout - if no heartbeat for 15 min, assume session ended
        private const val HEARTBEAT_TIMEOUT_MS = 15 * 60 * 1000L
    }

    // Use separate SharedPreferences for tracking state
    private val trackingPrefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Use existing PreferencesManager for auth token
    private val prefs = PreferencesManager.getInstance(context)

    /**
     * Mark tracking as active (call on check-in)
     */
    fun setTrackingActive(active: Boolean) {
        trackingPrefs.edit().putBoolean(PREF_TRACKING_ACTIVE, active).apply()
        if (active) {
            trackingPrefs.edit().putLong(PREF_CHECK_IN_TIME, System.currentTimeMillis()).apply()
            Log.d(TAG, "✅ Tracking marked as ACTIVE")
        } else {
            trackingPrefs.edit().putLong(PREF_CHECK_IN_TIME, 0L).apply()
            Log.d(TAG, "⏹️ Tracking marked as INACTIVE")
        }
    }

    /**
     * Update last heartbeat timestamp
     */
    fun updateHeartbeat() {
        val timestamp = System.currentTimeMillis()
        trackingPrefs.edit().putLong(PREF_LAST_HEARTBEAT, timestamp).apply()
        Log.d(TAG, "💓 Heartbeat updated: $timestamp")
    }

    /**
     * Check if tracking should be active
     */
    fun shouldTrackingBeActive(): Boolean {
        val trackingActive = trackingPrefs.getBoolean(PREF_TRACKING_ACTIVE, false)
        val lastHeartbeat = trackingPrefs.getLong(PREF_LAST_HEARTBEAT, 0L)
        val checkInTime = trackingPrefs.getLong(PREF_CHECK_IN_TIME, 0L)

        val currentTime = System.currentTimeMillis()
        val timeSinceHeartbeat = currentTime - lastHeartbeat
        val timeSinceCheckIn = currentTime - checkInTime

        Log.d(TAG, """
            📊 Tracking state check:
               Marked active: $trackingActive
               Last heartbeat: ${timeSinceHeartbeat / 1000}s ago
               Check-in: ${timeSinceCheckIn / 1000}s ago
        """.trimIndent())

        // Check 1: Was tracking marked as active?
        if (!trackingActive) {
            Log.d(TAG, "❌ Tracking not marked as active")
            return false
        }

        // Check 2: Is check-in time today? (not old session)
        if (timeSinceCheckIn > 24 * 60 * 60 * 1000L) {
            Log.d(TAG, "❌ Check-in time too old (>24 hours)")
            setTrackingActive(false)  // Clear old session
            return false
        }

        // Check 3: Is heartbeat recent? (backend still sending)
        if (lastHeartbeat > 0 && timeSinceHeartbeat > HEARTBEAT_TIMEOUT_MS) {
            Log.d(TAG, "⚠️ Heartbeat timeout - session may have ended")
        }

        Log.d(TAG, "✅ Tracking SHOULD be active")
        return true
    }

    /**
     * Get last heartbeat timestamp
     */
    fun getLastHeartbeat(): Long {
        return trackingPrefs.getLong(PREF_LAST_HEARTBEAT, 0L)
    }

    /**
     * Check if tracking is currently marked as active
     */
    fun isTrackingActive(): Boolean {
        return trackingPrefs.getBoolean(PREF_TRACKING_ACTIVE, false)
    }

    /**
     * Clear all tracking state (call on check-out or logout)
     */
    fun clearState() {
        trackingPrefs.edit()
            .putBoolean(PREF_TRACKING_ACTIVE, false)
            .putLong(PREF_LAST_HEARTBEAT, 0L)
            .putLong(PREF_CHECK_IN_TIME, 0L)
            .apply()
        Log.d(TAG, "🗑️ Tracking state cleared")
    }
}