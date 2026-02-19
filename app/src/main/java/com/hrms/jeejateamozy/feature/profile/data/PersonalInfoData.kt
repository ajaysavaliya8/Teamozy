package com.hrms.jeejateamozy.feature.profile.data

/**
 * Personal Info Response
 */
data class PersonalInfoResponse(
    val success: Boolean,
    val message: String,
    val data: PersonalInfoData? = null
)

data class PersonalInfoData(
    val full_name: String?,
    val first_name: String?,
    val last_name: String?,
    val gender: String?,
    val date_of_birth: String?,
    val nationality: String?,
    val religion: String?,
    val blood_group: String?,
    val marital_status: String?,
    val spouse_name: String?,
    val father_name: String?,
    val mother_name: String?,
    val no_of_children: Int?,
    val languages: List<String>?,
    val profile_picture_url: String?
)

/**
 * Sealed class for personal info outcomes
 */
sealed class PersonalInfoOutcome {
    data class Success(
        val message: String,
        val personalInfo: PersonalInfoData? = null
    ) : PersonalInfoOutcome()
    data class Error(val message: String) : PersonalInfoOutcome()
}
