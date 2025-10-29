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
    val out_of_range_reason_required: Boolean? = null
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