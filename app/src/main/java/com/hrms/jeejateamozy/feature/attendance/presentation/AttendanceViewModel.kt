package com.hrms.jeejateamozy.feature.attendance.presentation

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
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
// import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService  // ✅ DISABLED
import com.hrms.jeejateamozy.feature.location.sync.PersistentSyncManager
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingWorker
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingAlarmReceiver
import com.hrms.jeejateamozy.feature.location.keepalive.BatteryOptimizationHelper
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class AttendanceEvent {
    data class ShowError(val message: String) : AttendanceEvent()
    data class ShowSuccess(val message: String) : AttendanceEvent()
}

class AttendanceViewModel(
    private val repo: AttendanceRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AttendanceViewModel"
    }

    private val _ui = MutableStateFlow(AttendanceUiState())
    val ui: StateFlow<AttendanceUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<AttendanceEvent>()
    val events: SharedFlow<AttendanceEvent> = _events.asSharedFlow()

    private var hasLoadedInitialStatus = false

    private fun emitError(message: String) {
        viewModelScope.launch {
            Log.e(TAG, "Emitting error event: $message")
            _events.emit(AttendanceEvent.ShowError(message))
        }
    }

    private fun emitSuccess(message: String) {
        viewModelScope.launch {
            Log.d(TAG, "Emitting success event: $message")
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
            Log.d(TAG, "Already loading, skipping refresh")
            return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(isRefreshing = true)

            when (val outcome = repo.getStatus()) {
                is AttendanceOutcome.Success -> {
                    Log.d(TAG, "✅ Status refreshed: ${outcome.currentState}")
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
                        showPendingMessageDialog = false,
                        pendingMessage = null,
                        acknowledgmentNote = null
                    )
                }
                is AttendanceOutcome.Error -> {
                    Log.e(TAG, "❌ Status refresh error: ${outcome.message}")
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
                    performCheckIn(locResult.latitude, locResult.longitude, context)
                }
                is LocationResult.Error -> {
                    _ui.value = _ui.value.copy(isLoading = false)
                    emitError(locResult.message)
                }
            }
        }
    }

    private suspend fun performCheckIn(latitude: Double, longitude: Double, context: Context) {
        when (val outcome = repo.checkIn(latitude, longitude)) {
            is CheckInOutcome.RequiresFaceVerification -> {
                Log.d(TAG, "✅ Check-in requires face verification")

                val pendingMessage = outcome.pendingMessage
                Log.d(TAG, "  pending_message: ${if (pendingMessage != null) "ID=${pendingMessage.id}" else "null"}")

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
                    pendingMessage = pendingMessage,
                    showPendingMessageDialog = pendingMessage != null,
                    showFaceVerification = pendingMessage == null
                )
            }

            is CheckInOutcome.RequiresReasons -> {
                Log.d(TAG, "✅ Check-in requires reasons")

                val pendingMessage = outcome.pendingMessage
                Log.d(TAG, "  pending_message: ${if (pendingMessage != null) "ID=${pendingMessage.id}" else "null"}")

                _ui.value = _ui.value.copy(
                    isLoading = false,
                    checkInTToken = outcome.tToken,
                    checkInIsLate = outcome.isLate,
                    checkInIsOutOfRange = outcome.isOutOfRange,
                    checkInLateReasonRequired = outcome.lateReasonRequired,
                    checkInOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                    checkInMessage = outcome.message,
                    pendingMessage = pendingMessage,
                    showPendingMessageDialog = pendingMessage != null,
                    showReasonDialog = (outcome.lateReasonRequired || outcome.outOfRangeReasonRequired) && pendingMessage == null
                )

                if (!outcome.lateReasonRequired && !outcome.outOfRangeReasonRequired && pendingMessage == null) {
                    completeCheckIn(null, null, context)
                }
            }

            is CheckInOutcome.Success -> {
                Log.d(TAG, "✅ Check-in directly successful")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    currentState = "CHECK_OUT_NEEDED"
                )
                emitSuccess(outcome.message)
            }

            is CheckInOutcome.Error -> {
                Log.e(TAG, "❌ CHECK-IN ERROR: ${outcome.message}")
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
                    performCheckOut(locResult.latitude, locResult.longitude, context)
                }
                is LocationResult.Error -> {
                    _ui.value = _ui.value.copy(isLoading = false)
                    emitError(locResult.message)
                }
            }
        }
    }

    private suspend fun performCheckOut(latitude: Double, longitude: Double, context: Context) {
        when (val outcome = repo.checkOut(latitude, longitude)) {
            is CheckOutOutcome.RequiresFaceVerification -> {
                Log.d(TAG, "✅ Check-out requires face verification")
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
                Log.d(TAG, "✅ Check-out requires reasons")
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

                if (!outcome.earlyReasonRequired && !outcome.outOfRangeReasonRequired && !outcome.workReportRequired) {
                    completeCheckOut(null, null, null, null, context)
                }
            }

            is CheckOutOutcome.Success -> {
                Log.d(TAG, "✅ Check-out directly successful")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    currentState = "CHECK_IN_NEEDED",
                    lastCheckInTime = null,
                    isComplete = true
                )
                emitSuccess(outcome.message)
            }

            is CheckOutOutcome.Error -> {
                Log.e(TAG, "❌ CHECK-OUT ERROR: ${outcome.message}")
                _ui.value = _ui.value.copy(isLoading = false)
                emitError(outcome.message)
            }
        }
    }

    fun onPendingMessageAcknowledged(acknowledgmentNote: String?, context: Context) {
        Log.d(TAG, "✅ Message acknowledged, note: ${acknowledgmentNote?.take(50)}")

        _ui.value = _ui.value.copy(
            showPendingMessageDialog = false,
            acknowledgmentNote = acknowledgmentNote
        )

        val hasLateOrOutOfRangeReasons = _ui.value.checkInLateReasonRequired || _ui.value.checkInOutOfRangeReasonRequired
        val hasFaceVerification = _ui.value.checkInFaceVector != null

        when {
            hasFaceVerification -> {
                _ui.value = _ui.value.copy(showFaceVerification = true)
            }
            hasLateOrOutOfRangeReasons -> {
                _ui.value = _ui.value.copy(showReasonDialog = true)
            }
            else -> {
                completeCheckIn(null, null, context)
            }
        }
    }

    fun onPendingMessageDismissed() {
        Log.d(TAG, "❌ Message cancelled - stopping check-in process")
        _ui.value = _ui.value.copy(
            isLoading = false,
            showPendingMessageDialog = false,
            pendingMessage = null,
            acknowledgmentNote = null,
            checkInTToken = null,
            checkInFaceVector = null,
            checkInMessage = null,
            checkInMinimumQualityScore = null,
            checkInIsLate = false,
            checkInIsOutOfRange = false,
            checkInLateReasonRequired = false,
            checkInOutOfRangeReasonRequired = false,
            showFaceVerification = false,
            showReasonDialog = false
        )

        emitError("Check-in cancelled")
    }

    fun onFaceVerificationCancelled() {
        Log.d(TAG, "❌ Face verification cancelled - stopping process")

        val isCheckIn = _ui.value.checkInTToken != null

        if (isCheckIn) {
            _ui.value = _ui.value.copy(
                isLoading = false,
                showFaceVerification = false,
                showReasonDialog = false,
                showPendingMessageDialog = false,
                checkInTToken = null,
                checkInFaceVector = null,
                checkInMessage = null,
                checkInMinimumQualityScore = null,
                checkInIsLate = false,
                checkInIsOutOfRange = false,
                checkInLateReasonRequired = false,
                checkInOutOfRangeReasonRequired = false,
                pendingMessage = null,
                acknowledgmentNote = null,
                faceVerificationQualityScore = null,
                faceVerificationSuccess = false
            )
        } else {
            _ui.value = _ui.value.copy(
                isLoading = false,
                showFaceVerification = false,
                showReasonDialog = false,
                showWorkReportDialog = false,
                checkOutTToken = null,
                checkOutFaceVector = null,
                checkOutMessage = null,
                checkOutMinimumQualityScore = null,
                checkOutWorkHours = null,
                checkOutIsEarly = false,
                checkOutIsOutOfRange = false,
                checkOutEarlyReasonRequired = false,
                checkOutOutOfRangeReasonRequired = false,
                checkOutWorkReportRequired = false,
                faceVerificationQualityScore = null,
                faceVerificationSuccess = false,
                tempEarlyReason = null,
                tempOutOfRangeReason = null
            )
        }

        emitError("${if (isCheckIn) "Check-in" else "Check-out"} cancelled")
    }

    fun onFaceVerificationComplete(qualityScore: Float, verified: Boolean, context: Context) {
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
                completeCheckIn(null, null, context)
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
                    completeCheckOut(null, null, null, null, context)
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

    fun completeCheckIn(
        lateReason: String?,
        outOfRangeReason: String?,
        context: Context,
        activity: Activity? = null
    ) {
        val tToken = _ui.value.checkInTToken
        if (tToken == null) {
            Log.e(TAG, "❌ Invalid check-in session")
            emitError("Invalid check-in session")
            return
        }

        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(
                    isLoading = true,
                    loadingMessage = "Preparing check-in...",
                    showReasonDialog = false
                )

                Log.d(TAG, "🔄 Starting check-in process...")

                val syncManager = PersistentSyncManager.getInstance(context)

                val hasOldData = syncManager.hasPendingLocations()

                if (hasOldData) {
                    val oldCount = syncManager.getPendingCount()
                    Log.w(TAG, "⚠️ Found $oldCount old locations - handling in background with retry")

                    val syncResult = syncManager.backgroundSyncOrClearOldData()

                    when (syncResult) {
                        is PersistentSyncManager.BackgroundSyncResult.Synced -> {
                            Log.d(TAG, "✅ Background sync SUCCESS on attempt ${syncResult.attempts}: ${syncResult.count} old locations saved")
                        }

                        is PersistentSyncManager.BackgroundSyncResult.ClearedDueToNetwork -> {
                            Log.w(TAG, "🌐 Cleared ${syncResult.count} old locations after ${syncResult.attempts} attempts (no network)")
                        }

                        is PersistentSyncManager.BackgroundSyncResult.ClearedDueToTimeout -> {
                            Log.w(TAG, "⏱️ After ${syncResult.attempts} attempts: synced ${syncResult.synced}, cleared ${syncResult.cleared} old locations")
                        }

                        is PersistentSyncManager.BackgroundSyncResult.ClearedDueToError -> {
                            Log.e(TAG, "❌ Cleared ${syncResult.count} old locations after ${syncResult.attempts} attempts: ${syncResult.message}")
                        }

                        is PersistentSyncManager.BackgroundSyncResult.Failed -> {
                            Log.e(TAG, "❌ Failed to handle old data: ${syncResult.message}")
                        }

                        is PersistentSyncManager.BackgroundSyncResult.NoData -> {
                            Log.d(TAG, "✅ No old data found")
                        }
                    }
                } else {
                    Log.d(TAG, "✅ No old data - proceeding with clean check-in")
                }

                _ui.value = _ui.value.copy(
                    loadingMessage = "Completing check-in..."
                )

                val acknowledgmentNote = _ui.value.acknowledgmentNote

                Log.d(TAG, "Completing check-in with acknowledgment: ${acknowledgmentNote?.take(50)}")

                when (val outcome = repo.checkInSignature(
                    tToken = tToken,
                    faceRecognitionQualityScore = _ui.value.faceVerificationQualityScore,
                    faceVerify = _ui.value.faceVerificationSuccess,
                    lateReason = lateReason,
                    outOfRangeReason = outOfRangeReason,
                    acknowledgmentNote = acknowledgmentNote
                )) {
                    is SignatureOutcome.Success -> {
                        Log.d(TAG, "✅ Check-in signature SUCCESS")

                        Log.d(TAG, "🚀 Starting tracking components...")

                        try {
                            val trackingStateManager = TrackingStateManager(context)
                            trackingStateManager.setTrackingActive(true)
                            trackingStateManager.updateHeartbeat()

                            // ✅ DISABLED: LocationTrackingService.startTracking(context)

                            TrackingWorker.schedule(context)
                            TrackingAlarmReceiver.schedule(context)

                            activity?.let {
                                if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
                                    Log.d(TAG, "🔋 Requesting battery optimization exemption")
                                    BatteryOptimizationHelper.requestExemption(it)
                                }
                            }

                            Log.d(TAG, "✅ All tracking components started successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "⚠️ Error starting tracking components (non-critical)", e)
                        }

                        _ui.value = _ui.value.copy(
                            isLoading = false,
                            loadingMessage = null,
                            currentState = "CHECK_OUT_NEEDED",
                            lastCheckInTime = outcome.checkInTime,
                            checkInTToken = null,
                            checkInFaceVector = null,
                            checkInMessage = null,
                            faceVerificationQualityScore = null,
                            faceVerificationSuccess = false,
                            pendingMessage = null,
                            acknowledgmentNote = null
                        )

                        emitSuccess(outcome.message)
                    }

                    is SignatureOutcome.Error -> {
                        Log.e(TAG, "❌ CHECK-IN SIGNATURE ERROR: ${outcome.message}")
                        _ui.value = _ui.value.copy(
                            isLoading = false,
                            loadingMessage = null,
                            checkInTToken = null,
                            checkInFaceVector = null,
                            checkInMessage = null,
                            pendingMessage = null,
                            acknowledgmentNote = null
                        )
                        emitError(outcome.message)
                        delay(500)
                        refreshStatus(force = true)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Check-in error", e)
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    loadingMessage = null
                )
                emitError("Check-in failed: ${e.message}")
            }
        }
    }

    fun completeCheckOut(
        earlyReason: String?,
        outOfRangeReason: String?,
        workReport: String?,
        workReportFileUri: Uri?,
        context: Context
    ) {
        val tToken = _ui.value.checkOutTToken
        if (tToken == null) {
            Log.e(TAG, "❌ Invalid check-out session")
            emitError("Invalid check-out session")
            return
        }

        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(
                    isLoading = true,
                    loadingMessage = "Syncing location data...",
                    showReasonDialog = false,
                    showWorkReportDialog = false
                )

                Log.d(TAG, "🔄 Starting location sync before check-out...")

                try {
                    val syncManager = PersistentSyncManager.getInstance(context)
                    val syncResult = syncManager.forceSyncAllBeforeCheckout(
                        maxWaitTimeMs = 30_000L
                    )

                    when (syncResult) {
                        is PersistentSyncManager.SyncResult.Success -> {
                            Log.d(TAG, "✅ All locations synced: ${syncResult.synced} locations")
                        }

                        is PersistentSyncManager.SyncResult.Timeout -> {
                            Log.w(TAG, "⏱️ Sync timeout: ${syncResult.synced} synced, ${syncResult.failed} failed")
                        }

                        is PersistentSyncManager.SyncResult.NetworkError -> {
                            Log.e(TAG, "🌐 Network error during sync")
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                loadingMessage = null
                            )
                            emitError("Network error. Please check your connection and try again.")
                            return@launch
                        }

                        is PersistentSyncManager.SyncResult.Error -> {
                            Log.e(TAG, "❌ Sync error: ${syncResult.message}")
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                loadingMessage = null
                            )
                            emitError("Failed to sync locations: ${syncResult.message}")
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Error syncing locations (non-critical)", e)
                }

                _ui.value = _ui.value.copy(
                    loadingMessage = "Completing check-out..."
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
                        Log.d(TAG, "✅ Check-out signature SUCCESS")

                        Log.d(TAG, "⏹️ Stopping tracking components...")

                        try {
                            // ✅ DISABLED: LocationTrackingService.stopTracking(context)
                            TrackingWorker.cancel(context)
                            TrackingAlarmReceiver.cancel(context)

                            val trackingStateManager = TrackingStateManager(context)
                            trackingStateManager.clearState()

                            Log.d(TAG, "✅ All tracking components stopped")
                        } catch (e: Exception) {
                            Log.e(TAG, "⚠️ Error stopping tracking components (non-critical)", e)
                        }

                        _ui.value = _ui.value.copy(
                            isLoading = false,
                            loadingMessage = null,
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
                        Log.e(TAG, "❌ CHECK-OUT SIGNATURE ERROR: ${outcome.message}")
                        _ui.value = _ui.value.copy(
                            isLoading = false,
                            loadingMessage = null,
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

            } catch (e: Exception) {
                Log.e(TAG, "❌ Check-out error", e)
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    loadingMessage = null
                )
                emitError("Check-out failed: ${e.message}")
            }
        }
    }

    fun onReasonSubmitted(lateOrEarlyReason: String?, outOfRangeReason: String?, context: Context) {
        val isCheckIn = _ui.value.checkInTToken != null

        if (isCheckIn) {
            completeCheckIn(lateOrEarlyReason, outOfRangeReason, context)
        } else {
            if (_ui.value.checkOutWorkReportRequired) {
                _ui.value = _ui.value.copy(
                    showReasonDialog = false,
                    showWorkReportDialog = true,
                    tempEarlyReason = lateOrEarlyReason,
                    tempOutOfRangeReason = outOfRangeReason
                )
            } else {
                completeCheckOut(lateOrEarlyReason, outOfRangeReason, null, null, context)
            }
        }
    }

    fun onWorkReportSubmitted(workReport: String, fileUri: Uri?, context: Context) {
        completeCheckOut(
            _ui.value.tempEarlyReason,
            _ui.value.tempOutOfRangeReason,
            workReport,
            fileUri,
            context
        )
    }

    fun canCheckIn(): Boolean = _ui.value.currentState == "CHECK_IN_NEEDED"
    fun canCheckOut(): Boolean = _ui.value.currentState == "CHECK_OUT_NEEDED"
    fun isComplete(): Boolean = _ui.value.isComplete == true
}

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadingMessage: String? = null,

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
    val showPendingMessageDialog: Boolean = false,

    val faceVerificationQualityScore: Float? = null,
    val faceVerificationSuccess: Boolean = false,

    val tempEarlyReason: String? = null,
    val tempOutOfRangeReason: String? = null,

    val pendingMessage: PendingMessage? = null,
    val acknowledgmentNote: String? = null
)