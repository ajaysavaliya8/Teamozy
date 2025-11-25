package com.hrms.jeejateamozy.feature.attendance.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.core.network.CorrectionBadge

/**
 * Correction Request Badge Component
 * Displays status badge for correction requests
 */

@Composable
fun CorrectionRequestBadge(
    badge: CorrectionBadge,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val (icon, backgroundColor, textColor) = when (badge.type) {
        "pending" -> Triple(
            Icons.Default.Schedule,
            Color(0xFF2196F3), // Blue
            Color.White
        )
        "approved" -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF4CAF50), // Green
            Color.White
        )
        "rejected" -> Triple(
            Icons.Default.Cancel,
            Color(0xFFF44336), // Red
            Color.White
        )
        "info_needed" -> Triple(
            Icons.Default.Info,
            Color(0xFFFF9800), // Orange/Yellow
            Color.White
        )
        else -> Triple(
            Icons.Default.Circle,
            Color(0xFF9E9E9E), // Gray
            Color.White
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) 4.dp else 8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 4.dp else 8.dp,
                vertical = if (compact) 2.dp else 4.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = badge.text,
                tint = textColor,
                modifier = Modifier.size(if (compact) 12.dp else 16.dp)
            )
            if (!compact) {
                Text(
                    text = badge.text,
                    color = textColor,
                    fontSize = if (compact) 10.sp else 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Mini badge for calendar view
 */
@Composable
fun CorrectionRequestMiniBadge(
    badge: CorrectionBadge,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (badge.type) {
        "pending" -> Color(0xFF2196F3)
        "approved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFF44336)
        "info_needed" -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }

    Box(
        modifier = modifier
            .size(6.dp)
            .background(badgeColor, shape = RoundedCornerShape(3.dp))
    )
}

/**
 * Full badge with description for detail view
 */
@Composable
fun CorrectionRequestDetailBadge(
    badge: CorrectionBadge,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    val (icon, backgroundColor, textColor) = when (badge.type) {
        "pending" -> Triple(
            Icons.Default.Schedule,
            Color(0xFF2196F3),
            Color.White
        )
        "approved" -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF4CAF50),
            Color.White
        )
        "rejected" -> Triple(
            Icons.Default.Cancel,
            Color(0xFFF44336),
            Color.White
        )
        "info_needed" -> Triple(
            Icons.Default.Info,
            Color(0xFFFF9800),
            Color.White
        )
        else -> Triple(
            Icons.Default.Circle,
            Color(0xFF9E9E9E),
            Color.White
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = badge.text,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = badge.text,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Status summary badge showing count
 */
@Composable
fun CorrectionRequestSummaryBadge(
    pending: Int = 0,
    approved: Int = 0,
    rejected: Int = 0,
    infoNeeded: Int = 0,
    modifier: Modifier = Modifier
) {
    val total = pending + approved + rejected + infoNeeded

    if (total == 0) return

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Correction Requests",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pending > 0) {
                    StatusChip(
                        count = pending,
                        label = "Pending",
                        color = Color(0xFF2196F3)
                    )
                }
                if (approved > 0) {
                    StatusChip(
                        count = approved,
                        label = "Approved",
                        color = Color(0xFF4CAF50)
                    )
                }
                if (rejected > 0) {
                    StatusChip(
                        count = rejected,
                        label = "Rejected",
                        color = Color(0xFFF44336)
                    )
                }
                if (infoNeeded > 0) {
                    StatusChip(
                        count = infoNeeded,
                        label = "Info Needed",
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    count: Int,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, shape = RoundedCornerShape(3.dp))
            )
            Text(
                text = "$count $label",
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}