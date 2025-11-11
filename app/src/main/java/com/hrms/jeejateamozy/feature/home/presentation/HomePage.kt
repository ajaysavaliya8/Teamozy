@file:OptIn(ExperimentalMaterial3Api::class)
package com.hrms.jeejateamozy.feature.home.presentation

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.attendance.presentation.AttendanceEvent
import com.hrms.jeejateamozy.feature.attendance.presentation.AttendanceViewModel
import com.hrms.jeejateamozy.feature.face.data.EmbeddingExtractor
import com.hrms.jeejateamozy.feature.face.presentation.FaceCaptureScreen
import com.hrms.jeejateamozy.feature.face.util.FaceVectorUtil
import com.hrms.jeejateamozy.feature.home.presentation.components.AttendanceStatusCard
import com.hrms.jeejateamozy.feature.home.presentation.components.MessageCard
import com.hrms.jeejateamozy.feature.home.presentation.components.QuickAccessSection
import com.hrms.jeejateamozy.feature.home.presentation.dialogs.ReasonBottomSheet
import com.hrms.jeejateamozy.feature.home.presentation.dialogs.WorkReportBottomSheet
import com.hrms.jeejateamozy.feature.home.presentation.utils.calculateElapsedSeconds
import com.hrms.jeejateamozy.feature.home.presentation.utils.rememberPermissionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

private const val TAG = "HomePage"

/**
 * HomePage - Refactored Main Screen
 *
 * This file now only contains:
 * - Main HomePage composable
 * - ViewModel integration
 * - State management
 * - Timer logic
 *
 * UI Components moved to:
 * - components/AttendanceStatusCard.kt
 * - components/QuickAccessSection.kt
 * - components/MessageCard.kt
 * - components/HomeTopBar.kt
 * - dialogs/ReasonBottomSheet.kt
 * - dialogs/WorkReportBottomSheet.kt
 *
 * Utilities moved to:
 * - utils/HomeUtils.kt
 * - utils/PermissionChecker.kt
 */
