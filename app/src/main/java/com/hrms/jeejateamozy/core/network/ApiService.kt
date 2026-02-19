package com.hrms.jeejateamozy.core.network

import com.hrms.jeejateamozy.feature.profile.data.BankingInfoResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import com.hrms.jeejateamozy.feature.profile.data.ContactInfoResponse
import com.hrms.jeejateamozy.feature.profile.data.PersonalInfoResponse
import com.hrms.jeejateamozy.feature.profile.data.EmploymentDetailResponse
import com.hrms.jeejateamozy.feature.profile.data.EmploymentIdentityResponse
import com.hrms.jeejateamozy.feature.profile.data.ShiftDetailsResponse
import com.hrms.jeejateamozy.feature.location.data.remote.LocationSyncRequest
import okhttp3.ResponseBody


data class DeviceChangeResponse(
    val detail: String
)

interface ApiService {

    // ========================================
    // AUTH ENDPOINTS
    // ========================================

    @POST("send-login")
    suspend fun sendLogin(
        @Query("mobile_number") mobileNumber: Long,
        @Query("device_id") deviceId: String
    ): Response<AuthResponse>

    @FormUrlEncoded
    @POST("verify-login")
    suspend fun verifyLogin(
        @Query("mobile_number") mobileNumber: Long,
        @Query("device_id") deviceId: String,
        @Query("password") password: String? = null,
        @Query("otp") otp: String? = null,
        @Query("app_version") appVersion: String? = null,
        @Field("fcm_token") fcmToken: String? = null,
        @Field("device_manufacturer") deviceManufacturer: String? = null,
        @Field("device_model") deviceModel: String? = null,
        @Field("device_os_version") deviceOsVersion: String? = null
    ): Response<LoginResponse>

    @GET("verify-token")
    suspend fun verifyToken(
        @Query("app_version") appVersion: String,
        @Query("fcm_token") fcmToken: String? = null
    ): Response<VerifyTokenResponse>

    @POST("forgot-password")
    suspend fun forgotPassword(@Query("number") number: Long): Response<AuthResponse>

    @POST("verify-reset-otp")
    suspend fun verifyResetOtp(
        @Query("number") number: Long,
        @Query("otp") otp: String
    ): Response<ResetOtpResponse>

    @POST("reset-password")
    suspend fun resetPassword(
        @Query("reset_token") resetToken: String,
        @Query("new_password") newPassword: String,
        @Query("confirm_password") confirmPassword: String
    ): Response<AuthResponse>

    @FormUrlEncoded
    @POST("send-change-device-otp")
    suspend fun sendChangeDeviceOtp(
        @Field("mobile_number") mobileNumber: Long
    ): Response<AuthResponse>

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

    // ========================================
    // ATTENDANCE ENDPOINTS
    // ========================================

    @GET("check-status")
    suspend fun checkStatus(
        @Query("device_id") deviceId: String,
        @Query("token") token: String
    ): Response<CheckStatusResponse>

    @POST("check-in")
    suspend fun checkIn(
        @Query("device_id") deviceId: String,
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("token") token: String
    ): Response<CheckInResponse>

    @FormUrlEncoded
    @POST("check-in-signature")
    suspend fun checkInSignature(
        @Field("t_token") tToken: String,
        @Field("face_recognition_quality_score") faceRecognitionQualityScore: Float? = null,
        @Field("face_verify") faceVerify: Boolean = false,
        @Field("late_reason") lateReason: String? = null,
        @Field("out_of_range_reason") outOfRangeReason: String? = null,
        @Field("acknowledgment_note") acknowledgmentNote: String? = null,
        // Location tracking data
        @Field("first_location_recorded_at") firstLocationRecordedAt: String? = null,
        @Field("first_location_latitude") firstLocationLatitude: Double? = null,
        @Field("first_location_longitude") firstLocationLongitude: Double? = null,
        @Field("first_location_accuracy") firstLocationAccuracy: Float? = null,
        @Field("first_location_altitude") firstLocationAltitude: Double? = null,
        @Field("first_location_vertical_accuracy") firstLocationVerticalAccuracy: Float? = null,
        @Field("first_location_speed") firstLocationSpeed: Float? = null,
        @Field("first_location_heading") firstLocationHeading: Float? = null,
        @Field("first_location_app_version") firstLocationAppVersion: String? = null,
        @Field("first_location_network_type") firstLocationNetworkType: String? = null,
        @Field("first_location_wifi_name") firstLocationWifiName: String? = null,
        @Field("first_location_wifi_mac_address") firstLocationWifiMacAddress: String? = null,
        @Field("first_location_battery_level") firstLocationBatteryLevel: Int? = null,
        @Query("token") token: String
    ): Response<CheckInSignatureResponse>

