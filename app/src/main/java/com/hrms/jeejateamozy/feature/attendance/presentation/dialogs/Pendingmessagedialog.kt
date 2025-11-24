package com.hrms.jeejateamozy.feature.attendance.presentation.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hrms.jeejateamozy.core.network.PendingMessage
import com.hrms.jeejateamozy.core.utils.PreferencesManager

/**
 * Message type styling configuration
 */
private data class MessageTypeStyle(
    val label: String,
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color,
    val textColor: Color
)

/**
 * Pending Message Dialog - Compact Version
 * Displays messages from management to employees during check-in
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingMessageDialog(
    message: PendingMessage,
    onDismiss: () -> Unit,
    onAcknowledge: (acknowledgmentNote: String?) -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager.getInstance(context) }
    var acknowledgmentNote by remember { mutableStateOf("") }
    val messageStyle = getMessageTypeStyle(message.type)

    Dialog(
        onDismissRequest = {
            if (!message.requires_acknowledgment) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !message.requires_acknowledgment,
            dismissOnClickOutside = !message.requires_acknowledgment,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Compact Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(messageStyle.backgroundColor)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = messageStyle.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = messageStyle.iconColor
                        )
                        Text(
                            text = messageStyle.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = messageStyle.textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Content - Reduced padding
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title
                    Text(
                        text = message.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Body
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    // Image display
                    if (message.has_attachment && message.attachment_url != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(message.attachment_url)
                                    .addHeader("Authorization", "Bearer ${preferencesManager.authToken.orEmpty()}")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Attachment Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 150.dp, max = 400.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.Center
                            )
                        }
                    }

                    // Acknowledgment input
                    if (message.requires_acknowledgment) {
                        HorizontalDivider()

                        Text(
                            text = "Acknowledgment Required",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = acknowledgmentNote,
                            onValueChange = { acknowledgmentNote = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Enter your acknowledgment") },
                            placeholder = {
                                Text(
                                    text = "e.g., I have read and understood this message",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            minLines = 2,
                            maxLines = 4
                        )

                        Text(
                            text = "Please confirm that you have read and understood this message",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!message.requires_acknowledgment) {
                            Button(
                                onClick = { onDismiss() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Text(
                                    text = "OK",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    if (message.type != "CRITICAL") {
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = message.type != "CRITICAL",
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            Button(
                                onClick = {
                                    if (acknowledgmentNote.isNotBlank()) {
                                        onAcknowledge(acknowledgmentNote.trim())
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = acknowledgmentNote.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Text(
                                    text = "Acknowledge",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getMessageTypeStyle(type: String): MessageTypeStyle {
    val colorScheme = MaterialTheme.colorScheme

    return when (type.uppercase()) {
        "REMINDER" -> MessageTypeStyle(
            label = "REMINDER",
            icon = Icons.Outlined.Notifications,
            iconColor = colorScheme.primary,
            backgroundColor = colorScheme.primaryContainer.copy(alpha = 0.3f),
            textColor = colorScheme.onPrimaryContainer
        )
        "NOTICE" -> MessageTypeStyle(
            label = "NOTICE",
            icon = Icons.Outlined.Info,
            iconColor = colorScheme.tertiary,
            backgroundColor = colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            textColor = colorScheme.onTertiaryContainer
        )
        "WARNING" -> MessageTypeStyle(
            label = "WARNING",
            icon = Icons.Outlined.Warning,
            iconColor = Color(0xFFFFA726),
            backgroundColor = Color(0xFFFFF3E0),
            textColor = Color(0xFFE65100)
        )
        "CRITICAL" -> MessageTypeStyle(
            label = "⚠️ CRITICAL",
            icon = Icons.Filled.ErrorOutline,
            iconColor = colorScheme.error,
            backgroundColor = colorScheme.errorContainer.copy(alpha = 0.3f),
            textColor = colorScheme.onErrorContainer
        )
        else -> MessageTypeStyle(
            label = "MESSAGE",
            icon = Icons.Outlined.Mail,
            iconColor = colorScheme.primary,
            backgroundColor = colorScheme.surfaceVariant,
            textColor = colorScheme.onSurfaceVariant
        )
    }
}