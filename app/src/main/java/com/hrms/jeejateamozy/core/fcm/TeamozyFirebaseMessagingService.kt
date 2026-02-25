package com.hrms.jeejateamozy.core.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingWorker
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingAlarmReceiver
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService
import com.hrms.jeejateamozy.core.network.PostShiftAction
import com.hrms.jeejateamozy.core.network.PostShiftCheckPayload
import com.hrms.jeejateamozy.feature.notification.utils.NotificationHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Firebase Cloud Messaging Service
 * Handles push notifications
 *
 * NOTE: This service receives messages ONLY when:
 * 1. App is in foreground
 * 2. App is in background BUT message contains ONLY data payload (no notification payload)
 */
class TeamozyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "TeamozyFCM"

        // Notification types - Tracking
        private const val TYPE_HEARTBEAT = "heartbeat"
        private const val TYPE_KEEP_ALIVE = "keep_alive"
        private const val TYPE_TRACKING_START = "tracking_start"
        private const val TYPE_TRACKING_STOP = "tracking_stop"

        // Notification types - Post-shift check
        private const val TYPE_POST_SHIFT_CHECK = "post_shift_check"
        private const val TYPE_POST_SHIFT_REMINDER = "post_shift_reminder"

        // Fixed notification ID for post-shift (new replaces old)
        private const val POST_SHIFT_NOTIFICATION_ID = Int.MAX_VALUE - 1
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "✅ FCM Token refreshed: ${token.take(20)}...")
        PreferencesManager.getInstance(this).fcmToken = token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "========================================")
        Log.d(TAG, "📨 FCM MESSAGE RECEIVED")
        Log.d(TAG, "  From: ${message.from}")
        Log.d(TAG, "  Data: ${message.data}")
        Log.d(TAG, "========================================")

        val data = message.data
        val type = data["type"] ?: data["notification_type"] ?: ""

        when (type) {
            TYPE_HEARTBEAT, TYPE_KEEP_ALIVE -> handleHeartbeat()
            TYPE_TRACKING_START -> startTracking()
            TYPE_TRACKING_STOP -> stopTracking()
            TYPE_POST_SHIFT_CHECK, TYPE_POST_SHIFT_REMINDER -> handlePostShiftCheck(data, message.notification)
            else -> handleNotification(data, message.notification)
        }
    }

    /**
     * Handle all notification types (circular, leave, attendance, general)
     */
    private fun handleNotification(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val title = data["title"] ?: notification?.title ?: "Teamozy"
        val messageText = data["message"] ?: data["body"] ?: notification?.body ?: ""
        val type = data["type"] ?: data["notification_type"] ?: "general"
        val priority = data["priority"] ?: "normal"

        if (messageText.isEmpty()) {
            Log.d(TAG, "⚠️ Empty notification message, skipping")
            return
        }

        // Build extras for deep linking
        val extras = mutableMapOf<String, String>()
        data["circular_id"]?.let { extras["circular_id"] = it }
        data["leave_id"]?.let { extras["leave_id"] = it }
        data["application_id"]?.let { extras["application_id"] = it }
        data["entity_type"]?.let { extras["entity_type"] = it }
        data["date"]?.let { extras["date"] = it }
        data["notification_uid"]?.let { extras["notification_uid"] = it }

        // Extract screen field (support both "screen" and "mobile_screen")
        val screen = data["screen"] ?: data["mobile_screen"]
        screen?.let { extras["screen"] = it }

        // Parse nested "data" JSON for action and screen (legacy format)
        var action: String? = null
        val nestedData = data["data"]
        if (!nestedData.isNullOrBlank()) {
            try {
                val json = JSONObject(nestedData)
                action = json.optString("action", "").ifEmpty { null }
                // Only use nested screen if top-level screen wasn't provided
                if (!extras.containsKey("screen")) {
                    val screen = json.optString("screen", "").ifEmpty { null }
                    screen?.let { extras["screen"] = it }
                }
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Failed to parse nested data JSON", e)
            }
        }

        // Generate unique notification ID
        val notificationId = data["circular_id"]?.toIntOrNull()
            ?: data["leave_id"]?.toIntOrNull()
            ?: data["application_id"]?.toIntOrNull()
            ?: System.currentTimeMillis().toInt()

        Log.d(TAG, "📬 Showing notification - Type: $type, Title: $title, Action: $action")

        NotificationHelper.showNotification(
            context = applicationContext,
            notificationId = notificationId,
            title = title,
            message = messageText,
            type = type,
            priority = priority,
            extras = extras
        )

        // Handle location tracking actions
        when (action) {
            "start_location_tracking" -> {
                Log.d(TAG, "🚀 Bulk check-in: starting location tracking")
                startTracking()
                LocationTrackingService.startTracking(applicationContext)
                NotificationEventBus.notifyAttendanceRefresh()
            }
            "stop_location_tracking" -> {
                Log.d(TAG, "⏹️ Bulk checkout: stopping location tracking")
                stopTracking()
                LocationTrackingService.stopTracking(applicationContext)
                NotificationEventBus.notifyAttendanceRefresh()
            }
        }

        // Notify the notification screen and home screen to refresh
        NotificationEventBus.notifyNewNotification()
    }

    // ============================================
    // POST-SHIFT CHECK HANDLER
    // ============================================

    private fun handlePostShiftCheck(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val title = data["title"] ?: notification?.title ?: "Shift Check"
        val messageText = data["message"] ?: data["body"] ?: notification?.body ?: "Are you still working?"
        val type = data["type"] ?: data["notification_type"] ?: TYPE_POST_SHIFT_CHECK

        // Parse attendance_record_id from top-level or nested data
        var attendanceRecordId: Int? = data["attendance_record_id"]?.toIntOrNull()
        var attendanceDate: String? = data["attendance_date"]
        var employeeId: Int? = data["employee_id"]?.toIntOrNull()
        var shiftEndTime: String? = data["shift_end_time"]
        val actions = mutableListOf<PostShiftAction>()

        // Parse nested "data" JSON if present
        val nestedData = data["data"]
        if (!nestedData.isNullOrBlank()) {
            try {
                val json = JSONObject(nestedData)
                if (attendanceRecordId == null) {
                    attendanceRecordId = json.optInt("attendance_record_id", 0).takeIf { it > 0 }
                }
                if (attendanceDate == null) {
                    attendanceDate = json.optString("attendance_date", "").ifEmpty { null }
                }
                if (employeeId == null) {
                    employeeId = json.optInt("employee_id", 0).takeIf { it > 0 }
                }
                if (shiftEndTime == null) {
                    shiftEndTime = json.optString("shift_end_time", "").ifEmpty { null }
                }
                // Parse actions array
                val actionsArray = json.optJSONArray("actions")
                if (actionsArray != null) {
                    for (i in 0 until actionsArray.length()) {
                        val actionObj = actionsArray.getJSONObject(i)
                        actions.add(
                            PostShiftAction(
                                id = actionObj.optString("id", ""),
                                label = actionObj.optString("label", "")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse post-shift nested data", e)
            }
        }

        if (attendanceRecordId == null || attendanceRecordId == 0) {
            Log.e(TAG, "Post-shift check missing attendance_record_id, skipping")
            return
        }

        // Default actions if none provided
        if (actions.isEmpty()) {
            actions.add(PostShiftAction("working", "Yes, Working"))
            actions.add(PostShiftAction("done", "No, Done"))
        }

        val payload = PostShiftCheckPayload(
            attendanceRecordId = attendanceRecordId,
            attendanceDate = attendanceDate,
            employeeId = employeeId,
            shiftEndTime = shiftEndTime,
            actions = actions,
            title = title,
            message = messageText
        )

        // Emit via event bus (for foreground + deferred resume)
        NotificationEventBus.notifyPostShiftCheck(payload)

        // Build post_shift_data JSON and persist to SharedPreferences
        val postShiftDataJson = JSONObject().apply {
            put("attendance_record_id", attendanceRecordId)
            put("attendance_date", attendanceDate ?: "")
            put("employee_id", employeeId ?: 0)
            put("shift_end_time", shiftEndTime ?: "")
            put("title", title)
            put("message", messageText)
            val actionsArr = JSONArray()
            actions.forEach { action ->
                actionsArr.put(JSONObject().apply {
                    put("id", action.id)
                    put("label", action.label)
                })
            }
            put("actions", actionsArr)
        }.toString()
        PreferencesManager.getInstance(applicationContext).postShiftDataJson = postShiftDataJson
        Log.d(TAG, "Persisted post-shift data to SharedPreferences (recordId: $attendanceRecordId)")

        // If the UI collector is active (app is open), dialog shows directly — skip system tray notification
        if (NotificationEventBus.isPostShiftCollectorActive) {
            Log.d(TAG, "App is in foreground, dialog shown directly — skipping system notification")
        } else {
            // Show system notification with fixed ID (replaces previous)
            val extras = mutableMapOf<String, String>(
                "screen" to "post_shift_check",
                "post_shift_data" to postShiftDataJson
            )

            NotificationHelper.showNotification(
                context = applicationContext,
                notificationId = POST_SHIFT_NOTIFICATION_ID,
                title = title,
                message = messageText,
                type = type,
                priority = "high",
                extras = extras
            )
        }

        // Also notify new notification for badge refresh
        NotificationEventBus.notifyNewNotification()

        Log.d(TAG, "Post-shift check handled - recordId: $attendanceRecordId")
    }

    // ============================================
    // TRACKING HANDLERS
    // ============================================

    private fun handleHeartbeat() {
        try {
            val stateManager = TrackingStateManager(this)
            if (stateManager.shouldTrackingBeActive()) {
                Log.d(TAG, "✅ Processing heartbeat")
                stateManager.updateHeartbeat()
                TrackingWorker.schedule(this)
                TrackingAlarmReceiver.schedule(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling heartbeat", e)
        }
    }

    private fun startTracking() {
        try {
            val stateManager = TrackingStateManager(this)
            stateManager.setTrackingActive(true)
            stateManager.updateHeartbeat()
            TrackingWorker.schedule(this)
            TrackingAlarmReceiver.schedule(this)
            Log.d(TAG, "✅ Tracking started via FCM")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting tracking", e)
        }
    }

    private fun stopTracking() {
        try {
            val stateManager = TrackingStateManager(this)
            stateManager.clearState()
            TrackingWorker.cancel(this)
            TrackingAlarmReceiver.cancel(this)
            Log.d(TAG, "✅ Tracking stopped via FCM")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping tracking", e)
        }
    }
}
