package com.hrms.jeejateamozy.core.network

import com.hrms.jeejateamozy.feature.profile.data.BankingInfoResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import com.hrms.jeejateamozy.feature.profile.data.ContactInfoResponse
import com.hrms.jeejateamozy.feature.profile.data.PersonalInfoResponse
import com.hrms.jeejateamozy.feature.profile.data.EmploymentDetailResponse

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
        @Query("token") token: String
    ): Response<CheckInSignatureResponse>

    @POST("check-out")
    suspend fun checkOut(
        @Query("device_id") deviceId: String,
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("token") token: String
    ): Response<CheckOutResponse>

    @FormUrlEncoded
    @POST("check-out-signature")
    suspend fun checkOutSignature(
        @Field("t_token") tToken: String,
        @Field("face_recognition_quality_score") faceRecognitionQualityScore: Float? = null,
        @Field("face_verify") faceVerify: Boolean = false,
        @Field("early_reason") earlyReason: String? = null,
        @Field("out_of_range_reason") outOfRangeReason: String? = null,
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
}