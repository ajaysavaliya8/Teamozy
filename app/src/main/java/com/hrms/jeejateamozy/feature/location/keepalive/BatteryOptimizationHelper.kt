package com.hrms.jeejateamozy.feature.location.keepalive

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog

/**
 * Battery Optimization Helper
 *
 * Helps request exemption from battery optimization
 * This greatly improves background service reliability
 *
 * Benefits:
 * - Android won't aggressively kill your service
 * - Background tasks run more reliably
 * - Alarms and WorkManager execute more consistently
 * - Overall 90%+ reliability improvement
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptimization"

    /**
     * Check if app is exempted from battery optimization
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true  // Not applicable on older versions
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName

        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    /**
     * Request battery optimization exemption with explanation dialog
     * Call this on check-in or when setting up tracking
     */
    fun requestExemption(activity: Activity) {
        if (isIgnoringBatteryOptimizations(activity)) {
            Log.d(TAG, "✅ Already exempted from battery optimization")
            return
        }

        // Show explanation dialog
        showExplanationDialog(activity)
    }

    /**
     * Show explanation dialog before requesting exemption
     */
    private fun showExplanationDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Allow Background Location Tracking")
            .setMessage(
                "For accurate attendance tracking during work hours, Teamozy needs to run " +
                        "in the background without restrictions.\n\n" +
                        "This allows:\n" +
                        "• Continuous location tracking\n" +
                        "• Automatic service restart if stopped\n" +
                        "• Reliable attendance records\n\n" +
                        "Your privacy is protected - tracking only runs during checked-in hours."
            )
            .setPositiveButton("Allow") { _, _ ->
                openBatteryOptimizationSettings(activity)
            }
            .setNegativeButton("Later") { dialog, _ ->
                dialog.dismiss()
                Log.d(TAG, "⏸️ User declined battery optimization exemption")
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Open battery optimization settings
     */
    private fun openBatteryOptimizationSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        try {
            // Try to open app-specific battery settings
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)

            Log.d(TAG, "✅ Opened battery optimization settings")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open app-specific settings, trying general settings", e)

            try {
                // Fallback: Open general battery optimization settings
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                activity.startActivity(fallbackIntent)

                Log.d(TAG, "✅ Opened general battery optimization settings")

            } catch (e2: Exception) {
                Log.e(TAG, "❌ Failed to open battery settings", e2)
            }
        }
    }

    /**
     * Show status message about battery optimization
     */
    fun showStatusMessage(context: Context): String {
        return if (isIgnoringBatteryOptimizations(context)) {
            "✅ Battery optimization: Disabled (Good for tracking)"
        } else {
            "⚠️ Battery optimization: Enabled (May affect tracking)"
        }
    }

    /**
     * Get manufacturer-specific battery optimization instructions
     */
    fun getManufacturerInstructions(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()

        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                """
                Xiaomi/MIUI Instructions:
                1. Go to Settings → Apps → Manage apps
                2. Find Teamozy
                3. Battery saver → No restrictions
                4. Autostart → Allow
                5. Background activity → Allow
                """.trimIndent()
            }

            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                """
                Huawei/Honor Instructions:
                1. Go to Settings → Apps → Teamozy
                2. Battery → App launch → Manage manually
                3. Enable: Auto-launch, Secondary launch, Run in background
                4. Go back → Notifications → Allow all notifications
                """.trimIndent()
            }

            manufacturer.contains("samsung") -> {
                """
                Samsung Instructions:
                1. Go to Settings → Apps → Teamozy
                2. Battery → Optimize battery usage
                3. Select "All apps" → Find Teamozy → Turn OFF
                4. Go back → Permissions → Location → Allow all the time
                """.trimIndent()
            }

            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                """
                Oppo/Realme Instructions:
                1. Go to Settings → Battery → Battery optimization
                2. Find Teamozy → Don't optimize
                3. Settings → Apps → Teamozy
                4. App auto-launch → Allow
                """.trimIndent()
            }

            manufacturer.contains("vivo") -> {
                """
                Vivo Instructions:
                1. Go to Settings → Battery → Background power consumption management
                2. Find Teamozy → Allow background activity
                3. Settings → Apps → Teamozy
                4. Auto-start → Allow
                """.trimIndent()
            }

            else -> {
                """
                General Instructions:
                1. Go to Settings → Apps → Teamozy
                2. Battery → Battery optimization → Don't optimize
                3. Permissions → Location → Allow all the time
                """.trimIndent()
            }
        }
    }

    /**
     * Show manufacturer-specific instructions dialog
     */
    fun showManufacturerInstructions(activity: Activity) {
        val instructions = getManufacturerInstructions()

        AlertDialog.Builder(activity)
            .setTitle("Improve Tracking Reliability")
            .setMessage(instructions)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}