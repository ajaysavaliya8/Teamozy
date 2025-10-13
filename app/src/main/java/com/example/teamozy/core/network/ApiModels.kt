package com.example.teamozy.core.network

import com.google.gson.annotations.SerializedName

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

// -------- Status (/check-status) --------
data class CheckStatusEnvelope(
    val status: String,
    val message: String? = null,
    val data: CheckStatusData? = null
)

data class CheckStatusData(
    @SerializedName("current-state")
    val currentState: String      // "CHECK_IN_NEEDED" | "CHECK_OUT_NEEDED"
)

// -------- Actions (/check-in, /check-out) --------
data class ActionResponse(
    val status: String,               // "success"
    val message: String? = null,
    // For check-in
    @SerializedName("is_late") val isLate: Boolean? = null,
    // For check-out
    @SerializedName("is_early") val isEarly: Boolean? = null,
    @SerializedName("location_verified") val locationVerified: Boolean? = null,
    @SerializedName("t_token") val tToken: String? = null
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