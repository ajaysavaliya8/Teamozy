package com.hrms.jeejateamozy.feature.location.keepalive

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService

/**
 * AlarmManager keep-alive (every 10 minutes)
 *
 * Uses setExactAndAllowWhileIdle() instead of setRepeating() so alarms
 * fire reliably even in Doze / power saving mode. Each alarm self-reschedules
 * the next one in onReceive().
 */
class TrackingAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TrackingAlarmReceiver"
        private const val ACTION_CHECK = "com.hrms.jeejateamozy.ACTION_TRACKING_CHECK"
        private const val INTERVAL_MS = 10 * 60 * 1000L // 10 minutes

        fun schedule(context: Context) {
            scheduleNext(context)
            Log.d(TAG, "✅ AlarmManager scheduled (Doze-safe)")
        }

        private fun scheduleNext(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = getPendingIntent(context)
            val triggerAt = SystemClock.elapsedRealtime() + INTERVAL_MS

            // setExactAndAllowWhileIdle fires even in Doze mode
            // Unlike setRepeating which gets batched/deferred
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(getPendingIntent(context))
            Log.d(TAG, "⏹️ AlarmManager cancelled")
        }

        private fun getPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, TrackingAlarmReceiver::class.java).apply {
                action = ACTION_CHECK
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CHECK) return

        Log.d(TAG, "🔄 Alarm check triggered")

        val stateManager = TrackingStateManager(context)

        if (stateManager.shouldTrackingBeActive()) {
            Log.d(TAG, "✅ Tracking should be active - ensuring service is running")
            LocationTrackingService.startTracking(context)

            // Self-reschedule for next check (since we're not using setRepeating)
            scheduleNext(context)
        } else {
            Log.d(TAG, "⏹️ Tracking should NOT be active - not rescheduling")
        }
    }
}
