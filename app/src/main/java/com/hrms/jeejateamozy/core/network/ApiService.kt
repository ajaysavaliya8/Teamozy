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
import com.hrms.jeejateamozy.core.network.WorkReportListResponse
import com.hrms.jeejateamozy.core.network.CreateWorkReportResponse
import okhttp3.ResponseBody

data class DeviceChangeResponse(
    val detail: String
)

interface ApiService {

    // ---------- AUTH ----------
    @POST("send-login")
    suspend fun sendLogin(
        @Query("mobile_number") mobileNumber: Long,
        @Query("device_id") deviceId: String
    ): Response<BasicResponse>

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
    ): Response<BasicResponse>

    @GET("verify-token")
    suspend fun verifyToken(
        @Query("app_version") appVersion: String
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

    // ---------- ATTENDANCE ----------
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
        @Query("token") token: String
    ): Response<CheckOutSignatureResponse>

    // ---------- FACE RECOGNITION ----------
    @GET("employees/face-recognition")
    suspend fun getFaceRecognitionData(): Response<FaceRecognitionDataResponse>

    @Multipart
    @POST("employees/face-recognition")
    suspend fun registerFaceRecognition(
        @Part face_image: MultipartBody.Part,
        @Part("face_vector") faceVector: RequestBody,
        @Part("priority") priority: RequestBody,
        @Part("reason_for_change") reasonForChange: RequestBody? = null
    ): Response<BasicResponse>

    // ---------- PROFILE ----------
// ---------- PROFILE ----------

    @FormUrlEncoded
    @PUT("profile/update-social-media")
    suspend fun updateSocialMedia(
        @Field("facebook") facebook: String? = null,
        @Field("linkedin") linkedin: String? = null,
        @Field("x") x: String? = null,
        @Field("instagram") instagram: String? = null,
        @Field("snapchat") snapchat: String? = null
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
        @Field("country_code") countryCode: Int? = null,
        @Field("alternate_phone_number") alternatePhoneNumber: Long? = null,
        @Field("emergency_phone_number") emergencyPhoneNumber: Long? = null,
        @Field("whatsapp_number") whatsappNumber: Long? = null,
        @Field("company_phone_number") companyPhoneNumber: Long? = null,
        @Field("current_address") currentAddress: String? = null,
        @Field("permanent_address") permanentAddress: String? = null
    ): Response<ContactInfoResponse>

    @GET("personal-info")
    suspend fun getPersonalInfo(): Response<PersonalInfoResponse>

    @FormUrlEncoded
    @PUT("personal-info")
    suspend fun updatePersonalInfo(
        @Field("blood_group") bloodGroup: String? = null,
        @Field("marital_status") maritalStatus: String? = null,
        @Field("no_of_family_members") noOfFamilyMembers: Int? = null,
        @Field("languages") languages: List<String>? = null
    ): Response<PersonalInfoResponse>

    // ---------- PROFILE - EMPLOYMENT DETAIL ----------
    @GET("employment-details")
    suspend fun getEmploymentDetails(): Response<EmploymentDetailResponse>

    @GET("banking-info")
    suspend fun getBankingInfo(): Response<BankingInfoResponse>

    @FormUrlEncoded
    @PUT("banking-info")
    suspend fun updateBankingInfo(
        @Field("account_holder_name") accountHolderName: String? = null,
        @Field("bank_name") bankName: String? = null,
        @Field("bank_account_number") bankAccountNumber: String? = null,
        @Field("account_type") accountType: String? = null,
        @Field("ifsc_code") ifscCode: String? = null
    ): Response<BankingInfoResponse>

    @GET("employment-identity")
    suspend fun getEmploymentIdentity(): Response<EmploymentIdentityResponse>

    @GET("employee-shift")
    suspend fun getEmploymentShift(): Response<ShiftDetailsResponse>

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

    // ---------- CIRCULARS ----------
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

    /**
     * Get all available leave types
     */
    @GET("leave-types")
    suspend fun getLeaveTypes(): Response<LeaveTypesResponse>

    /**
     * Apply for leave
     */
    @Multipart
    @POST("apply-leave")
    suspend fun applyLeave(
        @Part("leave_type_id") leaveTypeId: Int,
        @Part("start_date") startDate: RequestBody,
        @Part("end_date") endDate: RequestBody,
        @Part("leave_reason") leaveReason: RequestBody,
        @Part("alternate_contact") alternateContact: RequestBody?,
        @Part("task_depended_on_you") taskDependedOnYou: Boolean,
        @Part("dependency_handled_by") dependencyHandledBy: RequestBody?,
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
    ): Response<BasicResponse>  // Uses same structure as LeaveApplicationDto

    /**
     * Withdraw leave application
     */
    @FormUrlEncoded
    @POST("withdraw-leave/{application_id}")
    suspend fun withdrawLeave(
        @Path("application_id") applicationId: Int,
        @Field("withdrawal_reason") withdrawalReason: String
    ): Response<WithdrawLeaveResponse>

    /**
     * Get leave summary
     */
    @GET("leave-summary")
    suspend fun getLeaveSummary(
        @Query("year") year: Int? = null
    ): Response<LeaveSummaryResponse>

    @GET("timesheet/monthly")
    suspend fun getMonthlyTimesheet(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null
    ): Response<MonthlyTimesheetResponse>

    @GET("timesheet/day/{attendance_date}")
    suspend fun getDayTimesheet(
        @Path("attendance_date") attendanceDate: String
    ): Response<DayTimesheetResponse>


    @FormUrlEncoded
    @POST("logout")
    suspend fun logout(
        @Field("device_id") deviceId: String,
        @Field("clear_push_token") clearPushToken: Boolean = true
    ): Response<LogoutResponse>

    @GET("timesheet/correction-request/options")
    suspend fun getCorrectionRequestOptions(): Response<CorrectionRequestOptionsResponse>

    @Multipart
    @POST("timesheet/correction-request/submit")
    suspend fun submitCorrectionRequest(
        @Part("request_type") requestType: RequestBody,
        @Part("attendance_date") attendanceDate: RequestBody,
        @Part("reason") reason: RequestBody,
        @Part("attendance_record_id") attendanceRecordId: RequestBody? = null,
        @Part("attendance_session_id") attendanceSessionId: RequestBody? = null,
        @Part("leave_type_id") leaveTypeId: RequestBody? = null,
        @Part("requested_status") requestedStatus: RequestBody? = null,
        @Part("requested_check_in") requestedCheckIn: RequestBody? = null,
        @Part("requested_check_out") requestedCheckOut: RequestBody? = null,
        @Part("priority") priority: RequestBody? = null,
        @Part attachment: MultipartBody.Part? = null
    ): Response<SubmitCorrectionRequestResponse>

    @PATCH("timesheet/correction-request/{request_id}/withdraw")
    suspend fun withdrawCorrectionRequest(
        @Path("request_id") requestId: Int
    ): Response<WithdrawCorrectionRequestResponse>

    @Streaming
    @GET("timesheet/correction-request/attachment/{request_id}")
    suspend fun downloadCorrectionAttachment(
        @Path("request_id") requestId: Int
    ): Response<ResponseBody>

    @Streaming
    @GET("timesheet/correction-request/settled-attachment/{settled_id}")
    suspend fun downloadSettledCorrectionAttachment(
        @Path("settled_id") settledId: Int
    ): Response<ResponseBody>
}