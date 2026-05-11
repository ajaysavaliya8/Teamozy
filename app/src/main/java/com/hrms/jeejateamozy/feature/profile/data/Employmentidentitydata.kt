package com.hrms.jeejateamozy.feature.profile.data

/**
 * Employment Identity Response
 */
data class EmploymentIdentityResponse(
    val success: Boolean,
    val message: String,
    val data: EmploymentIdentityData? = null
)

data class EmploymentIdentityData(
    val documents: List<IdentityDocument> = emptyList()
)

data class IdentityDocument(
    val id: Int = 0,
    val category_system_code: String? = null,
    val category_name: String? = null,
    val document_number: String? = null,
    val file_type: String? = null,
    val file_path: String? = null,
    val verification_status: String? = null,
    val issue_date: String? = null,
    val expiry_date: String? = null
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
