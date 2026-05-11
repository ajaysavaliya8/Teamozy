package com.hrms.jeejateamozy.feature.attendance.presentation

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.core.network.PendingMessage
import com.hrms.jeejateamozy.core.network.WorkReportQuestionSet
import com.hrms.jeejateamozy.core.utils.LocationHelper
import com.hrms.jeejateamozy.core.utils.LocationResult
import com.hrms.jeejateamozy.feature.attendance.data.AttendanceOutcome
import com.hrms.jeejateamozy.feature.attendance.data.AttendanceRepository
import com.hrms.jeejateamozy.feature.attendance.data.CheckInOutcome
import com.hrms.jeejateamozy.feature.attendance.data.CheckOutOutcome
import com.hrms.jeejateamozy.feature.attendance.data.LocationReverifyOutcome
import com.hrms.jeejateamozy.feature.attendance.data.SignatureOutcome
import com.hrms.jeejateamozy.feature.location.model.LocationData
import com.hrms.jeejateamozy.feature.location.data.repository.LocationRepository
import com.hrms.jeejateamozy.feature.location.data.repository.ForceSyncResult
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingWorker
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingAlarmReceiver
import com.hrms.jeejateamozy.feature.location.keepalive.BatteryOptimizationHelper
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.util.LiveLocationHelper
import com.hrms.jeejateamozy.feature.location.util.LiveLocationResult
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

sealed class AttendanceEvent {
    data class ShowError(val message: String) : AttendanceEvent()
    data class ShowSuccess(val message: String) : AttendanceEvent()
}

