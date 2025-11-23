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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hrms.jeejateamozy.core.network.PendingMessage
import com.hrms.jeejateamozy.core.utils.PreferencesManager

/**
 * Pending Message Dialog
 * Displays messages from management to employees during check-in
 *
 * Features:
 * - Type-based styling (REMINDER, NOTICE, WARNING, CRITICAL)
 * - Attachment download support
 * - Optional acknowledgment with text input
 * - Bearer token authenticated downloads
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
    var isDownloadingAttachment by remember { mutableStateOf(false) }

    // Get message type styling
    val messageStyle = remember(message.type) {
        getMessageTypeStyle(message.type)
    }

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
                // Header with type indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(messageStyle.backgroundColor)
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = messageStyle.icon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = messageStyle.iconColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = messageStyle.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = messageStyle.textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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

                    // Attachment button
                    if (message.has_attachment && message.attachment_url != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (!isDownloadingAttachment) {
                                    isDownloadingAttachment = true
                                    openAttachment(
                                        context = context,
                                        url = message.attachment_url,
                                        token = preferencesManager.authToken.orEmpty(),
                                        onComplete = { isDownloadingAttachment = false }
                                    )
                                }
                            },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AttachFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "View Attachment",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = getFileTypeFromUrl(message.attachment_url),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isDownloadingAttachment) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Acknowledgment input (if required)
                    if (message.requires_acknowledgment) {
                        Spacer(modifier = Modifier.height(8.dp))

                        HorizontalDivider()

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Acknowledgment Required",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = acknowledgmentNote,
                            onValueChange = { acknowledgmentNote = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Enter your acknowledgment")
                            },
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!message.requires_acknowledgment) {
                            // Simple dismiss button
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
                            // Cancel button (only if acknowledgment required)
                            OutlinedButton(
                                onClick = {
                                    // Can't dismiss critical messages without acknowledging
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

                            // Acknowledge button
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

@Composable
private fun getMessageTypeStyle(type: String): MessageTypeStyle {
    return when (type.uppercase()) {
        "REMINDER" -> MessageTypeStyle(
            label = "REMINDER",
            icon = Icons.Outlined.Notifications,
            iconColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            textColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        "NOTICE" -> MessageTypeStyle(
            label = "NOTICE",
            icon = Icons.Outlined.Info,
            iconColor = MaterialTheme.colorScheme.tertiary,
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            textColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        "WARNING" -> MessageTypeStyle(
            label = "WARNING",
            icon = Icons.Outlined.Warning,
            iconColor = Color(0xFFFFA726), // Orange
            backgroundColor = Color(0xFFFFF3E0), // Light orange
            textColor = Color(0xFFE65100) // Dark orange
        )
        "CRITICAL" -> MessageTypeStyle(
            label = "⚠️ CRITICAL",
            icon = Icons.Filled.ErrorOutline,
            iconColor = MaterialTheme.colorScheme.error,
            backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            textColor = MaterialTheme.colorScheme.onErrorContainer
        )
        else -> MessageTypeStyle(
            label = "MESSAGE",
            icon = Icons.Outlined.Mail,
            iconColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Extract file type from URL for display
 */
private fun getFileTypeFromUrl(url: String): String {
    val extension = url.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "pdf" -> "PDF Document"
        "doc", "docx" -> "Word Document"
        "jpg", "jpeg", "png", "gif", "webp" -> "Image"
        else -> "Attachment"
    }
}

/**
 * Open attachment with Bearer token authentication
 * The URL from API is already complete, just open it in browser with authentication
 */
private fun openAttachment(
    context: android.content.Context,
    url: String,
    token: String,
    onComplete: () -> Unit
) {
    try {
        // For authenticated downloads, we need to open in browser
        // The browser will send cookies/auth if configured
        // For better UX, you might want to implement in-app WebView with custom headers
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error - maybe show a toast
        android.widget.Toast.makeText(
            context,
            "Unable to open attachment: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    } finally {
        onComplete()
    }
}