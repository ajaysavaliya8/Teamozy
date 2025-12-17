package com.hrms.jeejateamozy.feature.notification.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.feature.notification.data.NotificationRepository
import com.hrms.jeejateamozy.feature.notification.database.NotificationEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for Notification List Screen
 */
data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationEntity> = emptyList(),
    val errorMessage: String? = null,
    val selectedFilter: NotificationFilter = NotificationFilter.ALL
)

/**
 * Filter options for notifications - ONLY All, Unread, Read
 */
enum class NotificationFilter {
    ALL,
    UNREAD,
    READ
}

/**
 * Events from NotificationViewModel
 */
sealed class NotificationEvent {
    data class ShowError(val message: String) : NotificationEvent()
    data class ShowSuccess(val message: String) : NotificationEvent()
    data class NavigateToCircular(val circularId: Int) : NotificationEvent()
}

/**
 * ViewModel for Notification feature
 */
class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    companion object {
        private const val TAG = "NotificationVM"
    }

    // UI State
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    // Events
    private val _events = MutableSharedFlow<NotificationEvent>()
    val events: SharedFlow<NotificationEvent> = _events.asSharedFlow()

    // Unread count as StateFlow (for badge)
    val unreadCount: StateFlow<Int> = repository.getUnreadCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    init {
        loadNotifications()
    }

    /**
     * Load notifications based on current filter
     */
    fun loadNotifications() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                // Collect notifications from Flow
                val flow = when (_uiState.value.selectedFilter) {
                    NotificationFilter.ALL -> repository.getAllNotifications()
                    NotificationFilter.UNREAD -> repository.getUnreadNotifications()
                    NotificationFilter.READ -> repository.getReadNotifications()
                }

                flow.collect { notifications ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notifications = notifications,
                            errorMessage = null
                        )
                    }
                    Log.d(TAG, "Loaded ${notifications.size} notifications")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load notifications"
                    )
                }
                Log.e(TAG, "Error loading notifications", e)
            }
        }
    }

    /**
     * Set filter and reload notifications
     */
    fun setFilter(filter: NotificationFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        loadNotifications()
    }

    /**
     * Handle notification click - mark as read and navigate
     */
    fun onNotificationClick(notification: NotificationEntity) {
        viewModelScope.launch {
            try {
                // Mark as read
                repository.markAsRead(notification.id)
                Log.d(TAG, "Marked notification ${notification.id} as read")

                // Navigate to circular detail
                _events.emit(NotificationEvent.NavigateToCircular(notification.circularId))
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification click", e)
                _events.emit(NotificationEvent.ShowError("Failed to open notification"))
            }
        }
    }

    /**
     * Mark notification as read without navigation
     */
    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                repository.markAsRead(notificationId)
                Log.d(TAG, "Marked notification $notificationId as read")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification as read", e)
            }
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                repository.markAllAsRead()
                _events.emit(NotificationEvent.ShowSuccess("All notifications marked as read"))
                Log.d(TAG, "All notifications marked as read")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking all as read", e)
                _events.emit(NotificationEvent.ShowError("Failed to mark all as read"))
            }
        }
    }

    /**
     * Delete notification
     */
    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteById(notificationId)
                _events.emit(NotificationEvent.ShowSuccess("Notification deleted"))
                Log.d(TAG, "Notification $notificationId deleted")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting notification", e)
                _events.emit(NotificationEvent.ShowError("Failed to delete notification"))
            }
        }
    }

    /**
     * Delete all notifications
     */
    fun deleteAllNotifications() {
        viewModelScope.launch {
            try {
                repository.deleteAll()
                _events.emit(NotificationEvent.ShowSuccess("All notifications deleted"))
                Log.d(TAG, "All notifications deleted")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all notifications", e)
                _events.emit(NotificationEvent.ShowError("Failed to delete notifications"))
            }
        }
    }

    /**
     * Cleanup old notifications
     */
    fun cleanupOldNotifications() {
        viewModelScope.launch {
            try {
                val deleted = repository.cleanupOldNotifications()
                if (deleted > 0) {
                    Log.d(TAG, "Cleaned up $deleted old notifications")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up notifications", e)
            }
        }
    }
}