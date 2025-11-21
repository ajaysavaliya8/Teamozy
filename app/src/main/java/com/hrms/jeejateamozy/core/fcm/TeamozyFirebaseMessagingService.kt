package com.hrms.jeejateamozy.core.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hrms.jeejateamozy.R
import com.hrms.jeejateamozy.app.MainActivity
import com.hrms.jeejateamozy.core.utils.PreferencesManager

/**
 * Firebase Cloud Messaging Service
 * Handles incoming push notifications and FCM token refresh
 */
class TeamozyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_SERVICE"
        private const val CHANNEL_ID = "teamozy_notifications"
        private const val CHANNEL_NAME = "Teamozy Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for attendance, leaves, and circulars"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Firebase Messaging Service created")
    }

    /**
     * Called when a new FCM token is generated
     * This happens on:
     * - First app install
     * - App reinstall
     * - App data cleared
     * - Device restored from backup
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔄 New FCM token received: ${token.take(30)}...")

        // Save the new token to preferences
        val prefsManager = PreferencesManager.getInstance(applicationContext)
        prefsManager.fcmToken = token

        Log.d(TAG, "✅ FCM token saved to PreferencesManager")

        // TODO: If user is logged in, send the new token to the server
        // This can be done by checking if authToken exists in prefsManager
        // and calling an API endpoint to update the token
    }

    /**
     * Called when a message is received
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "📬 Message received from: ${remoteMessage.from}")

        // Log message data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "📦 Message data payload: ${remoteMessage.data}")
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "📨 Message Notification Title: ${notification.title}")
            Log.d(TAG, "📨 Message Notification Body: ${notification.body}")

            // Show notification
            showNotification(
                title = notification.title ?: "Teamozy",
                message = notification.body ?: "",
                data = remoteMessage.data
            )
        }

        // If there's only data payload (no notification), handle it here
        if (remoteMessage.notification == null && remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Teamozy"
            val body = remoteMessage.data["body"] ?: remoteMessage.data["message"] ?: ""

            showNotification(
                title = title,
                message = body,
                data = remoteMessage.data
            )
        }
    }

    /**
     * Create notification channel (Required for Android O and above)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "✅ Notification channel created: $CHANNEL_ID")
        }
    }

    /**
     * Show notification to user
     */
    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Pass notification data to the activity if needed
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 250, 500))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())

        Log.d(TAG, "✅ Notification shown: $title")
    }

    /**
     * Called when message delivery fails
     */
    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.w(TAG, "⚠️ Messages deleted on server")
    }

    // Note: onMessageSent and onSendError are deprecated in newer Firebase versions
    // They are only used for upstream messaging (app -> server), which is rarely used
    // If you need upstream messaging, consider using HTTP API instead
}