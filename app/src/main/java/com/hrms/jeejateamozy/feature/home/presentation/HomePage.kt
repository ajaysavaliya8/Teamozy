@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.hrms.jeejateamozy.feature.home.presentation

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.attendance.presentation.AttendanceViewModel
import com.hrms.jeejateamozy.feature.attendance.presentation.AttendanceEvent  // ✅ ADD THIS IMPORT
import com.hrms.jeejateamozy.feature.face.data.EmbeddingExtractor
import com.hrms.jeejateamozy.feature.face.presentation.FaceCaptureScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.compose.foundation.BorderStroke
import com.hrms.jeejateamozy.feature.face.util.FaceVectorUtil
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

private const val TAG = "HomePage"

private fun calculateElapsedSeconds(lastCheckInTime: String?): Int {
    if (lastCheckInTime.isNullOrBlank()) return 0

    try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val checkInDate = formatter.parse(lastCheckInTime) ?: return 0
        val now = java.util.Date()
        val elapsedMillis = now.time - checkInDate.time
        val elapsedSeconds = (elapsedMillis / 1000).toInt()
        return if (elapsedSeconds >= 0) elapsedSeconds else 0
    } catch (e: Exception) {
        android.util.Log.e(TAG, "Error parsing last_check_in_time: $lastCheckInTime", e)
        return 0
    }
}

@Composable
fun rememberAttendanceViewModel(context: android.content.Context): AttendanceViewModel {
    return koinViewModel()
}

