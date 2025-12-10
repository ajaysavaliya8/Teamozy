package com.hrms.jeejateamozy.feature.attendance.data

import android.content.Context
import android.util.Log
import com.hrms.jeejateamozy.core.network.*
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.core.utils.NetworkErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Updated AttendanceHistoryRepository with Correction Request Support
 * ✅ UPDATED: Now using NetworkErrorHandler
 */

/**
 * Sealed class for monthly timesheet outcomes
 */
sealed class MonthlyTimesheetOutcome {
    data class Success(val timesheet: MonthlyTimesheet) : MonthlyTimesheetOutcome()
    data class Error(val message: String) : MonthlyTimesheetOutcome()
}

/**
 * Sealed class for day timesheet outcomes
 */
sealed class DayTimesheetOutcome {
    data class Success(val timesheet: DayTimesheet) : DayTimesheetOutcome()
    data class Error(val message: String) : DayTimesheetOutcome()
}

/**
 * Repository for attendance history/timesheet operations
 * ✅ UPDATED: Now includes correction request data
 */
class AttendanceHistoryRepository(private val context: Context) {

    private val api = NetworkModule.apiService

    /**
     * Get monthly timesheet calendar view
     * ✅ UPDATED: Now includes correction request summary
     */
    suspend fun getMonthlyTimesheet(
        year: Int? = null,
        month: Int? = null
    ): MonthlyTimesheetOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("TIMESHEET", "Fetching monthly timesheet: year=$year, month=$month")

            val response = api.getMonthlyTimesheet(year = year, month = month)

            Log.d("TIMESHEET", "Monthly timesheet response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("TIMESHEET", "Successfully fetched monthly timesheet")

                        val data = responseBody.data
                        val timesheet = MonthlyTimesheet(
                            month = data.month,
                            year = data.year,
                            monthName = data.month_name,
                            calendarDays = data.calendar_days.map { it.toDomain() },
                            summary = data.summary.toDomain(),
                            chartData = data.chart_data.toDomain(),
                            correctionRequests = data.correction_requests?.toDomain()  // ✅ NEW
                        )

                        // Log correction request summary
                        timesheet.correctionRequests?.let { summary ->
                            Log.d("TIMESHEET", "Correction requests - " +
                                    "Pending: ${summary.pending}, " +
                                    "Approved: ${summary.approved}, " +
                                    "Rejected: ${summary.rejected}, " +
                                    "Info Needed: ${summary.infoNeeded}")
                        }

                        MonthlyTimesheetOutcome.Success(timesheet = timesheet)
                    } else {
                        MonthlyTimesheetOutcome.Error("Failed to fetch monthly timesheet")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    MonthlyTimesheetOutcome.Error("Unauthorized. Please login again.")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch monthly timesheet")
                    MonthlyTimesheetOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("TIMESHEET", "Error fetching monthly timesheet", e)
            MonthlyTimesheetOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Get detailed timesheet for a specific day
     * ✅ UPDATED: Now includes active and settled correction requests
     */
    suspend fun getDayTimesheet(attendanceDate: String): DayTimesheetOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Log.d("TIMESHEET", "Fetching day timesheet: date=$attendanceDate")

                val response = api.getDayTimesheet(attendanceDate = attendanceDate)

                Log.d("TIMESHEET", "Day timesheet response code: ${response.code()}")

                when {
                    response.isSuccessful && response.code() == 200 -> {
                        val responseBody = response.body()
                        if (responseBody?.status == "success") {
                            Log.d("TIMESHEET", "Successfully fetched day timesheet")

                            val data = responseBody.data
                            val timesheet = DayTimesheet(
                                hasAttendance = data.has_attendance,
                                attendanceRecordId = data.attendance_record_id,  // ✅ NEW
                                date = data.date,
                                dayName = data.day_name,
                                formattedDate = data.formatted_date,
                                message = data.message,
                                status = data.status?.toDomain(),
                                shift = data.shift?.toDomain(),
                                hours = data.hours?.toDomain(),
                                punches = data.punches?.map { it.toDomain() },
                                isComplete = data.is_complete,

                                // ✅ NEW: Correction request fields
                                correctionRequest = data.correction_request?.toDomain(),
                                canSubmitCorrection = data.can_submit_correction,
                                hasPendingRequest = data.has_pending_request,
                                availableActions = data.available_actions,
                                canRequestNewAttendance = data.can_request_new_attendance,
                                actionAvailable = data.action_available
                            )

                            // Log correction request status
                            timesheet.correctionRequest?.let { container ->
                                if (container.hasAny) {
                                    container.active?.let { active ->
                                        Log.d("TIMESHEET", "Active correction request: " +
                                                "ID=${active.id}, " +
                                                "Type=${active.requestType}, " +
                                                "Status=${active.status}")
                                    }
                                    container.settled?.let { settled ->
                                        Log.d("TIMESHEET", "Settled correction request: " +
                                                "ID=${settled.id}, " +
                                                "Final Status=${settled.finalStatus}")
                                    }
                                } else {
                                    Log.d("TIMESHEET", "No correction requests for this date")
                                }
                            }

                            DayTimesheetOutcome.Success(timesheet = timesheet)
                        } else {
                            DayTimesheetOutcome.Error("Failed to fetch day timesheet")
                        }
                    }

                    response.code() == 401 -> {
                        AppStateManager.emitUnauthorized()
                        DayTimesheetOutcome.Error("Unauthorized. Please login again.")
                    }

                    else -> {
                        val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch day timesheet")
                        DayTimesheetOutcome.Error(errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e("TIMESHEET", "Error fetching day timesheet", e)
                DayTimesheetOutcome.Error(e.message ?: "Network error occurred")
            }
        }
}