package com.hrms.jeejateamozy.core.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingWorker
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingAlarmReceiver
import com.hrms.jeejateamozy.feature.notification.data.NotificationRepository
import com.hrms.jeejateamozy.feature.notification.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Firebase Cloud Messaging Service
 * Handles push notifications including circular notifications
 *
 * NOTE: This service receives messages ONLY when:
 * 1. App is in foreground
 * 2. App is in background BUT message contains ONLY data payload (no notification payload)
 *
 * If backend sends notification+data payload and app is in background,
 * Android handles notification display and this service is NOT called.
 */
class TeamozyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "TeamozyFCM"

        // Notification types
        const val TYPE_HEARTBEAT = "heartbeat"
        const val TYPE_KEEP_ALIVE = "keep_alive"
        const val TYPE_TRACKING_START = "tracking_start"
        const val TYPE_TRACKING_STOP = "tracking_stop"
        const val TYPE_CIRCULAR_NEW = "circular_new"
        const val TYPE_CIRCULAR_REMINDER = "circular_reminder"

        // Reminder types (from backend)
        const val REMINDER_TYPE_GENERAL = "general"
        const val REMINDER_TYPE_URGENT = "urgent"
        const val REMINDER_TYPE_EXPIRING = "expiring"
    }

    // Coroutine scope for background operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "✅ FCM Token refreshed: ${token.take(20)}...")

        // Save token to preferences
        val prefs = PreferencesManager.getInstance(this)
        prefs.fcmToken = token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "========================================")
        Log.d(TAG, "📨 FCM MESSAGE RECEIVED")
        Log.d(TAG, "  From: ${message.from}")
        Log.d(TAG, "  MessageId: ${message.messageId}")
        Log.d(TAG, "  Data keys: ${message.data.keys}")
        Log.d(TAG, "  Data: ${message.data}")
        Log.d(TAG, "  Notification: ${message.notification?.title} - ${message.notification?.body}")
        Log.d(TAG, "========================================")

        val data = message.data
        val type = data["type"] ?: ""

        Log.d(TAG, "📋 Message type: '$type'")

        when (type) {
            TYPE_HEARTBEAT, TYPE_KEEP_ALIVE -> {
                Log.d(TAG, "💓 Heartbeat message received")
                handleHeartbeat()
            }

            TYPE_TRACKING_START -> {
                Log.d(TAG, "▶️ Start tracking command received")
                startTracking()
            }

            TYPE_TRACKING_STOP -> {
                Log.d(TAG, "⏹️ Stop tracking command received")
                stopTracking()
            }

            TYPE_CIRCULAR_NEW -> {
                Log.d(TAG, "📢 NEW CIRCULAR notification received")
                handleCircularNew(data, message.notification)
            }

            TYPE_CIRCULAR_REMINDER -> {
                Log.d(TAG, "🔔 CIRCULAR REMINDER notification received")
                handleCircularReminder(data, message.notification)
            }

            "" -> {
                // Empty type - might be a notification-only message
                Log.d(TAG, "⚠️ Empty message type - checking notification payload")
                handleGenericNotification(message)
            }

            else -> {
                Log.d(TAG, "📬 Unknown message type: '$type'")
                handleGenericNotification(message)
            }
        }
    }

    /**
     * Handle NEW circular notification
     */
    private fun handleCircularNew(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        try {
            val notificationId = data["notification_id"] ?: "notif_${UUID.randomUUID().toString().take(12)}"
            val circularIdStr = data["circular_id"] ?: "0"
            val circularId = circularIdStr.toIntOrNull() ?: 0
            val circularTitle = data["title"] ?: notification?.title ?: "New Circular"
            val message = data["message"] ?: notification?.body ?: "A new circular has been published"
            val priority = data["priority"] ?: "normal"
            val circularType = data["circular_type"] ?: "general"
            val createdAt = data["created_at"] ?: getCurrentIsoTime()

            // Generate display title with emoji
            val displayTitle = when (circularType.lowercase()) {
                "policy" -> "📋 New Policy"
                "announcement" -> "📢 New Announcement"
                "memo" -> "📝 New Memo"
                "notice" -> "📌 New Notice"
                "alert" -> "🚨 New Alert"
                else -> "📢 New Circular"
            }

            Log.d(TAG, "📋 Circular NEW processing:")
            Log.d(TAG, "   - Notification ID: $notificationId")
            Log.d(TAG, "   - Circular ID: $circularId")
            Log.d(TAG, "   - Title: $circularTitle")
            Log.d(TAG, "   - Message: $message")
            Log.d(TAG, "   - Priority: $priority")
            Log.d(TAG, "   - Type: $circularType")

            if (circularId <= 0) {
                Log.e(TAG, "❌ Invalid circular ID: $circularIdStr")
                return
            }

            // Save to database
            saveNotificationToDb(
                notificationId = notificationId,
                type = TYPE_CIRCULAR_NEW,
                circularId = circularId,
                title = circularTitle,
                message = message,
                priority = priority,
                circularType = circularType,
                createdAt = createdAt
            )

            // Show system notification
            NotificationHelper.showCircularNotification(
                context = applicationContext,
                notificationId = notificationId,
                circularId = circularId,
                title = displayTitle,
                message = message,
                priority = priority,
                type = TYPE_CIRCULAR_NEW
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling circular_new", e)
        }
    }

    /**
     * Handle REMINDER circular notification
     */
    private fun handleCircularReminder(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        try {
            val notificationId = data["notification_id"] ?: "notif_${UUID.randomUUID().toString().take(12)}"
            val circularIdStr = data["circular_id"] ?: "0"
            val circularId = circularIdStr.toIntOrNull() ?: 0
            val circularTitle = data["title"] ?: notification?.title ?: "Circular"
            val message = data["message"] ?: notification?.body ?: "You have unread circulars"
            val priority = data["priority"] ?: "normal"
            val circularType = data["circular_type"] ?: "general"
            val reminderType = data["reminder_type"] ?: REMINDER_TYPE_GENERAL
            val createdAt = data["created_at"] ?: getCurrentIsoTime()

            // Generate display title based on reminder type
            val displayTitle = when (reminderType.lowercase()) {
                REMINDER_TYPE_URGENT -> "⚠️ Urgent: Action Required"
                REMINDER_TYPE_EXPIRING -> "⏰ Circular Expiring Soon"
                else -> "🔔 Circular Reminder"
            }

            // Urgent reminders get high priority
            val effectivePriority = when (reminderType.lowercase()) {
                REMINDER_TYPE_URGENT -> "high"
                else -> priority
            }

            Log.d(TAG, "🔔 Circular REMINDER processing:")
            Log.d(TAG, "   - Notification ID: $notificationId")
            Log.d(TAG, "   - Circular ID: $circularId")
            Log.d(TAG, "   - Title: $circularTitle")
            Log.d(TAG, "   - Reminder Type: $reminderType")
            Log.d(TAG, "   - Priority: $effectivePriority")

            if (circularId <= 0) {
                Log.e(TAG, "❌ Invalid circular ID: $circularIdStr")
                return
            }

            // Save to database
            saveNotificationToDb(
                notificationId = notificationId,
                type = TYPE_CIRCULAR_REMINDER,
                circularId = circularId,
                title = circularTitle,
                message = message,
                priority = effectivePriority,
                circularType = circularType,
                createdAt = createdAt
            )

            // Show system notification
            NotificationHelper.showCircularNotification(
                context = applicationContext,
                notificationId = notificationId,
                circularId = circularId,
                title = displayTitle,
                message = message,
                priority = effectivePriority,
                type = TYPE_CIRCULAR_REMINDER
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling circular_reminder", e)
        }
    }

    /**
     * Save notification to local Room database
     */
    private fun saveNotificationToDb(
        notificationId: String,
        type: String,
        circularId: Int,
        title: String,
        message: String,
        priority: String,
        circularType: String,
        createdAt: String
    ) {
        serviceScope.launch {
            try {
                val repo = NotificationRepository.getInstance(applicationContext)
                val saved = repo.saveFromFcmData(
                    notificationId = notificationId,
                    type = type,
                    circularId = circularId,
                    title = title,
                    message = message,
                    priority = priority,
                    circularType = circularType,
                    createdAt = createdAt
                )

                if (saved) {
                    Log.d(TAG, "✅ Notification SAVED to database: $notificationId")
                } else {
                    Log.d(TAG, "⚠️ Notification already exists (duplicate): $notificationId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving to database", e)
            }
        }
    }

    /**
     * Handle generic/unknown notifications
     */
    private fun handleGenericNotification(message: RemoteMessage) {
        message.notification?.let { notification ->
            val title = notification.title ?: "Teamozy"
            val body = notification.body ?: ""
            Log.d(TAG, "📬 Generic notification - Title: $title, Body: $body")
        }

        // Log all data for debugging
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "📋 Data payload:")
            message.data.forEach { (key, value) ->
                Log.d(TAG, "   - $key: $value")
            }
        }
    }

    private fun getCurrentIsoTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        return sdf.format(Date())
    }

    // ============================================
    // TRACKING HANDLERS
    // ============================================

    private fun handleHeartbeat() {
        try {
            val stateManager = TrackingStateManager(this)

            if (stateManager.shouldTrackingBeActive()) {
                Log.d(TAG, "✅ Tracking active - processing heartbeat")
                stateManager.updateHeartbeat()
                TrackingWorker.schedule(this)
                TrackingAlarmReceiver.schedule(this)
            } else {
                Log.d(TAG, "⏹️ Tracking not active - ignoring heartbeat")
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