@Composable
fun HomePage(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val vm = rememberAttendanceViewModel(context)
    val ui by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val prefs = remember { PreferencesManager.getInstance(context) }

    var faceVerifyBusy by remember { mutableStateOf(false) }
    var faceVerifyError by remember { mutableStateOf<String?>(null) }
    var faceVerificationGeneration by remember { mutableIntStateOf(0) }

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        vm.refreshStatus()
    }

    // ✅ LISTEN TO ONE-TIME EVENTS FROM VIEWMODEL
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
            HomeTopBar(
                context = context,
                companyName = prefs.companyName,
                userName = prefs.userName,
                profileUrl = prefs.profileUrl,
                isRefreshing = ui.isRefreshing,
                onRefreshClick = { vm.refreshStatus(force = true) },
                onProfileClick = onNavigateToProfile
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(24.dp))

                if (ui.checkInMessage != null || ui.checkOutMessage != null) {
                    val message = ui.checkInMessage ?: ui.checkOutMessage
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3CD)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFF856404)
                            )
                            Text(
                                text = message ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF856404)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(130.dp)
                        ) {
                            val workdaySeconds = 8 * 60 * 60
                            val progress = if (isTimerRunning) {
                                (elapsedSeconds.toFloat() / workdaySeconds).coerceIn(0f, 1f)
                            } else {
                                0f
                            }

                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(130.dp),
                                color = if (isTimerRunning) Color(0xFF00C896) else Color(0xFF4DD0B8),
                                strokeWidth = 10.dp,
                                trackColor = Color(0xFFE0F2F1),
                            )
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                val hours = elapsedSeconds / 3600
                                val minutes = (elapsedSeconds % 3600) / 60
                                val seconds = elapsedSeconds % 60

                                Text(
                                    text = "%02d:%02d:%02d".format(hours, minutes, seconds),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTimerRunning) {
                                        Color(0xFF00C896)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }

                        when (ui.currentState) {
                            "CHECK_IN_NEEDED" -> {
                                Button(
                                    onClick = {
                                        Log.d(TAG, "CHECK IN BUTTON CLICKED")
                                        faceVerificationGeneration++
                                        checkInPermissionChecker()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    enabled = !ui.isLoading && !faceVerifyBusy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00C896)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (ui.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            "Check In",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            "CHECK_OUT_NEEDED" -> {
                                Button(
                                    onClick = {
                                        Log.d(TAG, "CHECK OUT BUTTON CLICKED")
                                        faceVerificationGeneration++
                                        checkOutPermissionChecker()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    enabled = !ui.isLoading && !faceVerifyBusy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF6B6B)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (ui.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            "Check Out",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            "COMPLETED" -> {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = Color(0xFF00C896)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Completed",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00C896)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                QuickAccessSection(
                    onCircularClick = { },
                    onApplyLeavesClick = { },
                    onWorkReportClick = { },
                    onTasksClick = { }
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    LaunchedEffect(ui.showFaceVerification, ui.showReasonDialog) {
        if (!ui.showFaceVerification && !ui.showReasonDialog && faceVerifyBusy) {
            Log.d(TAG, "✅ Screens dismissed, resetting faceVerifyBusy = false")
            faceVerifyBusy = false
            faceVerifyError = null
        }
    }

    if (ui.showFaceVerification) {
        key(faceVerificationGeneration) {
            FaceCaptureScreen(
                generation = faceVerificationGeneration,
                onDismiss = {
                    Log.d(TAG, "🔙 Face capture dismissed")
                    vm.onFaceVerificationCancelled()
                    faceVerifyBusy = false
                    faceVerifyError = null
                },
                onCaptured = { },
                onBitmapCaptured = { bitmap ->
                    if (faceVerifyBusy) {
                        bitmap.recycle()
                        return@FaceCaptureScreen
                    }

                    faceVerifyBusy = true
                    faceVerifyError = null

                    scope.launch {
                        try {
                            val extractor = EmbeddingExtractor.getInstance(context)
                            val liveEmbedding = withContext(Dispatchers.Default) {
                                extractor.extractOrNull(bitmap)
                            }
                            bitmap.recycle()

                            if (liveEmbedding == null) {
                                Log.e(TAG, "No face detected")
                                faceVerifyError = "No face detected. Please try again."
                                faceVerifyBusy = false
                                return@launch
                            }

                            val storedFaceVector = ui.checkInFaceVector ?: ui.checkOutFaceVector

                            if (storedFaceVector == null) {
                                Log.e(TAG, "❌ No stored face vector from API")
                                faceVerifyError = "Face verification data not available. Please contact support."
                                faceVerifyBusy = false
                                return@launch
                            }

                            if (!FaceVectorUtil.isValidFaceVector(storedFaceVector)) {
                                Log.e(TAG, "❌ Invalid stored face vector from API")
                                faceVerifyError = "Invalid face data. Please contact support."
                                faceVerifyBusy = false
                                return@launch
                            }

                            Log.d(TAG, "✅ Stored face vector loaded from API: ${storedFaceVector.size} dimensions")

                            val similarity = FaceVectorUtil.calculateSimilarity(liveEmbedding, storedFaceVector)

                            Log.d(TAG, "📊 Face similarity score: $similarity")

                            val minimumThreshold = ui.checkInMinimumQualityScore ?: ui.checkOutMinimumQualityScore ?: 0.55f

                            val isVerified = similarity >= minimumThreshold

                            if (isVerified) {
                                Log.d(TAG, "✅ Face VERIFIED! Similarity: $similarity >= $minimumThreshold")
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
                            bitmap.recycle()
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
    }

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
                if (isCheckIn) {
                    vm.completeCheckIn(lateOrEarlyReason, outOfRangeReason)
                } else {
                    vm.completeCheckOut(lateOrEarlyReason, outOfRangeReason)
                }
            }
        )
    }
}

@Composable
private fun QuickAccessSection(
    onCircularClick: () -> Unit = {},
    onApplyLeavesClick: () -> Unit = {},
    onWorkReportClick: () -> Unit = {},
    onTasksClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Access",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Outlined.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "All Your Work Related Tools.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // 4×1 Horizontal Row Layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickAccessItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Receipt,
                title = "Circular",
                iconColor = Color(0xFF00C896),
                onClick = onCircularClick
            )
            QuickAccessItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.EventNote,
                title = "Apply Leaves",
                iconColor = Color(0xFF3B82F6),
                onClick = onApplyLeavesClick
            )
            QuickAccessItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.EditNote,
                title = "Work Report",
                iconColor = Color(0xFF00C896),
                onClick = onWorkReportClick
            )
            QuickAccessItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.CheckCircle,
                title = "Tasks",
                iconColor = Color(0xFF00C896),
                onClick = onTasksClick
            )
        }
    }
}

@Composable
private fun QuickAccessItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    iconColor: Color = Color(0xFF00C896),
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Icon background circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = iconColor
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasonBottomSheet(
    isCheckIn: Boolean,
    isLateOrEarly: Boolean,
    isOutOfRange: Boolean,
    lateOrEarlyReasonRequired: Boolean,
    outOfRangeReasonRequired: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (lateOrEarlyReason: String?, outOfRangeReason: String?) -> Unit
) {
    // Single reason state variable
    var reason by remember { mutableStateOf("") }

    // Determine if any reason is required
    val reasonRequired = lateOrEarlyReasonRequired || outOfRangeReasonRequired

    // Determine the label for the input box
    val inputLabel = when {
        lateOrEarlyReasonRequired && outOfRangeReasonRequired -> "Reason"
        lateOrEarlyReasonRequired -> if (isCheckIn) "Late Reason" else "Early Check-Out Reason"
        outOfRangeReasonRequired -> "Out of Range Reason"
        else -> "Reason"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Note",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Warning messages
            if (isLateOrEarly || isOutOfRange) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLateOrEarly) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isCheckIn) "You are checking in late" else "You are checking out early",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                        if (isOutOfRange) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOff,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "You are outside the designated area",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // Single input box (shown only if reason is required)
            if (reasonRequired) {
                Text(
                    text = "$inputLabel *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    placeholder = {
                        Text(
                            text = when {
                                lateOrEarlyReasonRequired && outOfRangeReasonRequired ->
                                    "Provide reason for both violations"
                                lateOrEarlyReasonRequired ->
                                    if (isCheckIn) "Why are you late?" else "Why are you leaving early?"
                                outOfRangeReasonRequired ->
                                    "Why are you outside the designated area?"
                                else -> "Enter reason"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(Modifier.height(24.dp))
            } else {
                // No reason required
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isCheckIn) "Tap Continue to complete check-in" else "Tap Continue to complete check-out",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                // Continue Button
                Button(
                    onClick = {
                        // Smart parameter passing logic
                        val lateOrEarly: String?
                        val outRange: String?

                        when {
                            // Both reasons required → pass same value to both
                            lateOrEarlyReasonRequired && outOfRangeReasonRequired -> {
                                lateOrEarly = if (reason.isNotBlank()) reason else null
                                outRange = if (reason.isNotBlank()) reason else null
                            }
                            // Only late/early reason required
                            lateOrEarlyReasonRequired -> {
                                lateOrEarly = if (reason.isNotBlank()) reason else null
                                outRange = null
                            }
                            // Only out of range reason required
                            outOfRangeReasonRequired -> {
                                lateOrEarly = null
                                outRange = if (reason.isNotBlank()) reason else null
                            }
                            // No reason required
                            else -> {
                                lateOrEarly = null
                                outRange = null
                            }
                        }

                        // Validate: if reason is required, it must not be blank
                        if (reasonRequired && reason.isBlank()) {
                            return@Button
                        }

                        onSubmit(lateOrEarly, outRange)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !reasonRequired || reason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}




@Composable
fun HomeTopBar(
    context: Context,
    companyName: String?,
    userName: String?,
    profileUrl: String?,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = companyName ?: "Company Name",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Welcome, ${userName ?: "User"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRefreshClick,
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onProfileClick)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!profileUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profileUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}