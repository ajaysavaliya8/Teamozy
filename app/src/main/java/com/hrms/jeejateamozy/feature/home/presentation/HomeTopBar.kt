package com.hrms.jeejateamozy.feature.home.presentation

import android.content.Context
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hrms.jeejateamozy.core.image.CoilImageLoader
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PrivateFileUrl

/**
 * Redesigned Home screen top bar component
 * Cleaner, more modern design with better visual hierarchy
 */
@Composable
fun HomeTopBar(
    context: Context,
    companyName: String?,
    userName: String?,
    profileUrl: String?,
    gender: String? = null,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onNotificationClick: () -> Unit = {},
    notificationCount: Int = 0,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = com.hrms.jeejateamozy.core.designsystem.TeamozyColors.AppBar,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                val avatarPath = when {
                    !profileUrl.isNullOrBlank() -> profileUrl
                    gender == "FEMALE" -> "private/profile/default_female.webp"
                    else -> "private/profile/default_male.webp"
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(PrivateFileUrl.build(NetworkModule.BASE_URL, avatarPath))
                        .crossfade(true)
                        .build(),
                    imageLoader = CoilImageLoader.get(context),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name (primary) & Company name (secondary) — fills available space
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = userName ?: "Employee",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        animationMode = MarqueeAnimationMode.Immediately
                    )
                )
                if (!companyName.isNullOrBlank()) {
                    Text(
                        text = companyName.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right side - Action icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Refresh button
                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh Status",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Notification Icon with Badge
                Box {
                    IconButton(onClick = onNotificationClick) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    // Notification badge
                    if (notificationCount > 0) {
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp),
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
