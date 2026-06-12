package com.hrms.jeejateamozy.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Segmented toggle (Figma 18:28): track + white active pill. */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TeamozyShapes.Control)
            .background(TeamozyColors.TrackBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(TeamozyShapes.Pill)
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) TeamozyColors.Primary else TeamozyColors.SecondaryAlt
                )
            }
        }
    }
}

private data class BadgeStyle(val bg: Color, val fg: Color)

/** Status pill mapping common statuses to token tint pairs. */
@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val s = status.lowercase()
    val style = when {
        s.contains("present") || s.contains("approved") || s.contains("complete") || s.contains("verified") ->
            BadgeStyle(TeamozyColors.Success.copy(alpha = 0.12f), TeamozyColors.SuccessDark)
        s.contains("pending") || s.contains("progress") || s.contains("draft") || s.contains("info_needed") ->
            BadgeStyle(TeamozyColors.Warning.copy(alpha = 0.12f), TeamozyColors.WarningDark)
        s.contains("reject") || s.contains("absent") || s.contains("fail") || s.contains("irregular") ->
            BadgeStyle(TeamozyColors.Error.copy(alpha = 0.12f), TeamozyColors.ErrorDark)
        else ->
            BadgeStyle(TeamozyColors.SecondaryAlt.copy(alpha = 0.12f), TeamozyColors.SecondaryAlt)
    }
    Surface(modifier = modifier, shape = TeamozyShapes.Pill, color = style.bg) {
        Text(
            text = status.replace("_", " ").split(" ")
                .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } },
            color = style.fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** White quick-access card with a tinted icon tile (Figma 72:2). */
@Composable
fun QuickAccessCard(
    icon: ImageVector,
    label: String,
    tileColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TeamozyCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = TeamozyShapes.CardCompact
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(TeamozyShapes.Control)
                    .background(tileColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                color = TeamozyColors.CardLabel,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = TeamozyType.SectionHeader, modifier = modifier)
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(TeamozyColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TeamozyColors.Tertiary, modifier = Modifier.size(32.dp))
        }
        Text(title, style = TeamozyType.SectionHeader)
        Text(subtitle, style = TeamozyType.Caption, textAlign = TextAlign.Center)
    }
}
