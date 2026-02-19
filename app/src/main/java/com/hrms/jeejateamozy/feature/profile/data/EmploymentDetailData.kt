package com.hrms.jeejateamozy.feature.profile.data

/**
 * Employment Detail Response
 */
data class EmploymentDetailResponse(
    val success: Boolean,
    val message: String,
    val data: EmploymentDetailData? = null
)

data class EmploymentDetailData(
    val joining_date: String?,
    val probation_end_date: String?,
    val confirmation_date: String?,
    val work_location: String?,
    val employment_type: String?,
    val work_mode: String?,
    val designation_name: String?,
    val branch_name: String?,
    val department_name: String?,
    val sub_department_name: String?,
    val shift_name: String?
)

/**
 * Sealed class for employment detail outcomes
 */
sealed class EmploymentDetailOutcome {
    data class Success(
        val message: String,
        val employmentDetail: EmploymentDetailData? = null
    ) : EmploymentDetailOutcome()
    data class Error(val message: String) : EmploymentDetailOutcome()
}
