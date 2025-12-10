package com.hrms.jeejateamozy.core.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable Server Status Banner Component
 * ✅ Shows server loading, maintenance, or error states
 *
 * Usage:
 * ```kotlin
 * ServerStatusBanner(
 *     serverStatus = serverStatus,
 *     onRetry = { viewModel.retry() }
 * )
 * ```
 */

sealed class ServerStatus {
    object Available : ServerStatus()
    object Loading : ServerStatus()
    data class Unavailable(val message: String) : ServerStatus()
    data class Maintenance(val message: String) : ServerStatus()
}

@Composable
fun ServerStatusBanner(
    serverStatus: ServerStatus,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = serverStatus !is ServerStatus.Available,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        when (serverStatus) {
            is ServerStatus.Loading -> {
                LoadingBanner(modifier = modifier)
            }
            is ServerStatus.Unavailable -> {
                ErrorBanner(
                    message = serverStatus.message,
                    onRetry = onRetry,
                    modifier = modifier
                )
            }
            is ServerStatus.Maintenance -> {
                MaintenanceBanner(
                    message = serverStatus.message,
                    modifier = modifier
                )
            }
            else -> {
                // Available - show nothing
            }
        }
    }
}

@Composable
private fun LoadingBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFFFB347) // Orange
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "⏳ Connecting to server...",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFFF6B6B) // Red
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getIcon(message),
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun MaintenanceBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF9B59B6) // Purple
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🚧",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
    }
}

/**
 * Extract appropriate icon from error message
 */
private fun getIcon(message: String): String {
    return when {
        message.contains("timeout", ignoreCase = true) -> "⏱️"
        message.contains("maintenance", ignoreCase = true) -> "🚧"
        message.contains("unavailable", ignoreCase = true) -> "⚠️"
        message.contains("connect", ignoreCase = true) -> "🔌"
        message.contains("network", ignoreCase = true) -> "📡"
        message.contains("error", ignoreCase = true) -> "🔥"
        else -> "⚠️"
    }
}

/**
 * Helper function to determine server status from error message
 */
fun isServerError(message: String): Boolean {
    return message.contains("Server", ignoreCase = true) ||
            message.contains("server", ignoreCase = true) ||
            message.contains("maintenance", ignoreCase = true) ||
            message.contains("unavailable", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("not responding", ignoreCase = true) ||
            message.contains("⏱️") ||
            message.contains("🔌") ||
            message.contains("🚧") ||
            message.contains("🔥")
}

/**
 * Extension function for easier usage in ViewModels
 */
fun String.toServerStatus(): ServerStatus {
    return if (isServerError(this)) {
        when {
            contains("maintenance", ignoreCase = true) ->
                ServerStatus.Maintenance(this)
            else ->
                ServerStatus.Unavailable(this)
        }
    } else {
        ServerStatus.Available
    }
}