package com.hrms.jeejateamozy.feature.attendance.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.core.network.*
import com.hrms.jeejateamozy.feature.attendance.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class AttendanceHistoryUiState(
    val isLoading: Boolean = false,
    val currentMonth: Int = LocalDate.now().monthValue,
    val currentYear: Int = LocalDate.now().year,
    val monthName: String = "",
    val calendarDays: List<CalendarDay> = emptyList(),
    val summary: MonthSummary? = null,
    val errorMessage: String? = null,
    val selectedDate: String? = null
)

data class DayDetailUiState(
    val isLoading: Boolean = false,
    val timesheet: DayTimesheet? = null,
    val errorMessage: String? = null,
    val correctionOptions: CorrectionRequestOptionsData? = null,
    val showSubmitCorrectionDialog: Boolean = false,
    val isSubmittingCorrection: Boolean = false,
    val isWithdrawingCorrection: Boolean = false,
    val showWithdrawDialog: Boolean = false
)

sealed class AttendanceHistoryEvent {
    data class ShowError(val message: String) : AttendanceHistoryEvent()
    data class ShowSuccess(val message: String) : AttendanceHistoryEvent()
    data class NavigateToDayDetail(val date: String) : AttendanceHistoryEvent()
}

class AttendanceHistoryViewModel(
    private val repo: AttendanceHistoryRepository,
    private val correctionRepo: CorrectionRequestRepository
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

    fun loadCurrentMonth() {
        val now = LocalDate.now()
        loadMonthlyTimesheet(year = now.year, month = now.monthValue)
    }

    fun loadMonthlyTimesheet(year: Int, month: Int) {
        viewModelScope.launch {
            try {
                _historyUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.getMonthlyTimesheet(year = year, month = month)) {
                    is MonthlyTimesheetOutcome.Success -> {
                        val timesheet = outcome.timesheet
                        val calendarWithPadding = addPaddingDays(
                            days = timesheet.calendarDays,
                            year = year,
                            month = month
                        )

                        _historyUiState.update {
                            it.copy(
                                isLoading = false,
                                currentMonth = month,
                                currentYear = year,
                                monthName = timesheet.monthName,
                                calendarDays = calendarWithPadding,
                                summary = timesheet.summary,
                                errorMessage = null
                            )
                        }
                    }

                    is MonthlyTimesheetOutcome.Error -> {
                        _historyUiState.update {
                            it.copy(isLoading = false, errorMessage = outcome.message)
                        }
                        _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                    }
                }
            } catch (e: Exception) {
                _historyUiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }

    fun loadDayTimesheet(attendanceDate: String) {
        viewModelScope.launch {
            try {
                _dayDetailUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.getDayTimesheet(attendanceDate = attendanceDate)) {
                    is DayTimesheetOutcome.Success -> {
                        _dayDetailUiState.update {
                            it.copy(
                                isLoading = false,
                                timesheet = outcome.timesheet,
                                errorMessage = null
                            )
                        }
                    }

                    is DayTimesheetOutcome.Error -> {
                        _dayDetailUiState.update {
                            it.copy(isLoading = false, errorMessage = outcome.message)
                        }
                        _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                    }
                }
            } catch (e: Exception) {
                _dayDetailUiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }

    // ==========================================
    // CORRECTION REQUEST METHODS
    // ==========================================

    fun loadCorrectionOptions() {
        viewModelScope.launch {
            when (val outcome = correctionRepo.getCorrectionRequestOptions()) {
                is CorrectionRequestOptionsOutcome.Success -> {
                    _dayDetailUiState.update { it.copy(correctionOptions = outcome.options) }
                }
                is CorrectionRequestOptionsOutcome.Error -> {
                    _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                }
            }
        }
    }

    fun submitCorrectionRequest(
        attendanceDate: String,
        attendanceRecordId: Int?,
        requestType: String,
        reason: String,
        requestedStatus: String? = null,
        requestedCheckIn: String? = null,
        requestedCheckOut: String? = null,
        leaveTypeId: Int? = null,
        priority: String = "normal",
        attachmentUri: Uri? = null
    ) {
        viewModelScope.launch {
            _dayDetailUiState.update { it.copy(isSubmittingCorrection = true) }

            when (val outcome = correctionRepo.submitCorrectionRequest(
                requestType = requestType,
                attendanceDate = attendanceDate,
                reason = reason,
                attendanceRecordId = attendanceRecordId,
                requestedStatus = requestedStatus,
                requestedCheckIn = requestedCheckIn,
                requestedCheckOut = requestedCheckOut,
                leaveTypeId = leaveTypeId,
                priority = priority,
                attachmentUri = attachmentUri
            )) {
                is SubmitCorrectionRequestOutcome.Success -> {
                    _dayDetailUiState.update {
                        it.copy(
                            isSubmittingCorrection = false,
                            showSubmitCorrectionDialog = false
                        )
                    }
                    loadDayTimesheet(attendanceDate)
                    loadCurrentMonth()
                    _events.emit(AttendanceHistoryEvent.ShowSuccess(outcome.message))
                }

                is SubmitCorrectionRequestOutcome.Error -> {
                    _dayDetailUiState.update { it.copy(isSubmittingCorrection = false) }
                    _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                }
            }
        }
    }

    fun withdrawCorrectionRequest(requestId: Int, attendanceDate: String, withdrawalReason: String) {
        viewModelScope.launch {
            _dayDetailUiState.update { it.copy(isWithdrawingCorrection = true) }

            when (val outcome = correctionRepo.withdrawCorrectionRequest(requestId, withdrawalReason)) {
                is WithdrawCorrectionRequestOutcome.Success -> {
                    _dayDetailUiState.update {
                        it.copy(isWithdrawingCorrection = false, showWithdrawDialog = false)
                    }
                    loadDayTimesheet(attendanceDate)
                    loadCurrentMonth()
                    _events.emit(AttendanceHistoryEvent.ShowSuccess(outcome.message))
                }

                is WithdrawCorrectionRequestOutcome.Error -> {
                    _dayDetailUiState.update { it.copy(isWithdrawingCorrection = false) }
                    _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                }
            }
        }
    }

    fun downloadAttachment(requestId: Int) {
        viewModelScope.launch {
            val outcome = correctionRepo.downloadCorrectionAttachment(requestId)
            when (outcome) {
                is DownloadAttachmentOutcome.Success -> {
                    _events.emit(AttendanceHistoryEvent.ShowSuccess("Downloaded: ${outcome.file.name}"))
                }
                is DownloadAttachmentOutcome.Error -> {
                    _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                }
            }
        }
    }

    fun showSubmitCorrectionDialog(show: Boolean) {
        _dayDetailUiState.update { it.copy(showSubmitCorrectionDialog = show) }
        if (show && _dayDetailUiState.value.correctionOptions == null) {
            loadCorrectionOptions()
        }
    }

    fun showWithdrawDialog(show: Boolean) {
        _dayDetailUiState.update { it.copy(showWithdrawDialog = show) }
    }

    // ==========================================
    // NAVIGATION METHODS
    // ==========================================

    fun loadPreviousMonth() {
        val currentYearMonth = YearMonth.of(_historyUiState.value.currentYear, _historyUiState.value.currentMonth)
        val previousMonth = currentYearMonth.minusMonths(1)
        loadMonthlyTimesheet(year = previousMonth.year, month = previousMonth.monthValue)
    }

    fun loadNextMonth() {
        val currentYearMonth = YearMonth.of(_historyUiState.value.currentYear, _historyUiState.value.currentMonth)
        val nextMonth = currentYearMonth.plusMonths(1)
        loadMonthlyTimesheet(year = nextMonth.year, month = nextMonth.monthValue)
    }

    fun refresh() {
        loadMonthlyTimesheet(
            year = _historyUiState.value.currentYear,
            month = _historyUiState.value.currentMonth
        )
    }

    private fun addPaddingDays(days: List<CalendarDay>, year: Int, month: Int): List<CalendarDay> {
        if (days.isEmpty()) return emptyList()

        val firstDayOfMonth = LocalDate.of(year, month, 1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

        val paddingDays = List(dayOfWeek) {
            CalendarDay(
                day = 0,
                date = "",
                status = "",
                color = "transparent",
                isComplete = false,
                hasIrregularity = false,
                punchCount = 0
            )
        }

        val fullCalendar = paddingDays + days

        val remainingDays = 7 - (fullCalendar.size % 7)
        if (remainingDays < 7) {
            val endPadding = List(remainingDays) {
                CalendarDay(
                    day = 0,
                    date = "",
                    status = "",
                    color = "transparent",
                    isComplete = false,
                    hasIrregularity = false,
                    punchCount = 0
                )
            }
            return fullCalendar + endPadding
        }

        return fullCalendar
    }
}
