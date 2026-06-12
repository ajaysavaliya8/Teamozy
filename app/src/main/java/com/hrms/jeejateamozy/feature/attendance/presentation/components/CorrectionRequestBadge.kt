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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Correction Request Badge Components
 */

/**
 * Simple status badge for correction request status
 */
@Composable
fun CorrectionStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (icon, backgroundColor) = when (status) {
        "PENDING", "ACTIVE" -> Icons.Default.Schedule to Color(0xFFF59E0B)
        "APPROVED" -> Icons.Default.CheckCircle to Color(0xFF10B981)
        "REJECTED" -> Icons.Default.Cancel to Color(0xFFEF4444)
        "MORE_INFO_NEEDED" -> Icons.Default.Info to Color(0xFFF59E0B)
        "WITHDRAWN" -> Icons.Default.Close to Color(0xFF9E9E9E)
        else -> Icons.Default.Circle to Color(0xFF9E9E9E)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = status.replace("_", " ").split(" ")
                    .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Mini dot indicator for calendar view
 */
@Composable
fun CorrectionPendingDot(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(6.dp)
            .background(Color(0xFFF59E0B), shape = RoundedCornerShape(3.dp))
    )
}
