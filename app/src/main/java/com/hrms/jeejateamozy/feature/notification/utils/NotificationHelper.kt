package com.hrms.jeejateamozy.feature.notification.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hrms.jeejateamozy.R
import com.hrms.jeejateamozy.app.MainActivity
import com.hrms.jeejateamozy.navigation.DeepLink

/**
 * Helper class for creating and showing system notifications
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"

    // Notification Channel IDs
    private const val CHANNEL_ID_DEFAULT = "teamozy_notifications"
    private const val CHANNEL_ID_HIGH = "teamozy_notifications_high"

    // Intent extras
    const val EXTRA_SCREEN = "screen"
    const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    const val EXTRA_NOTIFICATION_UID = "notification_uid"

    /**
     * Create notification channels (call on app startup)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Default channel
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "Teamozy Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications"
                enableLights(true)
                enableVibration(true)
            }

            // High priority channel
            val highChannel = NotificationChannel(
                CHANNEL_ID_HIGH,
                "Important Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important notifications"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(defaultChannel)
            notificationManager.createNotificationChannel(highChannel)
            Log.d(TAG, "✅ Notification channels created")
        }
    }

    /**
     * Show notification - opens notification list screen when clicked
     */
    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        type: String = "general",
        priority: String = "normal",
        extras: Map<String, String> = emptyMap()
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create intent - always opens notification screen
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_SCREEN, DeepLink.SCREEN_NOTIFICATIONS)
                putExtra(EXTRA_NOTIFICATION_TYPE, type)
                putExtra("title", title)
                putExtra("message", message)
                extras.forEach { (key, value) -> putExtra(key, value) }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val isHighPriority = priority.equals("high", ignoreCase = true)
            val channelId = if (isHighPriority) CHANNEL_ID_HIGH else CHANNEL_ID_DEFAULT

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(if (isHighPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .build()

            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "✅ Notification shown - ID: $notificationId, Title: $title")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing notification", e)
        }
    }
}
