package com.hrms.jeejateamozy.feature.profile.data

/**
 * Shift Details Response
 */
data class ShiftDetailsResponse(
    val success: Boolean,
    val message: String,
    val data: ShiftDetailsData? = null
)

data class ShiftDetailsData(
    val shift_name: String?,
    val code: String?,
    val shift_type: String?,
    val week_off_days: List<String>?,
    val multiple_check_allowed: Boolean?,
    val max_checks_per_day: Int?,
    val face_recognition_enabled: Boolean?,
    val fingerprint_enabled: Boolean?,
    val gps_tracking_enabled: Boolean?,
    val geofence_required: Boolean?,
    val break_type: String?,
    val break_check_required: Boolean?,
    val attendance_calc_mode: String?,
    val weekly_schedule: Map<String, DaySchedule>?,
    val policy: ShiftPolicyData?,
    val break_rules: List<BreakRuleData>?
)

data class DaySchedule(
    val is_working_day: Boolean?,
    val shift_start_time: String?,
    val shift_end_time: String?,
    val crosses_midnight: Boolean?,
    val is_flexible: Boolean?,
    val core_start_time: String?,
    val core_end_time: String?,
    val is_split_shift: Boolean?,
    val breaks: String?,  // Comes as JSON string from backend (e.g. "[]")
    val min_quarter_day_hours: String?,
    val min_half_day_hours: String?,
    val min_three_quarter_day_hours: String?,
    val min_full_day_hours: String?,
    val half_day_after_time: String?,
    val half_day_before_time: String?,
    val max_personal_break_minutes: Int?
)

data class ShiftPolicyData(
    val late_in_grace_minutes: Int?,
    val late_in_max_minutes: Int?,
    val late_in_deduction_type: String?,
    val early_out_grace_minutes: Int?,
    val early_out_max_minutes: Int?,
    val early_out_deduction_type: String?,
    val overtime_enabled: Boolean?,
    val overtime_threshold_minutes: Int?,
    val max_overtime_hours_per_day: Double?,
    val short_leave_enabled: Boolean?,
    val short_leave_max_minutes: Int?,
    val short_leave_monthly_limit: Int?,
    val late_in_cover_with_overtime: Boolean?,
    val early_out_cover_with_early_arrival: Boolean?
)

data class BreakRuleData(
    val rule_name: String?,
    val rule_type: String?,
    val allowed_extension_minutes: Int?,
    val max_break_duration_minutes: Int?,
    val min_gap_between_breaks_minutes: Int?,
    val max_breaks_per_day: Int?,
    val penalty_type: String?,
    val penalty_value: Double?,
    val apply_after_count: Int?,
    val count_period: String?
)

/**
 * Sealed class for shift details outcomes
 */
sealed class ShiftDetailsOutcome {
    data class Success(
        val message: String,
        val shiftDetails: ShiftDetailsData? = null
    ) : ShiftDetailsOutcome()
    data class Error(val message: String) : ShiftDetailsOutcome()
}
