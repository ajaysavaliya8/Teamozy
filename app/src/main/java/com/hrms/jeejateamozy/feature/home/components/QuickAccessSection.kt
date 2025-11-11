package com.hrms.jeejateamozy.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Quick Access Section Component
 * Displays 3x1 horizontal grid with professional, clean design
 */
@Composable
fun QuickAccessSection(
    onCircularClick: () -> Unit = {},
    onApplyLeavesClick: () -> Unit = {},
    onWorkReportClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Access",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Outlined.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "All Your Work Related Tools.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // 3x1 Horizontal Grid with Professional Clean Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfessionalCleanCard(
                icon = Icons.Outlined.Description,
                title = "Circular",
                primaryColor = Color(0xFF1976D2),
                backgroundColor = Color(0xFFE3F2FD),
                onClick = onCircularClick,
                modifier = Modifier.weight(1f)
            )

            ProfessionalCleanCard(
                icon = Icons.Outlined.EventBusy,
                title = "Apply Leaves",
                primaryColor = Color(0xFFE65100),
                backgroundColor = Color(0xFFFFF3E0),
                onClick = onApplyLeavesClick,
                modifier = Modifier.weight(1f)
            )

            ProfessionalCleanCard(
                icon = Icons.Outlined.Assignment,
                title = "Work Report",
                primaryColor = Color(0xFF6A1B9A),
                backgroundColor = Color(0xFFF3E5F5),
                onClick = onWorkReportClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Professional Clean Card Component
 * Simple, elegant design suitable for corporate HRMS applications
 */
@Composable
private fun ProfessionalCleanCard(
    icon: ImageVector,
    title: String,
    primaryColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp)  // ✅ Increased from 100dp to 110dp
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),  // ✅ Reduced from 16dp to 12dp for more space
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with subtle background
            Surface(
                modifier = Modifier.size(42.dp),  // ✅ Slightly reduced from 44dp to 42dp
                shape = RoundedCornerShape(12.dp),
                color = primaryColor.copy(alpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))  // ✅ Reduced from 8dp to 6dp

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            )
        }
    }
}