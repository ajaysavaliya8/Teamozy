package com.hrms.jeejateamozy.feature.attendance.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.network.PendingMessage
import com.hrms.jeejateamozy.core.utils.PreferencesManager

// Accent colors per message type
private fun accentColor(type: String?): Color = when (type?.uppercase()) {
    "REMINDER" -> Color(0xFF6366F1)
    "NOTICE"   -> Color(0xFF0EA5E9)
    "WARNING"  -> Color(0xFFD97706)
    "CRITICAL" -> Color(0xFFDC2626)
    else       -> Color(0xFF6366F1)
}

private fun typeLabel(type: String?): String = when (type?.uppercase()) {
    "REMINDER" -> "Reminder"
    "NOTICE"   -> "Notice"
    "WARNING"  -> "Warning"
    "CRITICAL" -> "Critical"
    else       -> "Message"
}

/**
 * Pending Message Dialog — clean iOS-style alert
 */
@Composable
fun PendingMessageDialog(
    message: PendingMessage,
    onDismiss: () -> Unit,
    onAcknowledge: (acknowledgmentNote: String?) -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager.getInstance(context) }
    var acknowledgmentNote by remember { mutableStateOf("") }
    val accent = accentColor(message.type)

    Dialog(
        onDismissRequest = {
            if (!message.requires_acknowledgment) {
                onAcknowledge(null)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !message.requires_acknowledgment,
            dismissOnClickOutside = !message.requires_acknowledgment,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 20.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Thin colored top accent bar ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(accent)
                )

                // ── Scrollable content ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 20.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Type label
                    Text(
                        text = typeLabel(message.type).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title
                    Text(
                        text = message.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Body
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Attachment ──
                    val attachmentUrl = remember(message) {
                        message.resolveAttachmentUrl(NetworkModule.BASE_URL)
                    }
                    if (message.has_attachment && attachmentUrl != null) {
                        Spacer(modifier = Modifier.height(14.dp))

                        val imageLoader = remember {
                            ImageLoader.Builder(context)
                                .okHttpClient(NetworkModule.okHttp)
                                .build()
                        }
                        var imageLoadFailed by remember { mutableStateOf(false) }

                        if (!imageLoadFailed) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(attachmentUrl)
                                    .crossfade(true)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = "Attachment",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 80.dp, max = 300.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.Center,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = accent
                                        )
                                    }
                                },
                                error = { imageLoadFailed = true }
                            )
                        }
                    }

                    // ── Acknowledgment section ──
                    if (message.requires_acknowledgment) {
                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Acknowledgment Required",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = acknowledgmentNote,
                            onValueChange = { acknowledgmentNote = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "Type your acknowledgment here...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }

                // ── iOS-style action buttons ──
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                if (!message.requires_acknowledgment) {
                    // Single OK button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple()
                            ) { onAcknowledge(null) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        val isCritical = message.type?.uppercase() == "CRITICAL"

                        // Cancel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (!isCritical) Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple()
                                    ) { onDismiss() }
                                    else Modifier
                                )
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isCritical)
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Vertical divider
                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                        )

                        // Acknowledge
                        val ackEnabled = acknowledgmentNote.isNotBlank()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (ackEnabled) Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple()
                                    ) { onAcknowledge(acknowledgmentNote.trim()) }
                                    else Modifier
                                )
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Acknowledge",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (ackEnabled) accent
                                else accent.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}
