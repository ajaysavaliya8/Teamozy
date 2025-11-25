package com.hrms.jeejateamozy.feature.attendance.presentation.components
import com.hrms.jeejateamozy.core.network.RequestedChanges

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrms.jeejateamozy.core.network.CorrectionRequest
import com.hrms.jeejateamozy.core.network.SettledCorrectionRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Correction Request Card Component
 * Displays detailed information about active or settled correction requests
 */

@Composable
fun CorrectionRequestCard(
    correctionRequest: CorrectionRequest?,
    settledRequest: SettledCorrectionRequest?,
    onWithdraw: ((Int) -> Unit)? = null,
    onDownloadAttachment: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Determine which request to display (active takes precedence)
    val isActive = correctionRequest != null
    val displayRequest = correctionRequest ?: settledRequest

    if (displayRequest == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isActive) "Active Correction Request" else "Request History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Status Badge
                if (isActive) {
                    correctionRequest?.let {
                        StatusBadge(status = it.status, priority = it.priority)
                    }
                } else {
                    settledRequest?.let {
                        StatusBadge(status = it.finalStatus, priority = it.priority, isSettled = true)
                    }
                }
            }

            HorizontalDivider()

            // Request Type
            InfoRow(
                label = "Request Type",
                value = formatRequestType(
                    if (isActive) correctionRequest?.requestType
                    else settledRequest?.requestType
                )
            )

            // Priority
            InfoRow(
                label = "Priority",
                value = (if (isActive) correctionRequest?.priority else settledRequest?.priority)
                    ?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Normal"
            )

            // Requested Changes
            RequestedChangesSection(
                requestType = if (isActive) correctionRequest?.requestType else settledRequest?.requestType,
                requestedChanges = if (isActive) correctionRequest?.requestedChanges else settledRequest?.requestedChanges
            )

            // Reason
            InfoColumn(
                label = "Reason",
                value = if (isActive) correctionRequest?.reason else settledRequest?.reason
            )

            // Attachment
            val attachment = if (isActive) correctionRequest?.attachment else settledRequest?.attachment
            if (attachment?.hasAttachment == true) {
                AttachmentSection(
                    downloadUrl = attachment.downloadUrl,
                    onDownload = onDownloadAttachment
                )
            }

            // Request Date
            InfoRow(
                label = "Submitted",
                value = formatDate(
                    if (isActive) correctionRequest?.requestDate else settledRequest?.requestDate
                )
            )

            // Settled Date (for settled requests)
            if (!isActive && settledRequest != null) {
                settledRequest.settledDate?.let {
                    InfoRow(
                        label = "Settled On",
                        value = formatDate(it)
                    )
                }
            }

            // Review Info
            val reviewInfo = if (isActive) correctionRequest?.reviewInfo else settledRequest?.reviewInfo
            if (reviewInfo != null) {
                HorizontalDivider()
                ReviewInfoSection(
                    reviewerName = reviewInfo.reviewerName,
                    reviewedAt = reviewInfo.reviewedAt,
                    comments = reviewInfo.comments
                )
            }

            // Changes Applied (for approved settled requests)
            if (!isActive && settledRequest?.finalStatus == "APPROVED" && settledRequest.changesApplied != null) {
                HorizontalDivider()
                ChangesAppliedSection(changes = settledRequest.changesApplied)
            }

            // Withdraw Button (only for active PENDING or MORE_INFO_NEEDED)
            if (isActive && correctionRequest != null &&
                correctionRequest.status in listOf("PENDING", "MORE_INFO_NEEDED") &&
                onWithdraw != null) {
                Button(
                    onClick = { onWithdraw(correctionRequest.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Withdraw"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Withdraw Request")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: String,
    priority: String,
    isSettled: Boolean = false
) {
    val (backgroundColor, textColor) = when (status) {
        "PENDING" -> Color(0xFF2196F3) to Color.White
        "MORE_INFO_NEEDED" -> Color(0xFFFF9800) to Color.White
        "APPROVED" -> Color(0xFF4CAF50) to Color.White
        "REJECTED" -> Color(0xFFF44336) to Color.White
        "WITHDRAWN" -> Color(0xFF9E9E9E) to Color.White
        else -> Color(0xFF757575) to Color.White
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (status) {
                "PENDING" -> Icons.Default.Schedule
                "MORE_INFO_NEEDED" -> Icons.Default.Info
                "APPROVED" -> Icons.Default.CheckCircle
                "REJECTED" -> Icons.Default.Cancel
                "WITHDRAWN" -> Icons.Default.Close
                else -> Icons.Default.Circle
            }

            Icon(
                imageVector = icon,
                contentDescription = status,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = formatStatus(status),
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
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
            text = value ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoColumn(label: String, value: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value ?: "-",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RequestedChangesSection(
    requestType: String?,
    requestedChanges: com.hrms.jeejateamozy.core.network.RequestedChanges?
) {
    if (requestedChanges == null) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Requested Changes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                requestedChanges.status?.let {
                    InfoRow(label = "Status", value = formatStatus(it))
                }
                requestedChanges.checkIn?.let {
                    InfoRow(label = "Check In", value = formatDateTime(it))
                }
                requestedChanges.checkOut?.let {
                    InfoRow(label = "Check Out", value = formatDateTime(it))
                }
                requestedChanges.leaveTypeName?.let {
                    InfoRow(label = "Leave Type", value = it)
                }
            }
        }
    }
}

@Composable
private fun AttachmentSection(
    downloadUrl: String?,
    onDownload: ((String) -> Unit)?
) {
    if (downloadUrl == null || onDownload == null) return

    OutlinedButton(
        onClick = { onDownload(downloadUrl) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Download"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download Attachment")
    }
}

@Composable
private fun ReviewInfoSection(
    reviewerName: String?,
    reviewedAt: String?,
    comments: String?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Review Information",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        reviewerName?.let {
            InfoRow(label = "Reviewed By", value = it)
        }
        reviewedAt?.let {
            InfoRow(label = "Reviewed On", value = formatDateTime(it))
        }
        comments?.let {
            InfoColumn(label = "Comments", value = it)
        }
    }
}

@Composable
private fun ChangesAppliedSection(changes: RequestedChanges?) {
    if (changes == null) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Changes Applied",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show only non-null fields
                changes.status?.let { status ->
                    InfoRow(
                        label = "Status",
                        value = status
                    )
                }

                changes.checkIn?.let { checkIn ->
                    InfoRow(
                        label = "Check In",
                        value = checkIn
                    )
                }

                changes.checkOut?.let { checkOut ->
                    InfoRow(
                        label = "Check Out",
                        value = checkOut
                    )
                }

                changes.leaveTypeName?.let { leaveTypeName ->
                    InfoRow(
                        label = "Leave Type",
                        value = leaveTypeName
                    )
                }
            }
        }
    }
}


// Helper Functions
private fun formatRequestType(type: String?): String {
    return when (type) {
        "NEW_ATTENDANCE" -> "New Attendance"
        "CORRECTION" -> "Correction"
        "LEAVE_LINKAGE" -> "Leave Linkage"
        "STATUS_CHANGE" -> "Status Change"
        else -> type ?: "-"
    }
}

private fun formatStatus(status: String?): String {
    return status?.replace("_", " ")?.split(" ")
        ?.joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
        ?: "-"
}

private fun formatDate(dateString: String?): String {
    if (dateString == null) return "-"
    return try {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val date = LocalDateTime.parse(dateString, formatter)
        date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: Exception) {
        dateString
    }
}

private fun formatDateTime(dateString: String?): String {
    if (dateString == null) return "-"
    return try {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val date = LocalDateTime.parse(dateString, formatter)
        date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"))
    } catch (e: Exception) {
        dateString
    }
}