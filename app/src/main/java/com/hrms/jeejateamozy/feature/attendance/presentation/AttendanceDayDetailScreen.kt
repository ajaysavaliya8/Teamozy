package com.hrms.jeejateamozy.feature.attendance.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import com.hrms.jeejateamozy.core.network.DayCorrectionRequest
import com.hrms.jeejateamozy.core.network.DayTimesheet
import com.hrms.jeejateamozy.core.network.PunchRecord
import com.hrms.jeejateamozy.feature.attendance.presentation.components.CorrectionRequestCard
import com.hrms.jeejateamozy.feature.attendance.presentation.dialogs.SubmitCorrectionRequestBottomSheet
import com.hrms.jeejateamozy.feature.attendance.presentation.dialogs.WithdrawCorrectionDialog
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDayDetailScreen(
    date: String,
    viewModel: AttendanceHistoryViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState by viewModel.dayDetailUiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(date) {
        viewModel.loadDayTimesheet(date)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AttendanceHistoryEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is AttendanceHistoryEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = TeamozyColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Day Detail", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TeamozyColors.AppBar,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TeamozyColors.Background)
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    ErrorView(message = uiState.errorMessage!!)
                }

                uiState.timesheet != null -> {
                    DayDetailContent(
                        timesheet = uiState.timesheet!!,
                        onWithdrawCorrection = {
                            viewModel.showWithdrawDialog(true)
                        },
                        onDownloadAttachment = { requestId ->
                            viewModel.downloadAttachment(requestId)
                        },
                        onSubmitCorrection = {
                            viewModel.showSubmitCorrectionDialog(true)
                        },
                        bottomPadding = bottomPadding
                    )
                }
            }

            // Submit Correction Dialog
            if (uiState.showSubmitCorrectionDialog && uiState.correctionOptions != null) {
                SubmitCorrectionRequestBottomSheet(
                    date = date,
                    attendanceRecordId = uiState.timesheet?.attendanceRecordId,
                    options = uiState.correctionOptions!!,
                    onDismiss = {
                        viewModel.showSubmitCorrectionDialog(false)
                    },
                    onSubmit = { requestType, reason, requestedStatus, requestedCheckIn, requestedCheckOut, leaveTypeId, priority, attachmentUri ->
                        viewModel.submitCorrectionRequest(
                            attendanceDate = date,
                            attendanceRecordId = uiState.timesheet?.attendanceRecordId,
                            requestType = requestType,
                            reason = reason,
                            requestedStatus = requestedStatus,
                            requestedCheckIn = requestedCheckIn,
                            requestedCheckOut = requestedCheckOut,
                            leaveTypeId = leaveTypeId,
                            priority = priority,
                            attachmentUri = attachmentUri
                        )
                    }
                )
            }

            // Withdraw Correction Dialog
            if (uiState.showWithdrawDialog) {
                val correctionRequest = uiState.timesheet?.correctionRequest
                if (correctionRequest != null) {
                    WithdrawCorrectionDialog(
                        onDismiss = { viewModel.showWithdrawDialog(false) },
                        onConfirm = { reason ->
                            viewModel.withdrawCorrectionRequest(
                                requestId = correctionRequest.id,
                                attendanceDate = date,
                                withdrawalReason = reason
                            )
                        },
                        isLoading = uiState.isWithdrawingCorrection
                    )
                }
            }

            // Submit Correction Error Dialog — rendered above the bottom sheet so the
            // (now fully-parsed) validation message is actually visible to the user.
            uiState.submitCorrectionError?.let { errorMessage ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissSubmitCorrectionError() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    title = { Text("Couldn't Submit Request") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissSubmitCorrectionError() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DayDetailContent(
    timesheet: DayTimesheet,
    onWithdrawCorrection: () -> Unit,
    onDownloadAttachment: (Int) -> Unit,
    onSubmitCorrection: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Header
        item {
            DateHeaderCard(
                dayName = timesheet.dayName,
                formattedDate = timesheet.date
            )
        }

        // No Attendance Message
        if (!timesheet.hasAttendance) {
            item {
                NoAttendanceView(
                    date = timesheet.date,
                    dayName = timesheet.dayName,
                    message = "No attendance record for this day"
                )
            }
        } else {
            // Status Card
            timesheet.status?.let { status ->
                item {
                    StatusCard(
                        text = status.text,
                        color = status.color,
                        isComplete = timesheet.isComplete ?: false
                    )
                }
            }

            // Shift Information
            timesheet.shift?.let { shift ->
                item {
                    ShiftInfoCard(
                        shiftName = shift.name,
                        hours = shift.hours,
                        startTime = shift.startTime,
                        endTime = shift.endTime
                    )
                }
            }

            // Hours Summary
            timesheet.hours?.let { hours ->
                item {
                    HoursSummaryCard(
                        totalDisplay = hours.totalDisplay,
                        productiveDisplay = hours.productiveDisplay
                    )
                }
            }

            // Punch Records
            timesheet.checks?.let { punches ->
                if (punches.isNotEmpty()) {
                    item {
                        Text(
                            text = "Punch Records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(punches) { punch ->
                        PunchRecordCard(punch = punch)
                    }
                }
            }
        }

        // Correction Request Card
        timesheet.correctionRequest?.let { correction ->
            item {
                CorrectionRequestCard(
                    correctionRequest = correction,
                    allowWithdraw = timesheet.allowWithdraw,
                    onWithdraw = { onWithdrawCorrection() },
                    onDownloadAttachment = { onDownloadAttachment(correction.id) }
                )
            }
        }

        // Submit Correction Button - show when allowed
        if (timesheet.allowCorrection) {
            item {
                Button(
                    onClick = onSubmitCorrection,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TeamozyColors.Primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Submit Correction"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Correction Request")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DateHeaderCard(dayName: String, formattedDate: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = TeamozyColors.Primary.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TeamozyColors.Primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyLarge,
                color = TeamozyColors.Primary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun NoAttendanceView(
    date: String,
    dayName: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "No Attendance",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusCard(text: String, color: String, isComplete: Boolean) {
    val statusColor = parseHexColor(color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            if (isComplete) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Complete",
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ShiftInfoCard(shiftName: String?, hours: String, startTime: String, endTime: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Shift",
                    tint = TeamozyColors.Primary
                )
                Text(
                    text = "Shift Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            shiftName?.let {
                InfoRow(label = "Shift", value = it)
            }
            InfoRow(label = "Hours", value = hours)
            if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                InfoRow(label = "Timing", value = "$startTime - $endTime")
            }
        }
    }
}

@Composable
private fun HoursSummaryCard(totalDisplay: String, productiveDisplay: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Hours",
                    tint = TeamozyColors.Primary
                )
                Text(
                    text = "Hours Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            InfoRow(label = "Total Time", value = totalDisplay)
            InfoRow(label = "Productive Time", value = productiveDisplay)
        }
    }
}

@Composable
private fun PunchRecordCard(punch: PunchRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (punch.type == "CHECK IN") Icons.Default.Login else Icons.Default.Logout,
                    contentDescription = punch.type,
                    tint = if (punch.type == "CHECK IN") TeamozyColors.Primary else MaterialTheme.colorScheme.error
                )
                Column {
                    Text(
                        text = punch.type,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    punch.time?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            punch.location?.let { location ->
                if (location.latitude != null && location.longitude != null) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = TeamozyColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        Color(colorInt)
    } catch (e: Exception) {
        Color(0xFF9E9E9E)
    }
}
