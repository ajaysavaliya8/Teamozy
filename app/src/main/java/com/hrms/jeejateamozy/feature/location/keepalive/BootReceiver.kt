package com.hrms.jeejateamozy.feature.location.keepalive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService

/**
 * Boot & restart receiver — ensures tracking survives:
 * - Device reboot (normal + quick boot)
 * - App update (MY_PACKAGE_REPLACED)
 * - Direct boot (LOCKED_BOOT_COMPLETED on Android 7+)
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val isBootEvent = action in listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )

        if (!isBootEvent) return

        Log.d(TAG, "🔄 Restart event: $action — checking tracking state")

        val stateManager = TrackingStateManager(context)

        if (stateManager.shouldTrackingBeActive()) {
            Log.d(TAG, "✅ Tracking was active — restarting service + keep-alives")
            LocationTrackingService.startTracking(context)
            TrackingWorker.schedule(context)
            TrackingAlarmReceiver.schedule(context)
        } else {
            Log.d(TAG, "⏹️ Tracking was not active — no action needed")
        }
    }
}