    @POST("check-out")
    suspend fun checkOut(
        @Query("device_id") deviceId: String,
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("token") token: String
    ): Response<CheckOutResponse>

    @Multipart
    @POST("check-out-signature")
    suspend fun checkOutSignature(
        @Part("t_token") tToken: RequestBody,
        @Part("face_recognition_quality_score") faceRecognitionQualityScore: RequestBody? = null,
        @Part("face_verify") faceVerify: RequestBody? = null,
        @Part("early_reason") earlyReason: RequestBody? = null,
        @Part("out_of_range_reason") outOfRangeReason: RequestBody? = null,
        @Part("work_description") workReport: RequestBody? = null,
        @Part work_report_file: MultipartBody.Part? = null,
        // Location tracking data
        @Part("last_location_recorded_at") lastLocationRecordedAt: RequestBody? = null,
        @Part("last_location_latitude") lastLocationLatitude: RequestBody? = null,
        @Part("last_location_longitude") lastLocationLongitude: RequestBody? = null,
        @Part("last_location_accuracy") lastLocationAccuracy: RequestBody? = null,
        @Part("last_location_altitude") lastLocationAltitude: RequestBody? = null,
        @Part("last_location_vertical_accuracy") lastLocationVerticalAccuracy: RequestBody? = null,
        @Part("last_location_speed") lastLocationSpeed: RequestBody? = null,
        @Part("last_location_heading") lastLocationHeading: RequestBody? = null,
        @Part("last_location_app_version") lastLocationAppVersion: RequestBody? = null,
        @Part("last_location_network_type") lastLocationNetworkType: RequestBody? = null,
        @Part("last_location_wifi_name") lastLocationWifiName: RequestBody? = null,
        @Part("last_location_wifi_mac_address") lastLocationWifiMacAddress: RequestBody? = null,
        @Part("last_location_battery_level") lastLocationBatteryLevel: RequestBody? = null,
        @Query("token") token: String
    ): Response<CheckOutSignatureResponse>

    // ========================================
    // FACE RECOGNITION ENDPOINTS
    // ========================================

    @GET("employees/face-recognition")
    suspend fun getFaceRecognitionData(): Response<FaceRecognitionDataResponse>

    @Multipart
    @POST("employees/face-recognition")
    suspend fun registerFaceRecognition(
        @Part face_image: MultipartBody.Part,
        @Part("face_vector") faceVector: RequestBody,
        @Part("priority") priority: RequestBody,
        @Part("reason_for_change") reasonForChange: RequestBody? = null
    ): Response<FaceRegistrationResponse>

    @GET("employees/face-recognition/pending-request")
    suspend fun getPendingFaceRegistration(): Response<PendingFaceRegistrationResponse>

    // ========================================
    // PROFILE ENDPOINTS
    // ========================================

    @FormUrlEncoded
    @PUT("profile/update-social-media")
    suspend fun updateSocialMedia(
        @Field("facebook_url") facebookUrl: String? = null,
        @Field("linkedin_url") linkedinUrl: String? = null,
        @Field("x_url") xUrl: String? = null,
        @Field("instagram_url") instagramUrl: String? = null,
        @Field("snapchat_url") snapchatUrl: String? = null
    ): Response<SocialMediaUpdateResponse>

    @Multipart
    @POST("profile/update-picture")
    suspend fun updateProfilePicture(
        @Part profile_image: MultipartBody.Part
    ): Response<ProfilePictureUpdateResponse>

    @DELETE("profile/remove-picture")
    suspend fun removeProfilePicture(): Response<BasicResponse>

    @GET("contact-info")
    suspend fun getContactInfo(): Response<ContactInfoResponse>

    @FormUrlEncoded
    @PUT("contact-info")
    suspend fun updateContactInfo(
        @Field("alternate_mobile") alternateMobile: String? = null,
        @Field("whatsapp_number") whatsappNumber: String? = null,
        @Field("emergency_contact_name") emergencyContactName: String? = null,
        @Field("emergency_contact_relationship") emergencyContactRelationship: String? = null,
        @Field("emergency_contact_number") emergencyContactNumber: String? = null,
        @Field("current_address_line") currentAddressLine: String? = null,
        @Field("current_city") currentCity: String? = null,
        @Field("current_state") currentState: String? = null,
        @Field("current_country") currentCountry: String? = null,
        @Field("current_postal_code") currentPostalCode: String? = null,
        @Field("same_as_current") sameAsCurrent: Boolean? = null,
        @Field("permanent_address_line") permanentAddressLine: String? = null,
        @Field("permanent_city") permanentCity: String? = null,
        @Field("permanent_state") permanentState: String? = null,
        @Field("permanent_country") permanentCountry: String? = null,
        @Field("permanent_postal_code") permanentPostalCode: String? = null
    ): Response<BasicResponse>

