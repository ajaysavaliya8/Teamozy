package com.hrms.jeejateamozy.feature.home.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hrms.jeejateamozy.core.designsystem.QuickAccessCard
import com.hrms.jeejateamozy.core.designsystem.SectionHeader
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors

@Composable
fun QuickAccessSection(
    onCircularClick: () -> Unit = {},
    onApplyLeavesClick: () -> Unit = {},
    onWorkReportClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            text = "Quick Access",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Leaves — green tile (#d7f0e6)
            QuickAccessCard(
                icon = Icons.Outlined.BeachAccess,
                label = "Leaves",
                tileColor = TeamozyColors.TileLeaves,
                iconTint = TeamozyColors.TileIconLeaves,
                onClick = onApplyLeavesClick,
                modifier = Modifier.weight(1f)
            )
            // Circulars — blue tile (#e5f1ff)
            QuickAccessCard(
                icon = Icons.Outlined.Campaign,
                label = "Circulars",
                tileColor = TeamozyColors.TileCirculars,
                iconTint = TeamozyColors.TileIconCirculars,
                onClick = onCircularClick,
                modifier = Modifier.weight(1f)
            )
            // Reports — amber tile (#f8edd3)
            QuickAccessCard(
                icon = Icons.AutoMirrored.Outlined.Assignment,
                label = "Reports",
                tileColor = TeamozyColors.TileReports,
                iconTint = TeamozyColors.TileIconReports,
                onClick = onWorkReportClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
