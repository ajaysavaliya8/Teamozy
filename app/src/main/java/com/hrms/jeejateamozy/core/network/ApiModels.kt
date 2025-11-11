package com.hrms.jeejateamozy.core.network

// -------- Auth --------
data class BasicResponse(
    val status: String,           // "success" | "error" | "reject"
    val message: String? = null,
    val token: String? = null,    // present on verify-login success

    // ===== NEW FIELDS FROM UPDATED API =====
    // Employee Information
    val mobile_number: Long? = null,
    val full_name: String? = null,
    val profile_url: String? = null,
    val branch_name: String? = null,
    val department_name: String? = null,
    val shift_name: String? = null,

    // Social Media Links
    val facebook: String? = null,
    val linkedin: String? = null,
    val x: String? = null,  // Twitter/X
    val instagram: String? = null,
    val snapchat: String? = null,

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
    val out_of_range_reason_required: Boolean? = null
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

/**
 * Circular detail data model (includes additional fields)
 */
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

/**
 * Circular statistics
 */
data class CircularStats(
    val totalCirculars: Int,
    val highPriority: Int,
    val recent7Days: Int,
    val byType: Map<String, Int>
)

/**
 * Pagination info
 */
data class PaginationInfo(
    val currentPage: Int,
    val pageSize: Int,
    val totalCount: Int,
    val totalPages: Int
)

// -------- API Response Models --------

/**
 * API Response for GET /circulars
 */
data class CircularListResponse(
    val status: String,
    val data: CircularListData
)

data class CircularListData(
    val circulars: List<CircularDto>,
    val pagination: PaginationDto
)

/**
 * Circular DTO from API
 */
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
    /**
     * Convert DTO to domain model
     */
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

/**
 * API Response for GET /circulars/{circular_id}
 */
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

/**
 * API Response for GET /circulars/stats/summary
 */
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
/**
 * Work Report DTO from API
 */
data class WorkReportDto(
    val id: Int,
    val report_date: String,
    val work_description: String,
    val attachments: String,  // ⭐ CHANGED: String instead of List<String>
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
    /**
     * Convert DTO to domain model
     * Parses attachments JSON string to List
     */
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

    /**
     * Parse attachments from JSON string to List
     */
    private fun parseAttachments(attachmentsJson: String): List<String> {
        return try {
            if (attachmentsJson.isBlank() || attachmentsJson == "[]") {
                emptyList()
            } else {
                // Parse JSON array string
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                gson.fromJson(attachmentsJson, type) ?: emptyList()
            }
        } catch (e: Exception) {
            // If parsing fails, return empty list
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