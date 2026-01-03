package com.hrms.jeejateamozy.feature.leave.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.core.network.LeaveApplication
import com.hrms.jeejateamozy.core.network.LeaveSummary
import com.hrms.jeejateamozy.core.network.PaginationInfo
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Enhanced Leave History Screen with Workflow Visualization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveHistoryScreen(
    viewModel: LeaveViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToApplyLeave: () -> Unit = {},
    onNavigateToDetail: ((Int) -> Unit)? = null
) {
    val uiState by viewModel.historyUiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dialog states
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var selectedApplicationId by remember { mutableStateOf(0) }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LeaveEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = "❌ ${event.message}",
                        duration = SnackbarDuration.Long
                    )
                }
                is LeaveEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = "✅ ${event.message}",
                        duration = SnackbarDuration.Short
                    )
                }
                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val isSuccess = data.visuals.message.contains("✅")
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Leave History",
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
                actions = {
                    // Apply Leave button
                    IconButton(onClick = onNavigateToApplyLeave) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Apply Leave"
                        )
                    }
                    // Refresh button
                    IconButton(onClick = {
                        viewModel.loadLeaveHistory()
                        viewModel.loadLeaveSummary()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToApplyLeave,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Apply Leave") },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Leave Summary
                uiState.summary?.let { summary ->
                    item {
                        LeaveSummaryCard(summary = summary)
                    }
                }

                // Status Filter
                item {
                    StatusFilterRow(
                        selectedStatus = uiState.selectedStatus,
                        onStatusSelected = { status ->
                            viewModel.filterByStatus(status)
                        }
                    )
                }

                // Error Message
                uiState.errorMessage?.let { error ->
                    item {
                        ErrorCard(message = error)
                    }
                }

                // Leave Applications List
                if (uiState.applications.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyState()
                    }
                } else {
                    items(uiState.applications) { application ->
                        EnhancedLeaveApplicationCard(
                            application = application,
                            onWithdraw = {
                                selectedApplicationId = application.id
                                showWithdrawDialog = true
                            },
                            onClick = {
                                onNavigateToDetail?.invoke(application.id)
                            }
                        )
                    }
                }

                // Pagination Controls
                uiState.pagination?.let { pagination ->
                    item {
                        PaginationControls(
                            pagination = pagination,
                            onPreviousPage = { viewModel.loadPreviousPage() },
                            onNextPage = { viewModel.loadNextPage() }
                        )
                    }
                }
            }

            // Loading overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Withdraw Dialog
    if (showWithdrawDialog) {
        WithdrawLeaveDialog(
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { reason ->
                viewModel.withdrawLeave(selectedApplicationId, reason)
                showWithdrawDialog = false
            }
        )
    }
}

