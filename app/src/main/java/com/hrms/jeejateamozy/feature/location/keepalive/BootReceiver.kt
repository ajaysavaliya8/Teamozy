package com.hrms.jeejateamozy.feature.location.keepalive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService

/**
 * Boot receiver - restarts tracking after device reboot
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "🔄 Device booted - checking tracking state")

            val stateManager = TrackingStateManager(context)

            if (stateManager.shouldTrackingBeActive()) {
                Log.d(TAG, "✅ Tracking was active - restarting after boot")
                LocationTrackingService.startTracking(context)
                TrackingWorker.schedule(context)
                TrackingAlarmReceiver.schedule(context)
            } else {
                Log.d(TAG, "⏹️ Tracking was not active")
            }
        }
    }
}