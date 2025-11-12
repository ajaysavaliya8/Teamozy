package com.hrms.jeejateamozy.feature.attendance.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.core.network.*
import com.hrms.jeejateamozy.feature.attendance.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * UI State for Attendance History Screen (Monthly View)
 */
data class AttendanceHistoryUiState(
    val isLoading: Boolean = false,
    val currentMonth: Int = LocalDate.now().monthValue,
    val currentYear: Int = LocalDate.now().year,
    val monthName: String = "",
    val calendarDays: List<CalendarDay> = emptyList(),
    val summary: MonthSummary? = null,
    val chartData: ChartData? = null,
    val errorMessage: String? = null,
    val selectedDate: String? = null  // For navigating to day detail
)

/**
 * UI State for Day Detail Screen
 */
data class DayDetailUiState(
    val isLoading: Boolean = false,
    val timesheet: DayTimesheet? = null,
    val errorMessage: String? = null
)

/**
 * Events for attendance history
 */
sealed class AttendanceHistoryEvent {
    data class ShowError(val message: String) : AttendanceHistoryEvent()
    data class NavigateToDayDetail(val date: String) : AttendanceHistoryEvent()
}

/**
 * ViewModel for Attendance History
 */
class AttendanceHistoryViewModel(
    private val repo: AttendanceHistoryRepository
) : ViewModel() {

    private val _historyUiState = MutableStateFlow(AttendanceHistoryUiState())
    val historyUiState: StateFlow<AttendanceHistoryUiState> = _historyUiState.asStateFlow()

    private val _dayDetailUiState = MutableStateFlow(DayDetailUiState())
    val dayDetailUiState: StateFlow<DayDetailUiState> = _dayDetailUiState.asStateFlow()

    private val _events = MutableSharedFlow<AttendanceHistoryEvent>()
    val events: SharedFlow<AttendanceHistoryEvent> = _events.asSharedFlow()

    init {
        loadCurrentMonth()
    }

    /**
     * Load timesheet for current month
     */
    fun loadCurrentMonth() {
        val now = LocalDate.now()
        loadMonthlyTimesheet(year = now.year, month = now.monthValue)
    }

    /**
     * Load timesheet for a specific month
     */
    fun loadMonthlyTimesheet(year: Int, month: Int) {
        viewModelScope.launch {
            try {
                _historyUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.getMonthlyTimesheet(year = year, month = month)) {
                    is MonthlyTimesheetOutcome.Success -> {
                        val timesheet = outcome.timesheet
                        _historyUiState.update {
                            it.copy(
                                isLoading = false,
                                currentMonth = timesheet.month,
                                currentYear = timesheet.year,
                                monthName = timesheet.monthName,
                                calendarDays = timesheet.calendarDays,
                                summary = timesheet.summary,
                                chartData = timesheet.chartData,
                                errorMessage = null
                            )
                        }
                        Log.d(
                            "AttendanceHistoryVM",
                            "Loaded ${timesheet.calendarDays.size} calendar days"
                        )
                    }

                    is MonthlyTimesheetOutcome.Error -> {
                        _historyUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                        Log.e("AttendanceHistoryVM", "Error loading timesheet: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _historyUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(AttendanceHistoryEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("AttendanceHistoryVM", "Exception loading timesheet", e)
            }
        }
    }

    /**
     * Navigate to previous month
     */
    fun loadPreviousMonth() {
        val currentYearMonth = YearMonth.of(_historyUiState.value.currentYear, _historyUiState.value.currentMonth)
        val previousMonth = currentYearMonth.minusMonths(1)
        loadMonthlyTimesheet(year = previousMonth.year, month = previousMonth.monthValue)
    }

    /**
     * Navigate to next month
     */
    fun loadNextMonth() {
        val currentYearMonth = YearMonth.of(_historyUiState.value.currentYear, _historyUiState.value.currentMonth)
        val nextMonth = currentYearMonth.plusMonths(1)
        loadMonthlyTimesheet(year = nextMonth.year, month = nextMonth.monthValue)
    }

    /**
     * Load day detail when user clicks on a calendar day
     */
    fun loadDayDetail(date: String) {
        viewModelScope.launch {
            try {
                _dayDetailUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.getDayTimesheet(attendanceDate = date)) {
                    is DayTimesheetOutcome.Success -> {
                        _dayDetailUiState.update {
                            it.copy(
                                isLoading = false,
                                timesheet = outcome.timesheet,
                                errorMessage = null
                            )
                        }
                        Log.d("AttendanceHistoryVM", "Loaded day timesheet for $date")
                    }

                    is DayTimesheetOutcome.Error -> {
                        _dayDetailUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                        Log.e("AttendanceHistoryVM", "Error loading day timesheet: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _dayDetailUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(AttendanceHistoryEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("AttendanceHistoryVM", "Exception loading day timesheet", e)
            }
        }
    }

    /**
     * Select a date (for navigation)
     */
    fun selectDate(date: String) {
        _historyUiState.update { it.copy(selectedDate = date) }
    }

    /**
     * Clear selected date
     */
    fun clearSelectedDate() {
        _dayDetailUiState.update { DayDetailUiState() }
    }

    /**
     * Refresh current month
     */
    fun refresh() {
        loadMonthlyTimesheet(
            year = _historyUiState.value.currentYear,
            month = _historyUiState.value.currentMonth
        )
    }
}