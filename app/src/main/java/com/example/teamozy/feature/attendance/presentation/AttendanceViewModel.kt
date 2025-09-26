package com.example.teamozy.feature.attendance.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamozy.core.utils.LocationHelper
import com.example.teamozy.core.utils.LocationResult
import com.example.teamozy.feature.attendance.data.AttendanceOutcome
import com.example.teamozy.feature.attendance.data.AttendanceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Adds:
 * - refreshStatus() with isRefreshing toggle
 * - violation bottom-sheet flow (token + message + submitReason)
 * - single source of truth for button: canCheckIn
 */
class AttendanceViewModel(
    private val repo: AttendanceRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AttendanceUiState())
    val ui: StateFlow<AttendanceUiState> = _ui.asStateFlow()

    fun refreshStatus() {
        if (_ui.value.isRefreshing) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isRefreshing = true, errorMessage = null)
            when (val out = repo.getStatus()) {
                is AttendanceOutcome.Success -> {
                    _ui.value = _ui.value.copy(
                        isRefreshing = false,
                        canCheckIn = out.canCheckIn
                    )
                }
                is AttendanceOutcome.Error -> {
                    _ui.value = _ui.value.copy(
                        isRefreshing = false,
                        errorMessage = out.message
                    )
                    autoClearMessages()
                }
                is AttendanceOutcome.Violation -> {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        // open the sheet and populate its content
                        showViolationSheet = true,
                        violationToken = out.token,
                        violationMessage = out.message,
                        // clear any toasts/banners
                        errorMessage = null,
                        successMessage = null
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _ui.value = _ui.value.copy(successMessage = null, errorMessage = null)
    }

    fun checkIn(context: Context) {
        if (_ui.value.isLoading) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, errorMessage = null, successMessage = null)

            when (val loc = LocationHelper(context).getCurrentLocation()) {
                is LocationResult.Error -> postError(loc.message)
                is LocationResult.Success -> {
                    _ui.value = _ui.value.copy(lastAccuracyMeters = loc.accuracy)
                    when (val out = repo.checkIn(loc.latitude, loc.longitude, loc.accuracy , faceVerify = _ui.value.faceVerifyEnabled)) {
                        is AttendanceOutcome.Success -> {
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                canCheckIn = out.canCheckIn,
                                successMessage = "Checked in successfully."
                            )
                            autoClearMessages()
                        }
                        is AttendanceOutcome.Error -> postError(out.message)
                        is AttendanceOutcome.Violation -> {
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                // open the sheet and populate its content
                                showViolationSheet = true,
                                violationToken = out.token,
                                violationMessage = out.message,
                                // clear any toasts/banners
                                errorMessage = null,
                                successMessage = null
                            )
                        }
                    }
                }
            }
        }
    }

    fun checkOut(context: Context) {
        if (_ui.value.isLoading) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, errorMessage = null, successMessage = null)

            when (val loc = LocationHelper(context).getCurrentLocation()) {
                is LocationResult.Error -> postError(loc.message)
                is LocationResult.Success -> {
                    _ui.value = _ui.value.copy(lastAccuracyMeters = loc.accuracy)
                    when (val out = repo.checkOut(loc.latitude, loc.longitude, loc.accuracy , faceVerify = _ui.value.faceVerifyEnabled )) {
                        is AttendanceOutcome.Success -> {
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                canCheckIn = out.canCheckIn,
                                successMessage = "Checked out successfully."
                            )
                            autoClearMessages()
                        }
                        is AttendanceOutcome.Error -> postError(out.message)
                        is AttendanceOutcome.Violation -> {
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                // open the sheet and populate its content
                                showViolationSheet = true,
                                violationToken = out.token,
                                violationMessage = out.message,
                                // clear any toasts/banners
                                errorMessage = null,
                                successMessage = null
                            )
                        }
                    }
                }
            }
        }
    }

    fun submitViolation(reason: String) {
        val token = _ui.value.violationToken ?: return
        if (reason.isBlank() || _ui.value.isSubmittingViolation) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(isSubmittingViolation = true, errorMessage = null)
            when (val out = repo.submitViolation(token, reason)) {
                is AttendanceOutcome.Success -> {
                    _ui.value = _ui.value.copy(
                        isSubmittingViolation = false,
                        showViolationSheet = false,
                        violationToken = null,
                        violationMessage = null,
                        canCheckIn = out.canCheckIn,
                        successMessage = "Submitted reason. You're good to go."
                    )
                    autoClearMessages()
                }
                is AttendanceOutcome.Error -> {
                    _ui.value = _ui.value.copy(
                        isSubmittingViolation = false,
                        errorMessage = out.message
                    )
                    autoClearMessages()
                }
                is AttendanceOutcome.Violation -> {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        // open the sheet and populate its content
                        showViolationSheet = true,
                        violationToken = out.token,
                        violationMessage = out.message,
                        // clear any toasts/banners
                        errorMessage = null,
                        successMessage = null
                    )
                }
            }
        }
    }

    fun dismissViolationSheet() {
        _ui.value = _ui.value.copy(
            showViolationSheet = false,
            violationToken = null,
            violationMessage = null
        )
    }

    private fun postError(msg: String) {
        _ui.value = _ui.value.copy(isLoading = false, errorMessage = msg)
        autoClearMessages()
    }

    private fun autoClearMessages() {
        viewModelScope.launch {
            delay(4500)
            clearMessages()
        }
    }

    fun setFaceVerifyEnabled(enabled: Boolean) {
        _ui.value = _ui.value.copy(faceVerifyEnabled = enabled)
    }
}

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val canCheckIn: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastAccuracyMeters: Float? = null,

    val faceVerifyEnabled: Boolean = false,

    // Violation flow
    val showViolationSheet: Boolean = false,
    val violationToken: String? = null,
    val violationMessage: String? = null,
    val isSubmittingViolation: Boolean = false
)

