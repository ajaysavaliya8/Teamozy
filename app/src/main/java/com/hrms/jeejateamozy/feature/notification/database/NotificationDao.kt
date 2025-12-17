package com.hrms.jeejateamozy.feature.notification.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    // ============================================
    // INSERT OPERATIONS
    // ============================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    // ============================================
    // QUERY OPERATIONS - FLOWS (Real-time updates)
    // ============================================

    /**
     * Get all notifications ordered by received time (newest first)
     */
    @Query("SELECT * FROM notifications ORDER BY receivedAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    /**
     * Get only unread notifications
     */
    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY receivedAt DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>

    /**
     * Get only read notifications
     */
    @Query("SELECT * FROM notifications WHERE isRead = 1 ORDER BY receivedAt DESC")
    fun getReadNotifications(): Flow<List<NotificationEntity>>

    /**
     * Get notifications by type
     */
    @Query("SELECT * FROM notifications WHERE type = :type ORDER BY receivedAt DESC")
    fun getNotificationsByType(type: String): Flow<List<NotificationEntity>>

    /**
     * Get unread count as Flow (for real-time badge updates)
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCountFlow(): Flow<Int>

    // ============================================
    // QUERY OPERATIONS - SUSPEND (One-time queries)
    // ============================================

    /**
     * Get notification by ID
     */
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getById(id: Long): NotificationEntity?

    /**
     * Get notification by backend notification ID
     */
    @Query("SELECT * FROM notifications WHERE notificationId = :notificationId LIMIT 1")
    suspend fun getByNotificationId(notificationId: String): NotificationEntity?

    /**
     * Check if notification already exists (for duplicate prevention)
     */
    @Query("SELECT EXISTS(SELECT 1 FROM notifications WHERE notificationId = :notificationId)")
    suspend fun exists(notificationId: String): Boolean

    /**
     * Get unread count (one-time)
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    suspend fun getUnreadCount(): Int

    // ============================================
    // UPDATE OPERATIONS
    // ============================================

    /**
     * Mark notification as read
     */
    @Query("UPDATE notifications SET isRead = 1, readAt = :readAt WHERE id = :id")
    suspend fun markAsRead(id: Long, readAt: Long = System.currentTimeMillis())

    /**
     * Mark notification as read by notification ID (from backend)
     */
    @Query("UPDATE notifications SET isRead = 1, readAt = :readAt WHERE notificationId = :notificationId")
    suspend fun markAsReadByNotificationId(notificationId: String, readAt: Long = System.currentTimeMillis())

    /**
     * Mark all notifications as read
     */
    @Query("UPDATE notifications SET isRead = 1, readAt = :readAt WHERE isRead = 0")
    suspend fun markAllAsRead(readAt: Long = System.currentTimeMillis())

    /**
     * Mark notifications as read by circular ID
     */
    @Query("UPDATE notifications SET isRead = 1, readAt = :readAt WHERE circularId = :circularId AND isRead = 0")
    suspend fun markAsReadByCircularId(circularId: Int, readAt: Long = System.currentTimeMillis())

    // ============================================
    // DELETE OPERATIONS
    // ============================================

    /**
     * Delete notification by ID
     */
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Delete all notifications
     */
    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    /**
     * Delete old read notifications (cleanup)
     * @param olderThan timestamp - delete read notifications older than this
     */
    @Query("DELETE FROM notifications WHERE isRead = 1 AND receivedAt < :olderThan")
    suspend fun deleteOldReadNotifications(olderThan: Long): Int

    /**
     * Delete notifications by circular ID
     */
    @Query("DELETE FROM notifications WHERE circularId = :circularId")
    suspend fun deleteByCircularId(circularId: Int)
}