// ==========================================
// ENHANCED LEAVE APPLICATION CARD
// ==========================================
@Composable
private fun EnhancedLeaveApplicationCard(
    application: LeaveApplication,
    onWithdraw: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row with Reference Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Leave Type
                    Text(
                        text = application.leaveType.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Reference Number
                    application.referenceNumber?.let { ref ->
                        Text(
                            text = ref,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Days count
                    Text(
                        text = "${application.numDays} day${if (application.numDays > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge with Workflow Status
                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(status = application.status)

                    // Workflow Status (if different from main status)
                    application.workflowStatus?.let { workflowStatus ->
                        if (workflowStatus.lowercase() != application.status.lowercase()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            WorkflowStatusChip(status = workflowStatus)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Date Range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDateRange(application.startDate, application.endDate),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Leave Reason
            Text(
                text = application.leaveReason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // ==========================================
            // WORKFLOW INFO SECTION (NEW!)
            // ==========================================
            application.workflow?.let { workflow ->
                WorkflowInfoSection(
                    workflow = workflow,
                    status = application.status
                )
            }

            // Approver Info (for approved/rejected)
            application.approver?.let { approver ->
                ApproverInfoRow(
                    approverName = approver.name,
                    status = application.status,
                    remarks = approver.remarks
                )
            }

            // Rejection reason
            application.rejectionReason?.let { reason ->
                RejectionReasonCard(reason = reason)
            }

            // Actions
            if (application.status.lowercase() in listOf("pending", "draft")) {
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Withdraw Application")
                }
            }

            // Applied Date Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Applied on ${formatDate(application.appliedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Details",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// WORKFLOW INFO SECTION
// ==========================================
@Composable
private fun WorkflowInfoSection(
    workflow: com.hrms.jeejateamozy.core.network.LeaveWorkflowInfo,
    status: String
) {
    val isPending = status.lowercase() in listOf("pending", "in_progress")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Workflow Progress Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountTree,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Approval Workflow",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Step Progress
                if (workflow.currentStep != null && workflow.totalSteps != null) {
                    Text(
                        text = "Step ${workflow.currentStep}/${workflow.totalSteps}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress Bar
            if (workflow.currentStep != null && workflow.totalSteps != null && workflow.totalSteps > 0) {
                val progress = (workflow.currentStep - 1).toFloat() / workflow.totalSteps
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when (status.lowercase()) {
                        "approved" -> Color(0xFF4CAF50)
                        "rejected" -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Current Step Name
            workflow.currentStepName?.let { stepName ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        modifier = Modifier.size(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stepName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Pending With (Important!)
            if (workflow.pendingWith.isNotEmpty() && isPending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HourglassTop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFF9800)
                    )
                    Text(
                        text = "Pending with: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = workflow.pendingWith.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

// ==========================================
// APPROVER INFO ROW
// ==========================================
@Composable
private fun ApproverInfoRow(
    approverName: String?,
    status: String,
    remarks: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (status.lowercase()) {
                "approved" -> Icons.Default.CheckCircle
                "rejected" -> Icons.Default.Cancel
                else -> Icons.Outlined.Person
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = when (status.lowercase()) {
                "approved" -> Color(0xFF4CAF50)
                "rejected" -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Column {
            Text(
                text = when (status.lowercase()) {
                    "approved" -> "Approved by: ${approverName ?: "N/A"}"
                    "rejected" -> "Rejected by: ${approverName ?: "N/A"}"
                    else -> "Approver: ${approverName ?: "N/A"}"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            remarks?.let {
                Text(
                    text = "\"$it\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

// ==========================================
// REJECTION REASON CARD
// ==========================================
@Composable
private fun RejectionReasonCard(reason: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Column {
                Text(
                    text = "Rejection Reason",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// ==========================================
// STATUS BADGES
// ==========================================
@Composable
private fun StatusBadge(status: String) {
    val (backgroundColor, textColor, icon) = when (status.lowercase()) {
        "approved" -> Triple(
            Color(0xFF4CAF50),
            Color.White,
            Icons.Default.CheckCircle
        )
        "pending" -> Triple(
            Color(0xFF2196F3),
            Color.White,
            Icons.Default.Schedule
        )
        "in_progress" -> Triple(
            Color(0xFF9C27B0),
            Color.White,
            Icons.Default.Autorenew
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
            Icons.AutoMirrored.Filled.Undo
        )
        "draft" -> Triple(
            Color(0xFFFF9800),
            Color.White,
            Icons.Default.Edit
        )
        "on_leave" -> Triple(
            Color(0xFF00BCD4),
            Color.White,
            Icons.Default.BeachAccess
        )
        "completed" -> Triple(
            Color(0xFF8BC34A),
            Color.White,
            Icons.Default.DoneAll
        )
        "upcoming" -> Triple(
            Color(0xFF03A9F4),
            Color.White,
            Icons.Default.EventAvailable
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Circle
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = textColor
            )
            Text(
                text = status.replace("_", " ").uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun WorkflowStatusChip(status: String) {
    val color = when (status.lowercase()) {
        "pending" -> Color(0xFF2196F3)
        "in_progress" -> Color(0xFF9C27B0)
        "approved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFF44336)
        "draft" -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "WF: ${status.replace("_", " ")}",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp
        )
    }
}

// ==========================================
// LEAVE SUMMARY CARD
// ==========================================
@Composable
private fun LeaveSummaryCard(summary: LeaveSummary) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Leave Summary ${summary.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            // Total Days Taken
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Days Taken",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${summary.totalDaysTaken} days",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // By Status
            if (summary.byStatus.isNotEmpty()) {
                Text(
                    text = "By Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                summary.byStatus.forEach { (status, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = status.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${count.count} (${count.totalDays} days)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// STATUS FILTER ROW
// ==========================================
@Composable
private fun StatusFilterRow(
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit
) {
    val statuses = listOf(
        "All" to null,
        "Pending" to "pending",
        "Approved" to "approved",
        "Rejected" to "rejected",
        "Cancelled" to "cancelled",
        "Withdrawn" to "withdrawn"
    )

    Column {
        Text(
            text = "Filter by Status",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(statuses) { (label, value) ->
                FilterChip(
                    selected = selectedStatus == value,
                    onClick = { onStatusSelected(value) },
                    label = { Text(label) },
                    leadingIcon = if (selectedStatus == value) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
    }
}

// ==========================================
// PAGINATION CONTROLS
// ==========================================
@Composable
private fun PaginationControls(
    pagination: PaginationInfo,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onPreviousPage,
            enabled = pagination.currentPage > 1
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Previous")
        }

        Text(
            text = "Page ${pagination.currentPage} of ${pagination.totalPages}",
            style = MaterialTheme.typography.bodyMedium
        )

        TextButton(
            onClick = onNextPage,
            enabled = pagination.currentPage < pagination.totalPages
        ) {
            Text("Next")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

// ==========================================
// WITHDRAW DIALOG
// ==========================================
@Composable
private fun WithdrawLeaveDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Withdraw Leave Application",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Are you sure you want to withdraw this leave application?")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for withdrawal") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Withdraw")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==========================================
// ERROR & EMPTY STATES
// ==========================================
@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.EventBusy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No leave applications found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Your leave history will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// UTILITY FUNCTIONS
// ==========================================
private fun formatDate(dateString: String): String {
    return try {
        val dateTime = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    } catch (e: Exception) {
        try {
            val date = LocalDate.parse(dateString)
            date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        } catch (e: Exception) {
            dateString
        }
    }
}

private fun formatDateRange(startDate: String, endDate: String): String {
    return try {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        val formatter = DateTimeFormatter.ofPattern("dd MMM")
        val yearFormatter = DateTimeFormatter.ofPattern("yyyy")

        if (start == end) {
            start.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        } else if (start.year == end.year) {
            "${start.format(formatter)} - ${end.format(formatter)} ${end.format(yearFormatter)}"
        } else {
            "${start.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))} - ${end.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
        }
    } catch (e: Exception) {
        "$startDate - $endDate"
    }
}