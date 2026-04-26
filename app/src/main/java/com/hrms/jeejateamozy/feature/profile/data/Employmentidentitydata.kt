package com.hrms.jeejateamozy.feature.profile.data

/**
 * Employment Identity Response
 */
data class EmploymentIdentityResponse(
    val success: Boolean,
    val message: String,
    val data: EmploymentIdentityData? = null
)

/**
 * Backend returns identity documents as a typed list keyed by `category_system_code`
 * ("AADHAAR", "PAN", etc.). The UI displays just the document number for each, so the
 * DTO only parses what's read — Gson ignores the other fields the server sends.
 */
data class EmploymentIdentityData(
    val documents: List<IdentityDocument> = emptyList()
) {
    private fun docFor(code: String): IdentityDocument? =
        documents.firstOrNull { it.category_system_code.equals(code, ignoreCase = true) }

    val aadhaar_number: String? get() = docFor("AADHAAR")?.document_number
    val pan_number: String? get() = docFor("PAN")?.document_number
}

data class IdentityDocument(
    val category_system_code: String? = null,
    val document_number: String? = null
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
