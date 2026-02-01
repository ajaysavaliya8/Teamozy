package com.hrms.jeejateamozy.feature.leave.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hrms.jeejateamozy.core.network.LeaveApplicationDetail
import com.hrms.jeejateamozy.core.network.TimelineEvent
import com.hrms.jeejateamozy.feature.leave.data.LeaveApplicationDetailOutcome
import com.hrms.jeejateamozy.feature.leave.data.LeaveRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveDetailScreen(
    applicationId: Int,
    onNavigateBack: () -> Unit
) {
    val repository: LeaveRepository = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(true) }
    var detail by remember { mutableStateOf<LeaveApplicationDetail?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isActionLoading by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Handle system back button
    BackHandler {
        onNavigateBack()
    }

    // Reload helper
    fun reloadDetail() {
        scope.launch {
            isLoading = true
            when (val result = repository.getLeaveApplicationDetail(applicationId)) {
                is LeaveApplicationDetailOutcome.Success -> {
                    detail = result.detail
                    errorMessage = null
                }
                is LeaveApplicationDetailOutcome.Error -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }

    // Load detail
    LaunchedEffect(applicationId) {
        reloadDetail()
    }

    // Withdraw Dialog
    if (showWithdrawDialog) {
        ReasonDialog(
            title = "Withdraw Leave",
            label = "Withdrawal Reason",
            confirmText = "Withdraw",
            confirmColor = Color(0xFF795548),
            isLoading = isActionLoading,
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { reason ->
                scope.launch {
                    isActionLoading = true
                    when (val result = repository.withdrawLeave(applicationId, reason)) {
                        is com.hrms.jeejateamozy.feature.leave.data.WithdrawLeaveOutcome.Success -> {
                            snackbarHostState.showSnackbar(result.message)
                            showWithdrawDialog = false
                            reloadDetail()
                        }
                        is com.hrms.jeejateamozy.feature.leave.data.WithdrawLeaveOutcome.Error -> {
                            snackbarHostState.showSnackbar(result.message)
                        }
                    }
                    isActionLoading = false
                }
            }
        )
    }

    // Cancel Dialog
    if (showCancelDialog) {
        ReasonDialog(
            title = "Request Cancellation",
            label = "Cancellation Reason",
            confirmText = "Request Cancellation",
            confirmColor = Color(0xFFF44336),
            isLoading = isActionLoading,
            onDismiss = { showCancelDialog = false },
            onConfirm = { reason ->
                scope.launch {
                    isActionLoading = true
                    when (val result = repository.cancelLeave(applicationId, reason)) {
                        is com.hrms.jeejateamozy.feature.leave.data.CancelLeaveOutcome.Success -> {
                            snackbarHostState.showSnackbar(result.message)
                            showCancelDialog = false
                            reloadDetail()
                        }
                        is com.hrms.jeejateamozy.feature.leave.data.CancelLeaveOutcome.Error -> {
                            snackbarHostState.showSnackbar(result.message)
                        }
                    }
                    isActionLoading = false
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.referenceNumber ?: "Leave Details",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    ErrorState(
                        message = errorMessage!!,
                        onRetry = { reloadDetail() }
                    )
                }

                detail != null -> {
                    LeaveDetailContent(
                        detail = detail!!,
                        onWithdraw = { showWithdrawDialog = true },
                        onCancel = { showCancelDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaveDetailContent(
    detail: LeaveApplicationDetail,
    onWithdraw: () -> Unit,
    onCancel: () -> Unit
) {
    val leaveTypeColor = detail.colorCode?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } ?: MaterialTheme.colorScheme.primary

    val canWithdraw = detail.allowWithdraw
    val canCancel = detail.allowCancel

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Header Card
        item {
            StatusHeaderCard(
                leaveTypeName = detail.leaveTypeName,
                status = detail.status,
                isPaid = detail.isPaid,
                colorCode = leaveTypeColor
            )
        }

        // Date & Duration Card
        item {
            DateDurationCard(
                startDate = detail.startDate,
                endDate = detail.endDate,
                totalDays = detail.totalDays
            )
        }

        // Reason Card
        detail.reason?.let { reason ->
            item {
                InfoCard(
                    title = "Reason",
                    icon = Icons.Outlined.Description
                ) {
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Approvers (approval chain)
        if (!detail.approvers.isNullOrEmpty()) {
            item {
                ApproversCard(approvers = detail.approvers!!)
            }
        }

        // Pending With (if pending)
        if (!detail.pendingWith.isNullOrEmpty()) {
            item {
                PendingWithCard(pendingWith = detail.pendingWith!!)
            }
        }

        // Rejection Reason (if rejected)
        detail.rejectionReason?.let { reason ->
            item {
                RejectionCard(reason = reason)
            }
        }

        // Contact Information
        if (!detail.alternateContact.isNullOrBlank() || !detail.emergencyContact.isNullOrBlank()) {
            item {
                ContactInfoCard(
                    alternateContact = detail.alternateContact,
                    emergencyContact = detail.emergencyContact
                )
            }
        }

        // Handover Information
        detail.handover?.let { handover ->
            item {
                HandoverCard(
                    handledBy = handover.handledBy,
                    notes = handover.notes
                )
            }
        }

        // Document
        detail.document?.let { doc ->
            item {
                InfoCard(
                    title = "Supporting Document",
                    icon = Icons.Outlined.AttachFile
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Document attached",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Timeline
        if (detail.timeline.isNotEmpty()) {
            item {
                TimelineCard(timeline = detail.timeline)
            }
        }

        // Action Buttons
        if (canWithdraw || canCancel) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (canWithdraw) {
                        Button(
                            onClick = onWithdraw,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF795548)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Withdraw Leave")
                        }
                    }
                    if (canCancel) {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Request Cancellation")
                        }
                    }
                }
            }
        }

        // Bottom spacing to avoid content being hidden behind bottom nav bar
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// =============================================================================
// STATUS HEADER CARD
// =============================================================================
@Composable
private fun StatusHeaderCard(
    leaveTypeName: String,
    status: String,
    isPaid: Boolean,
    colorCode: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorCode.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Leave Type
            Text(
                text = leaveTypeName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorCode
            )

            // Status Badge
            StatusBadgeLarge(status = status)

            // Paid/Unpaid indicator
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isPaid) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFFF9800).copy(alpha = 0.1f)
            ) {
                Text(
                    text = if (isPaid) "Paid Leave" else "Unpaid Leave",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPaid) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun StatusBadgeLarge(status: String) {
    val (backgroundColor, textColor, icon) = getStatusColors(status)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = textColor
            )
            Text(
                text = status.replace("_", " "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private fun getStatusColors(status: String): Triple<Color, Color, ImageVector> {
    return when (status.lowercase()) {
        "approved", "upcoming", "completed", "on_leave" -> Triple(
            Color(0xFF4CAF50),
            Color.White,
            Icons.Default.CheckCircle
        )
        "pending" -> Triple(
            Color(0xFF2196F3),
            Color.White,
            Icons.Default.Schedule
        )
        "rejected" -> Triple(
            Color(0xFFF44336),
            Color.White,
            Icons.Default.Cancel
        )
        "cancelled" -> Triple(
            Color(0xFF9E9E9E),
            Color.White,
            Icons.Default.Block
        )
        "withdrawn" -> Triple(
            Color(0xFF795548),
            Color.White,
            Icons.Default.Undo
        )
        "draft" -> Triple(
            Color(0xFFFF9800),
            Color.White,
            Icons.Default.Edit
        )
        else -> Triple(
            Color(0xFF9E9E9E),
            Color.White,
            Icons.Default.Circle
        )
    }
}

// =============================================================================
// DATE & DURATION CARD
// =============================================================================
@Composable
private fun DateDurationCard(
    startDate: String,
    endDate: String,
    totalDays: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Start Date
            DateColumn(
                label = "From",
                date = startDate
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // End Date
            DateColumn(
                label = "To",
                date = endDate
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Duration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (totalDays == totalDays.toInt().toDouble()) {
                        "${totalDays.toInt()}"
                    } else {
                        String.format("%.1f", totalDays)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (totalDays <= 1) "day" else "days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DateColumn(label: String, date: String) {
    // Parse date outside of composable scope
    val localDate = try {
        LocalDate.parse(date)
    } catch (e: Exception) {
        null
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (localDate != null) {
            Text(
                text = localDate.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = localDate.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// =============================================================================
// INFO CARD
// =============================================================================
@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

// =============================================================================
// PENDING WITH CARD
// =============================================================================
@Composable
private fun PendingWithCard(pendingWith: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.HourglassTop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFFF9800)
                )
                Text(
                    text = "Pending Approval",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE65100)
                )
            }

            pendingWith.forEach { approver ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFF9800)
                    )
                    Text(
                        text = approver,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }
    }
}

// =============================================================================
// APPROVERS CARD
// =============================================================================
@Composable
private fun ApproversCard(approvers: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF1976D2)
                )
                Text(
                    text = "Approval Chain",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0D47A1)
                )
            }

            approvers.forEachIndexed { index, approver ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Number badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = approver,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF0D47A1)
                    )
                }
            }
        }
    }
}

// =============================================================================
// REJECTION CARD
// =============================================================================
@Composable
private fun RejectionCard(reason: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFF44336)
                )
                Text(
                    text = "Rejection Reason",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC62828)
                )
            }
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC62828)
            )
        }
    }
}

