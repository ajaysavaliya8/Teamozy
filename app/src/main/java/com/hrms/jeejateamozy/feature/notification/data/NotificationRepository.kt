package com.hrms.jeejateamozy.feature.notification.data

import android.content.Context
import android.util.Log
import com.hrms.jeejateamozy.core.network.ApiService
import com.hrms.jeejateamozy.core.network.NotificationsData
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Result wrapper for server API calls
 */
sealed class NotificationResult<out T> {
    data class Success<T>(val data: T) : NotificationResult<T>()
    data class Error(val message: String, val code: Int = -1) : NotificationResult<Nothing>()
}

/**
 * Repository for notification operations (Server API only)
 */
class NotificationRepository private constructor(
    context: Context,
    apiService: ApiService? = null
) {

    companion object {
        private const val TAG = "NotificationRepo"

        @Volatile
        private var INSTANCE: NotificationRepository? = null

        fun getInstance(context: Context, apiService: ApiService? = null): NotificationRepository {
            val instance = INSTANCE

            if (instance != null && apiService != null && instance.apiService == null) {
                synchronized(this) {
                    if (INSTANCE?.apiService == null) {
                        INSTANCE?.setApiService(apiService)
                    }
                }
                return INSTANCE!!
            }

            return instance ?: synchronized(this) {
                INSTANCE ?: NotificationRepository(context.applicationContext, apiService).also {
                    INSTANCE = it
                }
            }
        }
    }

    private var apiService: ApiService? = apiService
    private val preferencesManager = PreferencesManager.getInstance(context)

    // Server unread count (for badge on HomePage)
    private val _serverUnreadCount = MutableStateFlow(0)
    val serverUnreadCountFlow: StateFlow<Int> = _serverUnreadCount.asStateFlow()

    internal fun setApiService(api: ApiService) {
        this.apiService = api
        Log.d(TAG, "✅ ApiService updated")
    }

    private fun getToken(): String? = preferencesManager.authToken

    /**
     * Refresh unread count from server (for badge on HomePage)
     */
    suspend fun refreshUnreadCount() {
        try {
            val api = apiService ?: return
            val token = getToken() ?: return

            val response = api.getNotifications(token)
            if (response.isSuccessful && response.body()?.success == true) {
                val count = response.body()?.data?.unreadCount ?: 0
                _serverUnreadCount.value = count
                Log.d(TAG, "✅ Refreshed unread count: $count")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error refreshing unread count", e)
        }
    }

    /**
     * Fetch notifications from server
     */
    suspend fun fetchNotificationsFromServer(): NotificationResult<NotificationsData> {
        return try {
            val api = apiService ?: return NotificationResult.Error("API service not initialized")
            val token = getToken() ?: return NotificationResult.Error("Not authenticated")

            val response = api.getNotifications(token)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    _serverUnreadCount.value = body.data.unreadCount
                    Log.d(TAG, "✅ Fetched ${body.data.notifications.size} notifications, unread: ${body.data.unreadCount}")
                    NotificationResult.Success(body.data)
                } else {
                    NotificationResult.Error(body?.message ?: "Failed to fetch notifications")
                }
            } else {
                Log.e(TAG, "❌ Server error: ${response.code()}")
                NotificationResult.Error("Server error: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching notifications", e)
            NotificationResult.Error(e.message ?: "Network error")
        }
    }

    /**
     * Mark notification as read on server
     */
    suspend fun markAsReadOnServer(notificationId: Int): NotificationResult<Boolean> {
        return try {
            val api = apiService ?: return NotificationResult.Error("API service not initialized")
            val token = getToken() ?: return NotificationResult.Error("Not authenticated")

            val response = api.markNotificationRead(notificationId, token)

            if (response.isSuccessful && response.body()?.success == true) {
                if (_serverUnreadCount.value > 0) {
                    _serverUnreadCount.value = _serverUnreadCount.value - 1
                }
                Log.d(TAG, "✅ Marked notification $notificationId as read")
                NotificationResult.Success(true)
            } else {
                val message = response.body()?.message ?: "Failed to mark as read"
                Log.e(TAG, "❌ Failed to mark as read: $message")
                NotificationResult.Error(message, response.code())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking notification as read", e)
            NotificationResult.Error(e.message ?: "Network error")
        }
    }

    /**
     * Mark all notifications as read on server
     */
    suspend fun markAllAsReadOnServer(): NotificationResult<Boolean> {
        return try {
            val api = apiService ?: return NotificationResult.Error("API service not initialized")
            val token = getToken() ?: return NotificationResult.Error("Not authenticated")

            val response = api.markAllNotificationsRead(token)

            if (response.isSuccessful && response.body()?.success == true) {
                _serverUnreadCount.value = 0
                Log.d(TAG, "✅ Marked all notifications as read")
                NotificationResult.Success(true)
            } else {
                val message = response.body()?.message ?: "Failed to mark all as read"
                Log.e(TAG, "❌ Failed to mark all as read: $message")
                NotificationResult.Error(message, response.code())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking all as read", e)
            NotificationResult.Error(e.message ?: "Network error")
        }
    }

    /**
     * Delete notifications on server
     */
    suspend fun deleteNotificationsOnServer(notificationIds: List<Int>): NotificationResult<Boolean> {
        return try {
            val api = apiService ?: return NotificationResult.Error("API service not initialized")
            val token = getToken() ?: return NotificationResult.Error("Not authenticated")

            val response = api.deleteNotifications(notificationIds, token)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "✅ Deleted ${notificationIds.size} notifications")
                NotificationResult.Success(true)
            } else {
                val message = response.body()?.message ?: "Failed to delete notifications"
                Log.e(TAG, "❌ Failed to delete: $message")
                NotificationResult.Error(message, response.code())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting notifications", e)
            NotificationResult.Error(e.message ?: "Network error")
        }
    }
}
