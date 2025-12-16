package com.hrms.jeejateamozy.feature.location.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.util.Log

/**
 * Helper class to monitor GPS status changes
 */
class GpsStatusHelper(
    private val context: Context,
    private val onGpsStatusChanged: (isEnabled: Boolean) -> Unit
) {
    companion object {
        private const val TAG = "GpsStatusHelper"
    }

    private var isRegistered = false

    private val gpsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                val isEnabled = isGpsEnabled()
                Log.d(TAG, "📡 GPS status changed: ${if (isEnabled) "ENABLED" else "DISABLED"}")
                onGpsStatusChanged(isEnabled)
            }
        }
    }

    /**
     * Check if GPS is currently enabled
     */
    fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Start monitoring GPS status changes
     */
    fun startMonitoring() {
        if (isRegistered) return

        try {
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(gpsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(gpsReceiver, filter)
            }
            isRegistered = true
            Log.d(TAG, "✅ GPS status monitoring started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register GPS receiver", e)
        }
    }

    /**
     * Stop monitoring GPS status changes
     */
    fun stopMonitoring() {
        if (!isRegistered) return

        try {
            context.unregisterReceiver(gpsReceiver)
            isRegistered = false
            Log.d(TAG, "✅ GPS status monitoring stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to unregister GPS receiver", e)
        }
    }
}