// =============================================================================
// CONTACT INFO CARD
// =============================================================================
@Composable
private fun ContactInfoCard(
    alternateContact: String?,
    emergencyContact: String?
) {
    InfoCard(
        title = "Contact Information",
        icon = Icons.Outlined.ContactPhone
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            alternateContact?.let {
                ContactRow(label = "Alternate Contact", value = it)
            }
            emergencyContact?.let {
                ContactRow(label = "Emergency Contact", value = it)
            }
        }
    }
}

@Composable
private fun ContactRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// =============================================================================
// HANDOVER CARD
// =============================================================================
@Composable
private fun HandoverCard(handledBy: String?, notes: String?) {
    InfoCard(
        title = "Handover Details",
        icon = Icons.Outlined.SwapHoriz
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            handledBy?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Handled By",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            notes?.let {
                HorizontalDivider()
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// =============================================================================
// TIMELINE CARD
// =============================================================================
@Composable
private fun TimelineCard(timeline: List<TimelineEvent>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timeline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            timeline.forEachIndexed { index, event ->
                TimelineItem(
                    event = event,
                    isFirst = index == 0,
                    isLast = index == timeline.lastIndex
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    event: TimelineEvent,
    isFirst: Boolean,
    isLast: Boolean
) {
    val eventColor = when {
        event.event.contains("Approved", ignoreCase = true) -> Color(0xFF4CAF50)
        event.event.contains("Rejected", ignoreCase = true) -> Color(0xFFF44336)
        event.event.contains("Cancelled", ignoreCase = true) -> Color(0xFF9E9E9E)
        event.event.contains("Withdrawn", ignoreCase = true) -> Color(0xFF795548)
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Line above
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(eventColor)
            )

            // Line below
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        // Event content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Text(
                text = event.event,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            event.at?.let { at ->
                Text(
                    text = formatDateTime(at),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            event.remarks?.let { remarks ->
                Text(
                    text = "\"$remarks\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            event.reason?.let { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

private fun formatDateTime(dateTimeString: String): String {
    return try {
        val dateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME)
        dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
    } catch (e: Exception) {
        try {
            val dateTime = LocalDateTime.parse(dateTimeString)
            dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
        } catch (e: Exception) {
            dateTimeString
        }
    }
}

// =============================================================================
// REASON DIALOG
// =============================================================================
@Composable
private fun ReasonDialog(
    title: String,
    label: String,
    confirmText: String,
    confirmColor: Color,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Please provide a reason:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

// =============================================================================
// ERROR STATE
// =============================================================================
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}
