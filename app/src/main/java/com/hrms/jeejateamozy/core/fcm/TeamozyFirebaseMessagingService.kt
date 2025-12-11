package com.hrms.jeejateamozy.core.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase Messaging Service
 *
 * Handles FCM messages including:
 * - Tracking heartbeat messages (keeps service alive)
 * - Regular push notifications
 * - FCM token updates
 */
class TeamozyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "TeamozyFCM"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var trackingStateManager: TrackingStateManager

    override fun onCreate() {
        super.onCreate()
        trackingStateManager = TrackingStateManager(applicationContext)
    }

    /**
     * Called when FCM message is received
     * This can wake the app even if it's in background!
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📩 FCM message received from: ${message.from}")

        // Check message type
        val messageType = message.data["type"]

        when (messageType) {
            "tracking_heartbeat" -> handleTrackingHeartbeat(message)
            "notification" -> handleRegularNotification(message)
            else -> {
                Log.d(TAG, "Unknown message type: $messageType")
                handleRegularNotification(message)
            }
        }
    }

    /**
     * Handle tracking heartbeat message
     * This is sent periodically during active tracking sessions
     */
    private fun handleTrackingHeartbeat(message: RemoteMessage) {
        Log.d(TAG, "💓 Tracking heartbeat received")

        serviceScope.launch {
            try {
                // Update heartbeat timestamp
                trackingStateManager.updateHeartbeat()

                // Check if tracking should be active
                val shouldBeActive = trackingStateManager.shouldTrackingBeActive()

                if (shouldBeActive) {
                    // Check if service is actually running
                    val isServiceRunning = isLocationServiceRunning()

                    if (!isServiceRunning) {
                        Log.w(TAG, "⚠️ Tracking should be active but service is NOT running!")
                        Log.d(TAG, "🔄 Attempting to restart LocationTrackingService...")

                        // Restart the service
                        LocationTrackingService.startTracking(applicationContext)

                        Log.d(TAG, "✅ LocationTrackingService restart triggered")
                    } else {
                        Log.d(TAG, "✅ Service is running correctly")
                    }
                } else {
                    Log.d(TAG, "⏹️ Tracking should NOT be active - ignoring heartbeat")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling heartbeat", e)
            }
        }
    }

    /**
     * Handle regular push notification
     */
    private fun handleRegularNotification(message: RemoteMessage) {
        Log.d(TAG, "🔔 Regular notification received")

        // Show notification if there's a notification payload
        message.notification?.let { notification ->
            val title = notification.title ?: "Teamozy"
            val body = notification.body ?: ""

            Log.d(TAG, "Notification: $title - $body")

            // You can show notification here using NotificationManager
            // Or let FCM handle it automatically if notification payload exists
        }
    }

    /**
     * Check if LocationTrackingService is running
     */
    private fun isLocationServiceRunning(): Boolean {
        return try {
            val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            manager.getRunningServices(Int.MAX_VALUE).any { service ->
                service.service.className == LocationTrackingService::class.java.name
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service status", e)
            false
        }
    }

    /**
     * Called when FCM token is updated
     * Send new token to your backend
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔄 New FCM token: $token")

        // Save token locally
        val prefs = PreferencesManager.getInstance(applicationContext)
        prefs.saveFCMToken(token)

        // TODO: Send token to your backend
        serviceScope.launch {
            try {
                // Call your API to update FCM token
                // NetworkModule.apiService.updateFCMToken(token)
                Log.d(TAG, "✅ FCM token saved locally")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update FCM token on server", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Note: Don't cancel serviceScope here as messages may arrive after onDestroy
    }
}