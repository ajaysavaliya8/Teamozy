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
import com.hrms.jeejateamozy.feature.location.model.LocationData
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

    // ==========================================
    // LIFECYCLE & STATUS
    // ==========================================

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

    // ==========================================
    // CHECK-IN FLOW
    // ==========================================

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

    /**
     * REVISED: Complete check-in with CLEAR database + LIVE location capture
     * ✅ FIXED: All compilation errors resolved
     */
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
                    loadingMessage = "Clearing old location data...",
                    showReasonDialog = false
                )

                // ==========================================
                // STEP 1: CLEAR all existing pending locations (fresh start)
                // ==========================================
                Log.d(TAG, "🗑️ Clearing all old pending locations before check-in...")

                val syncManager = PersistentSyncManager.getInstance(context)

                try {
                    val cleared = syncManager.clearAllPendingLocations()
                    if (cleared) {
                        Log.d(TAG, "✅ Successfully cleared all old location data")
                    } else {
                        Log.w(TAG, "⚠️ Failed to clear old location data, but continuing...")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error clearing old locations", e)
                    // Continue anyway - this shouldn't block check-in
                }

                // ==========================================
                // STEP 2: Capture CURRENT LIVE location
                // ==========================================
                _ui.value = _ui.value.copy(
                    loadingMessage = "Getting current location..."
                )

                val firstLocationData = try {
                    Log.d(TAG, "📍 Capturing LIVE location for check-in...")

                    val locationHelper = LocationHelper(context)

                    // Check if location is enabled
                    if (!locationHelper.isLocationEnabled()) {
                        Log.w(TAG, "⚠️ Location is disabled - check-in will proceed without location")
                        null
                    } else {
                        // Capture fresh location
                        when (val result = locationHelper.getCurrentLocation(
                            desiredAccuracyMeters = 75f,
                            hardTimeoutMs = 10_000
                        )) {
                            is LocationResult.Success -> {
                                Log.d(TAG, "✅ Live location captured: (${result.latitude}, ${result.longitude}), accuracy: ${result.accuracy}m")

                                // Create LocationData from live location
                                createLocationData(context, result.latitude, result.longitude, result.accuracy)
                            }
                            is LocationResult.Error -> {
                                Log.w(TAG, "⚠️ Failed to capture live location: ${result.message}")
                                null
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error capturing live location", e)
                    null
                }

                if (firstLocationData == null) {
                    Log.w(TAG, "⚠️ No live location data available - check-in will proceed without location")
                }

                // ==========================================
                // STEP 3: Complete check-in with LIVE location data
                // ==========================================
                _ui.value = _ui.value.copy(
                    loadingMessage = "Completing check-in..."
                )

                val acknowledgmentNote = _ui.value.acknowledgmentNote

                Log.d(TAG, "Completing check-in with acknowledgment: ${acknowledgmentNote?.take(50)}")

                val outcome = repo.checkInSignature(
                    tToken = tToken,
                    faceRecognitionQualityScore = _ui.value.faceRecognitionQualityScore,
                    faceVerify = _ui.value.faceVerificationSuccess,
                    lateReason = lateReason,
                    outOfRangeReason = outOfRangeReason,
                    acknowledgmentNote = acknowledgmentNote,
                    firstLocationData = firstLocationData  // ✅ NEW: Pass LIVE location data
                )

                when (outcome) {
                    is SignatureOutcome.Success -> {
                        Log.d(TAG, "✅ Check-in completed: ${outcome.message}")

                        // Start location tracking after successful check-in
                        try {
                            val trackingStateManager = TrackingStateManager(context)
                            trackingStateManager.setTrackingActive(true)

                            TrackingWorker.schedule(context)
                            TrackingAlarmReceiver.schedule(context)

                            if (activity != null) {
                                // ✅ FIXED: Changed from requestIgnoreBatteryOptimizations to requestExemption
                                BatteryOptimizationHelper.requestExemption(activity)
                            }

                            Log.d(TAG, "✅ Location tracking started")
                        } catch (e: Exception) {
                            Log.e(TAG, "⚠️ Error starting tracking (non-critical)", e)
                        }

                        _ui.value = _ui.value.copy(
                            isLoading = false,
                            loadingMessage = null,
                            currentState = "CHECK_OUT_NEEDED",
                            lastCheckInTime = outcome.checkInTime,
                            checkInTToken = null,
                            checkInFaceVector = null,
                            checkInMessage = null,
                            faceRecognitionQualityScore = null,
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
                Log.e(TAG, "❌ Error in completeCheckIn", e)
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    loadingMessage = null
                )
                emitError("An error occurred during check-in")
            }
        }
    }

    // ==========================================
    // CHECK-OUT FLOW
    // ==========================================

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
                    showReasonDialog = outcome.earlyReasonRequired || outcome.outOfRangeReasonRequired
                )

                if (!outcome.earlyReasonRequired && !outcome.outOfRangeReasonRequired) {
                    if (outcome.workReportRequired) {
                        _ui.value = _ui.value.copy(showWorkReportDialog = true)
                    } else {
                        completeCheckOut(null, null, null, null, context)
                    }
                }
            }

            is CheckOutOutcome.Success -> {
                Log.d(TAG, "✅ Check-out directly successful")
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    currentState = "CHECK_IN_NEEDED"
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

    /**
     * REVISED: Complete check-out with SYNC ALL + LIVE location capture
     * ✅ FIXED: All compilation errors resolved
     */
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
                    loadingMessage = "Syncing location history...",
                    showReasonDialog = false,
                    showWorkReportDialog = false
                )

                // ==========================================
                // STEP 1: PUSH/SYNC all existing pending locations FIRST
                // ==========================================
                Log.d(TAG, "🔄 Syncing ALL pending locations before check-out...")

                val syncManager = PersistentSyncManager.getInstance(context)

                try {
                    val hasPending = syncManager.hasPendingLocations()

                    if (hasPending) {
                        Log.d(TAG, "📊 Found pending locations - starting sync...")

                        val syncResult = syncManager.forceSyncAllBeforeCheckout(
                            maxWaitTimeMs = 10_000L  // Give 10 seconds for sync
                        )

                        when (syncResult) {
                            is PersistentSyncManager.SyncResult.Success -> {
                                Log.d(TAG, "✅ All ${syncResult.synced} pending locations synced successfully")
                            }

                            is PersistentSyncManager.SyncResult.Timeout -> {
                                Log.w(TAG, "⏱️ Sync timeout: ${syncResult.synced} synced, ${syncResult.failed} failed")

                                // Show warning but allow check-out to continue
                                if (syncResult.failed > 0) {
                                    Log.w(TAG, "⚠️ ${syncResult.failed} locations could not be synced in time")
                                }
                            }

                            is PersistentSyncManager.SyncResult.NetworkError -> {
                                Log.e(TAG, "🌐 Network error during sync: ${syncResult.synced} synced, ${syncResult.failed} failed")

                                // Don't block check-out for network errors
                                Log.w(TAG, "⚠️ Proceeding with check-out despite network error")
                            }

                            is PersistentSyncManager.SyncResult.Error -> {
                                Log.e(TAG, "❌ Sync error: ${syncResult.message}")
                                Log.w(TAG, "⚠️ Proceeding with check-out despite sync error")
                            }
                        }
                    } else {
                        Log.d(TAG, "✅ No pending locations to sync")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error during location sync", e)
                    // Don't block check-out - this is best effort
                    Log.w(TAG, "⚠️ Proceeding with check-out despite sync error")
                }

                // ==========================================
                // STEP 2: Capture CURRENT LIVE location
                // ==========================================
                _ui.value = _ui.value.copy(
                    loadingMessage = "Getting current location..."
                )

                val lastLocationData = try {
                    Log.d(TAG, "📍 Capturing LIVE location for check-out...")

                    val locationHelper = LocationHelper(context)

                    // Check if location is enabled
                    if (!locationHelper.isLocationEnabled()) {
                        Log.w(TAG, "⚠️ Location is disabled - check-out will proceed without location")
                        null
                    } else {
                        // Capture fresh location
                        when (val result = locationHelper.getCurrentLocation(
                            desiredAccuracyMeters = 75f,
                            hardTimeoutMs = 10_000
                        )) {
                            is LocationResult.Success -> {
                                Log.d(TAG, "✅ Live location captured: (${result.latitude}, ${result.longitude}), accuracy: ${result.accuracy}m")

                                // Create LocationData from live location
                                createLocationData(context, result.latitude, result.longitude, result.accuracy)
                            }
                            is LocationResult.Error -> {
                                Log.w(TAG, "⚠️ Failed to capture live location: ${result.message}")
                                null
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error capturing live location", e)
                    null
                }

                if (lastLocationData == null) {
                    Log.w(TAG, "⚠️ No live location data available - check-out will proceed without location")
                }

                // ==========================================
                // STEP 3: Complete check-out with LIVE location data
                // ==========================================
                _ui.value = _ui.value.copy(
                    loadingMessage = "Completing check-out..."
                )

                val outcome = repo.checkOutSignature(
                    tToken = tToken,
                    faceRecognitionQualityScore = _ui.value.faceRecognitionQualityScore,
                    faceVerify = _ui.value.faceVerificationSuccess,
                    earlyReason = earlyReason,
                    outOfRangeReason = outOfRangeReason,
                    workReport = workReport,
                    workReportFileUri = workReportFileUri,
                    lastLocationData = lastLocationData  // ✅ NEW: Pass LIVE location data
                )

                when (outcome) {
                    is SignatureOutcome.Success -> {
                        Log.d(TAG, "✅ Check-out completed: ${outcome.message}")

                        // Stop location tracking after successful check-out
                        try {
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
                            faceRecognitionQualityScore = null,
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
                Log.e(TAG, "❌ Error in completeCheckOut", e)
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    loadingMessage = null
                )
                emitError("An error occurred during check-out")
            }
        }
    }

    // ==========================================
    // UI INTERACTION HANDLERS
    // ==========================================

    /**
     * ✅ FIXED: Changed requiresAcknowledgment to requires_acknowledgment
     */
    fun onPendingMessageAcknowledged(note: String?) {
        _ui.value = _ui.value.copy(
            acknowledgmentNote = note,
            showPendingMessageDialog = false,
            // ✅ FIXED: Changed from requiresAcknowledgment to requires_acknowledgment
            requiresAcknowledgment = _ui.value.pendingMessage?.requires_acknowledgment ?: false
        )

        val lateReasonRequired = _ui.value.checkInLateReasonRequired
        val outOfRangeReasonRequired = _ui.value.checkInOutOfRangeReasonRequired
        val faceVerificationRequired = _ui.value.checkInFaceVector != null

        when {
            faceVerificationRequired -> {
                _ui.value = _ui.value.copy(showFaceVerification = true)
            }
            lateReasonRequired || outOfRangeReasonRequired -> {
                _ui.value = _ui.value.copy(showReasonDialog = true)
            }
            else -> {
                // No additional requirements, complete check-in
                // This will be handled by the UI
            }
        }
    }

    /**
     * ✅ NEW: Added method for dismissing pending message dialog
     */
    fun onPendingMessageDismissed() {
        _ui.value = _ui.value.copy(
            showPendingMessageDialog = false,
            pendingMessage = null,
            acknowledgmentNote = null
        )
        emitError("Check-in cancelled")
    }

    fun onFaceVerificationComplete(qualityScore: Float, verified: Boolean, context: Context) {
        _ui.value = _ui.value.copy(
            faceRecognitionQualityScore = qualityScore,
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

    fun onFaceVerificationCancelled() {
        val isCheckIn = _ui.value.checkInTToken != null

        _ui.value = _ui.value.copy(
            showFaceVerification = false,
            checkInTToken = if (isCheckIn) null else _ui.value.checkInTToken,
            checkInFaceVector = if (isCheckIn) null else _ui.value.checkInFaceVector,
            checkInMessage = if (isCheckIn) null else _ui.value.checkInMessage,
            checkOutTToken = if (!isCheckIn) null else _ui.value.checkOutTToken,
            checkOutFaceVector = if (!isCheckIn) null else _ui.value.checkOutFaceVector,
            checkOutMessage = if (!isCheckIn) null else _ui.value.checkOutMessage
        )

        emitError("${if (isCheckIn) "Check-in" else "Check-out"} cancelled")
    }

    fun onReasonDialogDismissed() {
        val isCheckIn = _ui.value.checkInTToken != null

        _ui.value = _ui.value.copy(
            showReasonDialog = false,
            checkInTToken = if (isCheckIn) null else _ui.value.checkInTToken,
            checkInMessage = if (isCheckIn) null else _ui.value.checkInMessage,
            checkInIsLate = false,
            checkInIsOutOfRange = false,
            checkInLateReasonRequired = false,
            checkInOutOfRangeReasonRequired = false,
            checkOutTToken = if (!isCheckIn) null else _ui.value.checkOutTToken,
            checkOutMessage = if (!isCheckIn) null else _ui.value.checkOutMessage,
            checkOutIsEarly = false,
            checkOutIsOutOfRange = false,
            checkOutEarlyReasonRequired = false,
            checkOutOutOfRangeReasonRequired = false,
            faceRecognitionQualityScore = null,
            faceVerificationSuccess = false
        )

        emitError("${if (isCheckIn) "Check-in" else "Check-out"} cancelled")
    }

    fun onWorkReportDialogDismissed() {
        _ui.value = _ui.value.copy(
            showWorkReportDialog = false,
            checkOutTToken = null,
            checkOutMessage = null,
            checkOutMinimumQualityScore = null,
            checkOutWorkHours = null,
            checkOutIsEarly = false,
            checkOutIsOutOfRange = false,
            checkOutEarlyReasonRequired = false,
            checkOutOutOfRangeReasonRequired = false,
            checkOutWorkReportRequired = false,
            faceRecognitionQualityScore = null,
            faceVerificationSuccess = false,
            tempEarlyReason = null,
            tempOutOfRangeReason = null
        )

        emitError("Check-out cancelled")
    }

    // ==========================================
    // HELPER FUNCTIONS
    // ==========================================

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

    /**
     * ✅ FIXED: Changed ISO timestamp formatting to support older Android versions
     */
    /**
     * Creates ISO 8601 timestamp - guaranteed non-null
     */
    private fun createIsoTimestamp(): String {
        val timestamp = System.currentTimeMillis()
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.Instant.ofEpochMilli(timestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            } else {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getDefault()
                sdf.format(java.util.Date(timestamp))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting timestamp", e)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getDefault()
            sdf.format(java.util.Date(timestamp))
        }
    }

    private fun createLocationData(
        context: Context,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ): LocationData {
        // Create ISO timestamp - guaranteed non-null
        val isoTimestamp = createIsoTimestamp()

        // Get device info
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: ""

        val appVersion: String = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }

        // Get network info
        val networkType = getNetworkType(context)

        // Get WiFi info
        val (wifiName, wifiMac) = getWifiInfo(context)

        // Get battery level
        val batteryLevel = getBatteryLevel(context)

        return LocationData(
            recordedAt = isoTimestamp,
            latitude = latitude,
            longitude = longitude,
            locationAccuracy = accuracy,
            altitude = null, // Not available from getCurrentLocation
            verticalAccuracy = null,
            speed = null,
            heading = null,
            deviceId = deviceId,
            appVersion = appVersion,
            networkType = networkType,
            wifiName = wifiName,
            wifiMacAddress = wifiMac,
            batteryLevel = batteryLevel,
            geofenceId = null // Will be determined by backend
        )
    }

    private fun getNetworkType(context: Context): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            when {
                capabilities == null -> "NONE"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "UNKNOWN"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network type", e)
            "UNKNOWN"
        }
    }

    private fun getWifiInfo(context: Context): Pair<String?, String?> {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val wifiInfo = wifiManager.connectionInfo

            val ssid = wifiInfo?.ssid?.trim('"') ?: null
            val bssid = wifiInfo?.bssid ?: null

            Pair(ssid, bssid)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi info", e)
            Pair(null, null)
        }
    }

    private fun getBatteryLevel(context: Context): Int? {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level", e)
            null
        }
    }

    // ==========================================
    // UTILITY FUNCTIONS
    // ==========================================

    fun canCheckIn(): Boolean = _ui.value.currentState == "CHECK_IN_NEEDED"
    fun canCheckOut(): Boolean = _ui.value.currentState == "CHECK_OUT_NEEDED"
    fun isComplete(): Boolean = _ui.value.isComplete == true
}

// ==========================================
// UI STATE
// ==========================================

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadingMessage: String? = null,

    val currentState: String = "CHECK_IN_NEEDED",
    val statusMessage: String = "",
    val lastCheckInTime: String? = null,
    val attendanceStatus: String? = null,
    val isComplete: Boolean? = null,

    // Check-in state
    val checkInTToken: String? = null,
    val checkInMinimumQualityScore: Float? = null,
    val checkInIsLate: Boolean = false,
    val checkInIsOutOfRange: Boolean = false,
    val checkInLateReasonRequired: Boolean = false,
    val checkInOutOfRangeReasonRequired: Boolean = false,
    val checkInMessage: String? = null,
    val checkInFaceVector: FloatArray? = null,

    // Check-out state
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

    // Face verification state
    val faceRecognitionQualityScore: Float? = null,
    val faceVerificationSuccess: Boolean = false,

    // Dialog states
    val showFaceVerification: Boolean = false,
    val showReasonDialog: Boolean = false,
    val showWorkReportDialog: Boolean = false,
    val showPendingMessageDialog: Boolean = false,

    // Pending message state
    val pendingMessage: PendingMessage? = null,
    val requiresAcknowledgment: Boolean = false,
    val acknowledgmentNote: String? = null,

    // Temporary state for multi-step flows
    val tempEarlyReason: String? = null,
    val tempOutOfRangeReason: String? = null,
    val workReportFileUri: Uri? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttendanceUiState

        if (isLoading != other.isLoading) return false
        if (isRefreshing != other.isRefreshing) return false
        if (loadingMessage != other.loadingMessage) return false
        if (currentState != other.currentState) return false
        if (statusMessage != other.statusMessage) return false
        if (lastCheckInTime != other.lastCheckInTime) return false
        if (attendanceStatus != other.attendanceStatus) return false
        if (isComplete != other.isComplete) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + isRefreshing.hashCode()
        result = 31 * result + (loadingMessage?.hashCode() ?: 0)
        result = 31 * result + currentState.hashCode()
        result = 31 * result + statusMessage.hashCode()
        result = 31 * result + (lastCheckInTime?.hashCode() ?: 0)
        result = 31 * result + (attendanceStatus?.hashCode() ?: 0)
        result = 31 * result + (isComplete?.hashCode() ?: 0)
        return result
    }
}