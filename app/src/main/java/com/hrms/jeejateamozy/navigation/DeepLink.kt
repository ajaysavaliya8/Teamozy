package com.hrms.jeejateamozy.navigation

/**
 * Represents all possible deep link navigation targets from push notifications
 * Each type corresponds to a screen in the app
 */
sealed class DeepLink {
    // ==========================================
    // MAIN TABS
    // ==========================================
    data object Home : DeepLink()
    data object Attendance : DeepLink()
    data object Profile : DeepLink()

    // ==========================================
    // CIRCULAR SCREENS
    // ==========================================
    data object CircularList : DeepLink()
    data class CircularDetail(val circularId: Int) : DeepLink()

    // ==========================================
    // LEAVE SCREENS
    // ==========================================
    data object LeaveHistory : DeepLink()
    data object ApplyLeave : DeepLink()
    data class LeaveDetail(val leaveId: Int) : DeepLink()

    // ==========================================
    // ATTENDANCE SCREENS
    // ==========================================
    data class AttendanceDetail(val date: String) : DeepLink()

    // ==========================================
    // WORK REPORT
    // ==========================================
    data object WorkReport : DeepLink()

    // ==========================================
    // NOTIFICATION
    // ==========================================
    data object NotificationList : DeepLink()

    // ==========================================
    // PROFILE CHILD SCREENS
    // ==========================================
    data object EditSocialMedia : DeepLink()
    data object EditContact : DeepLink()
    data object EditPersonalInfo : DeepLink()
    data object ViewEmploymentDetail : DeepLink()
    data object EditBankingInfo : DeepLink()
    data object ViewEmploymentIdentity : DeepLink()
    data object ViewShiftDetails : DeepLink()

    companion object {
        // Screen type constants for FCM data payload
        const val SCREEN_HOME = "home"
        const val SCREEN_ATTENDANCE = "attendance"
        const val SCREEN_PROFILE = "profile"
        const val SCREEN_CIRCULAR_LIST = "circular_list"
        const val SCREEN_CIRCULAR_DETAIL = "circular_detail"
        const val SCREEN_LEAVE_HISTORY = "leave_history"
        const val SCREEN_APPLY_LEAVE = "apply_leave"
        const val SCREEN_LEAVE_DETAIL = "leave_detail"
        const val SCREEN_ATTENDANCE_DETAIL = "attendance_detail"
        const val SCREEN_WORK_REPORT = "work_report"
        const val SCREEN_NOTIFICATIONS = "notifications"
        const val SCREEN_EDIT_SOCIAL_MEDIA = "edit_social_media"
        const val SCREEN_EDIT_CONTACT = "edit_contact"
        const val SCREEN_EDIT_PERSONAL_INFO = "edit_personal_info"
        const val SCREEN_VIEW_EMPLOYMENT = "view_employment"
        const val SCREEN_EDIT_BANKING = "edit_banking"
        const val SCREEN_VIEW_IDENTITY = "view_identity"
        const val SCREEN_VIEW_SHIFT = "view_shift"

        /**
         * Parse deep link from FCM data payload
         * @param screen The screen type from FCM "screen" field
         * @param extras Additional data like IDs
         */
        fun fromFcmData(screen: String?, extras: Map<String, String?>): DeepLink? {
            return when (screen?.lowercase()) {
                SCREEN_HOME -> Home
                SCREEN_ATTENDANCE -> Attendance
                SCREEN_PROFILE -> Profile
                SCREEN_CIRCULAR_LIST -> CircularList
                SCREEN_CIRCULAR_DETAIL -> {
                    val id = extras["circular_id"]?.toIntOrNull() ?: return null
                    CircularDetail(id)
                }
                SCREEN_LEAVE_HISTORY -> LeaveHistory
                SCREEN_APPLY_LEAVE -> ApplyLeave
                SCREEN_LEAVE_DETAIL -> {
                    val id = extras["leave_id"]?.toIntOrNull()
                        ?: extras["application_id"]?.toIntOrNull()
                        ?: return null
                    LeaveDetail(id)
                }
                SCREEN_ATTENDANCE_DETAIL -> {
                    val date = extras["date"] ?: return null
                    AttendanceDetail(date)
                }
                SCREEN_WORK_REPORT -> WorkReport
                SCREEN_NOTIFICATIONS -> NotificationList
                SCREEN_EDIT_SOCIAL_MEDIA -> EditSocialMedia
                SCREEN_EDIT_CONTACT -> EditContact
                SCREEN_EDIT_PERSONAL_INFO -> EditPersonalInfo
                SCREEN_VIEW_EMPLOYMENT -> ViewEmploymentDetail
                SCREEN_EDIT_BANKING -> EditBankingInfo
                SCREEN_VIEW_IDENTITY -> ViewEmploymentIdentity
                SCREEN_VIEW_SHIFT -> ViewShiftDetails
                else -> null
            }
        }
    }
}
