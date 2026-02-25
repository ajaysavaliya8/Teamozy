package com.hrms.jeejateamozy.core.network

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody


// ========================================
// AUTH MODELS
// ========================================

data class BasicResponse(
    val success: Boolean,
    val message: String? = null
)

// Response for: POST /send-login, POST /send-change-device-otp
data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val error_code: String? = null
)

// Response for: POST /verify-reset-otp (returns reset_token)
data class ResetOtpResponse(
    val success: Boolean,
    val message: String? = null,
    val error_code: String? = null,
    val data: ResetOtpData? = null
)

data class ResetOtpData(
    val reset_token: String,
    val expires_in_minutes: Int
)

// Response for: POST /verify-login (login data nested in "data")
data class LoginResponse(
    val success: Boolean,
    val message: String? = null,
    val error_code: String? = null,
    val data: LoginData? = null
)

data class LoginData(
    val token: String? = null,
    val name: String? = null,
    val mobile_number: Long? = null,
    val full_name: String? = null,
    val profile_url: String? = null,
    val branch_name: String? = null,
    val department_name: String? = null,
    val shift_name: String? = null,

    // Social Media
    val facebook: String? = null,
    val linkedin: String? = null,
    val x: String? = null,
    val instagram: String? = null,
    val snapchat: String? = null,
    val tiktok: String? = null,

    // Company Information
    val company_name: String? = null,
    val company_address: String? = null,
    val company_email: String? = null,
    val company_contact: String? = null,
    val company_website: String? = null,
    val company_logo_url: String? = null,

    // Support Information
    val hr_email: String? = null,
    val technical_support_number: String? = null,
    val technical_support_email: String? = null
)

data class PushNotificationStatus(
    val registered: Boolean,
    val enabled: Boolean,
    val onesignal_player_id: String? = null,
    val onesignal_subscription_id: String? = null,
    val has_fcm_backup: Boolean,
    val registered_at: String? = null,
    val last_notification_sent: String? = null,
    val failure_count: Int
)

// ========================================
// CHECK STATUS / ATTENDANCE MODELS
// ========================================

data class CheckStatusResponse(
    val success: Boolean,
    val message: String? = null,
    val data: CheckStatusData? = null
)

data class CheckStatusData(
    val current_state: String,
    val message: String,
    val is_complete: Boolean? = null,
    val attendance_status: String? = null,
    val active_sessions_count: Int? = null,
    val last_check_in_time: String? = null,
    val attendance_date: String? = null,
    val check_out_time: String? = null
)

data class CheckInResponse(
    val success: Boolean,
    val message: String? = null,
    val data: CheckInData? = null
)

data class CheckInData(
    val t_token: String? = null,
    val face_verification_required: Boolean? = null,
    val minimum_quality_score: Float? = null,
    val face_vector: String? = null,
    val is_late: Boolean? = null,
    val late_minutes: Int? = null,
    val is_out_of_range: Boolean? = null,
    val late_reason_required: Boolean? = null,
    val out_of_range_reason_required: Boolean? = null,
    val pending_message: PendingMessage? = null,
    val shift_info: CheckInShiftInfo? = null
)

data class CheckInShiftInfo(
    val shift_start: String? = null,
    val shift_end: String? = null,
    val crosses_midnight: Boolean? = null,
    val grace_minutes: Int? = null
)

data class CheckInSignatureResponse(
    val success: Boolean,
    val message: String,
    val data: CheckInSignatureData? = null
)

data class CheckInSignatureData(
    val attendance_record_id: Int? = null,
    val session_id: Int? = null,
    val check_in_time: String? = null,
    val check_out_time: String? = null,
    val is_new_record: Boolean? = null,
    val first_location_tracked: Boolean? = null,
    val location_history_id: Int? = null
)

data class CheckOutResponse(
    val success: Boolean,
    val message: String? = null,
    val data: CheckOutData? = null
)

data class CheckOutData(
    val t_token: String? = null,
    val face_verification_required: Boolean? = null,
    val minimum_quality_score: Float? = null,
    val face_vector: String? = null,
    val is_early: Boolean? = null,
    val early_minutes: Int? = null,
    val is_out_of_range: Boolean? = null,
    val early_reason_required: Boolean? = null,
    val out_of_range_reason_required: Boolean? = null,
    val work_report_require: Boolean? = null,
    val work_minutes: Int? = null
)

data class CheckOutSignatureResponse(
    val success: Boolean,
    val message: String,
    val data: CheckOutSignatureData? = null
)

data class CheckOutSignatureData(
    val attendance_record_id: Int? = null,
    val session_id: Int? = null,
    val check_out_time: String? = null,
    val work_minutes: Int? = null,
    val attendance_status: String? = null,
    val is_complete: Boolean? = null,
    val work_report_id: Int? = null,
    val attachments_count: Int? = null,
    val last_location_tracked: Boolean? = null,
    val location_history_id: Int? = null
)

// ========================================
// ATTENDANCE HISTORY / TIMESHEET MODELS
// ========================================

