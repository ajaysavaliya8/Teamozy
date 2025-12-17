package com.hrms.jeejateamozy.feature.notification.data

import android.content.Context
import android.util.Log
import com.hrms.jeejateamozy.feature.notification.database.NotificationDatabase
import com.hrms.jeejateamozy.feature.notification.database.NotificationEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Repository for notification operations
 */
class NotificationRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "NotificationRepo"

        // Cleanup threshold: 30 days
        private val CLEANUP_THRESHOLD_MS = TimeUnit.DAYS.toMillis(30)

        @Volatile
        private var INSTANCE: NotificationRepository? = null

        fun getInstance(context: Context): NotificationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val dao = NotificationDatabase.getInstance(context).notificationDao()

    // ============================================
    // SAVE OPERATIONS
    // ============================================

    /**
     * Save notification to database
     * Returns true if saved, false if duplicate
     */
    suspend fun saveNotification(notification: NotificationEntity): Boolean {
        return try {
            // Check for duplicate
            if (dao.exists(notification.notificationId)) {
                Log.d(TAG, "Notification already exists: ${notification.notificationId}")
                return false
            }

            dao.insert(notification)
            Log.d(TAG, "✅ Notification saved: ${notification.notificationId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving notification", e)
            false
        }
    }

    /**
     * Save notification from FCM data payload
     */
    suspend fun saveFromFcmData(
        notificationId: String,
        type: String,
        circularId: Int,
        title: String,
        message: String,
        priority: String,
        circularType: String,
        createdAt: String
    ): Boolean {
        val entity = NotificationEntity(
            notificationId = notificationId,
            type = type,
            circularId = circularId,
            title = title,
            message = message,
            priority = priority,
            circularType = circularType,
            isRead = false,
            createdAt = createdAt,
            receivedAt = System.currentTimeMillis(),
            readAt = null
        )
        return saveNotification(entity)
    }

    // ============================================
    // QUERY OPERATIONS
    // ============================================

    /**
     * Get all notifications as Flow
     */
    fun getAllNotifications(): Flow<List<NotificationEntity>> {
        return dao.getAllNotifications()
    }

    /**
     * Get unread notifications as Flow
     */
    fun getUnreadNotifications(): Flow<List<NotificationEntity>> {
        return dao.getUnreadNotifications()
    }

    /**
     * Get read notifications as Flow
     */
    fun getReadNotifications(): Flow<List<NotificationEntity>> {
        return dao.getReadNotifications()
    }

    /**
     * Get notifications by type as Flow
     */
    fun getNotificationsByType(type: String): Flow<List<NotificationEntity>> {
        return dao.getNotificationsByType(type)
    }

    /**
     * Get unread count as Flow (for badge)
     */
    fun getUnreadCountFlow(): Flow<Int> {
        return dao.getUnreadCountFlow()
    }

    /**
     * Get unread count (one-time)
     */
    suspend fun getUnreadCount(): Int {
        return dao.getUnreadCount()
    }

    /**
     * Get notification by ID
     */
    suspend fun getById(id: Long): NotificationEntity? {
        return dao.getById(id)
    }

    /**
     * Check if notification exists
     */
    suspend fun exists(notificationId: String): Boolean {
        return dao.exists(notificationId)
    }

    // ============================================
    // UPDATE OPERATIONS
    // ============================================

    /**
     * Mark notification as read by database ID
     */
    suspend fun markAsRead(id: Long) {
        try {
            dao.markAsRead(id)
            Log.d(TAG, "✅ Marked as read: $id")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking as read", e)
        }
    }

    /**
     * Mark notification as read by notification ID (from backend)
     */
    suspend fun markAsReadByNotificationId(notificationId: String) {
        try {
            dao.markAsReadByNotificationId(notificationId)
            Log.d(TAG, "✅ Marked as read by notificationId: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking as read by notificationId", e)
        }
    }

    /**
     * Mark all notifications as read
     */
    suspend fun markAllAsRead() {
        try {
            dao.markAllAsRead()
            Log.d(TAG, "✅ All notifications marked as read")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking all as read", e)
        }
    }

    /**
     * Mark notifications as read by circular ID
     */
    suspend fun markAsReadByCircularId(circularId: Int) {
        try {
            dao.markAsReadByCircularId(circularId)
            Log.d(TAG, "✅ Marked as read for circular: $circularId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking as read by circularId", e)
        }
    }

    // ============================================
    // DELETE OPERATIONS
    // ============================================

    /**
     * Delete notification by ID
     */
    suspend fun deleteById(id: Long) {
        try {
            dao.deleteById(id)
            Log.d(TAG, "✅ Notification deleted: $id")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting notification", e)
        }
    }

    /**
     * Delete all notifications
     */
    suspend fun deleteAll() {
        try {
            dao.deleteAll()
            Log.d(TAG, "✅ All notifications deleted")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting all notifications", e)
        }
    }

    /**
     * Cleanup old read notifications (older than 30 days)
     * @return number of deleted notifications
     */
    suspend fun cleanupOldNotifications(): Int {
        return try {
            val threshold = System.currentTimeMillis() - CLEANUP_THRESHOLD_MS
            val deleted = dao.deleteOldReadNotifications(threshold)
            if (deleted > 0) {
                Log.d(TAG, "✅ Cleaned up $deleted old notifications")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cleaning up notifications", e)
            0
        }
    }
}