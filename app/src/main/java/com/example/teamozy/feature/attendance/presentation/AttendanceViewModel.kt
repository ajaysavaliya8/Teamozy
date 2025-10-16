package com.example.teamozy.feature.attendance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamozy.feature.attendance.data.AttendanceOutcome
import com.example.teamozy.feature.attendance.data.AttendanceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AttendanceViewModel
 * Handles checking attendance status and UI state management
 */
class AttendanceViewModel(
    private val repo: AttendanceRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AttendanceUiState())
    val ui: StateFlow<AttendanceUiState> = _ui.asStateFlow()

    /**
     * Refresh the current attendance status from server
     */
    fun refreshStatus() {
        if (_ui.value.isRefreshing) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                isRefreshing = true,
                errorMessage = null
            )

            when (val outcome = repo.getStatus()) {
                is AttendanceOutcome.Success -> {
                    _ui.value = _ui.value.copy(
                        isRefreshing = false,
                        currentState = outcome.currentState,
                        faceRecognitionEnabled = outcome.faceRecognitionEnabled,
                        faceVector = outcome.faceVector,
                        minimumQualityScore = outcome.minimumQualityScore,
                        statusMessage = outcome.message,
                        attendanceStatus = outcome.attendanceStatus,
                        isComplete = outcome.isComplete,
                        errorMessage = null
                    )
                }

                is AttendanceOutcome.Error -> {
                    _ui.value = _ui.value.copy(
                        isRefreshing = false,
                        errorMessage = outcome.message
                    )
                    autoClearMessages()
                }
            }
        }
    }

    /**
     * Clear success and error messages
     */
    fun clearMessages() {
        _ui.value = _ui.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    /**
     * Auto-clear messages after a delay
     */
    private fun autoClearMessages() {
        viewModelScope.launch {
            delay(4500)
            clearMessages()
        }
    }

    /**
     * Helper to determine if user can check in based on current state
     */
    fun canCheckIn(): Boolean {
        return _ui.value.currentState == "CHECK_IN_NEEDED"
    }

    /**
     * Helper to determine if user can check out based on current state
     */
    fun canCheckOut(): Boolean {
        return _ui.value.currentState == "CHECK_OUT_NEEDED"
    }

    /**
     * Helper to determine if attendance is complete for today
     */
    fun isComplete(): Boolean {
        return _ui.value.currentState == "COMPLETED"
    }
}

/**
 * UI State for Attendance
 */
data class AttendanceUiState(
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Status data from API
    val currentState: String = "CHECK_IN_NEEDED", // "CHECK_IN_NEEDED" | "CHECK_OUT_NEEDED" | "COMPLETED"
    val faceRecognitionEnabled: Boolean = false,
    val faceVector: String? = null,
    val minimumQualityScore: Float = 0.57f,
    val statusMessage: String = "",
    val attendanceStatus: String? = null,
    val isComplete: Boolean? = null
)