class AttendanceViewModel(
    private val repo: AttendanceRepository
) : ViewModel(), KoinComponent {

    companion object {
        private const val TAG = "AttendanceViewModel"
    }

    // Inject LocationRepository via Koin
    private val locationRepository: LocationRepository by inject()

    private val _ui = MutableStateFlow(AttendanceUiState())
    val ui: StateFlow<AttendanceUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<AttendanceEvent>()
    val events: SharedFlow<AttendanceEvent> = _events.asSharedFlow()

    private var hasLoadedInitialStatus = false

    // Pre-fetch live location during face verification so it's ready instantly after
    private var prefetchedLiveLocation: Deferred<LiveLocationResult>? = null

    private fun startLocationPrefetch(context: Context) {
        prefetchedLiveLocation?.cancel()
        prefetchedLiveLocation = viewModelScope.async {
            LiveLocationHelper(context).captureLiveLocation()
        }
    }

    private fun cancelLocationPrefetch() {
        prefetchedLiveLocation?.cancel()
        prefetchedLiveLocation = null
    }

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

    /**
     * Refresh attendance status from server
     *
     * @param force Force refresh even if already loading
     * @param context Optional context for auto-starting location tracking when CHECK_OUT_NEEDED
     */
    fun refreshStatus(force: Boolean = false, context: Context? = null) {
        if (_ui.value.isLoading && !force) {
            Log.d(TAG, "Already loading, skipping refresh")
            return
        }

        // Don't refresh if there's an active check-in/check-out flow in progress
        // (e.g. face verification just completed and reason dialog should be shown)
        if (!force && (_ui.value.checkInTToken != null || _ui.value.checkOutTToken != null)) {
            Log.d(TAG, "Active check-in/check-out flow in progress, skipping refresh")
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
                        checkOutTime = outcome.checkOutTime,
                        checkInTToken = null,
                        checkOutTToken = null,
                        showFaceVerification = false,
                        showReasonDialog = false,
                        showWorkReportDialog = false,
                        showPendingMessageDialog = false,
                        pendingMessage = null,
                        acknowledgmentNote = null
                    )

                    // Pre-warm GPS so check-in/check-out button responds instantly
                    if (context != null) {
                        try { LocationHelper(context).warmUpGps() } catch (_: Exception) {}
                    }

                    // ==========================================
                    // AUTO-START LOCATION TRACKING ON APP RESTART
                    // ==========================================
                    // If check-in is done and check-out is pending,
                    // ensure location tracking service is running
                    if (outcome.currentState == "CHECK_OUT_NEEDED" && context != null) {
                        ensureLocationTrackingRunning(context)
                    }
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

    /**
     * Ensure location tracking is running when check-in is done but check-out is pending
     * This is called when:
     * 1. App is killed and restarted
     * 2. check-status API returns CHECK_OUT_NEEDED
     */
    private fun ensureLocationTrackingRunning(context: Context) {
        try {
            val stateManager = TrackingStateManager(context)

            // Check if tracking should be active based on our state
            val shouldBeActive = stateManager.shouldTrackingBeActive()

            if (shouldBeActive) {
                Log.d(TAG, "✅ Location tracking state exists - ensuring service is running")
                // State exists, just make sure service is running
                LocationTrackingService.startTracking(context)
            } else {
                // No tracking state exists - this means either:
                // 1. App was freshly installed and check-in was done on another device (edge case)
                // 2. State was cleared somehow
                //
                // We should start tracking and mark state as active
                Log.d(TAG, "🚀 Starting location tracking (no existing state found)")

                // Mark tracking as active
                stateManager.setTrackingActive(true)
                stateManager.updateHeartbeat()

                // Schedule keep-alive mechanisms
                TrackingWorker.schedule(context)
                TrackingAlarmReceiver.schedule(context)

                // Start the service
                LocationTrackingService.startTracking(context)

                Log.d(TAG, "✅ Location tracking started on app restart")
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error ensuring location tracking is running", e)
        }
    }

    fun startCheckIn(context: Context) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, loadingMessage = "Location fetching...")

            when (val locResult = LocationHelper(context).getCurrentLocation()) {
                is LocationResult.Success -> {
                    if (locResult.isMocked) {
                        _ui.value = _ui.value.copy(isLoading = false, loadingMessage = null)
                        emitError("Fake GPS detected. Please disable mock location apps.")
                        return@launch
                    }
                    _ui.value = _ui.value.copy(loadingMessage = null)
                    performCheckIn(locResult.latitude, locResult.longitude, context)
                }
                is LocationResult.Error -> {
                    _ui.value = _ui.value.copy(isLoading = false, loadingMessage = null)
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

                // Pre-fetch location while user does face verification
                if (pendingMessage == null) {
                    startLocationPrefetch(context)
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
                    showReasonDialog = (outcome.lateReasonRequired || outcome.outOfRangeReasonRequired) && pendingMessage == null,
                    reverifyError = null
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
            _ui.value = _ui.value.copy(isLoading = true, loadingMessage = "Location fetching...")

            when (val locResult = LocationHelper(context).getCurrentLocation()) {
                is LocationResult.Success -> {
                    if (locResult.isMocked) {
                        _ui.value = _ui.value.copy(isLoading = false, loadingMessage = null)
                        emitError("Fake GPS detected. Please disable mock location apps.")
                        return@launch
                    }
                    _ui.value = _ui.value.copy(loadingMessage = null)
                    performCheckOut(locResult.latitude, locResult.longitude, context)
                }
                is LocationResult.Error -> {
                    _ui.value = _ui.value.copy(isLoading = false, loadingMessage = null)
                    emitError(locResult.message)
                }
            }
        }
    }

    private suspend fun performCheckOut(latitude: Double, longitude: Double, context: Context) {
        when (val outcome = repo.checkOut(latitude, longitude)) {
            is CheckOutOutcome.RequiresFaceVerification -> {
                Log.d(TAG, "✅ Check-out requires face verification")

                // If the server attached a pending message for AT_CHECKOUT delivery, show
                // that dialog first. The existing pendingMessage ack flow then advances
                // to the next step (face verification in this branch).
                val pending = outcome.pendingMessage
                if (pending == null) {
                    // Pre-fetch location while user does face verification
                    startLocationPrefetch(context)
                }

                _ui.value = _ui.value.copy(
                    isLoading = false,
                    checkOutTToken = outcome.tToken,
                    checkOutMinimumQualityScore = outcome.minimumQualityScore,
                    checkOutWorkMinutes = outcome.workMinutes,
                    checkOutIsEarly = outcome.isEarly,
                    checkOutIsOutOfRange = outcome.isOutOfRange,
                    checkOutEarlyReasonRequired = outcome.earlyReasonRequired,
                    checkOutOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                    checkOutWorkReportRequired = outcome.workReportRequired,
                    checkOutWorkReportQuestionSet = outcome.workReportQuestionSet,
                    checkOutPriorityOptions = outcome.priorityOptions,
                    checkOutMessage = outcome.message,
                    checkOutFaceVector = outcome.faceVector,
                    pendingMessage = pending,
                    showPendingMessageDialog = pending != null,
                    showFaceVerification = pending == null
                )
            }

            is CheckOutOutcome.RequiresReasons -> {
                Log.d(TAG, "✅ Check-out requires reasons")
                val pending = outcome.pendingMessage
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    checkOutTToken = outcome.tToken,
                    checkOutWorkMinutes = outcome.workMinutes,
                    checkOutIsEarly = outcome.isEarly,
                    checkOutIsOutOfRange = outcome.isOutOfRange,
                    checkOutEarlyReasonRequired = outcome.earlyReasonRequired,
                    checkOutOutOfRangeReasonRequired = outcome.outOfRangeReasonRequired,
                    checkOutWorkReportRequired = outcome.workReportRequired,
                    checkOutWorkReportQuestionSet = outcome.workReportQuestionSet,
                    checkOutPriorityOptions = outcome.priorityOptions,
                    checkOutMessage = outcome.message,
                    pendingMessage = pending,
                    showPendingMessageDialog = pending != null,
                    showReasonDialog = pending == null && (outcome.earlyReasonRequired || outcome.outOfRangeReasonRequired),
                    showWorkReportDialog = pending == null && outcome.workReportRequired && !outcome.earlyReasonRequired && !outcome.outOfRangeReasonRequired,
                    reverifyError = null
                )

                if (pending == null && !outcome.earlyReasonRequired && !outcome.outOfRangeReasonRequired && !outcome.workReportRequired) {
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

        // Route to the next step of whichever flow the message was attached to.
        // Checkout takes precedence if its t_token is set (the checkout pending
        // message is delivered at /check-out, by which point checkInTToken is null).
        val isCheckOut = _ui.value.checkOutTToken != null && _ui.value.checkInTToken == null

        if (isCheckOut) {
            val state = _ui.value
            val hasFaceVerification = state.checkOutFaceVector != null
            val hasReasons = state.checkOutEarlyReasonRequired || state.checkOutOutOfRangeReasonRequired
            val needsWorkReport = state.checkOutWorkReportRequired
            when {
                hasFaceVerification -> {
                    startLocationPrefetch(context)
                    _ui.value = state.copy(showFaceVerification = true)
                }
                hasReasons -> {
                    _ui.value = state.copy(showReasonDialog = true, reverifyError = null)
                }
                needsWorkReport -> {
                    _ui.value = state.copy(showWorkReportDialog = true)
                }
                else -> {
                    completeCheckOut(null, null, null, null, context)
                }
            }
            return
        }

        val hasLateOrOutOfRangeReasons = _ui.value.checkInLateReasonRequired || _ui.value.checkInOutOfRangeReasonRequired
        val hasFaceVerification = _ui.value.checkInFaceVector != null

        when {
            hasFaceVerification -> {
                startLocationPrefetch(context)
                _ui.value = _ui.value.copy(showFaceVerification = true)
            }
            hasLateOrOutOfRangeReasons -> {
                _ui.value = _ui.value.copy(showReasonDialog = true, reverifyError = null)
            }
            else -> {
                completeCheckIn(null, null, context)
            }
        }
    }

    fun onPendingMessageDismissed(context: Context? = null) {
        // Check-out takes precedence if its t_token is set (pending_message is
        // delivered at /check-out — by which point checkInTToken is null).
        val isCheckOut = _ui.value.checkOutTToken != null && _ui.value.checkInTToken == null
        Log.d(TAG, "❌ Message cancelled - stopping ${if (isCheckOut) "check-out" else "check-in"} process")

        cancelLocationPrefetch()

        // Re-warm GPS so the next attempt is fast
        context?.let { try { LocationHelper(it).warmUpGps() } catch (_: Exception) {} }

        if (isCheckOut) {
            _ui.value = _ui.value.copy(
                isLoading = false,
                showPendingMessageDialog = false,
                pendingMessage = null,
                acknowledgmentNote = null,
                checkOutTToken = null,
                checkOutFaceVector = null,
                checkOutMessage = null,
                checkOutMinimumQualityScore = null,
                checkOutIsEarly = false,
                checkOutIsOutOfRange = false,
                checkOutEarlyReasonRequired = false,
                checkOutOutOfRangeReasonRequired = false,
                checkOutWorkReportRequired = false,
                checkOutWorkReportQuestionSet = null,
                checkOutPriorityOptions = emptyList(),
                showFaceVerification = false,
                showReasonDialog = false,
                showWorkReportDialog = false
            )
            emitError("Check-out cancelled")
            return
        }

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

    fun onFaceVerificationCancelled(context: Context? = null) {
        Log.d(TAG, "❌ Face verification cancelled - stopping process")

        cancelLocationPrefetch()

        // Re-warm GPS so the next attempt is fast
        context?.let { try { LocationHelper(it).warmUpGps() } catch (_: Exception) {} }

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
                checkOutWorkMinutes = null,
                checkOutIsEarly = false,
                checkOutIsOutOfRange = false,
                checkOutEarlyReasonRequired = false,
                checkOutOutOfRangeReasonRequired = false,
                checkOutWorkReportRequired = false,
                checkOutWorkReportQuestionSet = null,
                checkOutPriorityOptions = emptyList(),
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
                _ui.value = _ui.value.copy(showReasonDialog = true, reverifyError = null)
            } else {
                completeCheckIn(null, null, context)
            }
        } else {
            val earlyReasonRequired = _ui.value.checkOutEarlyReasonRequired
            val outOfRangeReasonRequired = _ui.value.checkOutOutOfRangeReasonRequired
            val workReportRequired = _ui.value.checkOutWorkReportRequired

            when {
                earlyReasonRequired || outOfRangeReasonRequired -> {
                    _ui.value = _ui.value.copy(showReasonDialog = true, reverifyError = null)
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

    fun onLocationReverify(context: Context) {
        val isCheckIn = _ui.value.checkInTToken != null
        val tToken = if (isCheckIn) _ui.value.checkInTToken else _ui.value.checkOutTToken
        if (tToken == null) {
            emitError("No active session for reverification")
            return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(isReverifying = true, reverifyError = null)

            // Get fresh GPS location
            when (val locResult = LocationHelper(context).getCurrentLocation()) {
                is LocationResult.Success -> {
                    if (locResult.isMocked) {
                        _ui.value = _ui.value.copy(isReverifying = false)
                        emitError("Fake GPS detected. Please disable mock location apps.")
                        return@launch
                    }
                    when (val outcome = repo.locationReverify(tToken, locResult.latitude, locResult.longitude, isCheckOut = !isCheckIn)) {
                        is LocationReverifyOutcome.Success -> {
                            Log.d(TAG, "✅ Location reverify success: ${outcome.message}")
                            if (isCheckIn) {
                                _ui.value = _ui.value.copy(
                                    isReverifying = false,
                                    reverifyError = null,
                                    checkInTToken = outcome.newTToken ?: tToken,
                                    checkInIsOutOfRange = outcome.isOutOfRange,
                                    checkInOutOfRangeReasonRequired = outcome.isOutOfRange
                                )
                            } else {
                                _ui.value = _ui.value.copy(
                                    isReverifying = false,
                                    reverifyError = null,
                                    checkOutTToken = outcome.newTToken ?: tToken,
                                    checkOutIsOutOfRange = outcome.isOutOfRange,
                                    checkOutOutOfRangeReasonRequired = outcome.isOutOfRange
                                )
                            }
                            emitSuccess(outcome.message)
                        }
                        is LocationReverifyOutcome.NotApplicable -> {
                            // Policy doesn't enforce geofence → clear the out-of-range flag
                            // so the signature commit proceeds. Silent on purpose: the user
                            // tapped reverify but there was nothing to verify.
                            Log.i(TAG, "ℹ️ Reverify not applicable: ${outcome.message}")
                            _ui.value = _ui.value.copy(
                                isReverifying = false,
                                reverifyError = null,
                                checkInIsOutOfRange = if (isCheckIn) false else _ui.value.checkInIsOutOfRange,
                                checkInOutOfRangeReasonRequired = if (isCheckIn) false else _ui.value.checkInOutOfRangeReasonRequired,
                                checkOutIsOutOfRange = if (!isCheckIn) false else _ui.value.checkOutIsOutOfRange,
                                checkOutOutOfRangeReasonRequired = if (!isCheckIn) false else _ui.value.checkOutOutOfRangeReasonRequired
                            )
                        }
                        is LocationReverifyOutcome.StaleToken -> {
                            // t_token is spent/expired → restart the check-in flow.
                            Log.w(TAG, "🔁 Reverify stale token: ${outcome.message}")
                            _ui.value = _ui.value.copy(
                                isReverifying = false,
                                reverifyError = outcome.message,
                                checkInTToken = null,
                                checkOutTToken = null,
                                checkInFaceVector = null,
                                checkOutFaceVector = null,
                                checkInMessage = null,
                                checkOutMessage = null,
                                showReasonDialog = false
                            )
                            emitError(outcome.message)
                            delay(500)
                            refreshStatus(force = true, context = context)
                        }
                        is LocationReverifyOutcome.Error -> {
                            Log.e(TAG, "❌ Location reverify error: ${outcome.message}")
                            _ui.value = _ui.value.copy(
                                isReverifying = false,
                                reverifyError = outcome.message
                            )
                        }
                    }
                }
                is LocationResult.Error -> {
                    _ui.value = _ui.value.copy(
                        isReverifying = false,
                        reverifyError = locResult.message
                    )
                }
            }
        }
    }

    fun onReasonDialogDismissed(context: Context? = null) {
        // Re-warm GPS so the next attempt is fast
        context?.let { try { LocationHelper(it).warmUpGps() } catch (_: Exception) {} }
        _ui.value = _ui.value.copy(showReasonDialog = false)
    }

    fun onWorkReportDialogDismissed(context: Context? = null) {
        // Re-warm GPS so the next attempt is fast
        context?.let { try { LocationHelper(it).warmUpGps() } catch (_: Exception) {} }
        _ui.value = _ui.value.copy(showWorkReportDialog = false)
    }

    /**
     * Complete check-in with signature and first location data
     *
     * Flow:
     * 1. Clear all existing pending locations from database (fresh start)
     * 2. Capture CURRENT LIVE location (not from database)
     * 3. Send this live location as "first location"
     * 4. Complete check-in signature
     * 5. START LocationTrackingService
     */
    fun completeCheckIn(
        lateReason: String?,
        outOfRangeReason: String?,
        context: Context,
        activity: Activity? = null
    ) {
        Log.d(TAG, "🔍 completeCheckIn() CALLED")

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

                // ==========================================
                // STEP 1: Clear all existing pending locations (fresh start)
                // ==========================================
                val pendingCount = locationRepository.getPendingCount()

                if (pendingCount > 0) {
                    Log.d(TAG, "🗑️ Clearing $pendingCount old pending locations for fresh start...")
                    locationRepository.clearAllLocations()
                    Log.d(TAG, "✅ Old locations cleared")
                } else {
                    Log.d(TAG, "✅ No old locations to clear")
                }

                // ==========================================
                // STEP 2: Use pre-fetched location (started during face verification)
                // ==========================================
                _ui.value = _ui.value.copy(loadingMessage = "Capturing location...")

                Log.d(TAG, "📍 Getting live location for check-in...")
                val locationResult = prefetchedLiveLocation?.await()
                    ?: LiveLocationHelper(context).captureLiveLocation()
                prefetchedLiveLocation = null

                var firstLocation: LocationData? = null
                when (locationResult) {
                    is LiveLocationResult.Success -> {
                        firstLocation = locationResult.locationData
                        Log.d(TAG, "✅ Live location captured: lat=${firstLocation.latitude}, lng=${firstLocation.longitude}")
                    }
                    is LiveLocationResult.Error -> {
                        Log.w(TAG, "⚠️ Failed to capture live location: ${locationResult.message}")
                        // Continue without first location - non-blocking
                    }
                }

                // ==========================================
                // STEP 3: Complete check-in signature with first location
                // ==========================================
                _ui.value = _ui.value.copy(loadingMessage = "Completing check-in...")

                val acknowledgmentNote = _ui.value.acknowledgmentNote
                Log.d(TAG, "Completing check-in with acknowledgment: ${acknowledgmentNote?.take(50)}")

                when (val outcome = repo.checkInSignature(
                    tToken = tToken,
                    faceRecognitionQualityScore = _ui.value.faceVerificationQualityScore,
                    faceVerify = _ui.value.faceVerificationSuccess,
                    lateReason = lateReason,
                    outOfRangeReason = outOfRangeReason,
                    acknowledgmentNote = acknowledgmentNote,
                    firstLocation = firstLocation
                )) {
                    is SignatureOutcome.Success -> {
                        Log.d(TAG, "✅ Check-in signature SUCCESS")

                        // ==========================================
                        // STEP 4: START LOCATION TRACKING SERVICE
                        // ==========================================
                        Log.d(TAG, "🚀 Starting location tracking service...")

                        try {
                            // Start the foreground service
                            LocationTrackingService.startTracking(context, geofenceId = null)

                            // Update tracking state
                            val trackingStateManager = TrackingStateManager(context)
                            trackingStateManager.setTrackingActive(true)
                            trackingStateManager.updateHeartbeat()

                            // Schedule keep-alive mechanisms
                            TrackingWorker.schedule(context)
                            TrackingAlarmReceiver.schedule(context)

                            // Request battery optimization exemption
                            activity?.let {
                                if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
                                    Log.d(TAG, "🔋 Requesting battery optimization exemption")
                                    BatteryOptimizationHelper.requestExemption(it)
                                }
                            }

                            Log.d(TAG, "✅ Location tracking started successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "⚠️ Error starting location tracking (non-critical)", e)
                        }

                        _ui.value = _ui.value.copy(
                            isLoading = false,
                            loadingMessage = null,
                            currentState = "CHECK_OUT_NEEDED",
                            lastCheckInTime = outcome.checkInTime,
                            checkOutTime = outcome.checkOutTime,
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

                    is SignatureOutcome.Conflict -> {
                        // 409 CONFLICT — one of: concurrent signature race, multi-punch
                        // disabled ("already checked in"), or max check-ins reached.
                        // Clear the spent t_token + face state, show the server's message,
                        // and refresh so the UI snaps to the authoritative state
                        // (CHECK_OUT_NEEDED / DAY_COMPLETE / CHECK_IN_NEEDED).
                        Log.w(TAG, "⚠️ CHECK-IN SIGNATURE CONFLICT: ${outcome.message}")
                        _ui.value = _ui.value.copy(
                            isLoading = false,
                            loadingMessage = null,
                            checkInTToken = null,
                            checkInFaceVector = null,
                            checkInMessage = null,
                            pendingMessage = null,
                            acknowledgmentNote = null,
                            faceVerificationQualityScore = null,
                            faceVerificationSuccess = false
                        )
                        emitError(outcome.message)
                        delay(500)
                        refreshStatus(force = true, context = context)
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
                        refreshStatus(force = true, context = context)
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

    /**
     * Complete check-out with signature and last location data
     *
     * Flow:
     * 1. Push/Sync all existing pending locations from database to server first
     * 2. Capture CURRENT LIVE location (fresh capture)
     * 3. Send this live location as "last location"
     * 4. Complete check-out signature
     * 5. STOP LocationTrackingService
     */
    fun completeCheckOut(
        earlyReason: String?,
        outOfRangeReason: String?,
        workReport: String?,
        workReportFileUri: Uri?,
        context: Context,
        priority: String? = null,
        answers: String? = null
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

                Log.d(TAG, "🔄 Starting check-out process...")

                // ==========================================
                // STEP 1: Sync all existing pending locations to server
                // ==========================================
                try {
                    val pendingCount = locationRepository.getPendingCount()

                    if (pendingCount > 0) {
                        Log.d(TAG, "📤 Syncing $pendingCount pending locations before check-out...")

                        val syncResult = locationRepository.forceSyncAll(maxAttempts = 3)

                        when (syncResult) {
                            is ForceSyncResult.Success -> {
                                Log.d(TAG, "✅ All locations synced: ${syncResult.totalSynced} locations")
                            }

                            is ForceSyncResult.Partial -> {
                                Log.w(TAG, "⚠️ Partial sync: ${syncResult.synced} synced, ${syncResult.remaining} remaining")
                                // Continue anyway - don't block check-out
                            }

                            is ForceSyncResult.SessionEnded -> {
                                Log.w(TAG, "⚠️ Session ended during sync: ${syncResult.syncedBeforeEnd} synced")
                                // Continue with check-out
                            }

                            is ForceSyncResult.StopTracking -> {
                                Log.w(TAG, "⚠️ Stop tracking received during sync: ${syncResult.syncedBeforeStop} synced")
                                // Continue with check-out - the session has already ended server-side
                            }

                            is ForceSyncResult.NetworkError -> {
                                Log.e(TAG, "🌐 Network error during sync: ${syncResult.message}")
                                _ui.value = _ui.value.copy(
                                    isLoading = false,
                                    loadingMessage = null
                                )
                                emitError("Network error. Please check your connection and try again.")
                                return@launch
                            }
                        }
                    } else {
                        Log.d(TAG, "✅ No pending locations to sync")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Error syncing locations (non-critical)", e)
                    // Continue with check-out even if sync fails
                }

                // ==========================================
                // STEP 2: Use pre-fetched location (started during face verification)
                // ==========================================
                _ui.value = _ui.value.copy(loadingMessage = "Capturing location...")

                Log.d(TAG, "📍 Getting live location for check-out...")
                val locationResult = prefetchedLiveLocation?.await()
                    ?: LiveLocationHelper(context).captureLiveLocation()
                prefetchedLiveLocation = null

                var lastLocation: LocationData? = null
                when (locationResult) {
                    is LiveLocationResult.Success -> {
                        lastLocation = locationResult.locationData
                        Log.d(TAG, "✅ Live location captured: lat=${lastLocation.latitude}, lng=${lastLocation.longitude}")
                    }
                    is LiveLocationResult.Error -> {
                        Log.w(TAG, "⚠️ Failed to capture live location: ${locationResult.message}")
                        // Continue without last location - non-blocking
                    }
                }

                _ui.value = _ui.value.copy(loadingMessage = "Completing check-out...")

                when (val outcome = repo.checkOutSignature(
                    tToken = tToken,
                    faceRecognitionQualityScore = _ui.value.faceVerificationQualityScore,
                    faceVerify = _ui.value.faceVerificationSuccess,
                    earlyReason = earlyReason,
                    outOfRangeReason = outOfRangeReason,
                    priority = priority,
                    answers = answers,
                    workReport = workReport,
                    workReportFileUri = workReportFileUri,
                    lastLocation = lastLocation
                )) {
                    is SignatureOutcome.Success -> {
                        Log.d(TAG, "✅ Check-out signature SUCCESS")

                        // ==========================================
                        // STEP 3: STOP LOCATION TRACKING SERVICE
                        // ==========================================
                        Log.d(TAG, "⏹️ Stopping location tracking service...")

                        try {
                            // Stop the foreground service
                            LocationTrackingService.stopTracking(context)

                            // Cancel keep-alive mechanisms
                            TrackingWorker.cancel(context)
                            TrackingAlarmReceiver.cancel(context)

                            // Clear tracking state
                            val trackingStateManager = TrackingStateManager(context)
                            trackingStateManager.clearState()

                            Log.d(TAG, "✅ Location tracking stopped successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "⚠️ Error stopping location tracking (non-critical)", e)
                        }

                        _ui.value = _ui.value.copy(
                            isLoading = false,
                            loadingMessage = null,
                            currentState = "CHECK_IN_NEEDED",
                            lastCheckInTime = null,
                            checkOutTToken = null,
                            checkOutFaceVector = null,
                            checkOutMessage = null,
                            checkOutWorkReportQuestionSet = null,
                            checkOutPriorityOptions = emptyList(),
                            faceVerificationQualityScore = null,
                            faceVerificationSuccess = false,
                            tempEarlyReason = null,
                            tempOutOfRangeReason = null
                        )

                        emitSuccess(outcome.message)
                        outcome.workflowWarning?.let { emitSuccess(it) }

                        // Refresh from server to get accurate state (supports multiple check-ins)
                        delay(500)
                        refreshStatus(force = true, context = context)
                    }

                    is SignatureOutcome.Conflict -> {
                        // 409 CONFLICT — session was already closed (cron auto-close or
                        // another request won the race). Clear spent t_token + face state,
                        // show the server's message, and refresh so the UI snaps to
                        // CHECK_IN_NEEDED (or whatever the actual state is).
                        Log.w(TAG, "⚠️ CHECK-OUT SIGNATURE CONFLICT: ${outcome.message}")
                        _ui.value = _ui.value.copy(
                            isLoading = false,
                            loadingMessage = null,
                            checkOutTToken = null,
                            checkOutFaceVector = null,
                            checkOutMessage = null,
                            checkOutWorkReportQuestionSet = null,
                            checkOutPriorityOptions = emptyList(),
                            tempEarlyReason = null,
                            tempOutOfRangeReason = null
                        )
                        emitError(outcome.message)
                        delay(500)
                        refreshStatus(force = true, context = context)
                    }

                    is SignatureOutcome.Error -> {
                        Log.e(TAG, "❌ CHECK-OUT SIGNATURE ERROR: ${outcome.message}")
                        _ui.value = _ui.value.copy(
                            isLoading = false,
                            loadingMessage = null,
                            checkOutTToken = null,
                            checkOutFaceVector = null,
                            checkOutMessage = null,
                            checkOutWorkReportQuestionSet = null,
                            checkOutPriorityOptions = emptyList(),
                            tempEarlyReason = null,
                            tempOutOfRangeReason = null
                        )
                        emitError(outcome.message)
                        delay(500)
                        refreshStatus(force = true, context = context)
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

    fun onWorkReportSubmitted(workReport: String?, fileUri: Uri?, priority: String?, answers: String?, context: Context) {
        completeCheckOut(
            _ui.value.tempEarlyReason,
            _ui.value.tempOutOfRangeReason,
            workReport,
            fileUri,
            context,
            priority = priority,
            answers = answers
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
    val checkOutWorkMinutes: Int? = null,
    val checkOutIsEarly: Boolean = false,
    val checkOutIsOutOfRange: Boolean = false,
    val checkOutEarlyReasonRequired: Boolean = false,
    val checkOutOutOfRangeReasonRequired: Boolean = false,
    val checkOutWorkReportRequired: Boolean = false,
    val checkOutWorkReportQuestionSet: WorkReportQuestionSet? = null,
    val checkOutPriorityOptions: List<String> = emptyList(),
    val checkOutMessage: String? = null,
    val checkOutFaceVector: FloatArray? = null,

    val showFaceVerification: Boolean = false,
    val showReasonDialog: Boolean = false,
    val showWorkReportDialog: Boolean = false,
    val showPendingMessageDialog: Boolean = false,

    val faceVerificationQualityScore: Float? = null,
    val faceVerificationSuccess: Boolean = false,

    val pendingMessage: PendingMessage? = null,
    val acknowledgmentNote: String? = null,

    val tempEarlyReason: String? = null,
    val tempOutOfRangeReason: String? = null,

    val isReverifying: Boolean = false,
    val reverifyError: String? = null,

    val checkOutTime: String? = null
)