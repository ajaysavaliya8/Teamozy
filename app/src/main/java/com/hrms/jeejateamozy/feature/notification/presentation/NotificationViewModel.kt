package com.hrms.jeejateamozy.feature.notification.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.core.fcm.NotificationEventBus
import com.hrms.jeejateamozy.core.network.ServerNotification
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.notification.data.NotificationRepository
import com.hrms.jeejateamozy.feature.notification.data.NotificationResult
import com.hrms.jeejateamozy.navigation.DeepLink
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * UI State for Notification List Screen
 */
data class NotificationUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val notifications: List<ServerNotification> = emptyList(),
    val unreadCount: Int = 0,
    val errorMessage: String? = null,
    val selectedNotifications: Set<Int> = emptySet(),
    val isSelectionMode: Boolean = false
)

/**
 * Events from NotificationViewModel
 */
sealed class NotificationEvent {
    data class ShowError(val message: String) : NotificationEvent()
    data class ShowSuccess(val message: String) : NotificationEvent()
    data class NavigateToScreen(val deepLink: DeepLink) : NotificationEvent()
}

/**
 * ViewModel for Notification feature
 */
class NotificationViewModel(
    private val repository: NotificationRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "NotificationVM"
    }

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotificationEvent>()
    val events: SharedFlow<NotificationEvent> = _events.asSharedFlow()

    init {
        loadNotifications()
        observeNewNotifications()
    }

    /**
     * Listen for new FCM notifications and auto-refresh the list
     */
    private fun observeNewNotifications() {
        viewModelScope.launch {
            NotificationEventBus.newNotificationEvent.collect {
                // Small delay to allow the server to store the notification
                delay(1500)
                loadNotifications()
            }
        }
    }

    /**
     * Load notifications from server
     */
    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = repository.fetchNotificationsFromServer()) {
                is NotificationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = result.data.notifications,
                            unreadCount = result.data.unreadCount,
                            errorMessage = null
                        )
                    }
                    Log.d(TAG, "Loaded ${result.data.notifications.size} notifications, unread: ${result.data.unreadCount}")
                }
                is NotificationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message
                        )
                    }
                    Log.e(TAG, "Error loading notifications: ${result.message}")
                }
            }
        }
    }

    /**
     * Refresh notifications (pull-to-refresh)
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadNotifications()
        }
    }

    /**
     * Handle notification click - mark as read and navigate
     */
    fun onNotificationClick(notification: ServerNotification) {
        viewModelScope.launch {
            // Mark as read on server if not already read
            if (!notification.isRead) {
                when (val result = repository.markAsReadOnServer(notification.id)) {
                    is NotificationResult.Success -> {
                        updateNotificationReadStatus(notification.id)
                        Log.d(TAG, "Marked notification ${notification.id} as read")
                    }
                    is NotificationResult.Error -> {
                        Log.e(TAG, "Failed to mark as read: ${result.message}")
                    }
                }
            }

            // Navigate based on notification data
            createDeepLink(notification)?.let { deepLink ->
                _events.emit(NotificationEvent.NavigateToScreen(deepLink))
            }
        }
    }

    /**
     * Create DeepLink from notification
     */
    private fun createDeepLink(notification: ServerNotification): DeepLink? {
        // Handle post_shift_check specially — server doesn't include post_shift_data in history
        val isPostShift = notification.type == "post_shift_check" || notification.type == "post_shift_reminder"
                || notification.screen == "post_shift_check"
        if (isPostShift) {
            val postShiftData = notification.postShiftData ?: run {
                val pending = NotificationEventBus.pendingPostShiftPayload
                if (pending != null) {
                    Log.d(TAG, "Using in-memory pending payload for post-shift (recordId: ${pending.attendanceRecordId})")
                    JSONObject().apply {
                        put("attendance_record_id", pending.attendanceRecordId)
                        put("attendance_date", pending.attendanceDate ?: "")
                        put("employee_id", pending.employeeId ?: 0)
                        put("shift_end_time", pending.shiftEndTime ?: "")
                        put("title", notification.title ?: pending.title)
                        put("message", notification.message ?: pending.message)
                    }.toString()
                } else {
                    // Fallback: read from persisted SharedPreferences
                    val saved = preferencesManager.postShiftDataJson
                    Log.d(TAG, "In-memory pending null, persisted data: ${if (saved != null) "EXISTS" else "NULL"}")
                    saved
                }
            }
            Log.d(TAG, "Post-shift deep link created, data: ${if (postShiftData != null) "HAS_DATA" else "NULL"}")
            return DeepLink.PostShiftCheck(postShiftData)
        }

        // If screen is provided, use it
        notification.screen?.let { screen ->
            val extras = mutableMapOf<String, String?>()
            notification.circularId?.let { extras["circular_id"] = it.toString() }
            notification.leaveId?.let { extras["leave_id"] = it.toString() }
            notification.applicationId?.let { extras["application_id"] = it.toString() }
            notification.date?.let { extras["date"] = it }
            return DeepLink.fromFcmData(screen, extras)
        }

        // Fallback: determine deep link from notification data
        notification.circularId?.takeIf { it > 0 }?.let {
            return DeepLink.CircularDetail(it)
        }

        notification.leaveId?.takeIf { it > 0 }?.let {
            return DeepLink.LeaveDetail(it)
        }

        notification.applicationId?.takeIf { it > 0 }?.let {
            return DeepLink.LeaveDetail(it)
        }

        notification.date?.let {
            return DeepLink.AttendanceDetail(it)
        }

        return null
    }

    /**
     * Update notification read status in local state
     */
    private fun updateNotificationReadStatus(notificationId: Int) {
        _uiState.update { state ->
            val updatedList = state.notifications.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            state.copy(
                notifications = updatedList,
                unreadCount = updatedList.count { !it.isRead }
            )
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = repository.markAllAsReadOnServer()) {
                is NotificationResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            notifications = state.notifications.map { it.copy(isRead = true) },
                            unreadCount = 0
                        )
                    }
                    _events.emit(NotificationEvent.ShowSuccess("All notifications marked as read"))
                }
                is NotificationResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(NotificationEvent.ShowError(result.message))
                }
            }
        }
    }

    // ============================================
    // SELECTION MODE (for multi-delete)
    // ============================================

    fun toggleSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = !it.isSelectionMode,
                selectedNotifications = emptySet()
            )
        }
    }

    fun toggleNotificationSelection(notificationId: Int) {
        _uiState.update { state ->
            val newSelection = if (notificationId in state.selectedNotifications) {
                state.selectedNotifications - notificationId
            } else {
                state.selectedNotifications + notificationId
            }
            state.copy(selectedNotifications = newSelection)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedNotifications = state.notifications.map { it.id }.toSet())
        }
    }

    fun deleteSelectedNotifications() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedNotifications.toList()
            if (selectedIds.isEmpty()) return@launch

            _uiState.update { it.copy(isLoading = true) }

            when (val result = repository.deleteNotificationsOnServer(selectedIds)) {
                is NotificationResult.Success -> {
                    _uiState.update { state ->
                        val remaining = state.notifications.filter { it.id !in selectedIds }
                        state.copy(
                            isLoading = false,
                            notifications = remaining,
                            unreadCount = remaining.count { !it.isRead },
                            selectedNotifications = emptySet(),
                            isSelectionMode = false
                        )
                    }
                    _events.emit(NotificationEvent.ShowSuccess("${selectedIds.size} notifications deleted"))
                }
                is NotificationResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(NotificationEvent.ShowError(result.message))
                }
            }
        }
    }

    fun deleteNotification(notificationId: Int) {
        viewModelScope.launch {
            when (val result = repository.deleteNotificationsOnServer(listOf(notificationId))) {
                is NotificationResult.Success -> {
                    _uiState.update { state ->
                        val remaining = state.notifications.filter { it.id != notificationId }
                        state.copy(
                            notifications = remaining,
                            unreadCount = remaining.count { !it.isRead }
                        )
                    }
                    _events.emit(NotificationEvent.ShowSuccess("Notification deleted"))
                }
                is NotificationResult.Error -> {
                    _events.emit(NotificationEvent.ShowError(result.message))
                }
            }
        }
    }
}
