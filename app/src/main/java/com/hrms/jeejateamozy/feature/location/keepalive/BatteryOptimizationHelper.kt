package com.hrms.jeejateamozy.feature.location.keepalive

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Enhanced Battery Optimization Helper
 *
 * Handles both standard Android battery optimization AND
 * OEM-specific battery/autostart settings for aggressive manufacturers
 * (Xiaomi, Samsung, Huawei, Oppo, Vivo, etc.)
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptHelper"

    // ==========================================
    // STANDARD ANDROID BATTERY OPTIMIZATION
    // ==========================================

    /**
     * Check if app is exempt from battery optimization
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Request standard Android battery optimization exemption
     */
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

    // ==========================================
    // OEM-SPECIFIC AUTOSTART/BACKGROUND SETTINGS
    // ==========================================

    /**
     * List of known OEM-specific intents for autostart/background permissions
     * These are manufacturer-specific settings that override Android's standard behavior
     */
    private val OEM_INTENTS = listOf(
        // Xiaomi MIUI
        Intent().setComponent(ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )),

        // Xiaomi MIUI (alternate)
        Intent().setComponent(ComponentName(
            "com.miui.securitycenter",
            "com.miui.powercenter.PowerSettings"
        )),

        // Samsung
        Intent().setComponent(ComponentName(
            "com.samsung.android.lool",
            "com.samsung.android.sm.ui.battery.BatteryActivity"
        )),

        // Samsung (alternate - Device Care)
        Intent().setComponent(ComponentName(
            "com.samsung.android.lool",
            "com.samsung.android.sm.battery.ui.BatteryActivity"
        )),

        // Huawei
        Intent().setComponent(ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
        )),

        // Huawei (alternate)
        Intent().setComponent(ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.optimize.process.ProtectActivity"
        )),

        // Oppo ColorOS
        Intent().setComponent(ComponentName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        )),

        // Oppo (alternate)
        Intent().setComponent(ComponentName(
            "com.oppo.safe",
            "com.oppo.safe.permission.startup.StartupAppListActivity"
        )),

        // Vivo
        Intent().setComponent(ComponentName(
            "com.vivo.permissionmanager",
            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
        )),

        // Vivo (alternate)
        Intent().setComponent(ComponentName(
            "com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
        )),

        // OnePlus OxygenOS
        Intent().setComponent(ComponentName(
            "com.oneplus.security",
            "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
        )),

        // Asus ZenUI
        Intent().setComponent(ComponentName(
            "com.asus.mobilemanager",
            "com.asus.mobilemanager.MainActivity"
        )),

        // Letv
        Intent().setComponent(ComponentName(
            "com.letv.android.letvsafe",
            "com.letv.android.letvsafe.AutobootManageActivity"
        )),

        // HTC
        Intent().setComponent(ComponentName(
            "com.htc.pitroad",
            "com.htc.pitroad.landingpage.activity.LandingPageActivity"
        )),

        // Meizu
        Intent().setComponent(ComponentName(
            "com.meizu.safe",
            "com.meizu.safe.security.SHOW_APPSEC"
        )),

        // Transsion (Tecno, Infinix, itel)
        Intent().setComponent(ComponentName(
            "com.transsion.phonemanager",
            "com.itel.autobootmanager.activity.AutoBootMgrActivity"
        ))
    )

    /**
     * Check if current device is from a manufacturer with aggressive battery optimization
     */
    fun isAggressiveOEM(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer in listOf(
            "xiaomi", "redmi", "poco",
            "huawei", "honor",
            "oppo", "realme",
            "vivo", "iqoo",
            "samsung",
            "oneplus",
            "meizu",
            "asus",
            "letv", "leeco",
            "tecno", "infinix", "itel"
        )
    }

    /**
     * Get the manufacturer name for display
     */
    fun getManufacturerName(): String {
        return Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    }

    /**
     * Try to open OEM-specific autostart/background settings
     * Returns true if successfully opened, false otherwise
     */
    fun openOEMSettings(context: Context): Boolean {
        for (intent in OEM_INTENTS) {
            try {
                if (isIntentAvailable(context, intent)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.d(TAG, "✅ Opened OEM settings: ${intent.component}")
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Intent not available: ${intent.component}")
            }
        }

        Log.w(TAG, "⚠️ No OEM settings found for ${Build.MANUFACTURER}")
        return false
    }

    /**
     * Check if an intent can be resolved
     */
    private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return try {
            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            resolveInfo != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Open app info settings (fallback)
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "✅ Opened app settings")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open app settings", e)
        }
    }

    /**
     * Get instructions for the current device manufacturer
     */
    fun getOEMInstructions(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()

        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> """
                📱 Xiaomi/Redmi/POCO Settings:
                1. Go to Settings → Apps → Manage apps
                2. Find "Teamozy" → Autostart → Enable
                3. Battery saver → No restrictions
                4. Also check: Security app → Permissions → Autostart
            """.trimIndent()

            manufacturer.contains("samsung") -> """
                📱 Samsung Settings:
                1. Go to Settings → Apps → Teamozy
                2. Battery → Unrestricted
                3. Also check: Device Care → Battery → App power management
                4. Add Teamozy to "Never sleeping apps"
            """.trimIndent()

            manufacturer.contains("huawei") || manufacturer.contains("honor") -> """
                📱 Huawei/Honor Settings:
                1. Go to Settings → Apps → Teamozy
                2. Battery → Launch → Manage manually → Enable all
                3. Also check: Phone Manager → App launch
                4. Turn off "Manage automatically" for Teamozy
            """.trimIndent()

            manufacturer.contains("oppo") || manufacturer.contains("realme") -> """
                📱 Oppo/Realme Settings:
                1. Go to Settings → App Management → Teamozy
                2. Power Consumption Protection → Disable
                3. Allow Auto-start → Enable
                4. Also check: Battery → Power Saving Exclusions → Add Teamozy
            """.trimIndent()

            manufacturer.contains("vivo") -> """
                📱 Vivo Settings:
                1. Go to Settings → Battery → Background power consumption
                2. Find Teamozy → High background power consumption
                3. Also enable Autostart for Teamozy
            """.trimIndent()

            manufacturer.contains("oneplus") -> """
                📱 OnePlus Settings:
                1. Go to Settings → Apps → Teamozy
                2. Battery → Don't optimize
                3. Also check: Battery → Battery optimization → All apps
            """.trimIndent()

            else -> """
                📱 General Settings:
                1. Go to Settings → Apps → Teamozy
                2. Battery → Unrestricted / Don't optimize
                3. Look for "Autostart" or "Background activity" settings
            """.trimIndent()
        }
    }

    /**
     * Complete setup for maximum background reliability
     * Call this after check-in to ensure location tracking works
     */
    fun requestFullBackgroundPermissions(activity: Activity, onComplete: () -> Unit = {}) {
        // Step 1: Standard Android battery optimization
        if (!isIgnoringBatteryOptimizations(activity)) {
            requestExemption(activity)
        }

        // Step 2: For aggressive OEMs, we need to inform the user
        if (isAggressiveOEM()) {
            Log.w(TAG, "⚠️ Aggressive OEM detected: ${Build.MANUFACTURER}")
            Log.w(TAG, "User may need to manually enable autostart/background permissions")
        }

        onComplete()
    }
}