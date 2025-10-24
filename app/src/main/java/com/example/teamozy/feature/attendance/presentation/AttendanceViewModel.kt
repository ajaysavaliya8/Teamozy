package com.example.teamozy.feature.attendance.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamozy.core.utils.LocationHelper
import com.example.teamozy.core.utils.LocationResult
import com.example.teamozy.feature.attendance.data.AttendanceOutcome
import com.example.teamozy.feature.attendance.data.AttendanceRepository
import com.example.teamozy.feature.attendance.data.CheckInOutcome
import com.example.teamozy.feature.attendance.data.CheckOutOutcome
import com.example.teamozy.feature.attendance.data.SignatureOutcome
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AttendanceViewModel
 * Handles check-in flow with face verification and violation reasons
 */
class AttendanceViewModel(
    private val repo: AttendanceRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AttendanceUiState())
    val ui: StateFlow<AttendanceUiState> = _ui.asStateFlow()

    /**
     * Refresh the current attendance status from server
     */
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
     * Start check-in process
     * Gets location and makes initial check-in API call
     */
    fun startCheckIn(context: Context) {
        if (_ui.value.isLoading) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            // Get current location
            when (val loc = LocationHelper(context).getCurrentLocation()) {
                is LocationResult.Error -> {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        errorMessage = loc.message
                    )
                    autoClearMessages()
                }

                is LocationResult.Success -> {
                    // Call initial check-in API
                    when (val outcome = repo.checkIn(
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    )) {
                        is CheckInOutcome.RequiresFaceVerification -> {
                            // Face verification is required
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                checkInTToken = outcome.tToken,
                                checkInMinimumQualityScore = outcome.minimumQualityScore,
                                checkInIsLate = outcome.isLate,
                                checkInIsOutOfRange = outcome.isOutOfRange,
                                checkInLateReasonRequired = outcome.lateReasonRequired,
                                checkInOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                                checkInMessage = outcome.message,
                                showFaceVerification = true
                            )
                        }

                        is CheckInOutcome.RequiresReasons -> {
                            // Reasons required (late or out of range), or no verification needed
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                checkInTToken = outcome.tToken,
                                checkInIsLate = outcome.isLate,
                                checkInIsOutOfRange = outcome.isOutOfRange,
                                checkInLateReasonRequired = outcome.lateReasonRequired,
                                checkInOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                                checkInMessage = outcome.message,
                                showReasonDialog = true
                            )
                        }

                        is CheckInOutcome.Success -> {
                            // Directly successful (shouldn't happen with new flow)
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                successMessage = outcome.message
                            )
                            autoClearMessages()
                            // Refresh status after success
                            delay(1000)
                            refreshStatus()
                        }

                        is CheckInOutcome.Error -> {
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                            autoClearMessages()
                        }
                    }
                }
            }
        }
    }

    /**
     * Start check-out process
     * Gets location and makes initial check-out API call
     */
    fun startCheckOut(context: Context) {
        if (_ui.value.isLoading) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            // Get current location
            when (val loc = LocationHelper(context).getCurrentLocation()) {
                is LocationResult.Error -> {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        errorMessage = loc.message
                    )
                    autoClearMessages()
                }

                is LocationResult.Success -> {
                    // Call initial check-out API
                    when (val outcome = repo.checkOut(
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    )) {
                        is CheckOutOutcome.RequiresFaceVerification -> {
                            // Face verification is required
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                checkOutTToken = outcome.tToken,
                                checkOutMinimumQualityScore = outcome.minimumQualityScore,
                                checkOutWorkHours = outcome.workHours,
                                checkOutIsEarly = outcome.isEarly,
                                checkOutIsOutOfRange = outcome.isOutOfRange,
                                checkOutEarlyReasonRequired = outcome.earlyReasonRequired,
                                checkOutOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                                checkOutMessage = outcome.message,
                                showFaceVerification = true
                            )
                        }

                        is CheckOutOutcome.RequiresReasons -> {
                            // Reasons required (early or out of range), or no verification needed
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                checkOutTToken = outcome.tToken,
                                checkOutWorkHours = outcome.workHours,
                                checkOutIsEarly = outcome.isEarly,
                                checkOutIsOutOfRange = outcome.isOutOfRange,
                                checkOutEarlyReasonRequired = outcome.earlyReasonRequired,
                                checkOutOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                                checkOutMessage = outcome.message,
                                showReasonDialog = true
                            )
                        }

                        is CheckOutOutcome.Success -> {
                            // Directly successful (shouldn't happen with new flow)
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                successMessage = outcome.message
                            )
                            autoClearMessages()
                            // Refresh status after success
                            delay(1000)
                            refreshStatus()
                        }

                        is CheckOutOutcome.Error -> {
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                            autoClearMessages()
                        }
                    }
                }
            }
        }
    }

    /**
     * Called after face verification completes (for check-in OR check-out)
     */
    fun onFaceVerificationComplete(qualityScore: Float, verified: Boolean) {
        _ui.value = _ui.value.copy(
            showFaceVerification = false,
            faceVerificationQualityScore = qualityScore,
            faceVerificationSuccess = verified
        )

        // Determine if this was for check-in or check-out
        if (_ui.value.checkInTToken != null) {
            // Check-in flow
            if (_ui.value.checkInLateReasonRequired || _ui.value.checkInOutOfRangeReasonRequired) {
                _ui.value = _ui.value.copy(showReasonDialog = true)
            } else {
                completeCheckIn(null, null)
            }
        } else if (_ui.value.checkOutTToken != null) {
            // Check-out flow
            if (_ui.value.checkOutEarlyReasonRequired || _ui.value.checkOutOutOfRangeReasonRequired) {
                _ui.value = _ui.value.copy(showReasonDialog = true)
            } else {
                completeCheckOut(null, null)
            }
        }
    }

    /**
     * Called when face verification is cancelled
     */
    fun onFaceVerificationCancelled() {
        _ui.value = _ui.value.copy(
            showFaceVerification = false,
            checkInTToken = null,
            checkInMessage = null,
            checkOutTToken = null,
            checkOutMessage = null
        )
    }

    /**
     * Called when user dismisses reason dialog without submitting
     */
    fun onReasonDialogDismissed() {
        _ui.value = _ui.value.copy(
            showReasonDialog = false,
            checkInTToken = null,
            checkInMessage = null,
            checkOutTToken = null,
            checkOutMessage = null
        )
    }

    /**
     * Complete check-in with signature API call
     */
    fun completeCheckIn(lateReason: String?, outOfRangeReason: String?) {
        val tToken = _ui.value.checkInTToken
        if (tToken == null) {
            _ui.value = _ui.value.copy(
                errorMessage = "Invalid check-in session"
            )
            autoClearMessages()
            return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                isLoading = true,
                showReasonDialog = false,
                errorMessage = null
            )

            when (val outcome = repo.checkInSignature(
                tToken = tToken,
                faceRecognitionQualityScore = _ui.value.faceVerificationQualityScore,
                faceVerify = _ui.value.faceVerificationSuccess,
                lateReason = lateReason,
                outOfRangeReason = outOfRangeReason
            )) {
                is SignatureOutcome.Success -> {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        successMessage = outcome.message,
                        checkInTToken = null,
                        checkInMessage = null,
                        faceVerificationQualityScore = null,
                        faceVerificationSuccess = false
                    )
                    autoClearMessages()
                    // Refresh status after successful check-in
                    delay(1000)
                    refreshStatus()
                }

                is SignatureOutcome.Error -> {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        errorMessage = outcome.message,
                        checkInTToken = null,
                        checkInMessage = null
                    )
                    autoClearMessages()
                }
            }
        }
    }

    /**
     * Complete check-out with signature API call
     */
    fun completeCheckOut(earlyReason: String?, outOfRangeReason: String?) {
        val tToken = _ui.value.checkOutTToken
        if (tToken == null) {
            _ui.value = _ui.value.copy(
                errorMessage = "Invalid check-out session"
            )
            autoClearMessages()
            return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                isLoading = true,
                showReasonDialog = false,
                errorMessage = null
            )

            when (val outcome = repo.checkOutSignature(
                tToken = tToken,
                faceRecognitionQualityScore = _ui.value.faceVerificationQualityScore,
                faceVerify = _ui.value.faceVerificationSuccess,
                earlyReason = earlyReason,
                outOfRangeReason = outOfRangeReason
            )) {
                is SignatureOutcome.Success -> {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        successMessage = outcome.message,
                        checkOutTToken = null,
                        checkOutMessage = null,
                        faceVerificationQualityScore = null,
                        faceVerificationSuccess = false
                    )
                    autoClearMessages()
                    // Refresh status after successful check-out
                    delay(1000)
                    refreshStatus()
                }

                is SignatureOutcome.Error -> {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        errorMessage = outcome.message,
                        checkOutTToken = null,
                        checkOutMessage = null
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
/**
 * UI State for Attendance
 */
data class AttendanceUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Status data from check-status API
    val currentState: String = "CHECK_IN_NEEDED",
    val statusMessage: String = "",
    val attendanceStatus: String? = null,
    val isComplete: Boolean? = null,

    // Check-in flow state
    val checkInTToken: String? = null,
    val checkInMinimumQualityScore: Float? = null,
    val checkInIsLate: Boolean = false,
    val checkInIsOutOfRange: Boolean = false,
    val checkInLateReasonRequired: Boolean = false,
    val checkInOutOfRangeReasonRequired: Boolean = false,
    val checkInMessage: String? = null,

    // Check-out flow state
    val checkOutTToken: String? = null,
    val checkOutMinimumQualityScore: Float? = null,
    val checkOutWorkHours: Float? = null,
    val checkOutIsEarly: Boolean = false,
    val checkOutIsOutOfRange: Boolean = false,
    val checkOutEarlyReasonRequired: Boolean = false,
    val checkOutOutOfRangeReasonRequired: Boolean = false,
    val checkOutMessage: String? = null,

    // UI flags (shared between check-in and check-out)
    val showFaceVerification: Boolean = false,
    val showReasonDialog: Boolean = false,

    // Face verification results (shared)
    val faceVerificationQualityScore: Float? = null,
    val faceVerificationSuccess: Boolean = false
)