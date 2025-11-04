package com.hrms.jeejateamozy.feature.profile.data

/**
 * Contact Info Response
 */
data class ContactInfoResponse(
    val status: String,
    val message: String,
    val data: ContactInfoData? = null
)

data class ContactInfoData(
    val country_code: Int?,
    val alternate_phone_number: Long?,
    val emergency_phone_number: Long?,
    val whatsapp_number: Long?,
    val company_phone_number: Long?,
    val current_address: String?,
    val permanent_address: String?
)

/**
 * Sealed class for contact info outcomes
 */
sealed class ContactInfoOutcome {
    data class Success(
        val message: String,
        val contactInfo: ContactInfoData? = null
    ) : ContactInfoOutcome()
    data class Error(val message: String) : ContactInfoOutcome()
}