package com.hrms.jeejateamozy.feature.attendance.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hrms.jeejateamozy.core.network.PostShiftCheckPayload

private val IndigoColor = Color(0xFF6366F1)
private val GreenColor = Color(0xFF10B981)
private val ErrorColor = Color(0xFFEF4444)

@Composable
fun PostShiftCheckDialog(
    payload: PostShiftCheckPayload,
    isLoading: Boolean,
    resultMessage: String?,
    isError: Boolean = false,
    onAction: (actionId: String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Non-dismissable — employee must respond */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
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
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IndigoColor.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = IndigoColor
                        )
                        Text(
                            text = "SHIFT CHECK",
                            style = MaterialTheme.typography.labelMedium,
                            color = IndigoColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title
                    Text(
                        text = payload.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Message
                    Text(
                        text = payload.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    // Shift end time
                    if (!payload.shiftEndTime.isNullOrBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Shift ended at ${payload.shiftEndTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Result message
                    if (resultMessage != null) {
                        Text(
                            text = resultMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isError) ErrorColor else GreenColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Loading or action buttons
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = IndigoColor,
                                strokeWidth = 3.dp
                            )
                        }
                    } else if (resultMessage == null || isError) {
                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val actions = payload.actions
                            // "done" button (outlined)
                            val doneAction = actions.find { it.id == "done" }
                            if (doneAction != null) {
                                OutlinedButton(
                                    onClick = { onAction(doneAction.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 14.dp)
                                ) {
                                    Text(
                                        text = doneAction.label,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            // "working" button (filled green)
                            val workingAction = actions.find { it.id == "working" }
                            if (workingAction != null) {
                                Button(
                                    onClick = { onAction(workingAction.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GreenColor
                                    ),
                                    contentPadding = PaddingValues(vertical = 14.dp)
                                ) {
                                    Text(
                                        text = workingAction.label,
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
}
