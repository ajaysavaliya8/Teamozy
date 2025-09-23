//ApiModel.kt
package com.example.teamozy.network

import com.google.gson.annotations.SerializedName

// Response Models
data class ApiResponse<T>(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T? = null
)

// Direct login response that matches your actual API
data class DirectLoginResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String
)

data class CheckStatusResponse(
    @SerializedName("current-state") val currentState: String
)

data class CheckInOutResponse(
    @SerializedName("is_late") val isLate: Boolean = false,
    @SerializedName("location_verified") val locationVerified: Boolean = true,
    @SerializedName("t_token") val tToken: String = ""
)

data class LoginResponse(
    @SerializedName("token") val token: String
)

// Request Models
data class ViolationRequest(
    @SerializedName("t_token") val tToken: String,
    @SerializedName("late_reason") val lateReason: String? = null,
    @SerializedName("geo_reason") val geoReason: String? = null,
    @SerializedName("early_reason") val earlyReason: String? = null
)

data class FaceVectorResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("face_vector") val faceVector: List<Float>? = null
)

data class SaveFaceVectorResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)