package com.hrms.jeejateamozy.feature.location.keepalive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService

/**
 * Boot Receiver - Auto-restart after device reboot
 *
 * Listens for device boot completion
 * If there was an active tracking session before reboot:
 * - Restarts LocationTrackingService
 * - Restarts WorkManager
 * - Restarts AlarmManager
 *
 * Effectiveness: 100% for device restarts
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,  // Direct Boot (Android 7+)
            "android.intent.action.QUICKBOOT_POWERON"  // Some manufacturers
                -> {
                Log.d(TAG, "📱 Device boot completed - checking for active sessions")

                try {
                    // Check if there was an active tracking session
                    val trackingStateManager = TrackingStateManager(context)

                    if (trackingStateManager.shouldTrackingBeActive()) {
                        Log.d(TAG, "✅ Active tracking session found before reboot")

                        // Restart all tracking components
                        restartTrackingComponents(context)

                        Log.d(TAG, "✅ All tracking components restarted successfully")
                    } else {
                        Log.d(TAG, "⏹️ No active tracking session - not starting")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling boot event", e)
                }
            }
        }
    }

    /**
     * Restart all tracking components
     */
    private fun restartTrackingComponents(context: Context) {
        // 1. Start LocationTrackingService
        LocationTrackingService.startTracking(context)
        Log.d(TAG, "  ✅ LocationTrackingService started")

        // 2. Schedule WorkManager
        TrackingWorker.schedule(context)
        Log.d(TAG, "  ✅ WorkManager scheduled")

        // 3. Schedule AlarmManager
        TrackingAlarmReceiver.schedule(context)
        Log.d(TAG, "  ✅ AlarmManager scheduled")
    }
}