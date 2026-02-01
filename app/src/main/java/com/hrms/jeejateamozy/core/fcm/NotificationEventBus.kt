package com.hrms.jeejateamozy.core.fcm

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

    fun notifyNewNotification() {
        _newNotificationEvent.tryEmit(Unit)
    }

    fun notifyAttendanceRefresh() {
        _attendanceRefreshEvent.tryEmit(Unit)
    }
}