@Composable
fun HomePage(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToWorkReport: () -> Unit = {},
    onNavigateToCircular: () -> Unit = {},
    paddingValues: PaddingValues,
    vm: AttendanceViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val ui by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val prefs = remember { PreferencesManager.getInstance(context) }

    // Face verification state
    var faceVerifyBusy by remember { mutableStateOf(false) }
    var faceVerifyError by remember { mutableStateOf<String?>(null) }
    var faceVerificationGeneration by remember { mutableIntStateOf(0) }

    // Timer state
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Permission checkers
    val checkInPermissionChecker = rememberPermissionChecker(
        context = context,
        onPermissionsGranted = {
            Log.d(TAG, "✅ Permissions granted for Check In")
            vm.startCheckIn(context)
        }
    )

    val checkOutPermissionChecker = rememberPermissionChecker(
        context = context,
        onPermissionsGranted = {
            Log.d(TAG, "✅ Permissions granted for Check Out")
            vm.startCheckOut(context)
        }
    )

    // Timer management
    LaunchedEffect(ui.currentState) {
        if (ui.currentState == "CHECK_OUT_NEEDED") {
            if (!isTimerRunning) {
                val lastCheckInTime = try {
                    ui::class.java.getDeclaredField("lastCheckInTime").let { field ->
                        field.isAccessible = true
                        field.get(ui) as? String
                    }
                } catch (e: Exception) {
                    null
                }

                val elapsed = calculateElapsedSeconds(lastCheckInTime)
                elapsedSeconds = elapsed
                isTimerRunning = true

                if (lastCheckInTime != null) {
                    Log.d(TAG, "✅ Continuing timer from last_check_in_time: $lastCheckInTime")
                    Log.d(TAG, "⏱️ Elapsed time: ${elapsed}s (${elapsed/3600}h ${(elapsed%3600)/60}m)")
                } else {
                    Log.d(TAG, "✅ Timer started (lastCheckInTime not available yet)")
                }
            }
        } else {
            if (isTimerRunning) {
                isTimerRunning = false
                elapsedSeconds = 0
                Log.d(TAG, "⏹️ Timer stopped")
            }
        }
    }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000)
            elapsedSeconds++
        }
    }

    // Initial status refresh
    LaunchedEffect(Unit) {
        vm.refreshStatus()
    }

    // Listen to ViewModel events
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is AttendanceEvent.ShowError -> {
                    Log.d(TAG, "🔴 Event received: ${event.message}")
                    snack.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is AttendanceEvent.ShowSuccess -> {
                    Log.d(TAG, "🟢 Event received: ${event.message}")
                    snack.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snack,
                modifier = Modifier.padding(bottom = 80.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(paddingValues)
        ) {
            // Top Bar
            HomeTopBar(
                context = context,
                companyName = prefs.companyName,
                userName = prefs.userName,
                profileUrl = prefs.profileUrl,
                isRefreshing = ui.isRefreshing,
                onRefreshClick = { vm.refreshStatus(force = true) },
                onProfileClick = onNavigateToProfile
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(24.dp))

                // Attendance Status Card
                AttendanceStatusCard(
                    currentState = ui.currentState,
                    isTimerRunning = isTimerRunning,
                    elapsedSeconds = elapsedSeconds,
                    isLoading = ui.isLoading,
                    isFaceVerifyBusy = faceVerifyBusy,
                    onCheckInClick = {
                        Log.d(TAG, "CHECK IN BUTTON CLICKED")
                        faceVerificationGeneration++
                        checkInPermissionChecker()
                    },
                    onCheckOutClick = {
                        Log.d(TAG, "CHECK OUT BUTTON CLICKED")
                        faceVerificationGeneration++
                        checkOutPermissionChecker()
                    }
                )

                Spacer(Modifier.height(40.dp))

                // Quick Access Section
                QuickAccessSection(
                    onCircularClick = {
                        Log.d(TAG, "Circular clicked - navigating...")
                        onNavigateToCircular()
                    },
                    onApplyLeavesClick = {
                        Log.d(TAG, "Apply Leaves clicked - Coming Soon")
                    },
                    onWorkReportClick = {
                        Log.d(TAG, "Work Report clicked")
                        onNavigateToWorkReport()
                    }
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // Face Capture Dialog
    if (ui.showFaceVerification) {
        val isCheckIn = ui.checkInFaceVector != null
        val faceVector = if (isCheckIn) ui.checkInFaceVector else ui.checkOutFaceVector
        val minimumThreshold = if (isCheckIn) ui.checkInMinimumQualityScore else ui.checkOutMinimumQualityScore

        FaceCaptureScreen(
            generation = faceVerificationGeneration,
            onDismiss = {
                faceVerifyBusy = false
                vm.onFaceVerificationComplete(0f, false)
            },
            onCaptured = { }, // Empty lambda - we use onBitmapCaptured
            onBitmapCaptured = { bitmap ->
                scope.launch {
                    try {
                        faceVerifyBusy = true
                        faceVerifyError = null

                        val extractor = EmbeddingExtractor.getInstance(context)
                        val embedding = withContext(Dispatchers.Default) {
                            extractor.extractOrNull(bitmap)
                        }
                        bitmap.recycle()

                        if (embedding == null) {
                            faceVerifyError = "No face detected or face is unclear. Please try again."
                            faceVerifyBusy = false
                            return@launch
                        }

                        if (faceVector == null) {
                            faceVerifyError = "No registered face vector. Please contact admin."
                            faceVerifyBusy = false
                            return@launch
                        }

                        val similarity = EmbeddingExtractor.cosineSimilarity(faceVector, embedding)
                        Log.d(TAG, "Face similarity score: $similarity (threshold: $minimumThreshold)")

                        if (similarity >= (minimumThreshold ?: 0.55f)) {
                            Log.d(TAG, "✅ Face verified! Similarity: $similarity >= $minimumThreshold")
                            vm.onFaceVerificationComplete(
                                qualityScore = similarity,
                                verified = true
                            )
                        } else {
                            Log.e(TAG, "❌ Face NOT verified! Similarity: $similarity < $minimumThreshold")
                            faceVerifyError = "Face does not match (${String.format("%.2f", similarity * 100)}% match). Please try again."
                            faceVerifyBusy = false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Face capture error: ${e.message}", e)
                        faceVerifyError = "Face capture failed: ${e.message}"
                        faceVerifyBusy = false
                    }
                }
            },
            showReasonField = false,
            reasonMessage = null,
            isSubmitting = faceVerifyBusy,
            onSubmit = { },
            serverError = faceVerifyError
        )
    }

    // Reason Bottom Sheet
    if (ui.showReasonDialog) {
        val isCheckIn = ui.checkInTToken != null
        val isLateOrEarly = if (isCheckIn) ui.checkInIsLate else ui.checkOutIsEarly
        val isOutOfRange = if (isCheckIn) ui.checkInIsOutOfRange else ui.checkOutIsOutOfRange
        val lateOrEarlyReasonRequired = if (isCheckIn) ui.checkInLateReasonRequired else ui.checkOutEarlyReasonRequired
        val outOfRangeReasonRequired = if (isCheckIn) ui.checkInOutOfRangeReasonRequired else ui.checkOutOutOfRangeReasonRequired

        ReasonBottomSheet(
            isCheckIn = isCheckIn,
            isLateOrEarly = isLateOrEarly,
            isOutOfRange = isOutOfRange,
            lateOrEarlyReasonRequired = lateOrEarlyReasonRequired,
            outOfRangeReasonRequired = outOfRangeReasonRequired,
            onDismiss = { vm.onReasonDialogDismissed() },
            onSubmit = { lateOrEarlyReason, outOfRangeReason ->
                vm.onReasonSubmitted(lateOrEarlyReason, outOfRangeReason)
            }
        )
    }

    // Work Report Bottom Sheet
    if (ui.showWorkReportDialog) {
        WorkReportBottomSheet(
            onDismiss = { vm.onWorkReportDialogDismissed() },
            onSubmit = { workReport, fileUri ->
                vm.onWorkReportSubmitted(workReport, fileUri)
            }
        )
    }
}