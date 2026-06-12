package com.hrms.jeejateamozy.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors

@Composable
fun TodayShiftCard(
    shiftName: String,
    shiftTime: String?,
    shiftStatus: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TeamozyColors.TileReports),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TeamozyColors.TileIconReports, modifier = Modifier.size(22.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("TODAY'S SHIFT", fontSize = 11.sp, color = TeamozyColors.Secondary)
                Text(
                    text = if (!shiftTime.isNullOrBlank()) "$shiftName · $shiftTime" else shiftName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeamozyColors.Heading
                )
            }

            if (!shiftStatus.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(TeamozyColors.ShiftBadgeBg)
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = shiftStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TeamozyColors.ShiftBadgeText
                    )
                }
            }
        }
    }
}
