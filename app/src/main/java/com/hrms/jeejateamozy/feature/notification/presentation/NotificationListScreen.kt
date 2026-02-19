package com.hrms.jeejateamozy.feature.notification.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hrms.jeejateamozy.core.network.ServerNotification
import com.hrms.jeejateamozy.navigation.DeepLink
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    viewModel: NotificationViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScreen: (DeepLink) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        if (uiState.isSelectionMode) {
            viewModel.toggleSelectionMode()
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotificationEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is NotificationEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
                is NotificationEvent.NavigateToScreen -> onNavigateToScreen(event.deepLink)
            }
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectedNotifications.size,
                    totalCount = uiState.notifications.size,
                    onClose = { viewModel.toggleSelectionMode() },
                    onSelectAll = { viewModel.selectAll() },
                    onDelete = { viewModel.deleteSelectedNotifications() }
                )
            } else {
                NotificationTopBar(
                    onBack = onNavigateBack,
                    showMarkAllRead = uiState.unreadCount > 0,
                    onMarkAllRead = { viewModel.markAllAsRead() }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.notifications.isEmpty() -> {
                    LoadingView()
                }
                uiState.errorMessage != null && uiState.notifications.isEmpty() -> {
                    ErrorView(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.loadNotifications() }
                    )
                }
                uiState.notifications.isEmpty() -> {
                    EmptyView()
                }
                else -> {
                    NotificationList(
                        notifications = uiState.notifications,
                        isSelectionMode = uiState.isSelectionMode,
                        selectedIds = uiState.selectedNotifications,
                        onItemClick = { notification ->
                            if (uiState.isSelectionMode) {
                                viewModel.toggleNotificationSelection(notification.id)
                            } else {
                                viewModel.onNotificationClick(notification)
                            }
                        },
                        onItemLongClick = { notification ->
                            if (!uiState.isSelectionMode) {
                                viewModel.toggleSelectionMode()
                                viewModel.toggleNotificationSelection(notification.id)
                            }
                        },
                        onDelete = { viewModel.deleteNotification(it.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTopBar(
    onBack: () -> Unit,
    showMarkAllRead: Boolean,
    onMarkAllRead: () -> Unit
) {
    TopAppBar(
        title = { Text("Notifications") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            if (showMarkAllRead) {
                IconButton(onClick = onMarkAllRead) {
                    Icon(Icons.Default.DoneAll, "Mark all read")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Cancel")
            }
        },
        actions = {
            if (selectedCount < totalCount) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.DoneAll, "Select all")
                }
            }
            if (selectedCount > 0) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationList(
    notifications: List<ServerNotification>,
    isSelectionMode: Boolean,
    selectedIds: Set<Int>,
    onItemClick: (ServerNotification) -> Unit,
    onItemLongClick: (ServerNotification) -> Unit,
    onDelete: (ServerNotification) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notifications, key = { it.id }) { notification ->
            if (isSelectionMode) {
                NotificationItem(
                    notification = notification,
                    isSelected = notification.id in selectedIds,
                    isSelectionMode = true,
                    onClick = { onItemClick(notification) }
                )
            } else {
                SwipeToDeleteContainer(
                    onDelete = { onDelete(notification) }
                ) {
                    NotificationItem(
                        notification = notification,
                        isSelected = false,
                        isSelectionMode = false,
                        onClick = { onItemClick(notification) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        },
        enableDismissFromStartToEnd = false,
        content = { content() }
    )
}

@Composable
private fun NotificationItem(
    notification: ServerNotification,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit
) {
    val isUnread = !notification.isRead
    val type = notification.type ?: ""

    // Colors based on state (fully opaque to hide swipe background)
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isUnread -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val iconBgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isUnread -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val iconTint = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isUnread -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        tonalElevation = if (isUnread) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                if (isSelectionMode && isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = getIcon(type),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                // Title + Time row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title ?: "Notification",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    notification.createdAt?.let {
                        Text(
                            text = formatTime(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Message
                Text(
                    text = notification.message ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun getIcon(type: String): ImageVector {
    return when {
        type.contains("circular") -> Icons.Default.Campaign
        type.contains("leave") -> Icons.Default.BeachAccess
        type.contains("attendance") -> Icons.Default.Schedule
        else -> Icons.Default.Notifications
    }
}

private fun formatTime(iso: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(iso) ?: return iso.take(10)
        val diff = System.currentTimeMillis() - date.time

        when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        iso.take(10)
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun EmptyView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No notifications",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
