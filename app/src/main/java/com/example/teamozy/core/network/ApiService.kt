package com.example.teamozy.core.network

import retrofit2.Response
import retrofit2.http.*

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
        @Query("otp") otp: Int? = null
    ): Response<BasicResponse> // includes token on success

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
}
