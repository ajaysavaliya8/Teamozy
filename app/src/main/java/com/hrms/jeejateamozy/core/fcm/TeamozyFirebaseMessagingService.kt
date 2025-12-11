package com.hrms.jeejateamozy.core.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hrms.jeejateamozy.core.utils.PreferencesManager
// import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService  // ✅ DISABLED
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingWorker
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingAlarmReceiver

/**
 * Firebase Cloud Messaging Service
 */
class TeamozyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "TeamozyFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "✅ FCM Token refreshed")

        // Save token to preferences
        val prefs = PreferencesManager.getInstance(this)
        prefs.fcmToken = token

        // TODO: Send token to your backend if needed
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📨 FCM message received")
        Log.d(TAG, "  From: ${message.from}")
        Log.d(TAG, "  Data: ${message.data}")

        val data = message.data

        // Handle heartbeat/keep-alive messages
        when (data["type"]) {
            "heartbeat", "keep_alive" -> {
                Log.d(TAG, "💓 Heartbeat message received")
                handleHeartbeat()
            }

            "tracking_start" -> {
                Log.d(TAG, "▶️ Start tracking command received")
                startTracking()
            }

            "tracking_stop" -> {
                Log.d(TAG, "⏹️ Stop tracking command received")
                stopTracking()
            }

            else -> {
                Log.d(TAG, "📬 Other message type: ${data["type"]}")
                // Handle other message types
            }
        }
    }

    private fun handleHeartbeat() {
        try {
            val stateManager = TrackingStateManager(this)

            if (stateManager.shouldTrackingBeActive()) {
                Log.d(TAG, "✅ Tracking is active - ensuring components are running")

                // Update heartbeat timestamp
                stateManager.updateHeartbeat()

                // Ensure tracking service is running
                // LocationTrackingService.startTracking(this)  // ✅ DISABLED

                // Ensure keep-alive mechanisms are scheduled
                TrackingWorker.schedule(this)
                TrackingAlarmReceiver.schedule(this)

                Log.d(TAG, "✅ Heartbeat processed - tracking ensured")
            } else {
                Log.d(TAG, "⏹️ Tracking is not active - ignoring heartbeat")
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

            // LocationTrackingService.startTracking(this)  // ✅ DISABLED
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

            // Stop all tracking components
            // LocationTrackingService.stopTracking(this)  // ✅ DISABLED
            TrackingWorker.cancel(this)
            TrackingAlarmReceiver.cancel(this)

            Log.d(TAG, "✅ Tracking stopped via FCM")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping tracking", e)
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.w(TAG, "⚠️ Messages deleted on server")
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "✅ Message sent: $msgId")
    }

    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "❌ Message send error: $msgId", exception)
    }
}