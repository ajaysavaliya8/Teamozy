package com.hrms.jeejateamozy.feature.attendance.presentation.components

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
import com.hrms.jeejateamozy.core.network.DayCorrectionRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CorrectionRequestCard(
    correctionRequest: DayCorrectionRequest,
    allowWithdraw: Boolean = false,
    onWithdraw: (() -> Unit)? = null,
    onDownloadAttachment: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
                    text = "Correction Request",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = correctionRequest.status)
            }

            HorizontalDivider()

            // Reference Number
            correctionRequest.referenceNumber?.let {
                InfoRow(label = "Reference", value = it)
            }

            // Request Type
            InfoRow(
                label = "Request Type",
                value = formatRequestType(correctionRequest.requestType)
            )

            // Requested Changes
            val hasChanges = correctionRequest.requestedStatus != null ||
                    correctionRequest.requestedCheckIn != null ||
                    correctionRequest.requestedCheckOut != null ||
                    correctionRequest.leaveTypeName != null

            if (hasChanges) {
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
                        correctionRequest.requestedStatus?.let {
                            InfoRow(label = "Status", value = formatStatus(it))
                        }
                        correctionRequest.requestedCheckIn?.let {
                            InfoRow(label = "Check In", value = formatDateTime(it))
                        }
                        correctionRequest.requestedCheckOut?.let {
                            InfoRow(label = "Check Out", value = formatDateTime(it))
                        }
                        correctionRequest.leaveTypeName?.let {
                            InfoRow(label = "Leave Type", value = it)
                        }
                    }
                }
            }

            // Reason
            correctionRequest.reason?.let { reason ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Reason",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Request Date
            correctionRequest.requestDate?.let {
                InfoRow(label = "Submitted", value = formatDate(it))
            }

            // Pending With
            correctionRequest.pendingWith?.let { pendingWith ->
                if (pendingWith.isNotEmpty()) {
                    InfoRow(
                        label = "Pending With",
                        value = pendingWith.joinToString(", ")
                    )
                }
            }

            // Withdraw Button
            if (allowWithdraw && onWithdraw != null) {
                Button(
                    onClick = onWithdraw,
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
private fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "PENDING", "ACTIVE" -> Color(0xFFF59E0B) to Color.White
        "MORE_INFO_NEEDED" -> Color(0xFFF59E0B) to Color.White
        "APPROVED" -> Color(0xFF10B981) to Color.White
        "REJECTED" -> Color(0xFFEF4444) to Color.White
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
                "PENDING", "ACTIVE" -> Icons.Default.Schedule
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

private fun formatRequestType(type: String?): String {
    return when (type) {
        "NEW_ATTENDANCE" -> "New Attendance"
        "CORRECTION" -> "Correction"
        "LEAVE_LINKAGE" -> "Leave Linkage"
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
        try {
            val formatter = DateTimeFormatter.ISO_DATE
            val date = java.time.LocalDate.parse(dateString, formatter)
            date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        } catch (e2: Exception) {
            dateString
        }
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