data class MonthlyTimesheetResponse(
    val success: Boolean,
    val message: String? = null,
    val data: MonthlyTimesheetData
)

data class MonthlyTimesheetData(
    val month: Int,
    val year: Int,
    val month_name: String,
    val calendar_days: List<CalendarDayDto>,
    val summary: MonthSummaryDto
)

data class CalendarDayDto(
    val day: Int,
    val date: String,
    val status: String,
    val color: String,
    val is_complete: Boolean,
    val has_irregularity: Boolean,
    val check_count: Int,
    val shift_name: String? = null,
    val is_correction_request_pending: Boolean = false
) {
    fun toDomain() = CalendarDay(
        day = day,
        date = date,
        status = status,
        color = color,
        isComplete = is_complete,
        hasIrregularity = has_irregularity,
        checkCount = check_count,
        shiftName = shift_name,
        isCorrectionRequestPending = is_correction_request_pending
    )
}

data class MonthSummaryDto(
    val total_time: String,
    val total_minutes: Int,
    val present_days: Int,
    val irregular_days: Int
) {
    fun toDomain() = MonthSummary(
        totalTime = total_time,
        totalMinutes = total_minutes,
        presentDays = present_days,
        irregularDays = irregular_days
    )
}

data class DayTimesheetResponse(
    val success: Boolean,
    val message: String? = null,
    val data: DayTimesheetData
)

data class DayTimesheetData(
    val has_attendance: Boolean,
    val date: String,
    val day_name: String,
    val attendance_record_id: Int? = null,
    val status: DayStatusDto? = null,
    val shift: ShiftInfoDto? = null,
    val hours: HoursInfoDto? = null,
    val checks: List<PunchRecordDto>? = null,
    val is_complete: Boolean? = null,
    val correction_request: DayCorrectionRequestDto? = null,
    val allow_correction: Boolean = true,
    val allow_withdraw: Boolean = false
) {
    fun toDomain() = DayTimesheet(
        hasAttendance = has_attendance,
        date = date,
        dayName = day_name,
        attendanceRecordId = attendance_record_id,
        status = status?.toDomain(),
        shift = shift?.toDomain(),
        hours = hours?.toDomain(),
        checks = checks?.map { it.toDomain() },
        isComplete = is_complete,
        correctionRequest = correction_request?.toDomain(),
        allowCorrection = allow_correction,
        allowWithdraw = allow_withdraw
    )
}

data class DayStatusDto(
    val text: String,
    val color: String,
    val raw_status: String
) {
    fun toDomain() = DayStatus(text = text, color = color, rawStatus = raw_status)
}

data class ShiftInfoDto(
    val name: String? = null,
    val hours: String,
    val start_time: String,
    val end_time: String
) {
    fun toDomain() = ShiftInfo(
        name = name,
        hours = hours,
        startTime = start_time,
        endTime = end_time
    )
}

data class HoursInfoDto(
    val total_display: String,
    val productive_display: String
) {
    fun toDomain() = HoursInfo(
        totalDisplay = total_display,
        productiveDisplay = productive_display
    )
}

data class PunchRecordDto(
    val type: String,
    val time: String?,
    val location: PunchLocationDto?
) {
    fun toDomain() = PunchRecord(
        type = type,
        time = time,
        location = location?.toDomain()
    )
}

data class PunchLocationDto(
    val latitude: Double?,
    val longitude: Double?
) {
    fun toDomain() = PunchLocation(latitude = latitude, longitude = longitude)
}

// Day correction request (embedded in day timesheet)
data class DayCorrectionRequestDto(
    val id: Int,
    val reference_number: String? = null,
    val request_type: String,
    val status: String,
    val requested_status: String? = null,
    val requested_check_in: String? = null,
    val requested_check_out: String? = null,
    val leave_type_name: String? = null,
    val reason: String? = null,
    val request_date: String? = null,
    val pending_with: List<String>? = null
) {
    fun toDomain() = DayCorrectionRequest(
        id = id,
        referenceNumber = reference_number,
        requestType = request_type,
        status = status,
        requestedStatus = requested_status,
        requestedCheckIn = requested_check_in,
        requestedCheckOut = requested_check_out,
        leaveTypeName = leave_type_name,
        reason = reason,
        requestDate = request_date,
        pendingWith = pending_with
    )
}

// Correction Request Options
data class CorrectionRequestOptionsResponse(
    val success: Boolean,
    val message: String? = null,
    val data: CorrectionRequestOptionsDataDto
)

data class CorrectionRequestOptionsDataDto(
    val request_types: List<RequestTypeOptionDto>,
    val status_options: List<StatusOptionDto>,
    val priority_options: List<PriorityOptionDto>,
    val leave_types: List<LeaveTypeOptionDto>
) {
    fun toDomain() = CorrectionRequestOptionsData(
        requestTypes = request_types.map { it.toDomain() },
        statusOptions = status_options.map { it.toDomain() },
        priorityOptions = priority_options.map { it.toDomain() },
        leaveTypes = leave_types.map { it.toDomain() }
    )
}

