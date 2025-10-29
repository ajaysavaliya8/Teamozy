package com.hrms.jeejateamozy.feature.attendance.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.core.utils.LocationHelper
import com.hrms.jeejateamozy.core.utils.LocationResult
import com.hrms.jeejateamozy.feature.attendance.data.AttendanceOutcome
import com.hrms.jeejateamozy.feature.attendance.data.AttendanceRepository
import com.hrms.jeejateamozy.feature.attendance.data.CheckInOutcome
import com.hrms.jeejateamozy.feature.attendance.data.CheckOutOutcome
import com.hrms.jeejateamozy.feature.attendance.data.SignatureOutcome
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.hrms.jeejateamozy.feature.face.util.FaceVectorUtil
import android.util.Log

/**
 * AttendanceViewModel
 * Handles check-in flow with face verification and violation reasons
 */
class AttendanceViewModel(
    private val repo: AttendanceRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AttendanceUiState())
    val ui: StateFlow<AttendanceUiState> = _ui.asStateFlow()
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
                        lastCheckInTime = outcome.lastCheckInTime,
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

                            Log.d("AttendanceViewModel", "✅ Check-in requires face verification")
                            Log.d("AttendanceViewModel", "   face_vector present: ${outcome.faceVector != null}")
                            if (outcome.faceVector != null) {
                                Log.d("AttendanceViewModel", "   face_vector size: ${outcome.faceVector.size}")
                                Log.d("AttendanceViewModel", "   face_vector valid: ${FaceVectorUtil.isValidFaceVector(outcome.faceVector)}")
                            }

                            // Face verification is required
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                checkInTToken = outcome.tToken,
                                checkInFaceVector = outcome.faceVector,
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
                            Log.d("AttendanceViewModel", "✅ Check-out requires face verification")
                            Log.d("AttendanceViewModel", "   face_vector present: ${outcome.faceVector != null}")
                            if (outcome.faceVector != null) {
                                Log.d("AttendanceViewModel", "   face_vector size: ${outcome.faceVector.size}")
                                Log.d("AttendanceViewModel", "   face_vector valid: ${FaceVectorUtil.isValidFaceVector(outcome.faceVector)}")
                            }
                            // Face verification is required
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                checkOutTToken = outcome.tToken,
                                checkOutFaceVector = outcome.faceVector,
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

    fun onFaceVerificationCancelled() {
        _ui.value = _ui.value.copy(
            showFaceVerification = false,
            checkInTToken = null,
            checkInFaceVector = null,
            checkInMessage = null,
            checkOutTToken = null,
            checkOutFaceVector = null,
            checkOutMessage = null
        )
    }

    fun onReasonDialogDismissed() {
        _ui.value = _ui.value.copy(
            showReasonDialog = false,
            checkInTToken = null,
            checkInMessage = null,
            checkOutTToken = null,
            checkOutMessage = null
        )
    }


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
                        checkInFaceVector = null,
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
                        checkInFaceVector = null,
                        checkInMessage = null
                    )
                    autoClearMessages()
                }
            }
        }
    }

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
                        checkOutFaceVector = null,
                        checkOutMessage = null,
                        faceVerificationQualityScore = null,
                        faceVerificationSuccess = false
                    )
                    autoClearMessages()

                    delay(1000)
                    refreshStatus()
                }

                is SignatureOutcome.Error -> {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        errorMessage = outcome.message,
                        checkOutTToken = null,
                        checkOutFaceVector = null,
                        checkOutMessage = null
                    )
                    autoClearMessages()
                }
            }
        }
    }


    fun clearMessages() {
        _ui.value = _ui.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    private fun autoClearMessages() {
        viewModelScope.launch {
            delay(4500)
            clearMessages()
        }
    }

    fun canCheckIn(): Boolean {
        return _ui.value.currentState == "CHECK_IN_NEEDED"
    }

    fun canCheckOut(): Boolean {
        return _ui.value.currentState == "CHECK_OUT_NEEDED"
    }

    fun isComplete(): Boolean {
        return _ui.value.currentState == "COMPLETED"
    }
}

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Status data from check-status API
    val currentState: String = "CHECK_IN_NEEDED",
    val statusMessage: String = "",
    val lastCheckInTime: String? = null,
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

    val checkInFaceVector: FloatArray? = null,
    val checkOutFaceVector: FloatArray? = null,

    // UI flags (shared between check-in and check-out)
    val showFaceVerification: Boolean = false,
    val showReasonDialog: Boolean = false,

    // Face verification results (shared)
    val faceVerificationQualityScore: Float? = null,
    val faceVerificationSuccess: Boolean = false
)