package com.hrms.jeejateamozy.feature.location.keepalive

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService
import android.app.ActivityManager

/**
 * AlarmManager Receiver - Precise Periodic Check
 *
 * More frequent and precise than WorkManager
 * Checks every 10 minutes if tracking should be active
 * Automatically restarts service if needed
 *
 * Advantages:
 * - More frequent checks (10 min vs 15 min)
 * - More precise timing
 * - Can wake device
 * - Survives app kills
 */
class TrackingAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TrackingAlarmReceiver"
        private const val ALARM_REQUEST_CODE = 12346
        private const val CHECK_INTERVAL_MS = 10 * 60 * 1000L  // 10 minutes

        /**
         * Schedule tracking alarm
         * Call this on check-in
         */
        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, TrackingAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calculate next trigger time
            val triggerTime = System.currentTimeMillis() + CHECK_INTERVAL_MS

            // Schedule alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6+ - allow while idle for battery efficiency
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d(TAG, "✅ Alarm scheduled for ${CHECK_INTERVAL_MS / 1000}s from now")
        }

        /**
         * Schedule exact alarm (Android 12+)
         * More precise but requires permission
         */
        fun scheduleExact(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // Fall back to regular alarm on older versions
                schedule(context)
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Check if can schedule exact alarms
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "⚠️ Cannot schedule exact alarms - permission not granted")
                schedule(context)  // Fall back to regular alarm
                return
            }

            val intent = Intent(context, TrackingAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + CHECK_INTERVAL_MS

            // Schedule exact alarm
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

            Log.d(TAG, "✅ Exact alarm scheduled for ${CHECK_INTERVAL_MS / 1000}s from now")
        }

        /**
         * Cancel tracking alarm
         * Call this on check-out
         */
        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, TrackingAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "⏹️ Alarm cancelled")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 Tracking alarm triggered")

        try {
            val trackingStateManager = TrackingStateManager(context)

            // Check if tracking should be active
            val shouldBeActive = trackingStateManager.shouldTrackingBeActive()

            if (shouldBeActive) {
                // Check if service is running
                val isRunning = isLocationServiceRunning(context)

                if (!isRunning) {
                    Log.w(TAG, "⚠️ Service not running but should be - restarting")
                    LocationTrackingService.startTracking(context)
                    Log.d(TAG, "✅ Service restarted by AlarmManager")
                } else {
                    Log.d(TAG, "✅ Service is running correctly")
                }

                // Reschedule next alarm
                schedule(context)
            } else {
                Log.d(TAG, "⏹️ Tracking should NOT be active - not rescheduling")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in alarm receiver", e)
            // Reschedule to try again
            schedule(context)
        }
    }

    /**
     * Check if LocationTrackingService is running
     */
    private fun isLocationServiceRunning(context: Context): Boolean {
        return try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            manager.getRunningServices(Int.MAX_VALUE).any { service ->
                service.service.className == LocationTrackingService::class.java.name
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service status", e)
            false
        }
    }
}