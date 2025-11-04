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
 * Handles check-in/check-out flow with face verification and violation reasons
 *
 * ✅ OPTIMIZED: Uses optimistic updates on success, refreshes only on errors
 *
 * STATE FLOW (only 2 states):
 * CHECK_IN_NEEDED → (check-in) → CHECK_OUT_NEEDED → (check-out) → CHECK_IN_NEEDED
 *
 * After successful check-out, state resets to CHECK_IN_NEEDED for the next day
 */
class AttendanceViewModel(
    private val repo: AttendanceRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AttendanceUiState())
    val ui: StateFlow<AttendanceUiState> = _ui.asStateFlow()
    private var hasLoadedInitialStatus = false

    fun refreshStatus(force: Boolean = false) {
        if (hasLoadedInitialStatus && !force) {
            Log.d("AttendanceViewModel", "⏭️ Skipping refreshStatus - already loaded (force=$force)")
            return
        }

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
                    hasLoadedInitialStatus = true
                    Log.d("AttendanceViewModel", "✅ Status loaded successfully")
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

                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                checkInTToken = outcome.tToken,
                                checkInMinimumQualityScore = outcome.minimumQualityScore,
                                checkInIsLate = outcome.isLate,
                                checkInIsOutOfRange = outcome.isOutOfRange,
                                checkInLateReasonRequired = outcome.lateReasonRequired,
                                checkInOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                                checkInMessage = outcome.message,
                                checkInFaceVector = outcome.faceVector,
                                showFaceVerification = true
                            )
                        }

                        is CheckInOutcome.RequiresReasons -> {
                            Log.d("AttendanceViewModel", "✅ Check-in requires reasons (no face verification)")

                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                checkInTToken = outcome.tToken,
                                checkInIsLate = outcome.isLate,
                                checkInIsOutOfRange = outcome.isOutOfRange,
                                checkInLateReasonRequired = outcome.lateReasonRequired,
                                checkInOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                                checkInMessage = outcome.message,
                                showReasonDialog = outcome.lateReasonRequired || outcome.outOfRangeReasonRequired
                            )

                            // If no reasons needed either, complete immediately
                            if (!outcome.lateReasonRequired && !outcome.outOfRangeReasonRequired) {
                                completeCheckIn(null, null)
                            }
                        }

                        is CheckInOutcome.Success -> {
                            Log.d("AttendanceViewModel", "✅ Check-in directly successful (rare case)")

                            // ✅ OPTIMISTIC UPDATE: Directly set state to CHECK_OUT_NEEDED
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                currentState = "CHECK_OUT_NEEDED",
                                successMessage = outcome.message
                            )
                            autoClearMessages()
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
                                checkOutFaceVector = outcome.faceVector,
                                showFaceVerification = true
                            )
                        }

                        is CheckOutOutcome.RequiresReasons -> {
                            Log.d("AttendanceViewModel", "✅ Check-out requires reasons (no face verification)")

                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                checkOutTToken = outcome.tToken,
                                checkOutWorkHours = outcome.workHours,
                                checkOutIsEarly = outcome.isEarly,
                                checkOutIsOutOfRange = outcome.isOutOfRange,
                                checkOutEarlyReasonRequired = outcome.earlyReasonRequired,
                                checkOutOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                                checkOutMessage = outcome.message,
                                showReasonDialog = outcome.earlyReasonRequired || outcome.outOfRangeReasonRequired
                            )

                            // If no reasons needed either, complete immediately
                            if (!outcome.earlyReasonRequired && !outcome.outOfRangeReasonRequired) {
                                completeCheckOut(null, null)
                            }
                        }

                        is CheckOutOutcome.Success -> {
                            Log.d("AttendanceViewModel", "✅ Check-out directly successful (rare case)")

                            // ✅ OPTIMISTIC UPDATE: After check-out, state goes back to CHECK_IN_NEEDED
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                currentState = "CHECK_IN_NEEDED",  // ✅ Reset to CHECK_IN_NEEDED
                                successMessage = outcome.message,
                                lastCheckInTime = null,
                                isComplete = true
                            )
                            autoClearMessages()
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

    fun onFaceVerified(qualityScore: Float, success: Boolean) {
        Log.d("AttendanceViewModel", "Face verified: quality=$qualityScore, success=$success")

        _ui.value = _ui.value.copy(
            faceVerificationQualityScore = qualityScore,
            faceVerificationSuccess = success,
            showFaceVerification = false
        )

        // Determine if we need to show reason dialog
        val needsCheckInReasons = _ui.value.checkInTToken != null &&
                (_ui.value.checkInLateReasonRequired || _ui.value.checkInOutOfRangeReasonRequired)

        val needsCheckOutReasons = _ui.value.checkOutTToken != null &&
                (_ui.value.checkOutEarlyReasonRequired || _ui.value.checkOutOutOfRangeReasonRequired)

        if (needsCheckInReasons || needsCheckOutReasons) {
            _ui.value = _ui.value.copy(showReasonDialog = true)
        } else {
            // No reasons needed, complete directly
            if (_ui.value.checkInTToken != null) {
                completeCheckIn(null, null)
            } else if (_ui.value.checkOutTToken != null) {
                completeCheckOut(null, null)
            }
        }
    }

    // ✅ Compatibility function for HomePage.kt
    fun onFaceVerificationComplete(qualityScore: Float, verified: Boolean) {
        Log.d("AttendanceViewModel", "Face verification complete: quality=$qualityScore, verified=$verified")

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
        Log.d("AttendanceViewModel", "Face verification cancelled")

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
        Log.d("AttendanceViewModel", "Reason dialog dismissed")

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
                    Log.d("AttendanceViewModel", "✅ Check-in signature SUCCESS")

                    // ✅ OPTIMISTIC UPDATE: Directly set state to CHECK_OUT_NEEDED
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        currentState = "CHECK_OUT_NEEDED",  // ✅ Direct state update
                        lastCheckInTime = outcome.checkInTime,  // ✅ Update from API response
                        successMessage = outcome.message,
                        checkInTToken = null,
                        checkInFaceVector = null,
                        checkInMessage = null,
                        faceVerificationQualityScore = null,
                        faceVerificationSuccess = false
                    )
                    autoClearMessages()

                    Log.d("AttendanceViewModel", "✅ State updated to CHECK_OUT_NEEDED (no API call needed)")
                    // ❌ NO refreshStatus() call - we already know the new state!
                }

                is SignatureOutcome.Error -> {
                    Log.d("AttendanceViewModel", "❌ Check-in signature ERROR: ${outcome.message}")

                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        errorMessage = outcome.message,
                        checkInTToken = null,
                        checkInFaceVector = null,
                        checkInMessage = null
                    )
                    autoClearMessages()

                    // ✅ On error, refresh to sync with server state
                    delay(500)
                    refreshStatus(force = true)
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
                    Log.d("AttendanceViewModel", "✅ Check-out signature SUCCESS")

                    // ✅ OPTIMISTIC UPDATE: After check-out, state goes back to CHECK_IN_NEEDED
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        currentState = "CHECK_IN_NEEDED",  // ✅ Reset to CHECK_IN_NEEDED for next day
                        successMessage = outcome.message,
                        checkOutTToken = null,
                        checkOutFaceVector = null,
                        checkOutMessage = null,
                        faceVerificationQualityScore = null,
                        faceVerificationSuccess = false,
                        lastCheckInTime = null,  // ✅ Clear check-in time
                        isComplete = true  // ✅ Mark today as complete
                    )
                    autoClearMessages()

                    Log.d("AttendanceViewModel", "✅ State updated to CHECK_IN_NEEDED (no API call needed)")
                    // ❌ NO refreshStatus() call - we already know the new state!
                }

                is SignatureOutcome.Error -> {
                    Log.d("AttendanceViewModel", "❌ Check-out signature ERROR: ${outcome.message}")

                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        errorMessage = outcome.message,
                        checkOutTToken = null,
                        checkOutFaceVector = null,
                        checkOutMessage = null
                    )
                    autoClearMessages()

                    // ✅ On error, refresh to sync with server state
                    delay(500)
                    refreshStatus(force = true)
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
        // Note: There are only 2 states: CHECK_IN_NEEDED and CHECK_OUT_NEEDED
        // isComplete flag indicates today's attendance is done (user has checked out)
        return _ui.value.isComplete == true
    }
}

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Status data from check-status API
    // currentState has only 2 possible values: "CHECK_IN_NEEDED" or "CHECK_OUT_NEEDED"
    val currentState: String = "CHECK_IN_NEEDED",
    val statusMessage: String = "",
    val lastCheckInTime: String? = null,
    val attendanceStatus: String? = null,
    val isComplete: Boolean? = null,  // true when user has checked out for the day

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