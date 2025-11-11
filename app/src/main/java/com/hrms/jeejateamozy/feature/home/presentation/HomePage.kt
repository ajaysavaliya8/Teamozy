@file:OptIn(ExperimentalMaterial3Api::class)
package com.hrms.jeejateamozy.feature.home.presentation

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.hrms.jeejateamozy.feature.home.presentation.components.HomeTopBar
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
 * HomePage - Main Home Screen
 *
 * Features:
 * - Attendance check-in/check-out with face verification
 * - Quick Access to Circular, Apply Leaves, Work Report
 * - Timer for tracking work hours
 * - Status cards and messages
 */
@Composable
fun HomePage(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToWorkReport: () -> Unit = {},
    onNavigateToCircular: () -> Unit = {},  // ⭐ NEW: Circular navigation callback
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
    val isTimerRunning = ui.currentState == "CHECK_OUT_NEEDED"
    val elapsedSeconds = calculateElapsedSeconds(ui.lastCheckInTime, isTimerRunning)

    // Reason dialog states
    var showLateReasonDialog by remember { mutableStateOf(false) }
    var showOutOfRangeCheckInDialog by remember { mutableStateOf(false) }
    var showEarlyReasonDialog by remember { mutableStateOf(false) }
    var showOutOfRangeCheckOutDialog by remember { mutableStateOf(false) }
    var showWorkReportDialog by remember { mutableStateOf(false) }

    // Permission checkers
    val checkInPermissionChecker = rememberPermissionChecker(
        onAllGranted = {
            Log.d(TAG, "All check-in permissions granted, calling checkIn")
            vm.checkIn()
        },
        onDenied = {
            scope.launch {
                snack.showSnackbar("Location permission is required for attendance")
            }
        }
    )

    val checkOutPermissionChecker = rememberPermissionChecker(
        onAllGranted = {
            Log.d(TAG, "All check-out permissions granted, calling checkOut")
            vm.checkOut()
        },
        onDenied = {
            scope.launch {
                snack.showSnackbar("Location permission is required for attendance")
            }
        }
    )

    // Listen to events
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is AttendanceEvent.ShowError -> {
                    Log.e(TAG, "AttendanceEvent.ShowError: ${event.message}")
                    snack.showSnackbar(event.message)
                }
                is AttendanceEvent.ShowSuccess -> {
                    Log.d(TAG, "AttendanceEvent.ShowSuccess: ${event.message}")
                    snack.showSnackbar(event.message)
                }
            }
        }
    }

    // Show dialogs based on UI state
    LaunchedEffect(ui.showLateReasonDialog, ui.showOutOfRangeCheckInDialog, ui.showEarlyReasonDialog, ui.showOutOfRangeCheckOutDialog, ui.showWorkReportDialog) {
        showLateReasonDialog = ui.showLateReasonDialog
        showOutOfRangeCheckInDialog = ui.showOutOfRangeCheckInDialog
        showEarlyReasonDialog = ui.showEarlyReasonDialog
        showOutOfRangeCheckOutDialog = ui.showOutOfRangeCheckOutDialog
        showWorkReportDialog = ui.showWorkReportDialog
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // Attendance Status Card
                item {
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
                }

                // Message Card (if needed)
                if (ui.message.isNotBlank()) {
                    item {
                        MessageCard(message = ui.message)
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }

                // Quick Access Section
                item {
                    QuickAccessSection(
                        onCircularClick = {
                            Log.d(TAG, "Circular clicked")
                            onNavigateToCircular()  // ⭐ NEW: Navigate to Circular
                        },
                        onApplyLeavesClick = {
                            Log.d(TAG, "Apply Leaves clicked - Coming Soon")
                        },
                        onWorkReportClick = {
                            Log.d(TAG, "Work Report clicked")
                            onNavigateToWorkReport()
                        }
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
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
                            faceVerifyError = "Face verification failed. Please try again."
                            delay(2000)
                            faceVerificationGeneration++
                        }

                        faceVerifyBusy = false
                    } catch (e: Exception) {
                        Log.e(TAG, "Face verification exception", e)
                        faceVerifyError = "Face verification error: ${e.message}"
                        faceVerifyBusy = false
                    }
                }
            }
        )

        // Show error if face verification failed
        faceVerifyError?.let { error ->
            AlertDialog(
                onDismissRequest = { faceVerifyError = null },
                title = { Text("Face Verification Failed") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { faceVerifyError = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }

    // Late Reason Dialog
    if (showLateReasonDialog) {
        ReasonBottomSheet(
            title = "Late Arrival Reason",
            onDismiss = { showLateReasonDialog = false },
            onSubmit = { reason ->
                vm.submitLateReason(reason)
                showLateReasonDialog = false
            }
        )
    }

    // Out of Range Check-In Dialog
    if (showOutOfRangeCheckInDialog) {
        ReasonBottomSheet(
            title = "Out of Range Check-In Reason",
            onDismiss = { showOutOfRangeCheckInDialog = false },
            onSubmit = { reason ->
                vm.submitOutOfRangeCheckInReason(reason)
                showOutOfRangeCheckInDialog = false
            }
        )
    }

    // Early Reason Dialog
    if (showEarlyReasonDialog) {
        ReasonBottomSheet(
            title = "Early Checkout Reason",
            onDismiss = { showEarlyReasonDialog = false },
            onSubmit = { reason ->
                vm.submitEarlyReason(reason)
                showEarlyReasonDialog = false
            }
        )
    }

    // Out of Range Check-Out Dialog
    if (showOutOfRangeCheckOutDialog) {
        ReasonBottomSheet(
            title = "Out of Range Check-Out Reason",
            onDismiss = { showOutOfRangeCheckOutDialog = false },
            onSubmit = { reason ->
                vm.submitOutOfRangeCheckOutReason(reason)
                showOutOfRangeCheckOutDialog = false
            }
        )
    }

    // Work Report Dialog
    if (showWorkReportDialog) {
        WorkReportBottomSheet(
            onDismiss = { showWorkReportDialog = false },
            onSubmit = { workReport, fileUri ->
                vm.submitWorkReport(workReport, fileUri)
                showWorkReportDialog = false
            }
        )
    }
}