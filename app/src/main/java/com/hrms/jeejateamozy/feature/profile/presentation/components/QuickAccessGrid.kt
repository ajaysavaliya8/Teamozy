package com.hrms.jeejateamozy.feature.profile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Quick Access Grid Section
 * 2 rows x 3 columns = 6 items total
 *
 * Row 1: Contact Detail, Personal Info, Employment Detail
 * Row 2: Banking Info, Employment Identity, Shift Details
 */
@Composable
fun QuickAccessGrid(
    modifier: Modifier = Modifier,
    onContactDetailClick: () -> Unit = {},
    onPersonalInfoClick: () -> Unit = {},
    onEmploymentDetailClick: () -> Unit = {},
    onBankingInfoClick: () -> Unit = {},
    onEmploymentIdentityClick: () -> Unit = {},
    onShiftDetailsClick: () -> Unit = {}
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Quick Access",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TeamozyColors.Primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Row 1: Contact, Personal, Employment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAccessItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.ContactPage,
                        title = "Contact\nDetail",
                        onClick = onContactDetailClick
                    )
                    QuickAccessItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Person,
                        title = "Personal\nInfo",
                        onClick = onPersonalInfoClick
                    )
                    QuickAccessItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Work,
                        title = "Employment\nDetail",
                        onClick = onEmploymentDetailClick
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Row 2: Banking, Identity, Shift
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAccessItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.AccountBalance,
                        title = "Banking\nInfo",
                        onClick = onBankingInfoClick
                    )
                    QuickAccessItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Badge,
                        title = "Employment\nIdentity",
                        onClick = onEmploymentIdentityClick
                    )
                    QuickAccessItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.AccessTime,
                        title = "Shift\nDetails",
                        onClick = onShiftDetailsClick
                    )
                }
            }
        }
    }
}

/**
 * Individual Quick Access Item
 */
@Composable
private fun QuickAccessItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(TeamozyColors.TileLeaves),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = TeamozyColors.Primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}