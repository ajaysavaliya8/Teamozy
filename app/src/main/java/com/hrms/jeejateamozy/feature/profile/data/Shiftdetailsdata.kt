package com.hrms.jeejateamozy.feature.profile.data

/**
 * Shift Details Response
 */
data class ShiftDetailsResponse(
    val status: String,
    val message: String,
    val data: ShiftDetailsData? = null
)

data class ShiftDetailsData(
    val shift_name: String?,
    val code: String?,
    val multiple_punch_allowed: Boolean?,
    val face_recognition_enabled: Boolean?,
    val fingerprint_recognition_enabled: Boolean?,
    val week_off_days: List<String>?,
    val late_in_max_minutes: Int?,
    val early_out_max_minutes: Int?,
    val allow_short_leave: Boolean?,
    val short_leave_minutes_in_shift_time: Int?,
    val break_type: String?,
    val break_timing_type: String?,
    val weekly_schedule: WeeklySchedule?
)

data class WeeklySchedule(
    val monday: DaySchedule?,
    val tuesday: DaySchedule?,
    val wednesday: DaySchedule?,
    val thursday: DaySchedule?,
    val friday: DaySchedule?,
    val saturday: DaySchedule?,
    val sunday: DaySchedule?
)

data class DaySchedule(
    val shift_start_time: String?,
    val shift_end_time: String?,
    val lunch_start_time: String?,
    val lunch_end_time: String?,
    val tea_start_time: String?,
    val tea_end_time: String?,
    val min_full_day_hours: String?,
    val min_half_day_hours: String?
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