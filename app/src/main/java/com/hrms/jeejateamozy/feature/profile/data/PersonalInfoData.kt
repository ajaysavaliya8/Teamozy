package com.hrms.jeejateamozy.feature.profile.data

/**
 * Personal Info Response
 */
data class PersonalInfoResponse(
    val status: String,
    val message: String,
    val data: PersonalInfoData? = null
)

data class PersonalInfoData(
    val alias_name: String?,              // ✅ This field
    val gender: String?,
    val birth_date: String?,              // ✅ This field
    val nationality: String?,
    val blood_group: String?,
    val marital_status: String?,
    val father_name: String?,             // ✅ This field
    val no_of_family_members: Int?,       // ✅ This field
    val languages: List<String>?          // ✅ This field
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