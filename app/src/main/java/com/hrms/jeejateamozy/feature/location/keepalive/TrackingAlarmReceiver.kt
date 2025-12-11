package com.hrms.jeejateamozy.feature.location.keepalive

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
// import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService  // ✅ DISABLED

/**
 * AlarmManager keep-alive (every 10 minutes)
 */
class TrackingAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TrackingAlarmReceiver"
        private const val ACTION_CHECK = "com.hrms.jeejateamozy.ACTION_TRACKING_CHECK"
        private const val INTERVAL_MS = 10 * 60 * 1000L

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TrackingAlarmReceiver::class.java).apply {
                action = ACTION_CHECK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INTERVAL_MS,
                INTERVAL_MS,
                pendingIntent
            )

            Log.d(TAG, "✅ AlarmManager scheduled")
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TrackingAlarmReceiver::class.java).apply {
                action = ACTION_CHECK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "⏹️ AlarmManager cancelled")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CHECK) {
            Log.d(TAG, "🔄 Alarm check triggered")

            val stateManager = TrackingStateManager(context)

            if (stateManager.shouldTrackingBeActive()) {
                Log.d(TAG, "✅ Tracking should be active - ensuring service is running")
                // LocationTrackingService.startTracking(context)  // ✅ DISABLED
            } else {
                Log.d(TAG, "⏹️ Tracking should NOT be active")
            }
        }
    }
}