package com.hrms.jeejateamozy.feature.profile.data

/**
 * Employment Identity Response
 */
data class EmploymentIdentityResponse(
    val status: String,
    val message: String,
    val data: EmploymentIdentityData? = null
)

data class EmploymentIdentityData(
    val aadhaar_number: String?,
    val pan_number: String?,
    val aadhar_front_image_url: String?,
    val aadhar_back_image_url: String?,
    val pan_card_image_url: String?
)

/**
 * Sealed class for employment identity outcomes
 */
sealed class EmploymentIdentityOutcome {
    data class Success(
        val message: String,
        val identityInfo: EmploymentIdentityData? = null
    ) : EmploymentIdentityOutcome()
    data class Error(val message: String) : EmploymentIdentityOutcome()
}