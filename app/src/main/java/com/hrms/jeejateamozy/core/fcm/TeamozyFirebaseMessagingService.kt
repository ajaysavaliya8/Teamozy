package com.hrms.jeejateamozy.core.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingWorker
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingAlarmReceiver
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService
import com.hrms.jeejateamozy.feature.notification.utils.NotificationHelper
import org.json.JSONObject

/**
 * Firebase Cloud Messaging Service
 * Handles push notifications
 *
 * NOTE: This service receives messages ONLY when:
 * 1. App is in foreground
 * 2. App is in background BUT message contains ONLY data payload (no notification payload)
 */
class TeamozyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "TeamozyFCM"

        // Notification types - Tracking
        private const val TYPE_HEARTBEAT = "heartbeat"
        private const val TYPE_KEEP_ALIVE = "keep_alive"
        private const val TYPE_TRACKING_START = "tracking_start"
        private const val TYPE_TRACKING_STOP = "tracking_stop"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "✅ FCM Token refreshed: ${token.take(20)}...")
        PreferencesManager.getInstance(this).fcmToken = token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "========================================")
        Log.d(TAG, "📨 FCM MESSAGE RECEIVED")
        Log.d(TAG, "  From: ${message.from}")
        Log.d(TAG, "  Data: ${message.data}")
        Log.d(TAG, "========================================")

        val data = message.data
        val type = data["type"] ?: ""

        when (type) {
            TYPE_HEARTBEAT, TYPE_KEEP_ALIVE -> handleHeartbeat()
            TYPE_TRACKING_START -> startTracking()
            TYPE_TRACKING_STOP -> stopTracking()
            else -> handleNotification(data, message.notification)
        }
    }

    /**
     * Handle all notification types (circular, leave, attendance, general)
     */
    private fun handleNotification(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val title = data["title"] ?: notification?.title ?: "Teamozy"
        val messageText = data["message"] ?: notification?.body ?: ""
        val type = data["type"] ?: "general"
        val priority = data["priority"] ?: "normal"

        if (messageText.isEmpty()) {
            Log.d(TAG, "⚠️ Empty notification message, skipping")
            return
        }

        // Build extras for deep linking
        val extras = mutableMapOf<String, String>()
        data["circular_id"]?.let { extras["circular_id"] = it }
        data["leave_id"]?.let { extras["leave_id"] = it }
        data["date"]?.let { extras["date"] = it }
        data["notification_uid"]?.let { extras["notification_uid"] = it }

        // Parse nested "data" JSON for action and screen
        var action: String? = null
        val nestedData = data["data"]
        if (!nestedData.isNullOrBlank()) {
            try {
                val json = JSONObject(nestedData)
                action = json.optString("action", "").ifEmpty { null }
                val screen = json.optString("screen", "").ifEmpty { null }
                screen?.let { extras["screen"] = it }
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Failed to parse nested data JSON", e)
            }
        }

        // Generate unique notification ID
        val notificationId = data["circular_id"]?.toIntOrNull()
            ?: data["leave_id"]?.toIntOrNull()
            ?: System.currentTimeMillis().toInt()

        Log.d(TAG, "📬 Showing notification - Type: $type, Title: $title, Action: $action")

        NotificationHelper.showNotification(
            context = applicationContext,
            notificationId = notificationId,
            title = title,
            message = messageText,
            type = type,
            priority = priority,
            extras = extras
        )

        // Handle location tracking actions
        when (action) {
            "start_location_tracking" -> {
                Log.d(TAG, "🚀 Bulk check-in: starting location tracking")
                startTracking()
                LocationTrackingService.startTracking(applicationContext)
                NotificationEventBus.notifyAttendanceRefresh()
            }
            "stop_location_tracking" -> {
                Log.d(TAG, "⏹️ Bulk checkout: stopping location tracking")
                stopTracking()
                LocationTrackingService.stopTracking(applicationContext)
                NotificationEventBus.notifyAttendanceRefresh()
            }
        }

        // Notify the notification screen and home screen to refresh
        NotificationEventBus.notifyNewNotification()
    }

    // ============================================
    // TRACKING HANDLERS
    // ============================================

    private fun handleHeartbeat() {
        try {
            val stateManager = TrackingStateManager(this)
            if (stateManager.shouldTrackingBeActive()) {
                Log.d(TAG, "✅ Processing heartbeat")
                stateManager.updateHeartbeat()
                TrackingWorker.schedule(this)
                TrackingAlarmReceiver.schedule(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling heartbeat", e)
        }
    }

    private fun startTracking() {
        try {
            val stateManager = TrackingStateManager(this)
            stateManager.setTrackingActive(true)
            stateManager.updateHeartbeat()
            TrackingWorker.schedule(this)
            TrackingAlarmReceiver.schedule(this)
            Log.d(TAG, "✅ Tracking started via FCM")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting tracking", e)
        }
    }

    private fun stopTracking() {
        try {
            val stateManager = TrackingStateManager(this)
            stateManager.clearState()
            TrackingWorker.cancel(this)
            TrackingAlarmReceiver.cancel(this)
            Log.d(TAG, "✅ Tracking stopped via FCM")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping tracking", e)
        }
    }
}
