package com.hrms.jeejateamozy.feature.workreport.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.core.network.WorkReport
import com.hrms.jeejateamozy.feature.workreport.data.CreateWorkReportOutcome
import com.hrms.jeejateamozy.feature.workreport.data.WorkReportListOutcome
import com.hrms.jeejateamozy.feature.workreport.domain.usecase.WorkReportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class WorkReportUiState(
    // Tab selection
    val selectedTab: WorkReportTab = WorkReportTab.HISTORY,  // ⭐ CHANGED: Default to HISTORY

    // Date selection
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedMonth: YearMonth = YearMonth.now(),

    // Add New form
    val workDescription: String = "",
    val attachmentUris: List<Uri> = emptyList(),
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitError: String? = null,

    // Reports today info
    val reportsToday: Int? = null,
    val remainingReportsToday: Int? = null,

    // History list
    val isLoadingHistory: Boolean = false,
    val historyReports: List<WorkReport> = emptyList(),
    val historyError: String? = null,
    val totalReports: Int = 0,

    // Detail view
    val selectedReport: WorkReport? = null,
    val showReportDetail: Boolean = false
)

enum class WorkReportTab {
    ADD_NEW,
    HISTORY
}

class WorkReportViewModel(
    private val useCase: WorkReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkReportUiState())
    val uiState: StateFlow<WorkReportUiState> = _uiState.asStateFlow()

    init {
        // Load current month's history by default
        loadWorkReportsForCurrentMonth()
    }

    // ==================== Tab Selection ====================

    fun onTabSelected(tab: WorkReportTab) {
        _uiState.update { it.copy(selectedTab = tab) }

        if (tab == WorkReportTab.HISTORY) {
            loadWorkReportsForSelectedMonth()
        }
    }

    // ==================== Date Selection ====================

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onMonthChanged(yearMonth: YearMonth) {
        _uiState.update { it.copy(selectedMonth = yearMonth) }
        loadWorkReportsForSelectedMonth()
    }

    fun onPreviousMonth() {
        val currentMonth = _uiState.value.selectedMonth
        val previousMonth = currentMonth.minusMonths(1)
        onMonthChanged(previousMonth)
    }

    fun onNextMonth() {
        val currentMonth = _uiState.value.selectedMonth
        val nextMonth = currentMonth.plusMonths(1)
        onMonthChanged(nextMonth)
    }

    // ==================== Add New Work Report ====================

    fun onWorkDescriptionChanged(description: String) {
        _uiState.update { it.copy(workDescription = description) }
    }

    fun onAddAttachment(uri: Uri) {
        val currentAttachments = _uiState.value.attachmentUris

        if (currentAttachments.size >= 5) {
            _uiState.update {
                it.copy(submitError = "Maximum 5 attachments allowed")
            }
            return
        }

        _uiState.update {
            it.copy(
                attachmentUris = currentAttachments + uri,
                submitError = null
            )
        }
    }

    fun onRemoveAttachment(uri: Uri) {
        val currentAttachments = _uiState.value.attachmentUris
        _uiState.update {
            it.copy(attachmentUris = currentAttachments.filter { it != uri })
        }
    }

    fun onClearAttachments() {
        _uiState.update { it.copy(attachmentUris = emptyList()) }
    }

    fun onSubmitWorkReport() {
        val description = _uiState.value.workDescription.trim()
        val attachments = _uiState.value.attachmentUris

        // Clear previous errors
        _uiState.update { it.copy(submitError = null) }

        // Validate input
        if (description.isEmpty()) {
            _uiState.update {
                it.copy(submitError = "Work description cannot be empty")
            }
            return
        }

        if (description.length < 10) {
            _uiState.update {
                it.copy(submitError = "Work description must be at least 10 characters")
            }
            return
        }

        // Submit work report
        _uiState.update { it.copy(isSubmitting = true, submitError = null) }

        viewModelScope.launch {
            try {
                val result = useCase.createWorkReport(description, attachments)

                when (result) {
                    is CreateWorkReportOutcome.Success -> {
                        Log.d("WORK_REPORT_VM", "Work report created successfully")

                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                submitSuccess = true,
                                submitError = null,
                                // Clear form
                                workDescription = "",
                                attachmentUris = emptyList(),
                                // Update reports today info
                                reportsToday = result.reportsToday,
                                remainingReportsToday = result.remainingReportsToday,
                                // Switch to history tab
                                selectedTab = WorkReportTab.HISTORY
                            )
                        }

                        // Reload history to show the new report
                        loadWorkReportsForCurrentMonth()
                    }

                    is CreateWorkReportOutcome.Error -> {
                        Log.e("WORK_REPORT_VM", "Failed to create work report: ${result.message}")

                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                submitSuccess = false,
                                submitError = result.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WORK_REPORT_VM", "Error creating work report", e)

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitSuccess = false,
                        submitError = e.message ?: "An error occurred"
                    )
                }
            }
        }
    }

    fun onDismissSubmitSuccess() {
        _uiState.update {
            it.copy(submitSuccess = false)
        }
    }

    fun onDismissSubmitError() {
        _uiState.update {
            it.copy(submitError = null)
        }
    }

    // ==================== History ====================

    private fun loadWorkReportsForCurrentMonth() {
        val now = LocalDate.now()
        loadWorkReports(now.monthValue, now.year)
    }

    private fun loadWorkReportsForSelectedMonth() {
        val selectedMonth = _uiState.value.selectedMonth
        loadWorkReports(selectedMonth.monthValue, selectedMonth.year)
    }

    private fun loadWorkReports(month: Int, year: Int) {
        _uiState.update {
            it.copy(
                isLoadingHistory = true,
                historyError = null
            )
        }

        viewModelScope.launch {
            try {
                val result = useCase.getWorkReports(month, year)

                when (result) {
                    is WorkReportListOutcome.Success -> {
                        Log.d("WORK_REPORT_VM", "Loaded ${result.reports.size} work reports")

                        _uiState.update {
                            it.copy(
                                isLoadingHistory = false,
                                historyReports = result.reports,
                                historyError = null,
                                totalReports = result.totalReports
                            )
                        }
                    }

                    is WorkReportListOutcome.Error -> {
                        Log.e("WORK_REPORT_VM", "Failed to load work reports: ${result.message}")

                        _uiState.update {
                            it.copy(
                                isLoadingHistory = false,
                                historyReports = emptyList(),
                                historyError = result.message,
                                totalReports = 0
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WORK_REPORT_VM", "Error loading work reports", e)

                _uiState.update {
                    it.copy(
                        isLoadingHistory = false,
                        historyReports = emptyList(),
                        historyError = e.message ?: "An error occurred",
                        totalReports = 0
                    )
                }
            }
        }
    }

    fun onRefreshHistory() {
        loadWorkReportsForSelectedMonth()
    }

    // ==================== Report Detail ====================

    fun onReportClicked(report: WorkReport) {
        _uiState.update {
            it.copy(
                selectedReport = report,
                showReportDetail = true
            )
        }
    }

    fun onDismissReportDetail() {
        _uiState.update {
            it.copy(
                selectedReport = null,
                showReportDetail = false
            )
        }
    }

    // ==================== Download Report ====================

    fun onDownloadReport(report: WorkReport) {
        // TODO: Implement download functionality
        // This would typically involve downloading attachments or generating a PDF
        Log.d("WORK_REPORT_VM", "Download report: ${report.id}")
    }
}