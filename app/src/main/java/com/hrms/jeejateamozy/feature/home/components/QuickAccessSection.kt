package com.hrms.jeejateamozy.feature.home.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hrms.jeejateamozy.core.designsystem.QuickAccessCard
import com.hrms.jeejateamozy.core.designsystem.SectionHeader
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors

/**
 * Quick Access Section — matches Figma home (node 72:2):
 * white cards with a small tinted icon tile (rose / teal / amber).
 */
@Composable
fun QuickAccessSection(
    onCircularClick: () -> Unit = {},
    onApplyLeavesClick: () -> Unit = {},
    onWorkReportClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    data class Item(
        val icon: ImageVector,
        val title: String,
        val tile: Color,
        val tint: Color,
        val onClick: () -> Unit
    )

    val items = listOf(
        Item(Icons.Outlined.BeachAccess, "Leaves", TeamozyColors.TileRose, TeamozyColors.Error, onApplyLeavesClick),
        Item(Icons.Outlined.Campaign, "Circulars", TeamozyColors.TileTeal, TeamozyColors.Success, onCircularClick),
        Item(Icons.AutoMirrored.Outlined.Assignment, "Reports", TeamozyColors.TileAmber, TeamozyColors.Warning, onWorkReportClick)
    )

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
            items.forEach { item ->
                QuickAccessCard(
                    icon = item.icon,
                    label = item.title,
                    tileColor = item.tile,
                    iconTint = item.tint,
                    onClick = item.onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
