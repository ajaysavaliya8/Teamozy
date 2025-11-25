package com.hrms.jeejateamozy.core.network

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody


// -------- Auth --------
data class BasicResponse(
    val status: String,           // "success" | "error" | "reject"
    val message: String? = null,
    val token: String? = null,    // present on verify-login success

    // ===== Employee Information =====
    val mobile_number: Long? = null,
    val full_name: String? = null,
    val profile_url: String? = null,
    val branch_name: String? = null,
    val department_name: String? = null,
    val shift_name: String? = null,

    // ===== Social Media Links =====
    val facebook: String? = null,
    val linkedin: String? = null,
    val x: String? = null,  // Twitter/X
    val instagram: String? = null,
    val snapchat: String? = null,

    // ===== Company Information =====
    val company_name: String? = null,
    val company_address: String? = null,
    val company_email: String? = null,
    val company_contact: String? = null,
    val company_website: String? = null,
    val company_logo_url: String? = null,

    // ===== Support Information =====
    val hr_email: String? = null,
    val technical_support_number: String? = null,
    val technical_support_email: String? = null

    // Note: Push notification status removed - no longer returned by API
    // Firebase FCM token is registered silently in the backend
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

// -------- Check Status Response --------
data class CheckStatusResponse(
    val status: String,           // "success" | "error"
    val message: String? = null,
    val data: CheckStatusData? = null
)

data class CheckStatusData(
    val current_state: String,    // "CHECK_IN_NEEDED" | "CHECK_OUT_NEEDED" | "COMPLETED"
    val last_check_in_time: String? = null,
    val message: String,
    val attendance_status: String? = null,
    val is_complete: Boolean? = null
)

// -------- Check-In Response --------
data class CheckInResponse(
    val status: String,
    val message: String? = null,
    val face_verification_required: Boolean? = null,
    val minimum_quality_score: Float? = null,
    val t_token: String? = null,
    val face_vector: String? = null,
    val is_late: Boolean? = null,
    val is_out_of_range: Boolean? = null,
    val late_reason_required: Boolean? = null,
    val out_of_range_reason_required: Boolean? = null,
    val pending_message: PendingMessage? = null
)

// -------- Check-In Signature Response --------
data class CheckInSignatureResponse(
    val status: String,           // "success" | "error"
    val message: String,
    val attendance_record_id: Int? = null,
    val check_in_time: String? = null
)

// -------- Check-Out Response --------
data class CheckOutResponse(
    val status: String,           // "success" | "error"
    val message: String? = null,
    val face_verification_required: Boolean? = null,
    val minimum_quality_score: Float? = null,
    val t_token: String? = null,
    val face_vector: String? = null,
    val work_hours: Float? = null,
    val is_early: Boolean? = null,
    val is_out_of_range: Boolean? = null,
    val early_reason_required: Boolean? = null,
    val out_of_range_reason_required: Boolean? = null,
    val work_report_require: Boolean? = null
)

// -------- Check-Out Signature Response --------
data class CheckOutSignatureResponse(
    val status: String,           // "success" | "error"
    val message: String,
    val check_out_time: String? = null,
    val work_hours: Float? = null,
    val work_minutes: Int? = null,
    val attendance_status: String? = null,
    val early_leave_minutes: Int? = null,
    val location_violation: Boolean? = null
)

// ========================================
// ATTENDANCE HISTORY / TIMESHEET MODELS
// ========================================

/**
 * GET /timesheet/monthly Response
 */
data class MonthlyTimesheetResponse(
    val status: String,
    val data: MonthlyTimesheetData
)

data class MonthlyTimesheetData(
    val month: Int,
    val year: Int,
    val month_name: String,
    val calendar_days: List<CalendarDayDto>,
    val summary: MonthSummaryDto,
    val chart_data: ChartDataDto,
    val correction_requests: CorrectionRequestSummaryDto? = null
)

data class CalendarDayDto(
    val day: Int,
    val date: String,
    val status: String,
    val color: String,
    val is_complete: Boolean,
    val has_irregularity: Boolean,
    val punch_count: Int,
    val has_correction_request: Boolean = false,
    val correction_badge: CorrectionBadgeDto? = null,
    val correction_request_id: Int? = null
) {
    fun toDomain() = CalendarDay(
        day = day,
        date = date,
        status = status,
        color = color,
        isComplete = is_complete,
        hasIrregularity = has_irregularity,
        punchCount = punch_count,
        hasCorrectionRequest = has_correction_request,
        correctionBadge = correction_badge?.toDomain(),
        correctionRequestId = correction_request_id
    )
}

data class MonthSummaryDto(
    val total_time: String,
    val total_minutes: Int,
    val total_hours: Int,
    val monthly_hours_spent: Int,
    val present_days: Int,
    val irregular_days: Int
) {
    fun toDomain() = MonthSummary(
        totalTime = total_time,
        totalMinutes = total_minutes,
        totalHours = total_hours,
        monthlyHoursSpent = monthly_hours_spent,
        presentDays = present_days,
        irregularDays = irregular_days
    )
}

data class ChartDataDto(
    val days: Int,
    val irregularities: Int
) {
    fun toDomain() = ChartData(
        days = days,
        irregularities = irregularities
    )
}

/**
 * GET /timesheet/day/{attendance_date} Response
 */
data class DayTimesheetResponse(
    val status: String,
    val data: DayTimesheetData
)

data class DayTimesheetData(
    val has_attendance: Boolean,
    val date: String,
    val day_name: String,
    val formatted_date: String?,
    val message: String?,
    val status: DayStatusDto?,
    val shift: ShiftInfoDto?,
    val hours: HoursInfoDto?,
    val punches: List<PunchRecordDto>?,
    val is_complete: Boolean?,
    val attendance_record_id: Int? = null,
    val correction_request: CorrectionRequestContainerDto? = null,
    val can_submit_correction: Boolean? = null,
    val has_pending_request: Boolean? = null,
    val available_actions: List<String>? = null,
    val can_request_new_attendance: Boolean? = null,
    val action_available: Map<String, Boolean>? = null
)

data class DayStatusDto(
    val text: String,
    val color: String,
    val raw_status: String
) {
    fun toDomain() = DayStatus(
        text = text,
        color = color,
        rawStatus = raw_status
    )
}

data class ShiftInfoDto(
    val name: String,
    val hours: String,
    val start_time: String,
    val end_time: String,
    val timing_display: String
) {
    fun toDomain() = ShiftInfo(
        name = name,
        hours = hours,
        startTime = start_time,
        endTime = end_time,
        timingDisplay = timing_display
    )
}

data class HoursInfoDto(
    val total: String,
    val total_display: String,
    val productive: String,
    val productive_display: String
) {
    fun toDomain() = HoursInfo(
        total = total,
        totalDisplay = total_display,
        productive = productive,
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
    fun toDomain() = PunchLocation(
        latitude = latitude,
        longitude = longitude
    )
}

// ========================================
// CORRECTION REQUEST DTOs
// ========================================

/**
 * Correction Badge DTO
 */
data class CorrectionBadgeDto(
    val type: String,
    val text: String,
    val color: String
) {
    fun toDomain() = CorrectionBadge(
        type = type,
        text = text,
        color = color
    )
}

/**
 * Correction Request Summary DTO
 */
data class CorrectionRequestSummaryDto(
    val total: Int,
    val pending: Int,
    val approved: Int,
    val rejected: Int,
    val info_needed: Int
) {
    fun toDomain() = CorrectionRequestSummary(
        total = total,
        pending = pending,
        approved = approved,
        rejected = rejected,
        infoNeeded = info_needed
    )
}

/**
 * Correction Request Container DTO
 */
data class CorrectionRequestContainerDto(
    val has_any: Boolean,
    val active: CorrectionRequestDto? = null,
    val settled: SettledCorrectionRequestDto? = null
) {
    fun toDomain() = CorrectionRequestContainer(
        hasAny = has_any,
        active = active?.toDomain(),
        settled = settled?.toDomain()
    )
}

/**
 * Active Correction Request DTO
 */
data class CorrectionRequestDto(
    val id: Int,
    val request_type: String,
    val status: String,
    val priority: String,
    val reason: String,
    val requested_changes: RequestedChangesDto,
    val attachment: AttachmentInfoDto? = null,
    val request_date: String,
    val review_info: ReviewInfoDto? = null
) {
    fun toDomain() = CorrectionRequest(
        id = id,
        requestType = request_type,
        status = status,
        priority = priority,
        reason = reason,
        requestedChanges = requested_changes.toDomain(),
        attachment = attachment?.toDomain(),
        requestDate = request_date,
        reviewInfo = review_info?.toDomain()
    )
}

/**
 * Settled Correction Request DTO
 */
data class SettledCorrectionRequestDto(
    val id: Int,
    val request_type: String,
    val final_status: String,
    val priority: String,
    val reason: String,
    val requested_changes: RequestedChangesDto,
    val attachment: AttachmentInfoDto? = null,
    val request_date: String,
    val settled_date: String,
    val changes_applied: RequestedChangesDto? = null,
    val review_info: ReviewInfoDto
) {
    fun toDomain() = SettledCorrectionRequest(
        id = id,
        requestType = request_type,
        finalStatus = final_status,
        priority = priority,
        reason = reason,
        requestedChanges = requested_changes.toDomain(),
        attachment = attachment?.toDomain(),
        requestDate = request_date,
        settledDate = settled_date,
        changesApplied = changes_applied?.toDomain(),
        reviewInfo = review_info.toDomain()
    )
}

/**
 * Requested Changes DTO
 */
data class RequestedChangesDto(
    val status: String? = null,
    val check_in: String? = null,
    val check_out: String? = null,
    val leave_type_name: String? = null
) {
    fun toDomain() = RequestedChanges(
        status = status,
        checkIn = check_in,
        checkOut = check_out,
        leaveTypeName = leave_type_name
    )
}

/**
 * Attachment Info DTO
 */
data class AttachmentInfoDto(
    val has_attachment: Boolean,
    val file_name: String? = null,
    val download_url: String? = null
) {
    fun toDomain() = AttachmentInfo(
        hasAttachment = has_attachment,
        fileName = file_name,
        downloadUrl = download_url
    )
}

/**
 * Review Info DTO
 */
data class ReviewInfoDto(
    val reviewer_name: String,
    val reviewed_at: String,
    val comments: String?
) {
    fun toDomain() = ReviewInfo(
        reviewerName = reviewer_name,
        reviewedAt = reviewed_at,
        comments = comments
    )
}

/**
 * Correction Request Options Response
 */
data class CorrectionRequestOptionsResponse(
    val status: String,
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
    fun toDomain() = RequestTypeOption(
        value = value,
        label = label,
        description = description
    )
}

data class StatusOptionDto(
    val value: String,
    val label: String
) {
    fun toDomain() = StatusOption(
        value = value,
        label = label
    )
}

data class PriorityOptionDto(
    val value: String,
    val label: String
) {
    fun toDomain() = PriorityOption(
        value = value,
        label = label
    )
}

data class LeaveTypeOptionDto(
    val id: Int,
    val label: String,
    val description: String?
) {
    fun toDomain() = LeaveTypeOption(
        id = id,
        label = label,
        description = description
    )
}

/**
 * Submit Correction Request Response
 */
data class SubmitCorrectionRequestResponse(
    val status: String,
    val message: String,
    val data: SubmittedCorrectionRequestDataDto
)

data class SubmittedCorrectionRequestDataDto(
    val request_id: Int,
    val status: String,
    val submitted_at: String
)

/**
 * Withdraw Correction Request Response
 */
data class WithdrawCorrectionRequestResponse(
    val status: String,
    val message: String,
    val data: WithdrawnCorrectionRequestDataDto
)

data class WithdrawnCorrectionRequestDataDto(
    val request_id: Int,
    val new_status: String,
    val withdrawn_at: String
)

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
    val punchCount: Int,
    val hasCorrectionRequest: Boolean = false,
    val correctionBadge: CorrectionBadge? = null,
    val correctionRequestId: Int? = null
)

data class MonthSummary(
    val totalTime: String,
    val totalMinutes: Int,
    val totalHours: Int,
    val monthlyHoursSpent: Int,
    val presentDays: Int,
    val irregularDays: Int
)

data class ChartData(
    val days: Int,
    val irregularities: Int
)

data class MonthlyTimesheet(
    val month: Int,
    val year: Int,
    val monthName: String,
    val calendarDays: List<CalendarDay>,
    val summary: MonthSummary,
    val chartData: ChartData,
    val correctionRequests: CorrectionRequestSummary? = null
)

data class DayStatus(
    val text: String,
    val color: String,
    val rawStatus: String
)

data class ShiftInfo(
    val name: String,
    val hours: String,
    val startTime: String,
    val endTime: String,
    val timingDisplay: String
)

data class HoursInfo(
    val total: String,
    val totalDisplay: String,
    val productive: String,
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
    val formattedDate: String?,
    val message: String?,
    val status: DayStatus?,
    val shift: ShiftInfo?,
    val hours: HoursInfo?,
    val punches: List<PunchRecord>?,
    val isComplete: Boolean?,
    val attendanceRecordId: Int? = null,
    val correctionRequest: CorrectionRequestContainer? = null,
    val canSubmitCorrection: Boolean? = null,
    val hasPendingRequest: Boolean? = null,
    val availableActions: List<String>? = null,
    val canRequestNewAttendance: Boolean? = null,
    val actionAvailable: Map<String, Boolean>? = null
)

// ========================================
// CORRECTION REQUEST DOMAIN MODELS
// ========================================

/**
 * Correction Badge (mini indicator on calendar)
 */
data class CorrectionBadge(
    val type: String,
    val text: String,
    val color: String
)

/**
 * Correction Request Summary (monthly totals)
 */
data class CorrectionRequestSummary(
    val total: Int,
    val pending: Int,
    val approved: Int,
    val rejected: Int,
    val infoNeeded: Int
)

/**
 * Correction Request Container
 */
data class CorrectionRequestContainer(
    val hasAny: Boolean,
    val active: CorrectionRequest? = null,
    val settled: SettledCorrectionRequest? = null
)

/**
 * Active Correction Request
 */
data class CorrectionRequest(
    val id: Int,
    val requestType: String,
    val status: String,
    val priority: String,
    val reason: String,
    val requestedChanges: RequestedChanges,
    val attachment: AttachmentInfo? = null,
    val requestDate: String,
    val reviewInfo: ReviewInfo? = null
)

/**
 * Settled Correction Request
 */
data class SettledCorrectionRequest(
    val id: Int,
    val requestType: String,
    val finalStatus: String,
    val priority: String,
    val reason: String,
    val requestedChanges: RequestedChanges,
    val attachment: AttachmentInfo? = null,
    val requestDate: String,
    val settledDate: String,
    val changesApplied: RequestedChanges? = null,
    val reviewInfo: ReviewInfo
)

/**
 * Requested Changes
 */
data class RequestedChanges(
    val status: String? = null,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val leaveTypeName: String? = null
)

/**
 * Attachment Info
 */
data class AttachmentInfo(
    val hasAttachment: Boolean,
    val fileName: String? = null,
    val downloadUrl: String? = null
)

/**
 * Review Info
 */
data class ReviewInfo(
    val reviewerName: String,
    val reviewedAt: String,
    val comments: String?
)

/**
 * Correction Request Options Data
 */
data class CorrectionRequestOptionsData(
    val requestTypes: List<RequestTypeOption>,
    val statusOptions: List<StatusOption>,
    val priorityOptions: List<PriorityOption>,
    val leaveTypes: List<LeaveTypeOption>
)

/**
 * Request Type Option
 */
data class RequestTypeOption(
    val value: String,
    val label: String,
    val description: String
)

/**
 * Status Option
 */
data class StatusOption(
    val value: String,
    val label: String
)

/**
 * Priority Option
 */
data class PriorityOption(
    val value: String,
    val label: String
)

/**
 * Leave Type Option
 */
data class LeaveTypeOption(
    val id: Int,
    val label: String,
    val description: String?
)

// ========================================
// OTHER API MODELS
// ========================================

// Verify token endpoint response
data class VerifyTokenResponse(
    val status: String,
    val message: String
)

// Face verify upload response
data class FaceVerifyResponse(
    val status: String? = null,
    val message: String? = null,
    val face_token: String? = null
)

// Face recognition GET endpoint response
data class FaceRecognitionDataResponse(
    val status: String? = null,
    val message: String,
    val face_vector: String? = null,
    val minimum_face_recognition_quality_score: Float? = null,
    val require_face_checkin: Boolean? = null,
    val require_face_break: Boolean? = null
)

// Pending face registration request response
data class PendingFaceRegistrationResponse(
    val status: String,
    val pending: Boolean,
    val message: String
)

// -------- Social Media Update Response --------
data class SocialMediaUpdateResponse(
    val status: String,
    val message: String,
    val social_media: SocialMediaData? = null
)

data class SocialMediaData(
    val facebook: String?,
    val linkedin: String?,
    val x: String?,
    val instagram: String?,
    val snapchat: String?
)

// -------- Profile Picture Update Response --------
data class ProfilePictureUpdateResponse(
    val status: String,
    val message: String,
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

/**
 * API Response for GET /work-report
 */
data class WorkReportListResponse(
    val status: String,
    val message: String,
    val month: Int,
    val year: Int,
    val total_reports: Int,
    val reports: List<WorkReportDto>
)

/**
 * Work Report DTO from API
 */
data class WorkReportDto(
    val id: Int,
    val report_date: String,
    val work_description: String,
    val attachments: String,
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
    fun toDomain() = WorkReport(
        id = id,
        reportDate = report_date,
        workDescription = work_description,
        attachments = parseAttachments(attachments),
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

    private fun parseAttachments(attachmentsJson: String): List<String> {
        return try {
            if (attachmentsJson.isBlank() || attachmentsJson == "[]") {
                emptyList()
            } else {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                gson.fromJson(attachmentsJson, type) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * API Response for POST /work-report
 */
data class CreateWorkReportResponse(
    val status: String,
    val message: String,
    val work_report: CreatedWorkReportDto?,
    val reports_today: Int?,
    val remaining_reports_today: Int?
)

data class CreatedWorkReportDto(
    val id: Int,
    val report_date: String,
    val work_description: String,
    val attachments_count: Int,
    val report_status: String,
    val submitted_at: String
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
    val publishedDate: String?,
    val status: String,
    val isCompanyWide: Boolean,
    val createdBy: String?,
    val approvedBy: String?,
    val approvedAt: String?,
    val createdAt: String?
)

data class CircularDetail(
    val id: Int,
    val title: String,
    val description: String,
    val circularType: String,
    val priority: String,
    val attachments: List<String>,
    val effectiveDate: String?,
    val expiryDate: String?,
    val publishedDate: String?,
    val status: String,
    val isCompanyWide: Boolean,
    val approvalComments: String?,
    val createdBy: CreatedBy?,
    val approvedBy: ApprovedBy?,
    val approvedAt: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class CreatedBy(
    val name: String?,
    val email: String?
)

data class ApprovedBy(
    val name: String?,
    val email: String?
)

data class CircularStats(
    val totalCirculars: Int,
    val highPriority: Int,
    val recent7Days: Int,
    val byType: Map<String, Int>
)

data class PaginationInfo(
    val currentPage: Int,
    val pageSize: Int,
    val totalCount: Int,
    val totalPages: Int
)

data class CircularListResponse(
    val status: String,
    val data: CircularListData
)

data class CircularListData(
    val circulars: List<CircularDto>,
    val pagination: PaginationDto
)

data class CircularDto(
    val id: Int,
    val title: String,
    val description: String,
    val circular_type: String,
    val priority: String,
    val attachments: List<String>,
    val effective_date: String?,
    val expiry_date: String?,
    val published_date: String?,
    val status: String,
    val is_company_wide: Boolean,
    val created_by: String?,
    val approved_by: String?,
    val approved_at: String?,
    val created_at: String?
) {
    fun toDomain() = Circular(
        id = id,
        title = title,
        description = description,
        circularType = circular_type,
        priority = priority,
        attachments = attachments,
        effectiveDate = effective_date,
        expiryDate = expiry_date,
        publishedDate = published_date,
        status = status,
        isCompanyWide = is_company_wide,
        createdBy = created_by,
        approvedBy = approved_by,
        approvedAt = approved_at,
        createdAt = created_at
    )
}

data class PaginationDto(
    val current_page: Int,
    val page_size: Int,
    val total_count: Int,
    val total_pages: Int
) {
    fun toDomain() = PaginationInfo(
        currentPage = current_page,
        pageSize = page_size,
        totalCount = total_count,
        totalPages = total_pages
    )
}

data class CircularDetailResponse(
    val status: String,
    val data: CircularDetailDto
)

data class CircularDetailDto(
    val id: Int,
    val title: String,
    val description: String,
    val circular_type: String,
    val priority: String,
    val attachments: List<String>,
    val effective_date: String?,
    val expiry_date: String?,
    val published_date: String?,
    val status: String,
    val is_company_wide: Boolean,
    val approval_comments: String?,
    val created_by: CreatedByDto?,
    val approved_by: ApprovedByDto?,
    val approved_at: String?,
    val created_at: String?,
    val updated_at: String?
) {
    fun toDomain() = CircularDetail(
        id = id,
        title = title,
        description = description,
        circularType = circular_type,
        priority = priority,
        attachments = attachments,
        effectiveDate = effective_date,
        expiryDate = expiry_date,
        publishedDate = published_date,
        status = status,
        isCompanyWide = is_company_wide,
        approvalComments = approval_comments,
        createdBy = created_by?.let { CreatedBy(it.name, it.email) },
        approvedBy = approved_by?.let { ApprovedBy(it.name, it.email) },
        approvedAt = approved_at,
        createdAt = created_at,
        updatedAt = updated_at
    )
}

data class CreatedByDto(
    val name: String?,
    val email: String?
)

data class ApprovedByDto(
    val name: String?,
    val email: String?
)

data class CircularStatsResponse(
    val status: String,
    val data: CircularStatsDto
)

data class CircularStatsDto(
    val total_circulars: Int,
    val high_priority: Int,
    val recent_7_days: Int,
    val by_type: Map<String, Int>
) {
    fun toDomain() = CircularStats(
        totalCirculars = total_circulars,
        highPriority = high_priority,
        recent7Days = recent_7_days,
        byType = by_type
    )
}

// ========================================
// LEAVE MODELS
// ========================================

data class LeaveType(
    val id: Int,
    val leaveTypeName: String,
    val code: String,
    val requiresApproval: Boolean,
    val applyOnHolidays: Boolean,
    val isPaid: Boolean,
    val applicableFor: String,
    val description: String?
)

data class LeaveApplication(
    val id: Int,
    val leaveType: LeaveTypeInfo,
    val startDate: String,
    val endDate: String,
    val numDays: Int,
    val leaveReason: String,
    val supportingDocumentUrl: String?,
    val alternateContact: String?,
    val taskDependedOnYou: Boolean,
    val dependencyHandledBy: String?,
    val status: String,
    val appliedAt: String,
    val approver: ApproverInfo?,
    val rejectionReason: String?,
    val cancelledAt: String?,
    val cancellationReason: String?,
    val withdrawnAt: String?,
    val withdrawalReason: String?,
    val createdAt: String,
    val updatedAt: String
)

data class LeaveTypeInfo(
    val id: Int,
    val name: String,
    val code: String,
    val isPaid: Boolean
)

data class ApproverInfo(
    val name: String?,
    val status: String?,
    val remarks: String?,
    val approvedAt: String?
)

data class LeaveSummary(
    val year: Int,
    val totalDaysTaken: Int,
    val byStatus: Map<String, StatusCount>,
    val byType: List<TypeCount>,
    val upcomingLeaves: List<UpcomingLeave>
)

data class StatusCount(
    val count: Int,
    val totalDays: Int
)

data class TypeCount(
    val leaveType: String,
    val isPaid: Boolean,
    val count: Int,
    val totalDays: Int
)

data class UpcomingLeave(
    val id: Int,
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val numDays: Int,
    val status: String
)

data class LeaveTypesResponse(
    val status: String,
    val data: List<LeaveTypeDto>
)

data class LeaveTypeDto(
    val id: Int,
    val leave_type_name: String,
    val code: String,
    val requires_approval: Boolean,
    val apply_on_holidays: Boolean,
    val is_paid: Boolean,
    val applicable_for: String,
    val description: String?
) {
    fun toDomain() = LeaveType(
        id = id,
        leaveTypeName = leave_type_name,
        code = code,
        requiresApproval = requires_approval,
        applyOnHolidays = apply_on_holidays,
        isPaid = is_paid,
        applicableFor = applicable_for,
        description = description
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
    val num_days: Int,
    val status: String,
    val requires_approval: Boolean
)

data class LeaveApplicationsResponse(
    val status: String,
    val data: LeaveApplicationsData
)

data class LeaveApplicationsData(
    val applications: List<LeaveApplicationDto>,
    val pagination: PaginationDto
)

data class LeaveApplicationDto(
    val id: Int,
    val leave_type: LeaveTypeInfoDto,
    val start_date: String,
    val end_date: String,
    val num_days: Int,
    val leave_reason: String,
    val supporting_document_url: String?,
    val alternate_contact: String?,
    val task_depended_on_you: Boolean,
    val dependency_handled_by: String?,
    val status: String,
    val applied_at: String,
    val approver: ApproverInfoDto?,
    val rejection_reason: String?,
    val cancelled_at: String?,
    val cancellation_reason: String?,
    val withdrawn_at: String?,
    val withdrawal_reason: String?,
    val created_at: String,
    val updated_at: String
) {
    fun toDomain() = LeaveApplication(
        id = id,
        leaveType = leave_type.toDomain(),
        startDate = start_date,
        endDate = end_date,
        numDays = num_days,
        leaveReason = leave_reason,
        supportingDocumentUrl = supporting_document_url,
        alternateContact = alternate_contact,
        taskDependedOnYou = task_depended_on_you,
        dependencyHandledBy = dependency_handled_by,
        status = status,
        appliedAt = applied_at,
        approver = approver?.toDomain(),
        rejectionReason = rejection_reason,
        cancelledAt = cancelled_at,
        cancellationReason = cancellation_reason,
        withdrawnAt = withdrawn_at,
        withdrawalReason = withdrawal_reason,
        createdAt = created_at,
        updatedAt = updated_at
    )
}

data class LeaveTypeInfoDto(
    val id: Int,
    val name: String,
    val code: String,
    val is_paid: Boolean
) {
    fun toDomain() = LeaveTypeInfo(
        id = id,
        name = name,
        code = code,
        isPaid = is_paid
    )
}

data class ApproverInfoDto(
    val name: String?,
    val status: String?,
    val remarks: String?,
    val approved_at: String?
) {
    fun toDomain() = ApproverInfo(
        name = name,
        status = status,
        remarks = remarks,
        approvedAt = approved_at
    )
}

data class LeaveSummaryResponse(
    val status: String,
    val data: LeaveSummaryDto
)

data class LeaveSummaryDto(
    val year: Int,
    val total_days_taken: Int,
    val by_status: Map<String, StatusCountDto>,
    val by_type: List<TypeCountDto>,
    val upcoming_leaves: List<UpcomingLeaveDto>
) {
    fun toDomain() = LeaveSummary(
        year = year,
        totalDaysTaken = total_days_taken,
        byStatus = by_status.mapValues { it.value.toDomain() },
        byType = by_type.map { it.toDomain() },
        upcomingLeaves = upcoming_leaves.map { it.toDomain() }
    )
}

data class StatusCountDto(
    val count: Int,
    val total_days: Int
) {
    fun toDomain() = StatusCount(
        count = count,
        totalDays = total_days
    )
}

data class TypeCountDto(
    val leave_type: String,
    val is_paid: Boolean,
    val count: Int,
    val total_days: Int
) {
    fun toDomain() = TypeCount(
        leaveType = leave_type,
        isPaid = is_paid,
        count = count,
        totalDays = total_days
    )
}

data class UpcomingLeaveDto(
    val id: Int,
    val leave_type: String,
    val start_date: String,
    val end_date: String,
    val num_days: Int,
    val status: String
) {
    fun toDomain() = UpcomingLeave(
        id = id,
        leaveType = leave_type,
        startDate = start_date,
        endDate = end_date,
        numDays = num_days,
        status = status
    )
}

data class WithdrawLeaveResponse(
    val status: String,
    val message: String
)

// ========================================
// LOGOUT & MISC MODELS
// ========================================

data class LogoutResponse(
    val status: String,
    val message: String,
    val logout_time: String? = null,
    val device_id: String? = null,
    val push_notifications: LogoutPushNotifications? = null
)

data class LogoutPushNotifications(
    val cleared: Boolean,
    val message: String? = null
)

data class PendingMessage(
    val id: Int,
    val type: String,
    val title: String,
    val body: String,
    val has_attachment: Boolean,
    val attachment_url: String?,
    val requires_acknowledgment: Boolean
)

// ========================================
// ENUMS
// ========================================

enum class RequestType {
    @SerializedName("NEW_ATTENDANCE") NEW_ATTENDANCE,
    @SerializedName("CORRECTION") CORRECTION,
    @SerializedName("LEAVE_LINKAGE") LEAVE_LINKAGE,
    @SerializedName("STATUS_CHANGE") STATUS_CHANGE
}

enum class CorrectionStatus {
    @SerializedName("PENDING") PENDING,
    @SerializedName("MORE_INFO_NEEDED") MORE_INFO_NEEDED,
    @SerializedName("APPROVED") APPROVED,
    @SerializedName("REJECTED") REJECTED,
    @SerializedName("WITHDRAWN") WITHDRAWN
}