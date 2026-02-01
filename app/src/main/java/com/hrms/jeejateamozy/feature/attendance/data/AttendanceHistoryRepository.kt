package com.hrms.jeejateamozy.feature.attendance.data

import android.content.Context
import android.util.Log
import com.hrms.jeejateamozy.core.network.*
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.core.utils.NetworkErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class MonthlyTimesheetOutcome {
    data class Success(val timesheet: MonthlyTimesheet) : MonthlyTimesheetOutcome()
    data class Error(val message: String) : MonthlyTimesheetOutcome()
}

sealed class DayTimesheetOutcome {
    data class Success(val timesheet: DayTimesheet) : DayTimesheetOutcome()
    data class Error(val message: String) : DayTimesheetOutcome()
}

class AttendanceHistoryRepository(private val context: Context) {

    private val api = NetworkModule.apiService

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
                        val data = responseBody.data
                        val timesheet = MonthlyTimesheet(
                            month = data.month,
                            year = data.year,
                            monthName = data.month_name,
                            calendarDays = data.calendar_days.map { it.toDomain() },
                            summary = data.summary.toDomain()
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
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch monthly timesheet")
                    MonthlyTimesheetOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("TIMESHEET", "Error fetching monthly timesheet", e)
            MonthlyTimesheetOutcome.Error(e.message ?: "Network error occurred")
        }
    }

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
                            val data = responseBody.data
                            val timesheet = data.toDomain()
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
