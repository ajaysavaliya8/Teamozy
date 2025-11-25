package com.hrms.jeejateamozy.feature.attendance.presentation

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
import com.hrms.jeejateamozy.core.network.DayTimesheet
import com.hrms.jeejateamozy.core.network.PunchRecord
import com.hrms.jeejateamozy.feature.attendance.presentation.components.CorrectionRequestCard
import com.hrms.jeejateamozy.feature.attendance.presentation.dialogs.SubmitCorrectionRequestBottomSheet
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * ✅ UPDATED: AttendanceDayDetailScreen with Correction Request Support
 *
 * Changes:
 * - Added correction request card display
 * - Added submit correction button
 * - Added submit correction dialog
 * - Added withdrawal and attachment download handling
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDayDetailScreen(
    date: String,
    viewModel: AttendanceHistoryViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.dayDetailUiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load day timesheet when screen opens
    LaunchedEffect(date) {
        viewModel.loadDayTimesheet(date)
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AttendanceHistoryEvent.ShowError -> {
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
        topBar = {
            TopAppBar(
                title = { Text("Day Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                        onWithdrawCorrection = { requestId ->
                            viewModel.withdrawCorrectionRequest(requestId, date)
                        },
                        onDownloadAttachment = { downloadUrl, isSettled, requestId ->
                            viewModel.downloadAttachment(downloadUrl, isSettled, requestId)
                        },
                        onSubmitCorrection = {
                            viewModel.showSubmitCorrectionDialog(true)
                        }
                    )
                }
            }

            // ✅ NEW: Submit Correction Dialog
            if (uiState.showSubmitCorrectionDialog && uiState.correctionOptions != null) {
                SubmitCorrectionRequestBottomSheet(
                    date = date,
                    attendanceRecordId = uiState.timesheet?.attendanceRecordId,
                    availableActions = uiState.timesheet?.availableActions ?: emptyList(),
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
        }
    }
}

@Composable
private fun DayDetailContent(
    timesheet: DayTimesheet,
    onWithdrawCorrection: (Int) -> Unit,
    onDownloadAttachment: (String, Boolean, Int) -> Unit,
    onSubmitCorrection: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Header
        item {
            DateHeaderCard(
                dayName = timesheet.dayName,
                formattedDate = timesheet.formattedDate ?: timesheet.date
            )
        }

        // No Attendance Message
        if (!timesheet.hasAttendance) {
            item {
                NoAttendanceView(
                    date = timesheet.date,
                    dayName = timesheet.dayName,
                    message = timesheet.message ?: "No attendance record"
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
                        timingDisplay = shift.timingDisplay
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
            timesheet.punches?.let { punches ->
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

        // ✅ NEW: Correction Request Card
        timesheet.correctionRequest?.let { container ->
            if (container.hasAny) {
                item {
                    CorrectionRequestCard(
                        correctionRequest = container.active,
                        settledRequest = container.settled,
                        onWithdraw = { requestId ->
                            onWithdrawCorrection(requestId)
                        },
                        onDownloadAttachment = { downloadUrl ->
                            val requestId = container.active?.id ?: container.settled?.id ?: 0
                            val isSettled = container.active == null
                            onDownloadAttachment(downloadUrl, isSettled, requestId)
                        }
                    )
                }
            }
        }

        // ✅ NEW: Submit Correction Button
        if (timesheet.canSubmitCorrection == true) {
            item {
                Button(
                    onClick = onSubmitCorrection,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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

        // Extra spacing at bottom
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
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
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
    val statusColor = when (color) {
        "green" -> Color(0xFF4CAF50)
        "red" -> Color(0xFFF44336)
        "orange" -> Color(0xFFFF9800)
        "blue" -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }

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
private fun ShiftInfoCard(shiftName: String?, hours: String, timingDisplay: String?) {
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
                    tint = MaterialTheme.colorScheme.primary
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
            timingDisplay?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    tint = MaterialTheme.colorScheme.primary
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
                    imageVector = if (punch.type == "PUNCH IN") Icons.Default.Login else Icons.Default.Logout,
                    contentDescription = punch.type,
                    tint = if (punch.type == "PUNCH IN") Color(0xFF4CAF50) else Color(0xFFF44336)
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
                        tint = MaterialTheme.colorScheme.primary
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