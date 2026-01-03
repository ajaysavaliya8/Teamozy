package com.hrms.jeejateamozy.feature.leave.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.core.network.*
import com.hrms.jeejateamozy.feature.leave.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

// =============================================================================
// UI STATES
// =============================================================================

/**
 * UI State for Apply Leave Screen
 */
data class ApplyLeaveUiState(
    val isLoading: Boolean = false,
    val leaveTypes: List<LeaveType> = emptyList(),
    val selectedLeaveType: LeaveType? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * UI State for Leave History Screen
 */
data class LeaveHistoryUiState(
    val isLoading: Boolean = false,
    val applications: List<LeaveApplication> = emptyList(),
    val pagination: PaginationInfo? = null,
    val selectedStatus: String? = null,
    val errorMessage: String? = null,
    val summary: LeaveSummary? = null
)

/**
 * UI State for Leave Calendar Screen
 */
data class LeaveCalendarUiState(
    val isLoading: Boolean = false,
    val calendarData: LeaveCalendarData? = null,
    val errorMessage: String? = null
)

// =============================================================================
// EVENTS
// =============================================================================

sealed class LeaveEvent {
    data class ShowError(val message: String) : LeaveEvent()
    data class ShowSuccess(val message: String) : LeaveEvent()
    data class NavigateToHistory(
        val applicationId: Int,
        val referenceNumber: String? = null,
        val pendingWith: List<String> = emptyList(),
        val autoApproved: Boolean = false,
        val workflowName: String? = null,
        val currentStep: Int? = null,
        val totalSteps: Int? = null
    ) : LeaveEvent()
    data class DraftSaved(val draftId: Int, val message: String) : LeaveEvent()
}

// =============================================================================
// VIEWMODEL
// =============================================================================

class LeaveViewModel(
    private val repo: LeaveRepository
) : ViewModel() {

    private val _applyLeaveUiState = MutableStateFlow(ApplyLeaveUiState())
    val applyLeaveUiState: StateFlow<ApplyLeaveUiState> = _applyLeaveUiState.asStateFlow()

    private val _historyUiState = MutableStateFlow(LeaveHistoryUiState())
    val historyUiState: StateFlow<LeaveHistoryUiState> = _historyUiState.asStateFlow()

    private val _calendarUiState = MutableStateFlow(LeaveCalendarUiState())
    val calendarUiState: StateFlow<LeaveCalendarUiState> = _calendarUiState.asStateFlow()

    private val _events = MutableSharedFlow<LeaveEvent>()
    val events: SharedFlow<LeaveEvent> = _events.asSharedFlow()

    init {
        loadLeaveTypes()
        loadLeaveHistory()
        loadLeaveSummary()
    }

    // =========================================================================
    // LOAD LEAVE TYPES
    // =========================================================================

    fun loadLeaveTypes() {
        viewModelScope.launch {
            try {
                _applyLeaveUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.getLeaveTypes()) {
                    is LeaveTypesOutcome.Success -> {
                        _applyLeaveUiState.update {
                            it.copy(
                                isLoading = false,
                                leaveTypes = outcome.leaveTypes,
                                errorMessage = null
                            )
                        }
                        Log.d("LeaveViewModel", "Loaded ${outcome.leaveTypes.size} leave types")
                    }

                    is LeaveTypesOutcome.Error -> {
                        _applyLeaveUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(LeaveEvent.ShowError(outcome.message))
                        Log.e("LeaveViewModel", "Error loading leave types: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _applyLeaveUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(LeaveEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("LeaveViewModel", "Exception loading leave types", e)
            }
        }
    }

    // =========================================================================
    // SELECT LEAVE TYPE
    // =========================================================================

    fun selectLeaveType(leaveType: LeaveType) {
        _applyLeaveUiState.update { it.copy(selectedLeaveType = leaveType) }
    }

    // =========================================================================
    // APPLY LEAVE - UPDATED WITH HALF-DAY SUPPORT
    // =========================================================================

    fun applyLeave(
        leaveTypeId: Int,
        startDate: String,
        endDate: String,
        leaveReason: String,
        isHalfDayStart: Boolean = false,
        isHalfDayEnd: Boolean = false,
        halfDayType: String? = null,
        alternateContact: String? = null,
        emergencyContact: String? = null,
        taskDependedOnYou: Boolean = false,
        dependencyHandledBy: String? = null,
        handoverNotes: String? = null,
        supportingDocumentFile: File? = null
    ) {
        viewModelScope.launch {
            try {
                _applyLeaveUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.applyLeave(
                    leaveTypeId = leaveTypeId,
                    startDate = startDate,
                    endDate = endDate,
                    leaveReason = leaveReason,
                    isHalfDayStart = isHalfDayStart,
                    isHalfDayEnd = isHalfDayEnd,
                    halfDayType = halfDayType,
                    alternateContact = alternateContact,
                    emergencyContact = emergencyContact,
                    taskDependedOnYou = taskDependedOnYou,
                    dependencyHandledBy = dependencyHandledBy,
                    handoverNotes = handoverNotes,
                    supportingDocumentFile = supportingDocumentFile
                )) {
                    is ApplyLeaveOutcome.Success -> {
                        _applyLeaveUiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = outcome.message,
                                errorMessage = null
                            )
                        }
                        _events.emit(LeaveEvent.ShowSuccess(outcome.message))
                        _events.emit(LeaveEvent.NavigateToHistory(
                            applicationId = outcome.applicationId,
                            referenceNumber = outcome.referenceNumber,
                            pendingWith = outcome.pendingWith,
                            autoApproved = outcome.autoApproved,
                            workflowName = outcome.workflowName,
                            currentStep = outcome.currentStep,
                            totalSteps = outcome.totalSteps
                        ))

                        // Refresh leave history
                        loadLeaveHistory()
                        loadLeaveSummary()

                        Log.d("LeaveViewModel", "Leave applied successfully: ID=${outcome.applicationId}")
                        Log.d("LeaveViewModel", "Reference: ${outcome.referenceNumber}, Workflow: ${outcome.workflowName}")
                        Log.d("LeaveViewModel", "Total days: ${outcome.totalDays}, Effective days: ${outcome.effectiveDays}")
                        Log.d("LeaveViewModel", "Pending with: ${outcome.pendingWith.joinToString()}")
                    }

                    is ApplyLeaveOutcome.Error -> {
                        _applyLeaveUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(LeaveEvent.ShowError(outcome.message))
                        Log.e("LeaveViewModel", "Error applying leave: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _applyLeaveUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(LeaveEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("LeaveViewModel", "Exception applying leave", e)
            }
        }
    }

    // =========================================================================
    // LOAD LEAVE HISTORY
    // =========================================================================

    fun loadLeaveHistory(
        status: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1
    ) {
        viewModelScope.launch {
            try {
                _historyUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.getLeaveApplications(
                    status = status,
                    startDate = startDate,
                    endDate = endDate,
                    page = page,
                    pageSize = 10
                )) {
                    is LeaveApplicationsOutcome.Success -> {
                        _historyUiState.update {
                            it.copy(
                                isLoading = false,
                                applications = outcome.applications,
                                pagination = outcome.pagination,
                                selectedStatus = status,
                                errorMessage = null
                            )
                        }
                        Log.d("LeaveViewModel", "Loaded ${outcome.applications.size} leave applications")
                    }

                    is LeaveApplicationsOutcome.Error -> {
                        _historyUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(LeaveEvent.ShowError(outcome.message))
                        Log.e("LeaveViewModel", "Error loading leave history: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _historyUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(LeaveEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("LeaveViewModel", "Exception loading leave history", e)
            }
        }
    }

    // =========================================================================
    // LOAD LEAVE SUMMARY
    // =========================================================================

    fun loadLeaveSummary(year: Int? = null) {
        viewModelScope.launch {
            try {
                when (val outcome = repo.getLeaveSummary(year)) {
                    is LeaveSummaryOutcome.Success -> {
                        _historyUiState.update { it.copy(summary = outcome.summary) }
                        Log.d("LeaveViewModel", "Loaded leave summary")
                    }

                    is LeaveSummaryOutcome.Error -> {
                        Log.e("LeaveViewModel", "Error loading leave summary: ${outcome.message}")
                        // Don't show error for summary - it's not critical
                    }
                }
            } catch (e: Exception) {
                Log.e("LeaveViewModel", "Exception loading leave summary", e)
            }
        }
    }

    // =========================================================================
    // FILTER BY STATUS
    // =========================================================================

    fun filterByStatus(status: String?) {
        loadLeaveHistory(status = status)
    }

    // =========================================================================
    // PAGINATION
    // =========================================================================

    fun loadNextPage() {
        val currentPagination = _historyUiState.value.pagination
        if (currentPagination != null && currentPagination.currentPage < currentPagination.totalPages) {
            loadLeaveHistory(
                status = _historyUiState.value.selectedStatus,
                page = currentPagination.currentPage + 1
            )
        }
    }

    fun loadPreviousPage() {
        val currentPagination = _historyUiState.value.pagination
        if (currentPagination != null && currentPagination.currentPage > 1) {
            loadLeaveHistory(
                status = _historyUiState.value.selectedStatus,
                page = currentPagination.currentPage - 1
            )
        }
    }

    // =========================================================================
    // REFRESH
    // =========================================================================

    fun refresh() {
        loadLeaveTypes()
        loadLeaveHistory(status = _historyUiState.value.selectedStatus, page = 1)
        loadLeaveSummary()
    }

    // =========================================================================
    // WITHDRAW LEAVE
    // =========================================================================

    fun withdrawLeave(applicationId: Int, withdrawalReason: String) {
        viewModelScope.launch {
            try {
                _historyUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.withdrawLeave(applicationId, withdrawalReason)) {
                    is WithdrawLeaveOutcome.Success -> {
                        _historyUiState.update { it.copy(isLoading = false) }
                        _events.emit(LeaveEvent.ShowSuccess(outcome.message))

                        // Refresh leave history
                        loadLeaveHistory(status = _historyUiState.value.selectedStatus)
                        loadLeaveSummary()

                        Log.d("LeaveViewModel", "Leave withdrawn successfully")
                    }

                    is WithdrawLeaveOutcome.Error -> {
                        _historyUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(LeaveEvent.ShowError(outcome.message))
                        Log.e("LeaveViewModel", "Error withdrawing leave: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _historyUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(LeaveEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("LeaveViewModel", "Exception withdrawing leave", e)
            }
        }
    }

    // =========================================================================
    // CANCEL LEAVE
    // =========================================================================

    fun cancelLeave(applicationId: Int, cancellationReason: String) {
        viewModelScope.launch {
            try {
                _historyUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.cancelLeave(applicationId, cancellationReason)) {
                    is CancelLeaveOutcome.Success -> {
                        _historyUiState.update { it.copy(isLoading = false) }
                        _events.emit(LeaveEvent.ShowSuccess(outcome.message))

                        // Refresh leave history
                        loadLeaveHistory(status = _historyUiState.value.selectedStatus)
                        loadLeaveSummary()

                        Log.d("LeaveViewModel", "Leave cancelled successfully")
                    }

                    is CancelLeaveOutcome.Error -> {
                        _historyUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(LeaveEvent.ShowError(outcome.message))
                        Log.e("LeaveViewModel", "Error cancelling leave: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _historyUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(LeaveEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("LeaveViewModel", "Exception cancelling leave", e)
            }
        }
    }

    // =========================================================================
    // LOAD LEAVE CALENDAR
    // =========================================================================

    fun loadLeaveCalendar(month: Int, year: Int) {
        viewModelScope.launch {
            try {
                _calendarUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.getLeaveCalendar(month, year)) {
                    is LeaveCalendarOutcome.Success -> {
                        _calendarUiState.update {
                            it.copy(
                                isLoading = false,
                                calendarData = outcome.data,
                                errorMessage = null
                            )
                        }
                        Log.d("LeaveViewModel", "Loaded leave calendar for $month/$year")
                    }

                    is LeaveCalendarOutcome.Error -> {
                        _calendarUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(LeaveEvent.ShowError(outcome.message))
                        Log.e("LeaveViewModel", "Error loading leave calendar: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _calendarUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(LeaveEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("LeaveViewModel", "Exception loading leave calendar", e)
            }
        }
    }

    // =========================================================================
    // SAVE DRAFT
    // =========================================================================

    fun saveDraftLeave(
        leaveTypeId: Int,
        startDate: String,
        endDate: String,
        leaveReason: String?,
        isHalfDayStart: Boolean = false,
        isHalfDayEnd: Boolean = false,
        halfDayType: String? = null,
        alternateContact: String? = null,
        emergencyContact: String? = null,
        taskDependedOnYou: Boolean = false,
        dependencyHandledBy: String? = null,
        handoverNotes: String? = null
    ) {
        viewModelScope.launch {
            try {
                _applyLeaveUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.saveDraftLeave(
                    leaveTypeId = leaveTypeId,
                    startDate = startDate,
                    endDate = endDate,
                    leaveReason = leaveReason,
                    isHalfDayStart = isHalfDayStart,
                    isHalfDayEnd = isHalfDayEnd,
                    halfDayType = halfDayType,
                    alternateContact = alternateContact,
                    emergencyContact = emergencyContact,
                    taskDependedOnYou = taskDependedOnYou,
                    dependencyHandledBy = dependencyHandledBy,
                    handoverNotes = handoverNotes
                )) {
                    is SaveDraftOutcome.Success -> {
                        _applyLeaveUiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = outcome.message,
                                errorMessage = null
                            )
                        }
                        _events.emit(LeaveEvent.DraftSaved(outcome.draftId, outcome.message))
                        Log.d("LeaveViewModel", "Draft saved: ID=${outcome.draftId}")
                    }

                    is SaveDraftOutcome.Error -> {
                        _applyLeaveUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(LeaveEvent.ShowError(outcome.message))
                        Log.e("LeaveViewModel", "Error saving draft: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _applyLeaveUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(LeaveEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("LeaveViewModel", "Exception saving draft", e)
            }
        }
    }

    // =========================================================================
    // SUBMIT DRAFT
    // =========================================================================

    fun submitDraftLeave(applicationId: Int) {
        viewModelScope.launch {
            try {
                _applyLeaveUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.submitDraftLeave(applicationId)) {
                    is SubmitDraftOutcome.Success -> {
                        _applyLeaveUiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = outcome.message,
                                errorMessage = null
                            )
                        }
                        _events.emit(LeaveEvent.ShowSuccess(outcome.message))
                        _events.emit(LeaveEvent.NavigateToHistory(applicationId))

                        // Refresh leave history
                        loadLeaveHistory()
                        loadLeaveSummary()

                        Log.d("LeaveViewModel", "Draft submitted: ID=$applicationId")
                    }

                    is SubmitDraftOutcome.Error -> {
                        _applyLeaveUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(LeaveEvent.ShowError(outcome.message))
                        Log.e("LeaveViewModel", "Error submitting draft: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _applyLeaveUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(LeaveEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("LeaveViewModel", "Exception submitting draft", e)
            }
        }
    }
}