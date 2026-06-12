package com.hrms.jeejateamozy.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Home screen violet app bar (Figma node 69:2).
 * Avatar + name/company on the left, refresh + notification bell (with badge) on the right.
 * The avatar slot is a composable so callers supply their existing async image.
 */
@Composable
fun HomeAppBar(
    userName: String?,
    companyName: String?,
    isRefreshing: Boolean,
    notificationCount: Int,
    onRefreshClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatar: (@Composable () -> Unit)? = null
) {
    Surface(modifier = modifier.fillMaxWidth(), color = TeamozyColors.AppBar) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) { avatar?.invoke() }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName ?: "Employee",
                    style = TeamozyType.AppBarName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!companyName.isNullOrBlank()) {
                    Text(
                        text = companyName.uppercase(),
                        style = TeamozyType.AppBarCompany,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onRefreshClick, enabled = !isRefreshing) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = TeamozyColors.OnAppBar
                    )
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = TeamozyColors.OnAppBar)
                }
            }

            Box {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = TeamozyColors.OnAppBar
                    )
                }
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-6).dp, y = 6.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(TeamozyColors.Error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Generic top bar for child/detail screens.
 * [filled] = true renders the violet app-bar variant with white content;
 * [filled] = false renders a white surface with dark content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamozyTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    filled: Boolean = false,
    actions: @Composable (() -> Unit) = {},
    modifier: Modifier = Modifier
) {
    val container = if (filled) TeamozyColors.AppBar else Color.White
    val content = if (filled) TeamozyColors.OnAppBar else TeamozyColors.Heading
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, color = content) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = content)
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = container,
            titleContentColor = content,
            navigationIconContentColor = content,
            actionIconContentColor = content
        )
    )
}