data class RequestTypeOptionDto(
    val value: String,
    val label: String,
    val description: String
) {
    fun toDomain() = RequestTypeOption(value = value, label = label, description = description)
}

data class StatusOptionDto(
    val value: String,
    val label: String
) {
    fun toDomain() = StatusOption(value = value, label = label)
}

data class PriorityOptionDto(
    val value: String,
    val label: String
) {
    fun toDomain() = PriorityOption(value = value, label = label)
}

data class LeaveTypeOptionDto(
    val value: Int,
    val label: String,
    val description: String?
) {
    fun toDomain() = LeaveTypeOption(id = value, label = label, description = description)
}

// Submit Correction Response (simple success/message)
data class SubmitCorrectionRequestResponse(
    val success: Boolean,
    val message: String
)

// Withdraw Correction Response
data class WithdrawCorrectionRequestResponse(
    val success: Boolean,
    val message: String
)

// Correction Request List Response (paginated)
data class CorrectionRequestListResponse(
    val success: Boolean,
    val message: String? = null,
    val data: CorrectionRequestListData
)

data class CorrectionRequestListData(
    val requests: List<CorrectionRequestListItemDto>,
    val pagination: PaginationDto
)

data class CorrectionRequestListItemDto(
    val id: Int,
    val reference_number: String? = null,
    val request_type: String,
    val attendance_date: String,
    val status: String,
    val request_date: String? = null
) {
    fun toDomain() = CorrectionRequestListItem(
        id = id,
        referenceNumber = reference_number,
        requestType = request_type,
        attendanceDate = attendance_date,
        status = status,
        requestDate = request_date
    )
}

// Correction Request Detail Response (with timeline)
data class CorrectionRequestDetailResponse(
    val success: Boolean,
    val message: String? = null,
    val data: CorrectionRequestDetailDto
)

data class CorrectionRequestDetailDto(
    val id: Int,
    val reference_number: String? = null,
    val request_type: String,
    val attendance_date: String,
    val reason: String? = null,
    val has_attachment: Boolean = false,
    val request_date: String? = null,
    val requested_status: String? = null,
    val requested_check_in: String? = null,
    val requested_check_out: String? = null,
    val leave_type_name: String? = null,
    val status: String,
    val timeline: List<TimelineEventDto> = emptyList(),
    val approvers: List<String>? = null,
    val pending_with: List<String>? = null,
    val rejection_reason: String? = null,
    val allow_withdraw: Boolean = false
) {
    fun toDomain() = CorrectionRequestDetail(
        id = id,
        referenceNumber = reference_number,
        requestType = request_type,
        attendanceDate = attendance_date,
        reason = reason,
        hasAttachment = has_attachment,
        requestDate = request_date,
        requestedStatus = requested_status,
        requestedCheckIn = requested_check_in,
        requestedCheckOut = requested_check_out,
        leaveTypeName = leave_type_name,
        status = status,
        timeline = timeline.map { it.toDomain() },
        approvers = approvers,
        pendingWith = pending_with,
        rejectionReason = rejection_reason,
        allowWithdraw = allow_withdraw
    )
}

// ========================================
// DOMAIN MODELS (for ViewModel/UI)
// ========================================

data class CalendarDay(
    val day: Int,
    val date: String,
    val status: String,
    val color: String,
    val isComplete: Boolean,
    val hasIrregularity: Boolean,
    val checkCount: Int,
    val shiftName: String? = null,
    val isCorrectionRequestPending: Boolean = false
)

data class MonthSummary(
    val totalTime: String,
    val totalMinutes: Int,
    val presentDays: Int,
    val irregularDays: Int
)

data class MonthlyTimesheet(
    val month: Int,
    val year: Int,
    val monthName: String,
    val calendarDays: List<CalendarDay>,
    val summary: MonthSummary
)

data class DayStatus(
    val text: String,
    val color: String,
    val rawStatus: String
)

data class ShiftInfo(
    val name: String?,
    val hours: String,
    val startTime: String,
    val endTime: String
)

data class HoursInfo(
    val totalDisplay: String,
    val productiveDisplay: String
)

data class PunchRecord(
    val type: String,
    val time: String?,
    val location: PunchLocation?
)

data class PunchLocation(
    val latitude: Double?,
    val longitude: Double?
)

data class DayTimesheet(
    val hasAttendance: Boolean,
    val date: String,
    val dayName: String,
    val attendanceRecordId: Int? = null,
    val status: DayStatus? = null,
    val shift: ShiftInfo? = null,
    val hours: HoursInfo? = null,
    val checks: List<PunchRecord>? = null,
    val isComplete: Boolean? = null,
    val correctionRequest: DayCorrectionRequest? = null,
    val allowCorrection: Boolean = true,
    val allowWithdraw: Boolean = false
)

