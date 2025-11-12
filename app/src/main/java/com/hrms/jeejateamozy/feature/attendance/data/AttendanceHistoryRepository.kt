package com.hrms.jeejateamozy.feature.attendance.data

import android.content.Context
import android.util.Log
import com.hrms.jeejateamozy.core.network.*
import com.hrms.jeejateamozy.core.state.AppStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 */
class AttendanceHistoryRepository(private val context: Context) {

    private val api = NetworkModule.apiService

    /**
     * Get monthly timesheet calendar view
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
                            chartData = data.chart_data.toDomain()
                        )

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
                    val errorMsg = extractErrorMessage(response)
                    MonthlyTimesheetOutcome.Error(errorMsg ?: "Failed to fetch monthly timesheet")
                }
            }
        } catch (e: Exception) {
            Log.e("TIMESHEET", "Error fetching monthly timesheet", e)
            MonthlyTimesheetOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Get detailed timesheet for a specific day
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
                                date = data.date,
                                dayName = data.day_name,
                                formattedDate = data.formatted_date,
                                message = data.message,
                                status = data.status?.toDomain(),
                                shift = data.shift?.toDomain(),
                                hours = data.hours?.toDomain(),
                                punches = data.punches?.map { it.toDomain() },
                                isComplete = data.is_complete
                            )

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
                        val errorMsg = extractErrorMessage(response)
                        DayTimesheetOutcome.Error(errorMsg ?: "Failed to fetch day timesheet")
                    }
                }
            } catch (e: Exception) {
                Log.e("TIMESHEET", "Error fetching day timesheet", e)
                DayTimesheetOutcome.Error(e.message ?: "Network error occurred")
            }
        }

    /**
     * Helper function to extract error message from response
     */
    private fun <T> extractErrorMessage(response: retrofit2.Response<T>): String? {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = org.json.JSONObject(errorBody)
                json.optString("message").takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("TIMESHEET", "Error extracting error message", e)
            null
        }
    }
}