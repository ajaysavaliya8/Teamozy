package com.hrms.jeejateamozy.feature.leave.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.core.network.*
import com.hrms.jeejateamozy.feature.leave.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

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

sealed class LeaveEvent {
    data class ShowError(val message: String) : LeaveEvent()
    data class ShowSuccess(val message: String) : LeaveEvent()
    data class NavigateToHistory(val applicationId: Int) : LeaveEvent()
}

class LeaveViewModel(
    private val repo: LeaveRepository
) : ViewModel() {

    private val _applyLeaveUiState = MutableStateFlow(ApplyLeaveUiState())
    val applyLeaveUiState: StateFlow<ApplyLeaveUiState> = _applyLeaveUiState.asStateFlow()

    private val _historyUiState = MutableStateFlow(LeaveHistoryUiState())
    val historyUiState: StateFlow<LeaveHistoryUiState> = _historyUiState.asStateFlow()

    private val _events = MutableSharedFlow<LeaveEvent>()
    val events: SharedFlow<LeaveEvent> = _events.asSharedFlow()

    init {
        loadLeaveTypes()
        loadLeaveHistory()
        loadLeaveSummary()
    }

    /**
     * Load available leave types
     */
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

    /**
     * Select leave type
     */
    fun selectLeaveType(leaveType: LeaveType) {
        _applyLeaveUiState.update { it.copy(selectedLeaveType = leaveType) }
    }

    /**
     * Apply for leave
     */
    fun applyLeave(
        leaveTypeId: Int,
        startDate: String,
        endDate: String,
        leaveReason: String,
        alternateContact: String?,
        taskDependedOnYou: Boolean,
        dependencyHandledBy: String?,
        supportingDocumentFile: File?
    ) {
        viewModelScope.launch {
            try {
                _applyLeaveUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.applyLeave(
                    leaveTypeId = leaveTypeId,
                    startDate = startDate,
                    endDate = endDate,
                    leaveReason = leaveReason,
                    alternateContact = alternateContact,
                    taskDependedOnYou = taskDependedOnYou,
                    dependencyHandledBy = dependencyHandledBy,
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
                        _events.emit(LeaveEvent.NavigateToHistory(outcome.applicationId))

                        // Refresh leave history
                        loadLeaveHistory()
                        loadLeaveSummary()

                        Log.d("LeaveViewModel", "Leave applied successfully: ID=${outcome.applicationId}")
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

    /**
     * Load leave history
     */
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

    /**
     * Load leave summary
     */
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

    /**
     * Filter by status
     */
    fun filterByStatus(status: String?) {
        loadLeaveHistory(status = status)
    }

    /**
     * Load next page
     */
    fun loadNextPage() {
        val currentPagination = _historyUiState.value.pagination
        if (currentPagination != null && currentPagination.currentPage < currentPagination.totalPages) {
            loadLeaveHistory(
                status = _historyUiState.value.selectedStatus,
                page = currentPagination.currentPage + 1
            )
        }
    }

    /**
     * Load previous page
     */
    fun loadPreviousPage() {
        val currentPagination = _historyUiState.value.pagination
        if (currentPagination != null && currentPagination.currentPage > 1) {
            loadLeaveHistory(
                status = _historyUiState.value.selectedStatus,
                page = currentPagination.currentPage - 1
            )
        }
    }

    /**
     * Refresh data
     */
    fun refresh() {
        loadLeaveTypes()
        loadLeaveHistory(status = _historyUiState.value.selectedStatus, page = 1)
        loadLeaveSummary()
    }

    /**
     * Withdraw leave
     */
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
}