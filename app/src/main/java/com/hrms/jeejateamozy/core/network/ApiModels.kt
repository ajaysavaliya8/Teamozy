package com.hrms.jeejateamozy.core.network

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody


// ========================================
// AUTH MODELS
// ========================================

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
    val status: String,
    val message: String? = null,
    val data: CheckStatusData? = null
)

data class CheckStatusData(
    val current_state: String,
    val last_check_in_time: String? = null,
    val message: String,
    val attendance_status: String? = null,
    val is_complete: Boolean? = null
)

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

data class CheckInSignatureResponse(
    val status: String,
    val message: String,
    val attendance_record_id: Int? = null,
    val check_in_time: String? = null
)

data class CheckOutResponse(
    val status: String,
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

data class CheckOutSignatureResponse(
    val status: String,
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

data class CorrectionBadgeDto(
    val type: String,
    val text: String,
    val color: String
) {
    fun toDomain() = CorrectionBadge(type = type, text = text, color = color)
}

data class CorrectionBadge(
    val type: String,
    val text: String,
    val color: String
)

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

data class CorrectionRequestSummary(
    val total: Int,
    val pending: Int,
    val approved: Int,
    val rejected: Int,
    val infoNeeded: Int
)

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
) {
    fun toDomain() = DayTimesheet(
        hasAttendance = has_attendance,
        date = date,
        dayName = day_name,
        formattedDate = formatted_date,
        message = message,
        status = status?.toDomain(),
        shift = shift?.toDomain(),
        hours = hours?.toDomain(),
        punches = punches?.map { it.toDomain() },
        isComplete = is_complete,
        attendanceRecordId = attendance_record_id,
        correctionRequest = correction_request?.toDomain(),
        canSubmitCorrection = can_submit_correction,
        hasPendingRequest = has_pending_request,
        availableActions = available_actions,
        canRequestNewAttendance = can_request_new_attendance,
        actionAvailable = action_available
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
    fun toDomain() = PunchLocation(latitude = latitude, longitude = longitude)
}

// Correction Request Container DTO
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

// Correction Request Options
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
    val id: Int,
    val label: String,
    val description: String?
) {
    fun toDomain() = LeaveTypeOption(id = id, label = label, description = description)
}

// Submit/Withdraw Correction Request Responses
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

data class CorrectionRequestContainer(
    val hasAny: Boolean,
    val active: CorrectionRequest? = null,
    val settled: SettledCorrectionRequest? = null
)

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

data class RequestedChanges(
    val status: String? = null,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val leaveTypeName: String? = null
)

data class AttachmentInfo(
    val hasAttachment: Boolean,
    val fileName: String? = null,
    val downloadUrl: String? = null
)

data class ReviewInfo(
    val reviewerName: String,
    val reviewedAt: String,
    val comments: String?
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
    val status: String,
    val message: String
)

data class FaceVerifyResponse(
    val status: String? = null,
    val message: String? = null,
    val face_token: String? = null
)

data class FaceRecognitionDataResponse(
    val status: String? = null,
    val message: String,
    val face_vector: String? = null,
    val minimum_face_recognition_quality_score: Float? = null,
    val require_face_checkin: Boolean? = null,
    val require_face_break: Boolean? = null
)

data class PendingFaceRegistrationResponse(
    val status: String,
    val pending: Boolean,
    val message: String
)

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

data class WorkReportListResponse(
    val status: String,
    val message: String,
    val month: Int,
    val year: Int,
    val total_reports: Int,
    val reports: List<WorkReportDto>
)

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
    val attachments: String?,
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
        attachments = parseAttachments(attachments),
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

    private fun parseAttachments(attachmentsJson: String?): List<String> {
        if (attachmentsJson.isNullOrBlank() || attachmentsJson == "[]") {
            return emptyList()
        }
        return try {
            if (attachmentsJson.startsWith("[")) {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                gson.fromJson(attachmentsJson, type) ?: emptyList()
            } else {
                listOf(attachmentsJson)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
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
    val attachments: String?,
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
        attachments = parseAttachments(attachments),
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

    private fun parseAttachments(attachmentsJson: String?): List<String> {
        if (attachmentsJson.isNullOrBlank() || attachmentsJson == "[]") {
            return emptyList()
        }
        return try {
            if (attachmentsJson.startsWith("[")) {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                gson.fromJson(attachmentsJson, type) ?: emptyList()
            } else {
                listOf(attachmentsJson)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
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
// LEAVE MODELS - UPDATED WITH HALF-DAY SUPPORT
// ========================================

// Domain model for LeaveType
data class LeaveType(
    val id: Int,
    val name: String?,
    val leaveTypeName: String,
    val code: String,
    val description: String?,
    val requiresApproval: Boolean,
    val applyOnHolidays: Boolean,
    val isPaid: Boolean,
    val applicableFor: String?,
    val requiresDocument: Boolean,
    val maxConsecutiveDays: Int?,
    val minNoticeDays: Int?,
    val isCarryForward: Boolean,
    val maxCarryForwardDays: Int?
)

data class LeaveTypesResponse(
    val status: String,
    val data: List<LeaveTypeDto>
)

data class LeaveTypeDto(
    val id: Int,
    val name: String? = null,
    val leave_type_name: String,
    val code: String,
    val description: String? = null,
    val requires_approval: Boolean,
    val apply_on_holidays: Boolean,
    val is_paid: Boolean,
    val applicable_for: String? = null,
    val requires_document: Boolean = false,
    val max_consecutive_days: Int? = null,
    val min_notice_days: Int? = null,
    val is_carry_forward: Boolean = false,
    val max_carry_forward_days: Int? = null
) {
    fun toDomain() = LeaveType(
        id = id,
        name = name,
        leaveTypeName = leave_type_name,
        code = code,
        description = description,
        requiresApproval = requires_approval,
        applyOnHolidays = apply_on_holidays,
        isPaid = is_paid,
        applicableFor = applicable_for,
        requiresDocument = requires_document,
        maxConsecutiveDays = max_consecutive_days,
        minNoticeDays = min_notice_days,
        isCarryForward = is_carry_forward,
        maxCarryForwardDays = max_carry_forward_days
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
    val total_days: Double,
    val effective_days: Double,
    val is_half_day_start: Boolean = false,
    val is_half_day_end: Boolean = false,
    val half_day_type: String? = null,
    val leave_reason: String,
    val supporting_document_url: String? = null,
    val alternate_contact: String? = null,
    val emergency_contact: String? = null,
    val task_depended_on_you: Boolean,
    val dependency_handled_by: String? = null,
    val handover_notes: String? = null,
    val status: String,
    val current_status: String,
    val workflow_status: String? = null,
    val is_paid: Boolean? = null,
    val paid_percentage: Double? = null,
    val applied_at: String? = null,
    val approver: ApproverInfoDto? = null,
    val rejection_reason: String? = null,
    val cancelled_by: String? = null,
    val cancelled_at: String? = null,
    val cancellation_reason: String? = null,
    val withdrawn_by: String? = null,
    val withdrawn_at: String? = null,
    val withdrawal_reason: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
) {
    fun toDomain() = LeaveApplication(
        id = id,
        leaveType = leave_type.toDomain(),
        startDate = start_date,
        endDate = end_date,
        totalDays = total_days,
        effectiveDays = effective_days,
        isHalfDayStart = is_half_day_start,
        isHalfDayEnd = is_half_day_end,
        halfDayType = half_day_type,
        leaveReason = leave_reason,
        supportingDocumentUrl = supporting_document_url,
        alternateContact = alternate_contact,
        emergencyContact = emergency_contact,
        taskDependedOnYou = task_depended_on_you,
        dependencyHandledBy = dependency_handled_by,
        handoverNotes = handover_notes,
        status = status,
        currentStatus = current_status,
        workflowStatus = workflow_status,
        isPaid = is_paid,
        paidPercentage = paid_percentage,
        appliedAt = applied_at ?: "",
        approver = approver?.toDomain(),
        rejectionReason = rejection_reason,
        cancelledBy = cancelled_by,
        cancelledAt = cancelled_at,
        cancellationReason = cancellation_reason,
        withdrawnBy = withdrawn_by,
        withdrawnAt = withdrawn_at,
        withdrawalReason = withdrawal_reason,
        createdAt = created_at ?: "",
        updatedAt = updated_at ?: ""
    )
}

// Domain model for LeaveApplication
data class LeaveApplication(
    val id: Int,
    val leaveType: LeaveTypeInfo,
    val startDate: String,
    val endDate: String,
    val totalDays: Double,
    val effectiveDays: Double,
    val isHalfDayStart: Boolean,
    val isHalfDayEnd: Boolean,
    val halfDayType: String?,
    val leaveReason: String,
    val supportingDocumentUrl: String?,
    val alternateContact: String?,
    val emergencyContact: String?,
    val taskDependedOnYou: Boolean,
    val dependencyHandledBy: String?,
    val handoverNotes: String?,
    val status: String,
    val currentStatus: String,
    val workflowStatus: String?,
    val isPaid: Boolean?,
    val paidPercentage: Double?,
    val appliedAt: String,
    val approver: ApproverInfo?,
    val rejectionReason: String?,
    val cancelledBy: String?,
    val cancelledAt: String?,
    val cancellationReason: String?,
    val withdrawnBy: String?,
    val withdrawnAt: String?,
    val withdrawalReason: String?,
    val createdAt: String,
    val updatedAt: String
) {
    // Backward compatibility - returns totalDays as Int
    val numDays: Int get() = totalDays.toInt()
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

data class LeaveTypeInfo(
    val id: Int,
    val name: String,
    val code: String,
    val isPaid: Boolean
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

// Leave Application Detail Response
data class LeaveApplicationDetailResponse(
    val status: String,
    val data: LeaveApplicationDetailDto
)

data class LeaveApplicationDetailDto(
    val id: Int,
    val leave_type: LeaveTypeDetailInfoDto,
    val start_date: String,
    val end_date: String,
    val total_days: Double,
    val effective_days: Double,
    val is_half_day_start: Boolean,
    val is_half_day_end: Boolean,
    val half_day_type: String?,
    val leave_reason: String,
    val supporting_document_url: String?,
    val alternate_contact: String?,
    val emergency_contact: String?,
    val task_depended_on_you: Boolean,
    val dependency_handled_by: String?,
    val handover_notes: String?,
    val status: String,
    val current_status: String,
    val workflow_status: String?,
    val is_paid: Boolean?,
    val paid_percentage: Double?,
    val applied_at: String?,
    val approver: ApproverInfoDto?,
    val rejection_reason: String?,
    val cancelled_by: String?,
    val cancelled_at: String?,
    val cancellation_reason: String?,
    val withdrawn_by: String?,
    val withdrawn_at: String?,
    val withdrawal_reason: String?,
    val approval_instance_id: Int?,
    val current_approval_step: Int?,
    val created_at: String?,
    val updated_at: String?
)

data class LeaveTypeDetailInfoDto(
    val id: Int,
    val name: String,
    val code: String,
    val is_paid: Boolean,
    val requires_approval: Boolean,
    val requires_document: Boolean
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
    val is_paid: Boolean,
    val count: Int,
    val total_days: Double
) {
    fun toDomain() = TypeCount(
        leaveType = leave_type,
        isPaid = is_paid,
        count = count,
        totalDays = total_days.toInt(),
        totalDaysDouble = total_days
    )
}

data class TypeCount(
    val leaveType: String,
    val isPaid: Boolean,
    val count: Int,
    val totalDays: Int,
    val totalDaysDouble: Double = 0.0
)

data class UpcomingLeaveDto(
    val id: Int,
    val leave_type: String,
    val start_date: String,
    val end_date: String,
    val total_days: Double,
    val effective_days: Double,
    val status: String,
    val current_status: String
) {
    fun toDomain() = UpcomingLeave(
        id = id,
        leaveType = leave_type,
        startDate = start_date,
        endDate = end_date,
        totalDays = total_days,
        effectiveDays = effective_days,
        status = status,
        currentStatus = current_status
    )
}

data class UpcomingLeave(
    val id: Int,
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val totalDays: Double,
    val effectiveDays: Double,
    val status: String,
    val currentStatus: String
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

// Save Draft
data class SaveDraftLeaveResponse(
    val status: String,
    val message: String,
    val data: SaveDraftData?
)

data class SaveDraftData(
    val draft_id: Int,
    val total_days: Double,
    val status: String,
    val created_at: String
)

// Submit Draft
data class SubmitDraftResponse(
    val status: String,
    val message: String
)

// Leave Calendar
data class LeaveCalendarResponse(
    val status: String,
    val data: LeaveCalendarData
)

data class LeaveCalendarData(
    val month: Int,
    val year: Int,
    val first_day: String,
    val last_day: String,
    val leaves: List<LeaveCalendarItemDto>
)

data class LeaveCalendarItemDto(
    val id: Int,
    val leave_type: String,
    val leave_type_code: String,
    val is_paid: Boolean,
    val start_date: String,
    val end_date: String,
    val display_start_date: String,
    val display_end_date: String,
    val total_days: Double,
    val effective_days: Double,
    val num_days_in_month: Int,
    val is_half_day_start: Boolean,
    val is_half_day_end: Boolean,
    val half_day_type: String?,
    val status: String,
    val current_status: String,
    val leave_reason: String?
) {
    fun toDomain() = LeaveCalendarItem(
        id = id,
        leaveType = leave_type,
        leaveTypeCode = leave_type_code,
        isPaid = is_paid,
        startDate = start_date,
        endDate = end_date,
        displayStartDate = display_start_date,
        displayEndDate = display_end_date,
        totalDays = total_days,
        effectiveDays = effective_days,
        numDaysInMonth = num_days_in_month,
        isHalfDayStart = is_half_day_start,
        isHalfDayEnd = is_half_day_end,
        halfDayType = half_day_type,
        status = status,
        currentStatus = current_status,
        leaveReason = leave_reason
    )
}

data class LeaveCalendarItem(
    val id: Int,
    val leaveType: String,
    val leaveTypeCode: String,
    val isPaid: Boolean,
    val startDate: String,
    val endDate: String,
    val displayStartDate: String,
    val displayEndDate: String,
    val totalDays: Double,
    val effectiveDays: Double,
    val numDaysInMonth: Int,
    val isHalfDayStart: Boolean,
    val isHalfDayEnd: Boolean,
    val halfDayType: String?,
    val status: String,
    val currentStatus: String,
    val leaveReason: String?
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
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    CANCELLED("CANCELLED"),
    WITHDRAWN("WITHDRAWN"),
    ON_LEAVE("ON_LEAVE"),
    COMPLETED("COMPLETED"),
    UPCOMING("UPCOMING")
}

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