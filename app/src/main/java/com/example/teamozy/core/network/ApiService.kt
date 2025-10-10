package com.example.teamozy.core.network

import retrofit2.Response
import retrofit2.http.*

data class DeviceChangeResponse(
    val detail: String
)

// Add verify token endpoint
data class VerifyTokenResponse(
    val status: String,
    val message: String,
    val face_threshold: Float? = null,
    val face_vector: String? = null
)

data class FaceRegistrationRequest(
    val face_recognition_data: List<Float>,
    val priority: String = "normal",
    val reason_for_change: String? = null
)

interface ApiService {

    // ---------- AUTH ----------
    @POST("send-login")
    suspend fun sendLogin(
        @Query("mobile_number") mobileNumber: Long,
        @Query("device_id") deviceId: String
    ): Response<BasicResponse>

    @POST("verify-login")
    suspend fun verifyLogin(
        @Query("mobile_number") mobileNumber: Long,
        @Query("device_id") deviceId: String,
        @Query("password") password: String? = null,
        @Query("otp") otp: String? = null
    ): Response<BasicResponse>

    @GET("verify-token")
    suspend fun verifyToken(
        @Query("token") token: String
    ): Response<VerifyTokenResponse>

    @FormUrlEncoded
    @POST("send-change-device-otp")
    suspend fun sendChangeDeviceOtp(
        @Field("mobile_number") mobileNumber: Long
    ): Response<BasicResponse>

    @FormUrlEncoded
    @POST("request-change-device")
    suspend fun requestChangeDevice(
        @Query("otp") otp: String,
        @Query("mobile_number") mobileNumber: Long,
        @Query("new_device_id") newDeviceId: String,
        @Field("new_device_os") newDeviceOs: String,
        @Field("new_device_model") newDeviceModel: String,
        @Field("new_device_company_name") newDeviceCompanyName: String,
        @Field("reason") reason: String? = null
    ): Response<DeviceChangeResponse>

    // ---------- ATTENDANCE (token passed as query) ----------
    @GET("check-status")
    suspend fun checkStatus(
        @Query("device_id") deviceId: String,
        @Query("token") token: String
    ): Response<CheckStatusEnvelope>

    @POST("check-in")
    suspend fun checkIn(
        @Query("device_id") deviceId: String,
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("face_verify") faceVerify: Boolean = false,
        @Query("token") token: String
    ): Response<ActionResponse>

    @POST("check-out")
    suspend fun checkOut(
        @Query("device_id") deviceId: String,
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("face_verify") faceVerify: Boolean = false,
        @Query("token") token: String
    ): Response<ActionResponse>

    @FormUrlEncoded
    @POST("check-in-violation")
    suspend fun submitCheckInViolation(
        @Field("t_token") tToken: String,
        @Field("late_reason") lateReason: String? = null,
        @Field("geo_reason") geoReason: String? = null,
        @Query("token") token: String
    ): Response<BasicResponse>

    @FormUrlEncoded
    @POST("check-out-violation")
    suspend fun submitCheckOutViolation(
        @Field("t_token") tToken: String,
        @Field("early_reason") earlyReason: String? = null,
        @Field("geo_reason") geoReason: String? = null,
        @Query("token") token: String
    ): Response<BasicResponse>

    // ---------- FACE RECOGNITION ----------
    @POST("employees/face-recognition")
    suspend fun registerFaceRecognition(
        @Body request: FaceRegistrationRequest
    ): Response<BasicResponse>
}