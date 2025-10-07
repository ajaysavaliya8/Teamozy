package com.example.teamozy.core.network

import com.google.gson.annotations.SerializedName

// -------- Auth --------
data class BasicResponse(
    val status: String,           // "success" | "error" | "reject"
    val message: String? = null,
    val token: String? = null,    // present on verify-login success
    @SerializedName("face_threshold")  // Backend response key
    val faceThreshold: Float? = null   // Face verification threshold from server (e.g., 0.57)
)

// -------- Status (/check-status) --------
data class CheckStatusEnvelope(
    val status: String,
    val message: String? = null,
    val data: CheckStatusData? = null
)

data class CheckStatusData(
    @SerializedName("current-state")
    val currentState: String,      // "CHECK_IN_NEEDED" | "CHECK_OUT_NEEDED"
    @SerializedName("face_recognition_enabled")
    val faceRecognitionEnabled: Boolean = false  // Whether face verification is required
)

// -------- Actions (/check-in, /check-out) --------
data class ActionResponse(
    val status: String,               // "success"
    val message: String? = null,
    @SerializedName("is_late") val isLate: Boolean? = null,
    @SerializedName("is_early") val isEarly: Boolean? = null,
    @SerializedName("location_verified") val locationVerified: Boolean? = null,
    @SerializedName("t_token") val tToken: String? = null
)

// Face verify upload response
data class FaceVerifyResponse(
    val status: String? = null,
    val message: String? = null,
    val face_token: String? = null
)