// Correction request as returned in day timesheet
data class DayCorrectionRequest(
    val id: Int,
    val referenceNumber: String?,
    val requestType: String,
    val status: String,
    val requestedStatus: String? = null,
    val requestedCheckIn: String? = null,
    val requestedCheckOut: String? = null,
    val leaveTypeName: String? = null,
    val reason: String? = null,
    val requestDate: String? = null,
    val pendingWith: List<String>? = null
)

// Correction request list item (from paginated list)
data class CorrectionRequestListItem(
    val id: Int,
    val referenceNumber: String?,
    val requestType: String,
    val attendanceDate: String,
    val status: String,
    val requestDate: String?
)

// Correction request detail (with timeline)
data class CorrectionRequestDetail(
    val id: Int,
    val referenceNumber: String?,
    val requestType: String,
    val attendanceDate: String,
    val reason: String?,
    val hasAttachment: Boolean,
    val requestDate: String?,
    val requestedStatus: String?,
    val requestedCheckIn: String?,
    val requestedCheckOut: String?,
    val leaveTypeName: String?,
    val status: String,
    val timeline: List<TimelineEvent>,
    val approvers: List<String>?,
    val pendingWith: List<String>?,
    val rejectionReason: String?,
    val allowWithdraw: Boolean
)

data class CorrectionRequestOptionsData(
    val requestTypes: List<RequestTypeOption>,
    val statusOptions: List<StatusOption>,
    val priorityOptions: List<PriorityOption>,
    val leaveTypes: List<LeaveTypeOption>
)

data class RequestTypeOption(
    val value: String,
    val label: String,
    val description: String
)

data class StatusOption(
    val value: String,
    val label: String
)

data class PriorityOption(
    val value: String,
    val label: String
)

data class LeaveTypeOption(
    val id: Int,
    val label: String,
    val description: String?
)

// ========================================
// OTHER API MODELS
// ========================================

data class VerifyTokenResponse(
    val success: Boolean,
    val message: String? = null,
    val error_code: String? = null,
    val data: LoginData? = null
)

data class FaceVerifyResponse(
    val status: String? = null,
    val message: String? = null,
    val face_token: String? = null
)

data class FaceRecognitionDataResponse(
    val success: Boolean,
    val message: String? = null,
    val data: FaceRecognitionData? = null
)

data class FaceRecognitionData(
    val face_vector: String? = null,
    val minimum_face_recognition_quality_score: Float? = null,
    val require_face_checkin: Boolean? = null,
    val require_face_break: Boolean? = null
)

data class FaceRegistrationResponse(
    val success: Boolean,
    val message: String? = null,
    val data: FaceRegistrationData? = null
)

data class FaceRegistrationData(
    val image_path: String? = null
)

data class PendingFaceRegistrationResponse(
    val success: Boolean,
    val message: String? = null,
    val data: PendingFaceRegistrationData? = null
)

data class PendingFaceRegistrationData(
    val pending: Boolean
)

data class SocialMediaUpdateResponse(
    val success: Boolean,
    val message: String,
    val data: SocialMediaData? = null
)

data class SocialMediaData(
    val facebook_url: String?,
    val linkedin_url: String?,
    val x_url: String?,
    val instagram_url: String?,
    val snapchat_url: String?
)

data class ProfilePictureUpdateResponse(
    val success: Boolean,
    val message: String,
    val data: ProfilePictureData? = null
)

data class ProfilePictureData(
    val profile_url: String? = null,
    val filename: String? = null
)

// ========================================
// WORK REPORT MODELS
// ========================================

data class WorkReport(
    val id: Int,
    val reportDate: String,
    val workDescription: String,
    val attachments: List<String>,
    val reportStatus: String,
    val branchName: String?,
    val submittedAt: String?,
    val reviewedAt: String?,
    val reviewedByName: String?,
    val reviewerComments: String?,
    val rejectionReason: String?,
    val createdAt: String,
    val updatedAt: String
)

data class WorkReportListResponse(
    val success: Boolean,
    val message: String,
    val data: List<WorkReportDto>? = null,
    val total: Int? = null
)

