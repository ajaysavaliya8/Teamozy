package com.example.teamozy.core.network

// -------- Auth --------
data class BasicResponse(
    val status: String,           // "success" | "error" | "reject"
    val message: String? = null,
    val token: String? = null,    // present on verify-login success
    val minimum_face_recognition_quality_score: Float? = null,
    val face_vector: String? = null,
    val require_face_checkin: Boolean? = null,
    val require_face_break: Boolean? = null
)

// -------- New Check Status Response --------
data class CheckStatusResponse(
    val status: String,           // "success" | "error"
    val message: String? = null,
    val data: CheckStatusData? = null
)

data class CheckStatusData(
    val current_state: String,    // "CHECK_IN_NEEDED" | "CHECK_OUT_NEEDED" | "COMPLETED"
    val face_recognition_enabled: Boolean,
    val face_vector: String? = null,
    val minimum_quality_score: Float,
    val message: String,
    val attendance_status: String? = null,
    val is_complete: Boolean? = null
)

// Verify token endpoint response
data class VerifyTokenResponse(
    val status: String,
    val message: String,
    val minimum_face_recognition_quality_score: Float? = null,
    val face_vector: String? = null,
    val require_face_checkin: Boolean? = null,
    val require_face_break: Boolean? = null
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