package com.hrms.jeejateamozy.feature.profile.data

/**
 * Banking Info Response
 */
data class BankingInfoResponse(
    val success: Boolean,
    val message: String,
    val data: BankingInfoData? = null
)

data class BankingInfoData(
    val account_holder_name: String?,
    val bank_name: String?,
    val bank_account_number: String?,
    val account_type: String?,
    val ifsc_code: String?,
    val branch_name: String?,
    val upi_id: String?,
    val bank_verified: Boolean?
)

/**
 * Sealed class for banking info outcomes
 */
sealed class BankingInfoOutcome {
    data class Success(
        val message: String,
        val bankingInfo: BankingInfoData? = null
    ) : BankingInfoOutcome()
    data class Error(val message: String) : BankingInfoOutcome()
}