data class WorkReportDto(
    val id: Int,
    val report_date: String,
    val work_description: String,
    val attachments: String? = null,
    val report_status: String,
    val branch_name: String?,
    val submitted_at: String?,
    val reviewed_at: String?,
    val reviewed_by_name: String?,
    val reviewer_comments: String?,
    val rejection_reason: String?,
    val created_at: String,
    val updated_at: String
) {
    private fun parseAttachments(): List<String> {
        if (attachments.isNullOrBlank() || attachments == "[]") return emptyList()
        return try {
            com.google.gson.Gson().fromJson(attachments, Array<String>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toDomain() = WorkReport(
        id = id,
        reportDate = report_date,
        workDescription = work_description,
        attachments = parseAttachments(),
        reportStatus = report_status,
        branchName = branch_name,
        submittedAt = submitted_at,
        reviewedAt = reviewed_at,
        reviewedByName = reviewed_by_name,
        reviewerComments = reviewer_comments,
        rejectionReason = rejection_reason,
        createdAt = created_at,
        updatedAt = updated_at
    )
}

data class CreateWorkReportResponse(
    val success: Boolean,
    val message: String,
    val data: CreatedWorkReportDto? = null
)

data class CreatedWorkReportDto(
    val id: Int,
    val report_date: String,
    val work_description: String,
    val attachments_count: Int,
    val report_status: String,
    val submitted_at: String,
    val reports_today: Int? = null,
    val remaining_reports_today: Int? = null
)

// ========================================
// CIRCULAR MODELS
// ========================================

data class Circular(
    val id: Int,
    val title: String,
    val description: String,
    val circularType: String,
    val priority: String,
    val attachments: List<String>,
    val effectiveDate: String?,
    val expiryDate: String?,
    val publishedDate: String?
)

// CircularDetail is same as Circular in the new API
typealias CircularDetail = Circular

data class CircularStats(
    val totalCirculars: Int,
    val highPriority: Int,
    val recent7Days: Int
)

data class PaginationInfo(
    val currentPage: Int,
    val pageSize: Int,
    val totalCount: Int,
    val totalPages: Int
)

data class CircularListResponse(
    val success: Boolean,
    val message: String? = null,
    val data: List<CircularDto>? = null,
    val total: Int? = null,
    val pagination: PaginationDto? = null
)

data class CircularDto(
    val id: Int,
    val title: String,
    val description: String,
    val circular_type: String,
    val priority: String,
    val attachments: String? = null,
    val effective_date: String?,
    val expiry_date: String?,
    val published_date: String?
) {
    private fun parseAttachments(): List<String> {
        if (attachments.isNullOrBlank() || attachments == "[]") return emptyList()
        return try {
            com.google.gson.Gson().fromJson(attachments, Array<String>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toDomain() = Circular(
        id = id,
        title = title,
        description = description,
        circularType = circular_type,
        priority = priority,
        attachments = parseAttachments(),
        effectiveDate = effective_date,
        expiryDate = expiry_date,
        publishedDate = published_date
    )
}

data class PaginationDto(
    val current_page: Int,
    val page_size: Int,
    val total_count: Int? = null,
    val total_pages: Int
) {
    fun toDomain() = PaginationInfo(
        currentPage = current_page,
        pageSize = page_size,
        totalCount = total_count ?: 0,
        totalPages = total_pages
    )
}

data class CircularDetailResponse(
    val success: Boolean,
    val message: String? = null,
    val data: CircularDetailDto? = null
)

// CircularDetailDto is same structure as CircularDto in the new API
data class CircularDetailDto(
    val id: Int,
    val title: String,
    val description: String,
    val circular_type: String,
    val priority: String,
    val attachments: String? = null,
    val effective_date: String?,
    val expiry_date: String?,
    val published_date: String?
) {
    private fun parseAttachments(): List<String> {
        if (attachments.isNullOrBlank() || attachments == "[]") return emptyList()
        return try {
            com.google.gson.Gson().fromJson(attachments, Array<String>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toDomain() = Circular(
        id = id,
        title = title,
        description = description,
        circularType = circular_type,
        priority = priority,
        attachments = parseAttachments(),
        effectiveDate = effective_date,
        expiryDate = expiry_date,
        publishedDate = published_date
    )
}

data class CircularStatsResponse(
    val success: Boolean,
    val message: String? = null,
    val data: CircularStatsDto? = null
)

data class CircularStatsDto(
    val total_circulars: Int,
    val high_priority: Int,
    val recent_7_days: Int
) {
    fun toDomain() = CircularStats(
        totalCirculars = total_circulars,
        highPriority = high_priority,
        recent7Days = recent_7_days
    )
}

// ========================================
// LEAVE MODELS - UPDATED WITH HALF-DAY SUPPORT
// ========================================

// Domain model for LeaveType
data class LeaveType(
    val id: Int,
    val name: String,
    val code: String,
    val description: String?,
    val requiresApproval: Boolean,
    val applyOnHolidays: Boolean,
    val isPaid: Boolean,
    val applicableFor: String?,
    val isActive: Boolean,
    val requiresDocument: Boolean,
    val maxDays: Int?,
    val minNoticeDays: Int?,
    val isCarryForward: Boolean,
    val maxCarryForwardDays: Int?,
    val colorCode: String?,
    val allowHalfDay: Boolean
)

data class LeaveTypesResponse(
    val status: String,
    val data: List<LeaveTypeDto>
)

data class LeaveTypeDto(
    val id: Int,
    val name: String,
    val code: String,
    val description: String? = null,
    val requires_approval: Boolean = true,
    val apply_on_holidays: Boolean = false,
    val is_paid: Boolean = true,
    val applicable_for: String? = null,
    val is_active: Boolean = true,
    val requires_document: Boolean = false,
    val max_days: Int? = null,
    val min_notice_days: Int? = null,
    val is_carry_forward: Boolean = false,
    val max_carry_forward_days: Int? = null,
    val color_code: String? = null,
    val allow_half_day: Boolean = true,
    val display_order: Int? = null
) {
    fun toDomain() = LeaveType(
        id = id,
        name = name,
        code = code,
        description = description,
        requiresApproval = requires_approval,
        applyOnHolidays = apply_on_holidays,
        isPaid = is_paid,
        applicableFor = applicable_for,
        isActive = is_active,
        requiresDocument = requires_document,
        maxDays = max_days,
        minNoticeDays = min_notice_days,
        isCarryForward = is_carry_forward,
        maxCarryForwardDays = max_carry_forward_days,
        colorCode = color_code,
        allowHalfDay = allow_half_day
    )
}

data class ApplyLeaveResponse(
    val status: String,
    val message: String,
    val data: ApplyLeaveData?
)

data class ApplyLeaveData(
    val application_id: Int,
    val applied_at: String,
    val total_days: Double,
    val effective_days: Double,
    val status: String,
    val requires_approval: Boolean,
    // New workflow fields
    val reference_number: String? = null,
    val workflow_status: String? = null,
    val approval_instance_id: Int? = null,
    val workflow_name: String? = null,
    val current_step: Int? = null,
    val current_step_name: String? = null,
    val total_steps: Int? = null,
    val pending_with: List<String>? = null,
    val auto_approved: Boolean? = null,
    val workflow_error: String? = null
)

data class LeaveApplicationsResponse(
    val status: String,
    val data: LeaveApplicationsData
)

data class LeaveApplicationsData(
    val applications: List<LeaveApplicationDto>,
    val pagination: PaginationDto
)

// Minimal DTO for leave applications list view
data class LeaveApplicationDto(
    val id: Int,
    val reference_number: String? = null,
    val leave_type_name: String,
    val color_code: String? = null,
    val start_date: String,
    val end_date: String,
    val total_days: Double,
    val status: String
) {
    fun toDomain() = LeaveApplication(
        id = id,
        referenceNumber = reference_number,
        leaveTypeName = leave_type_name,
        colorCode = color_code,
        startDate = start_date,
        endDate = end_date,
        totalDays = total_days,
        status = status
    )
}

// Minimal domain model for LeaveApplication list view
data class LeaveApplication(
    val id: Int,
    val referenceNumber: String?,
    val leaveTypeName: String,
    val colorCode: String?,
    val startDate: String,
    val endDate: String,
    val totalDays: Double,
    val status: String
) {
    // Backward compatibility - returns totalDays as Int
    val numDays: Int get() = totalDays.toInt()
}

data class LeaveTypeInfoDto(
    val id: Int,
    val name: String,
    val code: String,
    val is_paid: Boolean,
    val color_code: String? = null
) {
    fun toDomain() = LeaveTypeInfo(
        id = id,
        name = name,
        code = code,
        isPaid = is_paid,
        colorCode = color_code
    )
}

data class LeaveTypeInfo(
    val id: Int,
    val name: String,
    val code: String,
    val isPaid: Boolean,
    val colorCode: String? = null
)

data class ApproverInfoDto(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val remarks: String? = null,
    val status: String? = null,
    val approved_at: String? = null
) {
    fun toDomain() = ApproverInfo(
        name = name,
        status = status,
        remarks = remarks,
        approvedAt = approved_at
    )
}

data class ApproverInfo(
    val name: String?,
    val status: String?,
    val remarks: String?,
    val approvedAt: String?
)

// =============================================================================
// Leave Application Detail (Simplified)
// =============================================================================

data class LeaveApplicationDetailResponse(
    val status: String,
    val data: LeaveApplicationDetailDto
)

data class LeaveApplicationDetailDto(
    val id: Int,
    val reference_number: String? = null,
    val leave_type_name: String,
    val color_code: String? = null,
    val is_paid: Boolean = true,
    val start_date: String,
    val end_date: String,
    val total_days: Double,
    val reason: String? = null,
    val document: String? = null,
    val applied_at: String? = null,
    val status: String,
    val alternate_contact: String? = null,
    val emergency_contact: String? = null,
    val handover: HandoverInfoDto? = null,
    val timeline: List<TimelineEventDto> = emptyList(),
    val approvers: List<String>? = null,
    val pending_with: List<String>? = null,
    val rejection_reason: String? = null,
    val allow_withdraw: Boolean = false,
    val allow_cancel: Boolean = false
) {
    fun toDomain() = LeaveApplicationDetail(
        id = id,
        referenceNumber = reference_number,
        leaveTypeName = leave_type_name,
        colorCode = color_code,
        isPaid = is_paid,
        startDate = start_date,
        endDate = end_date,
        totalDays = total_days,
        reason = reason,
        document = document,
        appliedAt = applied_at,
        status = status,
        alternateContact = alternate_contact,
        emergencyContact = emergency_contact,
        handover = handover?.toDomain(),
        timeline = timeline.map { it.toDomain() },
        approvers = approvers,
        pendingWith = pending_with,
        rejectionReason = rejection_reason,
        allowWithdraw = allow_withdraw,
        allowCancel = allow_cancel
    )
}

data class HandoverInfoDto(
    val handled_by: String? = null,
    val notes: String? = null
) {
    fun toDomain() = HandoverInfo(
        handledBy = handled_by,
        notes = notes
    )
}

data class TimelineEventDto(
    val event: String,
    val at: String? = null,
    val remarks: String? = null,
    val reason: String? = null
) {
    fun toDomain() = TimelineEvent(
        event = event,
        at = at,
        remarks = remarks,
        reason = reason
    )
}

// Domain Models
data class LeaveApplicationDetail(
    val id: Int,
    val referenceNumber: String?,
    val leaveTypeName: String,
    val colorCode: String?,
    val isPaid: Boolean,
    val startDate: String,
    val endDate: String,
    val totalDays: Double,
    val reason: String?,
    val document: String?,
    val appliedAt: String?,
    val status: String,
    val alternateContact: String?,
    val emergencyContact: String?,
    val handover: HandoverInfo?,
    val timeline: List<TimelineEvent>,
    val approvers: List<String>?,
    val pendingWith: List<String>?,
    val rejectionReason: String?,
    val allowWithdraw: Boolean = false,
    val allowCancel: Boolean = false
) {
    val numDays: Int get() = totalDays.toInt()
}

data class HandoverInfo(
    val handledBy: String?,
    val notes: String?
)

data class TimelineEvent(
    val event: String,
    val at: String?,
    val remarks: String?,
    val reason: String?
)

// Leave Summary
data class LeaveSummaryResponse(
    val status: String,
    val data: LeaveSummaryDto
)

data class LeaveSummaryDto(
    val year: Int,
    val total_days_taken: Double,
    val by_status: Map<String, StatusCountDto>,
    val by_type: List<TypeCountDto>,
    val upcoming_leaves: List<UpcomingLeaveDto>
) {
    fun toDomain() = LeaveSummary(
        year = year,
        totalDaysTaken = total_days_taken.toInt(),
        totalDaysTakenDouble = total_days_taken,
        byStatus = by_status.mapValues { it.value.toDomain() },
        byType = by_type.map { it.toDomain() },
        upcomingLeaves = upcoming_leaves.map { it.toDomain() }
    )
}

data class LeaveSummary(
    val year: Int,
    val totalDaysTaken: Int,
    val totalDaysTakenDouble: Double = 0.0,
    val byStatus: Map<String, StatusCount>,
    val byType: List<TypeCount>,
    val upcomingLeaves: List<UpcomingLeave>
)

data class StatusCountDto(
    val count: Int,
    val total_days: Double
) {
    fun toDomain() = StatusCount(count = count, totalDays = total_days.toInt(), totalDaysDouble = total_days)
}

data class StatusCount(
    val count: Int,
    val totalDays: Int,
    val totalDaysDouble: Double = 0.0
)

data class TypeCountDto(
    val leave_type: String,
    val color_code: String? = null,
    val is_paid: Boolean,
    val count: Int,
    val total_days: Double
) {
    fun toDomain() = TypeCount(
        leaveType = leave_type,
        colorCode = color_code,
        isPaid = is_paid,
        count = count,
        totalDays = total_days.toInt(),
        totalDaysDouble = total_days
    )
}

data class TypeCount(
    val leaveType: String,
    val colorCode: String? = null,
    val isPaid: Boolean,
    val count: Int,
    val totalDays: Int,
    val totalDaysDouble: Double = 0.0
)

data class UpcomingLeaveDto(
    val id: Int,
    val leave_type: String,
    val color_code: String? = null,
    val start_date: String,
    val end_date: String,
    val total_days: Double,
    val effective_days: Double? = null,
    val status: String,
    val current_status: String? = null,
    val workflow_status: String? = null
) {
    fun toDomain() = UpcomingLeave(
        id = id,
        leaveType = leave_type,
        colorCode = color_code,
        startDate = start_date,
        endDate = end_date,
        totalDays = total_days,
        effectiveDays = effective_days ?: total_days,
        status = status,
        currentStatus = current_status ?: status,
        workflowStatus = workflow_status
    )
}

data class UpcomingLeave(
    val id: Int,
    val leaveType: String,
    val colorCode: String?,
    val startDate: String,
    val endDate: String,
    val totalDays: Double,
    val effectiveDays: Double,
    val status: String,
    val currentStatus: String,
    val workflowStatus: String?
) {
    // Backward compatibility - returns totalDays as Int
    val numDays: Int get() = totalDays.toInt()
}

// Withdraw / Cancel Leave
data class WithdrawLeaveResponse(
    val status: String,
    val message: String
)

data class CancelLeaveResponse(
    val status: String,
    val message: String
)

// Half Day Type Enum
enum class HalfDayType(val value: String) {
    FIRST_HALF("FIRST_HALF"),
    SECOND_HALF("SECOND_HALF")
}

// Leave Status Enum
enum class LeaveStatus(val value: String) {
    DRAFT("DRAFT"),
    PENDING("PENDING"),
    IN_PROGRESS("IN_PROGRESS"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    CANCELLED("CANCELLED"),
    WITHDRAWN("WITHDRAWN"),
    ON_LEAVE("ON_LEAVE"),
    COMPLETED("COMPLETED"),
    UPCOMING("UPCOMING")
}

// Workflow Status Enum
enum class WorkflowStatus(val value: String) {
    DRAFT("DRAFT"),
    PENDING("PENDING"),
    IN_PROGRESS("IN_PROGRESS"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    CANCELLED("CANCELLED"),
    WITHDRAWN("WITHDRAWN")
}

// ========================================
// LOGOUT & MISC MODELS
// ========================================

data class LogoutResponse(
    val success: Boolean,
    val message: String? = null,
    val error_code: String? = null
)

data class PendingMessage(
    val id: Int,
    @SerializedName("message_type")
    val type: String?,
    val title: String,
    @SerializedName("content")
    val body: String,
    @SerializedName("has_attachments")
    val has_attachment: Boolean,
    val attachment_url: String?,
    @SerializedName("require_acknowledgment")
    val requires_acknowledgment: Boolean
)

// ========================================
// ENUMS
// ========================================

enum class RequestType {
    @SerializedName("NEW_ATTENDANCE") NEW_ATTENDANCE,
    @SerializedName("CORRECTION") CORRECTION,
    @SerializedName("LEAVE_LINKAGE") LEAVE_LINKAGE
}

enum class CorrectionStatus {
    @SerializedName("PENDING") PENDING,
    @SerializedName("MORE_INFO_NEEDED") MORE_INFO_NEEDED,
    @SerializedName("APPROVED") APPROVED,
    @SerializedName("REJECTED") REJECTED,
    @SerializedName("WITHDRAWN") WITHDRAWN
}

// ========================================
// LOCATION SYNC MODELS
// ========================================

/**
 * Response model for location sync API error responses (400)
 * Contains stop_tracking flag that indicates session has ended
 */
data class LocationSyncErrorResponse(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
    @SerializedName("stop_tracking")
    val stopTracking: Boolean = false
)

// ========================================
// NOTIFICATION API MODELS
// ========================================

/**
 * Response for GET /notifications
 */
data class NotificationsResponse(
    val success: Boolean,
    val data: NotificationsData? = null,
    val message: String? = null
)

data class NotificationsData(
    val notifications: List<ServerNotification>,
    @SerializedName("unread_count")
    val unreadCount: Int
)

/**
 * Single notification from server
 * Note: Extra fields like circular_id, leave_id, date are flattened from mobile_params
 */
data class ServerNotification(
    val id: Int,
    @SerializedName("notification_uid")
    val notificationUid: String?,
    val type: String?,
    val title: String?,
    val message: String?,
    val priority: String?,
    val screen: String?,
    @SerializedName("is_read")
    val isRead: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String?,
    // Flattened params from mobile_params
    @SerializedName("circular_id")
    val circularId: Int? = null,
    @SerializedName("leave_id")
    val leaveId: Int? = null,
    @SerializedName("application_id")
    val applicationId: Int? = null,
    val date: String? = null,
    @SerializedName("post_shift_data")
    val postShiftData: String? = null
)

/**
 * Response for mark read / mark all read / delete operations
 */
data class NotificationActionResponse(
    val success: Boolean,
    val message: String? = null
)

// ========================================
// POST-SHIFT CHECK MODELS
// ========================================

data class PostShiftAction(
    val id: String,
    val label: String
)

data class PostShiftCheckPayload(
    val attendanceRecordId: Int,
    val attendanceDate: String? = null,
    val employeeId: Int? = null,
    val shiftEndTime: String? = null,
    val actions: List<PostShiftAction> = emptyList(),
    val title: String,
    val message: String,
    val receivedAtMillis: Long = System.currentTimeMillis()
)

data class PostShiftRequestBody(
    @SerializedName("attendance_record_id") val attendanceRecordId: Int,
    val working: Boolean
)

data class PostShiftResponseResult(
    val success: Boolean,
    val message: String? = null
)

data class PostShiftStatusResponse(
    val success: Boolean,
    val message: String? = null,
    val data: PostShiftStatusData? = null
)

data class PostShiftStatusData(
    val attendance_record_id: Int,
    val attendance_date: String? = null,
    val employee_id: Int? = null,
    val shift_end_time: String? = null,
    val title: String? = null,
    val message: String? = null,
    val actions: List<PostShiftAction>? = null
)

// ========================================
// LOCATION REVERIFY MODELS
// ========================================

data class LocationReverifyResponse(
    val success: Boolean,
    val message: String? = null,
    val data: LocationReverifyData? = null
)

data class LocationReverifyData(
    val t_token: String? = null,
    val is_out_of_range: Boolean? = null
)