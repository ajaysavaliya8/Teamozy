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

/**
 * ✅ UPDATED: AttendanceHistoryViewModel with Correction Request Support
 *
 * Changes:
 * - Added CorrectionRequestRepository dependency
 * - Added correction request state to DayDetailUiState
 * - Added methods for submit, withdraw, download
 * - Added dialog management
 * - Updated MonthlyTimesheet domain model to include correctionRequests
 * - Updated DayTimesheet domain model to include correction fields
 */

/**
 * UI State for Attendance History Screen (Monthly View)
 * ✅ UPDATED: Added correctionRequests field
 */
data class AttendanceHistoryUiState(
    val isLoading: Boolean = false,
    val currentMonth: Int = LocalDate.now().monthValue,
    val currentYear: Int = LocalDate.now().year,
    val monthName: String = "",
    val calendarDays: List<CalendarDay> = emptyList(),
    val summary: MonthSummary? = null,
    val chartData: ChartData? = null,
    val correctionRequests: CorrectionRequestSummary? = null,  // ✅ NEW
    val errorMessage: String? = null,
    val selectedDate: String? = null
)

/**
 * UI State for Day Detail Screen
 * ✅ UPDATED: Added correction request state
 */
data class DayDetailUiState(
    val isLoading: Boolean = false,
    val timesheet: DayTimesheet? = null,
    val errorMessage: String? = null,

    // ✅ NEW: Correction request state
    val correctionOptions: CorrectionRequestOptionsData? = null,
    val showSubmitCorrectionDialog: Boolean = false,
    val isSubmittingCorrection: Boolean = false,
    val isWithdrawingCorrection: Boolean = false
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
 * ✅ UPDATED: Added CorrectionRequestRepository parameter
 */
class AttendanceHistoryViewModel(
    private val repo: AttendanceHistoryRepository,
    private val correctionRepo: CorrectionRequestRepository  // ✅ NEW
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
     * ✅ UPDATED: Now includes correction request summary
     */
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
                                chartData = timesheet.chartData,
                                correctionRequests = timesheet.correctionRequests,  // ✅ NEW
                                errorMessage = null
                            )
                        }
                        Log.d("AttendanceHistoryVM", "Loaded timesheet for $month/$year")

                        // ✅ NEW: Log correction request summary
                        timesheet.correctionRequests?.let { summary ->
                            Log.d("AttendanceHistoryVM", "Correction Requests - " +
                                    "Pending: ${summary.pending}, " +
                                    "Approved: ${summary.approved}, " +
                                    "Rejected: ${summary.rejected}, " +
                                    "Info Needed: ${summary.infoNeeded}")
                        }
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
     * Load day timesheet
     * ✅ UPDATED: Now includes correction request data
     */
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
                        Log.d("AttendanceHistoryVM", "Loaded day timesheet for $attendanceDate")

                        // ✅ NEW: Log correction request status
                        outcome.timesheet.correctionRequest?.let { container ->
                            if (container.hasAny) {
                                container.active?.let { active ->
                                    Log.d("AttendanceHistoryVM", "Active correction request: " +
                                            "ID=${active.id}, Type=${active.requestType}, Status=${active.status}")
                                }
                                container.settled?.let { settled ->
                                    Log.d("AttendanceHistoryVM", "Settled correction request: " +
                                            "ID=${settled.id}, Final Status=${settled.finalStatus}")
                                }
                            }
                        }
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

    // ==========================================
    // ✅ NEW: CORRECTION REQUEST METHODS
    // ==========================================

    /**
     * Load correction request form options
     */
    fun loadCorrectionOptions() {
        viewModelScope.launch {
            when (val outcome = correctionRepo.getCorrectionRequestOptions()) {
                is CorrectionRequestOptionsOutcome.Success -> {
                    _dayDetailUiState.update { it.copy(correctionOptions = outcome.options) }
                    Log.d("AttendanceHistoryVM", "Loaded correction request options")
                }
                is CorrectionRequestOptionsOutcome.Error -> {
                    _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                    Log.e("AttendanceHistoryVM", "Error loading correction options: ${outcome.message}")
                }
            }
        }
    }

    /**
     * Submit correction request
     */
    fun submitCorrectionRequest(
        attendanceDate: String,
        attendanceRecordId: Int?,
        requestType: String,
        reason: String,
        requestedStatus: String? = null,
        requestedCheckIn: String? = null,
        requestedCheckOut: String? = null,
        leaveTypeId: Int? = null,
        priority: String = "NORMAL",
        attachmentUri: Uri? = null
    ) {
        viewModelScope.launch {
            _dayDetailUiState.update { it.copy(isSubmittingCorrection = true) }

            Log.d("AttendanceHistoryVM", "Submitting correction request for $attendanceDate")

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

                    Log.d("AttendanceHistoryVM", "Successfully submitted correction request: ${outcome.data.request_id}")

                    // Reload day timesheet to show new request
                    loadDayTimesheet(attendanceDate)

                    // Reload monthly calendar to show badge
                    loadCurrentMonth()

                    _events.emit(AttendanceHistoryEvent.ShowError("Correction request submitted successfully"))
                }

                is SubmitCorrectionRequestOutcome.Error -> {
                    _dayDetailUiState.update { it.copy(isSubmittingCorrection = false) }
                    _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                    Log.e("AttendanceHistoryVM", "Error submitting correction request: ${outcome.message}")
                }
            }
        }
    }

    /**
     * Withdraw correction request
     */
    fun withdrawCorrectionRequest(requestId: Int, attendanceDate: String) {
        viewModelScope.launch {
            _dayDetailUiState.update { it.copy(isWithdrawingCorrection = true) }

            Log.d("AttendanceHistoryVM", "Withdrawing correction request: $requestId")

            when (val outcome = correctionRepo.withdrawCorrectionRequest(requestId)) {
                is WithdrawCorrectionRequestOutcome.Success -> {
                    _dayDetailUiState.update { it.copy(isWithdrawingCorrection = false) }

                    Log.d("AttendanceHistoryVM", "Successfully withdrawn correction request: $requestId")

                    // Reload day timesheet to show updated status
                    loadDayTimesheet(attendanceDate)

                    // Reload monthly calendar to update badge
                    loadCurrentMonth()

                    _events.emit(AttendanceHistoryEvent.ShowError("Correction request withdrawn"))
                }

                is WithdrawCorrectionRequestOutcome.Error -> {
                    _dayDetailUiState.update { it.copy(isWithdrawingCorrection = false) }
                    _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                    Log.e("AttendanceHistoryVM", "Error withdrawing correction request: ${outcome.message}")
                }
            }
        }
    }

    /**
     * Download attachment
     */
    fun downloadAttachment(downloadUrl: String, isSettled: Boolean, requestId: Int) {
        viewModelScope.launch {
            Log.d("AttendanceHistoryVM", "Downloading attachment for request: $requestId (settled=$isSettled)")

            val outcome = if (isSettled) {
                correctionRepo.downloadSettledCorrectionAttachment(requestId)
            } else {
                correctionRepo.downloadCorrectionAttachment(requestId)
            }

            when (outcome) {
                is DownloadAttachmentOutcome.Success -> {
                    Log.d("AttendanceHistoryVM", "Successfully downloaded: ${outcome.file.name}")
                    _events.emit(AttendanceHistoryEvent.ShowError("Downloaded: ${outcome.file.name}"))
                }

                is DownloadAttachmentOutcome.Error -> {
                    Log.e("AttendanceHistoryVM", "Error downloading attachment: ${outcome.message}")
                    _events.emit(AttendanceHistoryEvent.ShowError(outcome.message))
                }
            }
        }
    }

    /**
     * Show/hide submit correction dialog
     */
    fun showSubmitCorrectionDialog(show: Boolean) {
        _dayDetailUiState.update { it.copy(showSubmitCorrectionDialog = show) }

        // Load options when opening dialog
        if (show && _dayDetailUiState.value.correctionOptions == null) {
            loadCorrectionOptions()
        }
    }

    // ==========================================
    // EXISTING METHODS (unchanged)
    // ==========================================

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
     * Refresh current view
     */
    fun refresh() {
        loadMonthlyTimesheet(
            year = _historyUiState.value.currentYear,
            month = _historyUiState.value.currentMonth
        )
    }

    /**
     * Add padding days to calendar for proper alignment
     */
    private fun addPaddingDays(days: List<CalendarDay>, year: Int, month: Int): List<CalendarDay> {
        if (days.isEmpty()) return emptyList()

        // Get first day of month
        val firstDayOfMonth = LocalDate.of(year, month, 1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0

        // Add empty padding days for alignment
        val paddingDays = List(dayOfWeek) {
            CalendarDay(
                day = 0, // 0 indicates empty padding
                date = "",
                status = "",
                color = "transparent",
                isComplete = false,
                hasIrregularity = false,
                punchCount = 0,
                hasCorrectionRequest = false,  // ✅ NEW
                correctionBadge = null,  // ✅ NEW
                correctionRequestId = null  // ✅ NEW
            )
        }

        val fullCalendar = paddingDays + days

        // Add padding at end to complete the grid if needed
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
                    punchCount = 0,
                    hasCorrectionRequest = false,  // ✅ NEW
                    correctionBadge = null,  // ✅ NEW
                    correctionRequestId = null  // ✅ NEW
                )
            }
            return fullCalendar + endPadding
        }

        return fullCalendar
    }
}