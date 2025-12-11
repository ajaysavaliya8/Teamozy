package com.hrms.jeejateamozy.feature.location.heartbeat

import android.content.Context
import android.util.Log
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.hrms.jeejateamozy.core.network.NetworkModule

/**
 * Tracking State Manager
 *
 * Manages and checks if location tracking should be active
 * Used by FCM heartbeat to determine if service needs restart
 */
class TrackingStateManager(private val context: Context) {

    companion object {
        private const val TAG = "TrackingStateManager"

        private const val PREF_LAST_HEARTBEAT = "last_heartbeat_timestamp"
        private const val PREF_TRACKING_ACTIVE = "tracking_active"
        private const val PREF_CHECK_IN_TIME = "check_in_timestamp"

        // Heartbeat timeout - if no heartbeat for 15 min, assume session ended
        private const val HEARTBEAT_TIMEOUT_MS = 15 * 60 * 1000L
    }

    private val prefs = PreferencesManager.getInstance(context)

    /**
     * Mark tracking as active (call on check-in)
     */
    fun setTrackingActive(active: Boolean) {
        prefs.saveBoolean(PREF_TRACKING_ACTIVE, active)
        if (active) {
            prefs.saveLong(PREF_CHECK_IN_TIME, System.currentTimeMillis())
            Log.d(TAG, "✅ Tracking marked as ACTIVE")
        } else {
            prefs.saveLong(PREF_CHECK_IN_TIME, 0L)
            Log.d(TAG, "⏹️ Tracking marked as INACTIVE")
        }
    }

    /**
     * Update last heartbeat timestamp
     */
    fun updateHeartbeat() {
        val timestamp = System.currentTimeMillis()
        prefs.saveLong(PREF_LAST_HEARTBEAT, timestamp)
        Log.d(TAG, "💓 Heartbeat updated: $timestamp")
    }

    /**
     * Check if tracking should be active
     *
     * Returns true if:
     * 1. Tracking was marked as active (checked in)
     * 2. Last heartbeat was recent (within timeout)
     * 3. Check-in time is today (not old session)
     */
    fun shouldTrackingBeActive(): Boolean {
        val trackingActive = prefs.getBoolean(PREF_TRACKING_ACTIVE, false)
        val lastHeartbeat = prefs.getLong(PREF_LAST_HEARTBEAT, 0L)
        val checkInTime = prefs.getLong(PREF_CHECK_IN_TIME, 0L)

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
            // Don't clear yet - could be network issue
            // Let it try to sync, backend will return 403 if session ended
        }

        Log.d(TAG, "✅ Tracking SHOULD be active")
        return true
    }

    /**
     * Get last heartbeat timestamp
     */
    fun getLastHeartbeat(): Long {
        return prefs.getLong(PREF_LAST_HEARTBEAT, 0L)
    }

    /**
     * Check if tracking is currently marked as active
     */
    fun isTrackingActive(): Boolean {
        return prefs.getBoolean(PREF_TRACKING_ACTIVE, false)
    }

    /**
     * Verify with backend if session is still active
     * Call this if heartbeat is old but local state says active
     */
    suspend fun verifySessionWithBackend(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = prefs.getAuthToken()
                if (token.isNullOrBlank()) {
                    Log.e(TAG, "No auth token")
                    return@withContext false
                }

                // Try to get attendance status
                val response = NetworkModule.apiService.getAttendanceStatus()

                if (response.isSuccessful) {
                    val status = response.body()?.data?.current_state
                    val sessionActive = status == "CHECK_OUT_NEEDED"

                    Log.d(TAG, "Backend verification: Session active = $sessionActive")

                    if (!sessionActive) {
                        // Backend says no active session - clear local state
                        setTrackingActive(false)
                    }

                    return@withContext sessionActive
                } else {
                    Log.e(TAG, "Failed to verify session: ${response.code()}")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception verifying session", e)
                return@withContext false
            }
        }
    }

    /**
     * Clear all tracking state (call on check-out or logout)
     */
    fun clearState() {
        prefs.saveBoolean(PREF_TRACKING_ACTIVE, false)
        prefs.saveLong(PREF_LAST_HEARTBEAT, 0L)
        prefs.saveLong(PREF_CHECK_IN_TIME, 0L)
        Log.d(TAG, "🗑️ Tracking state cleared")
    }
}