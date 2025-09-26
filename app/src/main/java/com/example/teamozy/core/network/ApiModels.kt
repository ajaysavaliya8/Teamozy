package com.example.teamozy.core.network

import com.google.gson.annotations.SerializedName

// -------- Auth --------
data class BasicResponse(
    val status: String,           // "success" | "error" | "reject"
    val message: String? = null,
    val token: String? = null     // present on verify-login success
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
// Your backend returns top-level fields (not nested "data")
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

// Face verify upload response (adjust fields to match your backend when ready)
data class FaceVerifyResponse(
    val status: String? = null,
    val message: String? = null,
    val face_token: String? = null // sometimes called "face_token" or similar
)
