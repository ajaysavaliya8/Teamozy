package com.hrms.jeejateamozy.feature.attendance.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.core.network.PendingMessage
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.hrms.jeejateamozy.feature.face.util.FaceVectorUtil
import android.util.Log

sealed class AttendanceEvent {
    data class ShowError(val message: String) : AttendanceEvent()
    data class ShowSuccess(val message: String) : AttendanceEvent()
}

class AttendanceViewModel(
    private val repo: AttendanceRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AttendanceUiState())
    val ui: StateFlow<AttendanceUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<AttendanceEvent>()
    val events: SharedFlow<AttendanceEvent> = _events.asSharedFlow()

    private var hasLoadedInitialStatus = false

    private fun emitError(message: String) {
        viewModelScope.launch {
            Log.e("AttendanceViewModel", "Emitting error event: $message")
            _events.emit(AttendanceEvent.ShowError(message))
        }
    }

    private fun emitSuccess(message: String) {
        viewModelScope.launch {
            Log.d("AttendanceViewModel", "Emitting success event: $message")
            _events.emit(AttendanceEvent.ShowSuccess(message))
        }
    }

    fun loadInitialStatusIfNeeded() {
        if (!hasLoadedInitialStatus) {
            hasLoadedInitialStatus = true
            refreshStatus(force = false)
        }
    }

    fun refreshStatus(force: Boolean = false) {
        if (_ui.value.isLoading && !force) {
            Log.d("AttendanceViewModel", "Already loading, skipping refresh")
            return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(isRefreshing = true)

            when (val outcome = repo.getStatus()) {
                is AttendanceOutcome.Success -> {
                    Log.d("AttendanceViewModel", "✅ Status refreshed: ${outcome.currentState}")
                    _ui.value = _ui.value.copy(
                        isRefreshing = false,
                        currentState = outcome.currentState,
                        statusMessage = outcome.message,
                        lastCheckInTime = outcome.lastCheckInTime,
                        attendanceStatus = outcome.attendanceStatus,
                        isComplete = outcome.isComplete,
                        checkInTToken = null,
                        checkOutTToken = null,
                        showFaceVerification = false,
                        showReasonDialog = false,
                        showWorkReportDialog = false,
                        showPendingMessageDialog = false,  // ✅ NEW
                        pendingMessage = null,  // ✅ NEW
                        acknowledgmentNote = null  // ✅ NEW
                    )
                }
                is AttendanceOutcome.Error -> {
                    Log.e("AttendanceViewModel", "❌ Status refresh error: ${outcome.message}")
                    _ui.value = _ui.value.copy(
                        isRefreshing = false,
                        statusMessage = outcome.message
                    )
                }
            }
        }
    }

    fun startCheckIn(context: Context) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true)

            when (val locResult = LocationHelper(context).getCurrentLocation()) {
                is LocationResult.Success -> {
                    performCheckIn(locResult.latitude, locResult.longitude)
                }
                is LocationResult.Error -> {
                    _ui.value = _ui.value.copy(isLoading = false)
                    emitError(locResult.message)
                }
            }
        }
    }

    private suspend fun performCheckIn(latitude: Double, longitude: Double) {
        when (val outcome = repo.checkIn(latitude, longitude)) {
            is CheckInOutcome.RequiresFaceVerification -> {
                Log.d("AttendanceViewModel", "✅ Check-in requires face verification")

                val pendingMessage = outcome.pendingMessage  // ✅ NEW: Extract pending message
                Log.d("AttendanceViewModel", "  pending_message: ${if (pendingMessage != null) "ID=${pendingMessage.id}" else "null"}")

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
                    pendingMessage = pendingMessage,  // ✅ NEW: Store pending message
                    showPendingMessageDialog = pendingMessage != null,  // ✅ NEW: Show message dialog if message exists
                    showFaceVerification = pendingMessage == null  // ✅ NEW: Only show face verification if no message
                )
            }

            is CheckInOutcome.RequiresReasons -> {
                Log.d("AttendanceViewModel", "✅ Check-in requires reasons")

                val pendingMessage = outcome.pendingMessage  // ✅ NEW: Extract pending message
                Log.d("AttendanceViewModel", "  pending_message: ${if (pendingMessage != null) "ID=${pendingMessage.id}" else "null"}")

                _ui.value = _ui.value.copy(
                    isLoading = false,
                    checkInTToken = outcome.tToken,
                    checkInIsLate = outcome.isLate,
                    checkInIsOutOfRange = outcome.isOutOfRange,
                    checkInLateReasonRequired = outcome.lateReasonRequired,
                    checkInOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                    checkInMessage = outcome.message,
                    pendingMessage = pendingMessage,  // ✅ NEW: Store pending message
                    showPendingMessageDialog = pendingMessage != null,  // ✅ NEW: Show message dialog if message exists
                    showReasonDialog = (outcome.lateReasonRequired || outcome.outOfRangeReasonRequired) && pendingMessage == null  // ✅ NEW: Only show reasons if no message
                )

                // ✅ NEW: Auto-complete only if no reasons required AND no message
                if (!outcome.lateReasonRequired && !outcome.outOfRangeReasonRequired && pendingMessage == null) {
                    completeCheckIn(null, null)
                }
            }

            is CheckInOutcome.Success -> {
                Log.d("AttendanceViewModel", "✅ Check-in directly successful")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    currentState = "CHECK_OUT_NEEDED"
                )
                emitSuccess(outcome.message)
            }

            is CheckInOutcome.Error -> {
                Log.e("AttendanceViewModel", "❌ CHECK-IN ERROR: ${outcome.message}")
                _ui.value = _ui.value.copy(isLoading = false)
                emitError(outcome.message)
            }
        }
    }

    fun startCheckOut(context: Context) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true)

            when (val locResult = LocationHelper(context).getCurrentLocation()) {
                is LocationResult.Success -> {
                    performCheckOut(locResult.latitude, locResult.longitude)
                }
                is LocationResult.Error -> {
                    _ui.value = _ui.value.copy(isLoading = false)
                    emitError(locResult.message)
                }
            }
        }
    }

    private suspend fun performCheckOut(latitude: Double, longitude: Double) {
        when (val outcome = repo.checkOut(latitude, longitude)) {
            is CheckOutOutcome.RequiresFaceVerification -> {
                Log.d("AttendanceViewModel", "✅ Check-out requires face verification")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    checkOutTToken = outcome.tToken,
                    checkOutMinimumQualityScore = outcome.minimumQualityScore,
                    checkOutWorkHours = outcome.workHours,
                    checkOutIsEarly = outcome.isEarly,
                    checkOutIsOutOfRange = outcome.isOutOfRange,
                    checkOutEarlyReasonRequired = outcome.earlyReasonRequired,
                    checkOutOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                    checkOutWorkReportRequired = outcome.workReportRequired,
                    checkOutMessage = outcome.message,
                    checkOutFaceVector = outcome.faceVector,
                    showFaceVerification = true
                )
            }

            is CheckOutOutcome.RequiresReasons -> {
                Log.d("AttendanceViewModel", "✅ Check-out requires reasons")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    checkOutTToken = outcome.tToken,
                    checkOutWorkHours = outcome.workHours,
                    checkOutIsEarly = outcome.isEarly,
                    checkOutIsOutOfRange = outcome.isOutOfRange,
                    checkOutEarlyReasonRequired = outcome.earlyReasonRequired,
                    checkOutOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                    checkOutWorkReportRequired = outcome.workReportRequired,
                    checkOutMessage = outcome.message,
                    showReasonDialog = outcome.earlyReasonRequired || outcome.outOfRangeReasonRequired,
                    showWorkReportDialog = outcome.workReportRequired && !outcome.earlyReasonRequired && !outcome.outOfRangeReasonRequired
                )

                // Auto-complete if no reasons or work report required
                if (!outcome.earlyReasonRequired && !outcome.outOfRangeReasonRequired && !outcome.workReportRequired) {
                    completeCheckOut(null, null, null, null)
                }
            }

            is CheckOutOutcome.Success -> {
                Log.d("AttendanceViewModel", "✅ Check-out directly successful")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    currentState = "CHECK_IN_NEEDED",
                    lastCheckInTime = null,
                    isComplete = true
                )
                emitSuccess(outcome.message)
            }

            is CheckOutOutcome.Error -> {
                Log.e("AttendanceViewModel", "❌ CHECK-OUT ERROR: ${outcome.message}")
                _ui.value = _ui.value.copy(isLoading = false)
                emitError(outcome.message)
            }
        }
    }

    // ✅ NEW: Handle pending message acknowledgment
    fun onPendingMessageAcknowledged(acknowledgmentNote: String?) {
        Log.d("AttendanceViewModel", "✅ Message acknowledged, note: ${acknowledgmentNote?.take(50)}")

        _ui.value = _ui.value.copy(
            showPendingMessageDialog = false,
            acknowledgmentNote = acknowledgmentNote
        )

        // Now proceed with the next step based on what's required
        val hasLateOrOutOfRangeReasons = _ui.value.checkInLateReasonRequired || _ui.value.checkInOutOfRangeReasonRequired
        val hasFaceVerification = _ui.value.checkInFaceVector != null

        when {
            hasFaceVerification -> {
                // Show face verification
                _ui.value = _ui.value.copy(showFaceVerification = true)
            }
            hasLateOrOutOfRangeReasons -> {
                // Show reason dialog
                _ui.value = _ui.value.copy(showReasonDialog = true)
            }
            else -> {
                // Complete check-in directly
                completeCheckIn(null, null)
            }
        }
    }

    // ✅ NEW: Handle pending message dismissal (if no acknowledgment required)
    fun onPendingMessageDismissed() {
        Log.d("AttendanceViewModel", "Message dismissed (no acknowledgment required)")
        onPendingMessageAcknowledged(null)  // Same flow, just no note
    }

    fun onFaceVerificationComplete(qualityScore: Float, verified: Boolean) {
        _ui.value = _ui.value.copy(
            faceVerificationQualityScore = qualityScore,
            faceVerificationSuccess = verified,
            showFaceVerification = false
        )

        val isCheckIn = _ui.value.checkInTToken != null

        if (isCheckIn) {
            val lateReasonRequired = _ui.value.checkInLateReasonRequired
            val outOfRangeReasonRequired = _ui.value.checkInOutOfRangeReasonRequired

            if (lateReasonRequired || outOfRangeReasonRequired) {
                _ui.value = _ui.value.copy(showReasonDialog = true)
            } else {
                completeCheckIn(null, null)
            }
        } else {
            val earlyReasonRequired = _ui.value.checkOutEarlyReasonRequired
            val outOfRangeReasonRequired = _ui.value.checkOutOutOfRangeReasonRequired
            val workReportRequired = _ui.value.checkOutWorkReportRequired

            when {
                earlyReasonRequired || outOfRangeReasonRequired -> {
                    _ui.value = _ui.value.copy(showReasonDialog = true)
                }
                workReportRequired -> {
                    _ui.value = _ui.value.copy(showWorkReportDialog = true)
                }
                else -> {
                    completeCheckOut(null, null, null, null)
                }
            }
        }
    }

    fun onReasonDialogDismissed() {
        _ui.value = _ui.value.copy(showReasonDialog = false)
    }

    fun onWorkReportDialogDismissed() {
        _ui.value = _ui.value.copy(showWorkReportDialog = false)
    }

    fun completeCheckIn(lateReason: String?, outOfRangeReason: String?) {
        val tToken = _ui.value.checkInTToken
        if (tToken == null) {
            Log.e("AttendanceViewModel", "❌ Invalid check-in session")
            emitError("Invalid check-in session")
            return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                isLoading = true,
                showReasonDialog = false
            )

            val acknowledgmentNote = _ui.value.acknowledgmentNote  // ✅ NEW: Get stored acknowledgment note

            Log.d("AttendanceViewModel", "Completing check-in with acknowledgment: ${acknowledgmentNote?.take(50)}")

            when (val outcome = repo.checkInSignature(
                tToken = tToken,
                faceRecognitionQualityScore = _ui.value.faceVerificationQualityScore,
                faceVerify = _ui.value.faceVerificationSuccess,
                lateReason = lateReason,
                outOfRangeReason = outOfRangeReason,
                acknowledgmentNote = acknowledgmentNote  // ✅ NEW: Pass acknowledgment note
            )) {
                is SignatureOutcome.Success -> {
                    Log.d("AttendanceViewModel", "✅ Check-in signature SUCCESS")
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        currentState = "CHECK_OUT_NEEDED",
                        lastCheckInTime = outcome.checkInTime,
                        checkInTToken = null,
                        checkInFaceVector = null,
                        checkInMessage = null,
                        faceVerificationQualityScore = null,
                        faceVerificationSuccess = false,
                        pendingMessage = null,  // ✅ NEW: Clear pending message
                        acknowledgmentNote = null  // ✅ NEW: Clear acknowledgment note
                    )
                    emitSuccess(outcome.message)
                }

                is SignatureOutcome.Error -> {
                    Log.e("AttendanceViewModel", "❌ CHECK-IN SIGNATURE ERROR: ${outcome.message}")
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        checkInTToken = null,
                        checkInFaceVector = null,
                        checkInMessage = null,
                        pendingMessage = null,  // ✅ NEW: Clear pending message on error
                        acknowledgmentNote = null  // ✅ NEW: Clear acknowledgment note on error
                    )
                    emitError(outcome.message)
                    delay(500)
                    refreshStatus(force = true)
                }
            }
        }
    }

    fun completeCheckOut(
        earlyReason: String?,
        outOfRangeReason: String?,
        workReport: String?,
        workReportFileUri: Uri?
    ) {
        val tToken = _ui.value.checkOutTToken
        if (tToken == null) {
            Log.e("AttendanceViewModel", "❌ Invalid check-out session")
            emitError("Invalid check-out session")
            return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                isLoading = true,
                showReasonDialog = false,
                showWorkReportDialog = false
            )

            when (val outcome = repo.checkOutSignature(
                tToken = tToken,
                faceRecognitionQualityScore = _ui.value.faceVerificationQualityScore,
                faceVerify = _ui.value.faceVerificationSuccess,
                earlyReason = earlyReason,
                outOfRangeReason = outOfRangeReason,
                workReport = workReport,
                workReportFileUri = workReportFileUri
            )) {
                is SignatureOutcome.Success -> {
                    Log.d("AttendanceViewModel", "✅ Check-out signature SUCCESS")
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        currentState = "CHECK_IN_NEEDED",
                        lastCheckInTime = null,
                        checkOutTToken = null,
                        checkOutFaceVector = null,
                        checkOutMessage = null,
                        faceVerificationQualityScore = null,
                        faceVerificationSuccess = false,
                        tempEarlyReason = null,
                        tempOutOfRangeReason = null,
                        isComplete = true
                    )
                    emitSuccess(outcome.message)
                }

                is SignatureOutcome.Error -> {
                    Log.e("AttendanceViewModel", "❌ CHECK-OUT SIGNATURE ERROR: ${outcome.message}")
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        checkOutTToken = null,
                        checkOutFaceVector = null,
                        checkOutMessage = null,
                        tempEarlyReason = null,
                        tempOutOfRangeReason = null
                    )
                    emitError(outcome.message)
                    delay(500)
                    refreshStatus(force = true)
                }
            }
        }
    }

    fun onReasonSubmitted(lateOrEarlyReason: String?, outOfRangeReason: String?) {
        val isCheckIn = _ui.value.checkInTToken != null

        if (isCheckIn) {
            completeCheckIn(lateOrEarlyReason, outOfRangeReason)
        } else {
            // Check if work report is required after reasons
            if (_ui.value.checkOutWorkReportRequired) {
                _ui.value = _ui.value.copy(
                    showReasonDialog = false,
                    showWorkReportDialog = true,
                    // Store reasons temporarily
                    tempEarlyReason = lateOrEarlyReason,
                    tempOutOfRangeReason = outOfRangeReason
                )
            } else {
                completeCheckOut(lateOrEarlyReason, outOfRangeReason, null, null)
            }
        }
    }

    fun onWorkReportSubmitted(workReport: String, fileUri: Uri?) {
        completeCheckOut(
            _ui.value.tempEarlyReason,
            _ui.value.tempOutOfRangeReason,
            workReport,
            fileUri
        )
    }

    fun canCheckIn(): Boolean = _ui.value.currentState == "CHECK_IN_NEEDED"
    fun canCheckOut(): Boolean = _ui.value.currentState == "CHECK_OUT_NEEDED"
    fun isComplete(): Boolean = _ui.value.isComplete == true
}

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,

    val currentState: String = "CHECK_IN_NEEDED",
    val statusMessage: String = "",
    val lastCheckInTime: String? = null,
    val attendanceStatus: String? = null,
    val isComplete: Boolean? = null,

    val checkInTToken: String? = null,
    val checkInMinimumQualityScore: Float? = null,
    val checkInIsLate: Boolean = false,
    val checkInIsOutOfRange: Boolean = false,
    val checkInLateReasonRequired: Boolean = false,
    val checkInOutOfRangeReasonRequired: Boolean = false,
    val checkInMessage: String? = null,
    val checkInFaceVector: FloatArray? = null,

    val checkOutTToken: String? = null,
    val checkOutMinimumQualityScore: Float? = null,
    val checkOutWorkHours: Float? = null,
    val checkOutIsEarly: Boolean = false,
    val checkOutIsOutOfRange: Boolean = false,
    val checkOutEarlyReasonRequired: Boolean = false,
    val checkOutOutOfRangeReasonRequired: Boolean = false,
    val checkOutWorkReportRequired: Boolean = false,
    val checkOutMessage: String? = null,
    val checkOutFaceVector: FloatArray? = null,

    val showFaceVerification: Boolean = false,
    val showReasonDialog: Boolean = false,
    val showWorkReportDialog: Boolean = false,
    val showPendingMessageDialog: Boolean = false,  // ✅ NEW: Show pending message dialog

    val faceVerificationQualityScore: Float? = null,
    val faceVerificationSuccess: Boolean = false,

    val tempEarlyReason: String? = null,
    val tempOutOfRangeReason: String? = null,

    // ✅ NEW: Pending message fields
    val pendingMessage: PendingMessage? = null,  // The pending message from server
    val acknowledgmentNote: String? = null  // User's acknowledgment note (if required)
)