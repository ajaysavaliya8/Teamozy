package com.hrms.jeejateamozy.feature.leave.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveHistoryScreen(
    viewModel: LeaveViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToApplyLeave: () -> Unit
) {
    val uiState by viewModel.historyUiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Withdraw dialog state
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var selectedApplicationId by remember { mutableIntStateOf(0) }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LeaveEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is LeaveEvent.ShowSuccess -> {
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
                title = { Text("Leave History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToApplyLeave,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Apply Leave") }
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
                contentPadding = PaddingValues(16.dp),
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
                        LeaveApplicationCard(
                            application = application,
                            onWithdraw = {
                                selectedApplicationId = application.id
                                showWithdrawDialog = true
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

            Divider()

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
                            text = status.replace("_", " ").capitalize(),
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
        "Cancelled" to "cancelled"
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

@Composable
private fun LeaveApplicationCard(
    application: LeaveApplication,
    onWithdraw: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = application.leaveType.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${application.numDays} day${if (application.numDays > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = application.status)
            }

            Divider()

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

            // Approver Info (if applicable)
            application.approver?.let { approver ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Approver: ${approver.name ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Actions
            if (application.status.lowercase() == "pending") {
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Withdraw Application")
                }
            }

            // Applied Date
            Text(
                text = "Applied on ${formatDate(application.appliedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "approved" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        "pending" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        "rejected" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        "cancelled" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = status.replace("_", " ").capitalize(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PaginationControls(
    pagination: PaginationInfo,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousPage,
                enabled = pagination.currentPage > 1
            ) {
                Icon(Icons.Default.ChevronLeft, "Previous")
            }

            Text(
                text = "Page ${pagination.currentPage} of ${pagination.totalPages}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            IconButton(
                onClick = onNextPage,
                enabled = pagination.currentPage < pagination.totalPages
            ) {
                Icon(Icons.Default.ChevronRight, "Next")
            }
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
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun WithdrawLeaveDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw Leave Application") },
        text = {
            Column {
                Text("Please provide a reason for withdrawing this leave application:")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Withdrawal Reason *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank()
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

// Helper functions
private fun formatDateRange(startDate: String, endDate: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE)
        val end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE)
        "${start.format(formatter)} - ${end.format(formatter)}"
    } catch (e: Exception) {
        "$startDate - $endDate"
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE)
        date.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

// Extension function
private fun String.capitalize(): String {
    return this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}