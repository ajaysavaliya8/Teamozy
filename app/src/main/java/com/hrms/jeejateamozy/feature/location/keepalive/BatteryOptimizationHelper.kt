package com.hrms.jeejateamozy.feature.location.keepalive

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Battery optimization helper
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptimizationHelper"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestExemption(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
            Log.d(TAG, "✅ Requested battery optimization exemption")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to request battery optimization exemption", e)
        }
    }
}