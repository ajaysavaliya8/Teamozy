//ApiService.kt
package com.example.teamozy.network


import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("check-in")
    suspend fun checkIn(
        @Query("device_id") deviceId: String,
        @Query("longitude") longitude: Double = 0.0,
        @Query("latitude") latitude: Double = 0.0,
        @Query("face_verify") faceVerify: Boolean = true,
        @Query("token") token: String
    ): Response<ApiResponse<CheckInOutResponse>>

    @POST("check-out")
    suspend fun checkOut(
        @Query("device_id") deviceId: String,
        @Query("longitude") longitude: Double = 0.0,
        @Query("latitude") latitude: Double = 0.0,
        @Query("face_verify") faceVerify: Boolean = true,
        @Query("token") token: String
    ): Response<ApiResponse<CheckInOutResponse>>

    @POST("check-in-violation")
    @FormUrlEncoded
    suspend fun submitCheckInViolation(
        @Query("token") token: String,
        @Field("t_token") tToken: String,
        @Field("late_reason") lateReason: String? = null,
        @Field("geo_reason") geoReason: String? = null
    ): Response<ApiResponse<Any>>

    @POST("check-out-violation")
    @FormUrlEncoded
    suspend fun submitCheckOutViolation(
        @Query("token") token: String,
        @Field("t_token") tToken: String,
        @Field("early_reason") earlyReason: String? = null,
        @Field("geo_reason") geoReason: String? = null
    ): Response<ApiResponse<Any>>

    @GET("check-status")
    suspend fun checkStatus(
        @Query("device_id") deviceId: String,
        @Query("token") token: String
    ): Response<ApiResponse<CheckStatusResponse>>

    @POST("send-login")
    suspend fun sendOtp(
        @Query("mobile_number") mobileNumber: Long,
        @Query("device_id") deviceId: String
    ): Response<ApiResponse<Any>>

    @POST("verify-login")
    suspend fun verifyLogin(
        @Query("mobile_number") mobileNumber: Long,
        @Query("device_id") deviceId: String,
        @Query("password") password: String? = null,
        @Query("otp") otp: Int? = null
    ): Response<DirectLoginResponse>
}