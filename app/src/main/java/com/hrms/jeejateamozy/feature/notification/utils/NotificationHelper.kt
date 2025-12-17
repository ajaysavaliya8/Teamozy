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

/**
 * Helper class for creating and showing system notifications
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"

    // Notification Channel IDs
    const val CHANNEL_ID_CIRCULAR = "circular_notifications"
    const val CHANNEL_ID_CIRCULAR_HIGH = "circular_notifications_high"
    const val CHANNEL_ID_DEFAULT = "teamozy_notifications"  // ✅ Backend's expected channel

    // Notification Channel Names
    private const val CHANNEL_NAME_CIRCULAR = "Circulars"
    private const val CHANNEL_NAME_CIRCULAR_HIGH = "Important Circulars"
    private const val CHANNEL_NAME_DEFAULT = "Teamozy Notifications"

    // Intent extras - ALL AS STRING to avoid ClassCastException
    const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    const val EXTRA_CIRCULAR_ID = "circular_id"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    /**
     * Create notification channels (call on app startup)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // ✅ Default channel (backend sends notifications with this channel)
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                CHANNEL_NAME_DEFAULT,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "General Teamozy notifications"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // Normal priority channel
            val normalChannel = NotificationChannel(
                CHANNEL_ID_CIRCULAR,
                CHANNEL_NAME_CIRCULAR,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for circulars and announcements"
                enableLights(true)
                enableVibration(true)
            }

            // High priority channel (for important circulars)
            val highChannel = NotificationChannel(
                CHANNEL_ID_CIRCULAR_HIGH,
                CHANNEL_NAME_CIRCULAR_HIGH,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important circular notifications"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(defaultChannel)
            notificationManager.createNotificationChannel(normalChannel)
            notificationManager.createNotificationChannel(highChannel)

            Log.d(TAG, "✅ Notification channels created (including teamozy_notifications)")
        }
    }

    /**
     * Show circular notification
     */
    fun showCircularNotification(
        context: Context,
        notificationId: String,
        circularId: Int,
        title: String,
        message: String,
        priority: String,
        type: String
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create intent for opening circular detail
            // ✅ Pass ALL extras as String to avoid ClassCastException
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NOTIFICATION_TYPE, type)
                putExtra(EXTRA_CIRCULAR_ID, circularId.toString())
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                // ✅ Also pass title and message for saving when clicked
                putExtra("title", title)
                putExtra("message", message)
                putExtra("priority", priority)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                circularId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Determine channel based on priority
            val isHighPriority = priority.equals("high", ignoreCase = true)
            val channelId = if (isHighPriority) {
                CHANNEL_ID_CIRCULAR_HIGH
            } else {
                CHANNEL_ID_CIRCULAR
            }

            // Determine notification priority for pre-O devices
            val notificationPriority = if (isHighPriority) {
                NotificationCompat.PRIORITY_HIGH
            } else {
                NotificationCompat.PRIORITY_DEFAULT
            }

            // Build notification
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(notificationPriority)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .apply {
                    when (type) {
                        "circular_reminder" -> setSubText("Reminder")
                        "circular_new" -> setSubText("New Circular")
                        else -> setSubText("Circular")
                    }
                }
                .build()

            // Use circular ID as notification ID
            notificationManager.notify(circularId, notification)

            Log.d(TAG, "✅ Notification shown - CircularID: $circularId, Title: $title, Priority: $priority")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing notification", e)
        }
    }

    /**
     * Cancel notification by circular ID
     */
    fun cancelNotification(context: Context, circularId: Int) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(circularId)
            Log.d(TAG, "✅ Notification cancelled: $circularId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling notification", e)
        }
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
            Log.d(TAG, "✅ All notifications cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling all notifications", e)
        }
    }
}