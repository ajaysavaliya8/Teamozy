package com.hrms.jeejateamozy.feature.profile.data

/**
 * Contact Info Response
 */
data class ContactInfoResponse(
    val success: Boolean,
    val message: String,
    val data: ContactInfoData? = null
)

data class ContactInfoData(
    val personal_mobile: String?,
    val alternate_mobile: String?,
    val whatsapp_number: String?,
    val personal_email: String?,
    val company_email: String?,
    val emergency_contact_name: String?,
    val emergency_contact_relationship: String?,
    val emergency_contact_number: String?,
    val current_address: AddressData?,
    val same_as_current: Boolean?,
    val permanent_address: AddressData?
)

data class AddressData(
    val address_line: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val postal_code: String?
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
