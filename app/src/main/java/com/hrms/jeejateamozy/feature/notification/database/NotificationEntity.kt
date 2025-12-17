package com.hrms.jeejateamozy.feature.notification.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing notifications locally
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Unique notification ID from backend
    val notificationId: String,

    // Type: "circular_new" or "circular_reminder"
    val type: String,

    // Reference ID for deep linking
    val circularId: Int,

    // Display content
    val title: String,
    val message: String,

    // Metadata
    val priority: String,        // "high", "medium", "low"
    val circularType: String,    // "policy", "announcement", "memo", etc.

    // Read status
    val isRead: Boolean = false,

    // Timestamps
    val createdAt: String,       // ISO 8601 from backend
    val receivedAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null
)