    @GET("personal-info")
    suspend fun getPersonalInfo(): Response<PersonalInfoResponse>

    @FormUrlEncoded
    @PUT("personal-info")
    suspend fun updatePersonalInfo(
        @Field("blood_group") bloodGroup: String? = null,
        @Field("marital_status") maritalStatus: String? = null,
        @Field("spouse_name") spouseName: String? = null,
        @Field("no_of_children") noOfChildren: Int? = null,
        @Field("languages") languages: List<String>? = null,
        @Field("religion") religion: String? = null
    ): Response<BasicResponse>

    @GET("employment-details")
    suspend fun getEmploymentDetails(): Response<EmploymentDetailResponse>

    @GET("banking-info")
    suspend fun getBankingInfo(): Response<BankingInfoResponse>

    @GET("employment-identity")
    suspend fun getEmploymentIdentity(): Response<EmploymentIdentityResponse>

    @GET("employee-shift")
    suspend fun getEmploymentShift(): Response<ShiftDetailsResponse>

    // ========================================
    // WORK REPORT ENDPOINTS
    // ========================================

    @GET("work-report")
    suspend fun getWorkReports(
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Response<WorkReportListResponse>

    @Multipart
    @POST("work-report")
    suspend fun createWorkReport(
        @Part("work_description") workDescription: RequestBody,
        @Part attachments: List<MultipartBody.Part>? = null
    ): Response<CreateWorkReportResponse>

    // ========================================
    // CIRCULAR ENDPOINTS
    // ========================================

    @GET("circulars")
    suspend fun getCirculars(
        @Query("status") status: String? = null,
        @Query("circular_type") circularType: String? = null,
        @Query("priority") priority: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<CircularListResponse>

    @GET("circulars/{circular_id}")
    suspend fun getCircularDetail(
        @Path("circular_id") circularId: Int
    ): Response<CircularDetailResponse>

    @GET("circulars/stats/summary")
    suspend fun getCircularStats(): Response<CircularStatsResponse>

    // ========================================
    // LEAVE ENDPOINTS - UPDATED
    // ========================================

    /**
     * Get all available leave types for the employee
     * Filters based on gender and marital status
     */
    @GET("leave-types")
    suspend fun getLeaveTypes(): Response<LeaveTypesResponse>

    /**
     * Apply for leave
     */
    @Multipart
    @POST("apply-leave")
    suspend fun applyLeave(
        @Part("leave_type_id") leaveTypeId: RequestBody,
        @Part("start_date") startDate: RequestBody,
        @Part("end_date") endDate: RequestBody,
        @Part("leave_reason") leaveReason: RequestBody,
        @Part("alternate_contact") alternateContact: RequestBody?,
        @Part("emergency_contact") emergencyContact: RequestBody?,
        @Part("task_depended_on_you") taskDependedOnYou: RequestBody,
        @Part("dependency_handled_by") dependencyHandledBy: RequestBody?,
        @Part("handover_notes") handoverNotes: RequestBody?,
        @Part("priority") priority: RequestBody,
        @Part supportingDocument: MultipartBody.Part?
    ): Response<ApplyLeaveResponse>

    /**
     * Get leave applications history
     */
    @GET("leave-applications")
    suspend fun getLeaveApplications(
        @Query("status") status: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<LeaveApplicationsResponse>

    /**
     * Get leave application detail
     */
    @GET("leave-applications/{application_id}")
    suspend fun getLeaveApplicationDetail(
        @Path("application_id") applicationId: Int
    ): Response<LeaveApplicationDetailResponse>

    /**
     * Withdraw a pending or approved leave application
     */
    @FormUrlEncoded
    @POST("withdraw-leave/{application_id}")
    suspend fun withdrawLeave(
        @Path("application_id") applicationId: Int,
        @Field("withdrawal_reason") withdrawalReason: String
    ): Response<WithdrawLeaveResponse>

    /**
     * Cancel an approved leave application before it starts
     */
    @FormUrlEncoded
    @POST("cancel-leave/{application_id}")
    suspend fun cancelLeave(
        @Path("application_id") applicationId: Int,
        @Field("cancellation_reason") cancellationReason: String
    ): Response<CancelLeaveResponse>

    /**
     * Get leave summary and statistics
     */
    @GET("leave-summary")
    suspend fun getLeaveSummary(
        @Query("year") year: Int? = null
    ): Response<LeaveSummaryResponse>

    // ========================================
    // TIMESHEET ENDPOINTS
    // ========================================

    @GET("timesheet/monthly")
    suspend fun getMonthlyTimesheet(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null
    ): Response<MonthlyTimesheetResponse>

    @GET("timesheet/day/{attendance_date}")
    suspend fun getDayTimesheet(
        @Path("attendance_date") attendanceDate: String
    ): Response<DayTimesheetResponse>

    // ========================================
    // CORRECTION REQUEST ENDPOINTS
    // ========================================

    @GET("correction-options-types")
    suspend fun getCorrectionRequestOptions(): Response<CorrectionRequestOptionsResponse>

    @Multipart
    @POST("apply-correction")
    suspend fun submitCorrectionRequest(
        @Part("request_type") requestType: RequestBody,
        @Part("attendance_date") attendanceDate: RequestBody,
        @Part("reason") reason: RequestBody,
        @Part("attendance_record_id") attendanceRecordId: RequestBody? = null,
        @Part("leave_type_id") leaveTypeId: RequestBody? = null,
        @Part("requested_status") requestedStatus: RequestBody? = null,
        @Part("requested_check_in") requestedCheckIn: RequestBody? = null,
        @Part("requested_check_out") requestedCheckOut: RequestBody? = null,
        @Part("priority") priority: RequestBody? = null,
        @Part supporting_document: MultipartBody.Part? = null
    ): Response<SubmitCorrectionRequestResponse>

    @GET("correction-requests")
    suspend fun getCorrectionRequests(
        @Query("status") status: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<CorrectionRequestListResponse>

    @GET("correction-requests/{request_id}")
    suspend fun getCorrectionRequestDetail(
        @Path("request_id") requestId: Int
    ): Response<CorrectionRequestDetailResponse>

    @FormUrlEncoded
    @POST("withdraw-correction/{request_id}")
    suspend fun withdrawCorrectionRequest(
        @Path("request_id") requestId: Int,
        @Field("withdrawal_reason") withdrawalReason: String
    ): Response<WithdrawCorrectionRequestResponse>

    @Streaming
    @GET("correction-requests/{request_id}/attachment")
    suspend fun downloadCorrectionAttachment(
        @Path("request_id") requestId: Int
    ): Response<ResponseBody>

    // ========================================
    // LOCATION SYNC ENDPOINT
    // ========================================

    @POST("location/sync")
    suspend fun syncLocationTracking(
        @Body locations: List<LocationSyncRequest>
    ): Response<Any>

    // ========================================
    // NOTIFICATION ENDPOINTS
    // ========================================

    /**
     * Get notifications list for the logged-in user
     */
    @GET("notifications")
    suspend fun getNotifications(
        @Query("token") token: String
    ): Response<NotificationsResponse>

    /**
     * Mark a single notification as read
     */
    @PUT("notifications/{notification_id}/read")
    suspend fun markNotificationRead(
        @Path("notification_id") notificationId: Int,
        @Query("token") token: String
    ): Response<NotificationActionResponse>

    /**
     * Mark all notifications as read
     */
    @PUT("notifications/mark-all-read")
    suspend fun markAllNotificationsRead(
        @Query("token") token: String
    ): Response<NotificationActionResponse>

    /**
     * Delete multiple notifications (soft delete)
     */
    @DELETE("notifications")
    suspend fun deleteNotifications(
        @Query("notification_ids") notificationIds: List<Int>,
        @Query("token") token: String
    ): Response<NotificationActionResponse>

    // ========================================
    // LOCATION REVERIFY ENDPOINT
    // ========================================

    @FormUrlEncoded
    @POST("check-in-location-reverify")
    suspend fun checkInLocationReverify(
        @Field("t_token") tToken: String,
        @Field("latitude") latitude: Double,
        @Field("longitude") longitude: Double,
        @Query("token") token: String
    ): Response<LocationReverifyResponse>

    // ========================================
    // LOGOUT ENDPOINT
    // ========================================

    @FormUrlEncoded
    @POST("logout")
    suspend fun logout(
        @Field("device_id") deviceId: String,
        @Field("clear_push_token") clearPushToken: Boolean = true
    ): Response<LogoutResponse>
}