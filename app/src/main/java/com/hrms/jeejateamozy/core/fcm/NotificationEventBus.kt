package com.hrms.jeejateamozy.core.fcm

import com.hrms.jeejateamozy.core.network.PostShiftCheckPayload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Simple event bus to notify the notification screen when a new FCM message arrives.
 */
object NotificationEventBus {
    private val _newNotificationEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val newNotificationEvent: SharedFlow<Unit> = _newNotificationEvent.asSharedFlow()

    private val _attendanceRefreshEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val attendanceRefreshEvent: SharedFlow<Unit> = _attendanceRefreshEvent.asSharedFlow()

    private val _postShiftCheckEvent = MutableSharedFlow<PostShiftCheckPayload>(extraBufferCapacity = 1)
    val postShiftCheckEvent: SharedFlow<PostShiftCheckPayload> = _postShiftCheckEvent.asSharedFlow()

    @Volatile
    var pendingPostShiftPayload: PostShiftCheckPayload? = null
        private set

    /** True when MainScreen has an active collector for postShiftCheckEvent */
    @Volatile
    var isPostShiftCollectorActive: Boolean = false

    fun notifyNewNotification() {
        _newNotificationEvent.tryEmit(Unit)
    }

    fun notifyAttendanceRefresh() {
        _attendanceRefreshEvent.tryEmit(Unit)
    }

    fun notifyPostShiftCheck(payload: PostShiftCheckPayload) {
        pendingPostShiftPayload = payload
        _postShiftCheckEvent.tryEmit(payload)
    }

    fun clearPendingPostShiftPayload() {
        pendingPostShiftPayload = null
    }
}
