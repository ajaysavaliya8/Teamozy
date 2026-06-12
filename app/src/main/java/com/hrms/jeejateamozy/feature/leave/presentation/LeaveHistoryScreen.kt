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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import com.hrms.jeejateamozy.core.network.LeaveApplication
import com.hrms.jeejateamozy.core.network.LeaveSummary
import com.hrms.jeejateamozy.core.network.PaginationInfo
import kotlinx.coroutines.delay
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
    onNavigateToDetail: ((Int) -> Unit)? = null,
    successMessage: String? = null,
    onSuccessMessageShown: () -> Unit = {}
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

    // Show success message from leave application
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(
                message = "✅ $successMessage",
                duration = SnackbarDuration.Short
            )
            onSuccessMessageShown()
        }
    }

    Scaffold(
        containerColor = TeamozyColors.Background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val isSuccess = data.visuals.message.contains("✅")
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
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
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(TeamozyColors.Primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Apply Leave",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
    ) { paddingValues ->
        // Pull to refresh state
        var isRefreshing by remember { mutableStateOf(false) }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    viewModel.loadLeaveHistory(status = uiState.selectedStatus)
                    viewModel.loadLeaveSummary()
                    delay(500) // Small delay for visual feedback
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(TeamozyColors.Background)
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

            // Loading overlay (only show on initial load, not on refresh)
            if (uiState.isLoading && !isRefreshing) {
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
// LEAVE APPLICATION CARD (Minimal)
// ==========================================
@Composable
private fun EnhancedLeaveApplicationCard(
    application: LeaveApplication,
    onWithdraw: () -> Unit,
    onClick: () -> Unit
) {
    // Parse color from colorCode
    val leaveTypeColor = application.colorCode?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(leaveTypeColor)
            )

            // Main content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Leave Type Name
                Text(
                    text = application.leaveTypeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Reference Number
                application.referenceNumber?.let { ref ->
                    Text(
                        text = ref,
                        style = MaterialTheme.typography.labelSmall,
                        color = TeamozyColors.Primary
                    )
                }

                // Date Range and Days
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateRange(application.startDate, application.endDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${application.numDays} day${if (application.numDays > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Status Badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge(status = application.status)

                // Withdraw button for pending/draft
                if (application.status.lowercase() in listOf("pending", "draft")) {
                    IconButton(
                        onClick = onWithdraw,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Withdraw",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
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
}

// ==========================================
// STATUS BADGES
// ==========================================
@Composable
private fun StatusBadge(status: String) {
    val (backgroundColor, textColor, icon) = when (status.lowercase()) {
        "approved" -> Triple(
            Color(0xFF10B981),
            Color.White,
            Icons.Default.CheckCircle
        )
        "pending" -> Triple(
            Color(0xFFF59E0B),
            Color.White,
            Icons.Default.Schedule
        )
        "in_progress" -> Triple(
            Color(0xFF8B5CF6),
            Color.White,
            Icons.Default.Autorenew
        )
        "rejected" -> Triple(
            Color(0xFFEF4444),
            Color.White,
            Icons.Default.Cancel
        )
        "cancelled" -> Triple(
            Color(0xFF9E9E9E),
            Color.White,
            Icons.Default.Block
        )
        "withdrawn" -> Triple(
            Color(0xFF64748B),
            Color.White,
            Icons.AutoMirrored.Filled.Undo
        )
        "draft" -> Triple(
            Color(0xFFF59E0B),
            Color.White,
            Icons.Default.Edit
        )
        "on_leave" -> Triple(
            Color(0xFF00BCD4),
            Color.White,
            Icons.Default.BeachAccess
        )
        "completed" -> Triple(
            Color(0xFF10B981),
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

// ==========================================
// LEAVE SUMMARY CARD
// ==========================================
@Composable
private fun LeaveSummaryCard(summary: LeaveSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = TeamozyColors.Primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Total taken
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = TeamozyColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${summary.year}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TeamozyColors.Primary
                )
            }

            // Status chips in a row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusOrder = listOf("APPROVED", "PENDING", "REJECTED")
                statusOrder.forEach { key ->
                    val count = summary.byStatus[key]
                    if (count != null && count.count > 0) {
                        val color = when (key) {
                            "APPROVED" -> Color(0xFF10B981)
                            "PENDING" -> Color(0xFFF59E0B)
                            "REJECTED" -> Color(0xFFEF4444)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${count.count}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }

                // Total days
                Text(
                    text = "${summary.totalDaysTaken}d",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TeamozyColors.Primary
                )
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
        "Rejected" to "rejected"
    )

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