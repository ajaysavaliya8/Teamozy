package com.hrms.jeejateamozy.feature.attendance.presentation.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrms.jeejateamozy.core.network.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitCorrectionRequestBottomSheet(
    date: String,
    attendanceRecordId: Int?,
    options: CorrectionRequestOptionsData,
    onDismiss: () -> Unit,
    onSubmit: (
        requestType: String,
        reason: String,
        requestedStatus: String?,
        requestedCheckIn: String?,
        requestedCheckOut: String?,
        leaveTypeId: Int?,
        priority: String,
        attachmentUri: Uri?
    ) -> Unit
) {
    var requestType by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var requestedStatus by remember { mutableStateOf<String?>(null) }
    var requestedCheckIn by remember { mutableStateOf("") }
    var requestedCheckOut by remember { mutableStateOf("") }
    var leaveTypeId by remember { mutableStateOf<Int?>(null) }
    var priority by remember { mutableStateOf("normal") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentFileName by remember { mutableStateOf<String?>(null) }

    var showReasonError by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            attachmentUri = it
            attachmentFileName = it.lastPathSegment ?: "attachment"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Submit Correction Request",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Date: $date",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Request Type Dropdown
            var expandedRequestType by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedRequestType,
                onExpandedChange = { expandedRequestType = it }
            ) {
                OutlinedTextField(
                    value = options.requestTypes.find { it.value == requestType }?.label ?: "Select Type",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Request Type *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRequestType) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expandedRequestType,
                    onDismissRequest = { expandedRequestType = false }
                ) {
                    options.requestTypes.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(option.label, fontWeight = FontWeight.Bold)
                                    Text(
                                        option.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                requestType = option.value
                                expandedRequestType = false
                            }
                        )
                    }
                }
            }

            // Conditional Fields based on Request Type
            when (requestType) {
                "NEW_ATTENDANCE" -> {
                    // Status Dropdown
                    var expandedStatus by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedStatus,
                        onExpandedChange = { expandedStatus = it }
                    ) {
                        OutlinedTextField(
                            value = requestedStatus?.replace("_", " ") ?: "Select Status",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Requested Status *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedStatus,
                            onDismissRequest = { expandedStatus = false }
                        ) {
                            options.statusOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        requestedStatus = option.value
                                        expandedStatus = false
                                    }
                                )
                            }
                        }
                    }
                }

                "CORRECTION" -> {
                    // Check In Time
                    OutlinedTextField(
                        value = requestedCheckIn,
                        onValueChange = { requestedCheckIn = it },
                        label = { Text("Check In Time") },
                        placeholder = { Text("YYYY-MM-DD HH:MM:SS") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Check Out Time
                    OutlinedTextField(
                        value = requestedCheckOut,
                        onValueChange = { requestedCheckOut = it },
                        label = { Text("Check Out Time") },
                        placeholder = { Text("YYYY-MM-DD HH:MM:SS") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "LEAVE_LINKAGE" -> {
                    // Leave Type Dropdown
                    var expandedLeave by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedLeave,
                        onExpandedChange = { expandedLeave = it }
                    ) {
                        OutlinedTextField(
                            value = options.leaveTypes.find { it.id == leaveTypeId }?.label ?: "Select Leave Type",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Leave Type *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLeave) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedLeave,
                            onDismissRequest = { expandedLeave = false }
                        ) {
                            options.leaveTypes.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(option.label, fontWeight = FontWeight.Bold)
                                            option.description?.let {
                                                Text(
                                                    it,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        leaveTypeId = option.id
                                        expandedLeave = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Priority Dropdown
            var expandedPriority by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedPriority,
                onExpandedChange = { expandedPriority = it }
            ) {
                OutlinedTextField(
                    value = options.priorityOptions.find { it.value == priority }?.label ?: priority,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priority") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPriority) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedPriority,
                    onDismissRequest = { expandedPriority = false }
                ) {
                    options.priorityOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                priority = option.value
                                expandedPriority = false
                            }
                        )
                    }
                }
            }

            // Reason TextField
            OutlinedTextField(
                value = reason,
                onValueChange = {
                    reason = it
                    showReasonError = it.length < 10
                },
                label = { Text("Reason *") },
                placeholder = { Text("Minimum 10 characters") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                minLines = 4,
                maxLines = 8,
                isError = showReasonError,
                supportingText = {
                    if (showReasonError) {
                        Text("Reason must be at least 10 characters")
                    } else {
                        Text("${reason.length}/1000 characters")
                    }
                }
            )

            // Attachment Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Attachment (Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Max size: 5MB (image/PDF)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (attachmentUri != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attached file"
                                )
                                Text(
                                    text = attachmentFileName ?: "File attached",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(onClick = {
                                attachmentUri = null
                                attachmentFileName = null
                            }) {
                                Icon(Icons.Default.Close, "Remove attachment")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AttachFile, "Attach file")
                            Spacer(Modifier.width(8.dp))
                            Text("Choose File")
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (requestType.isNotEmpty() && reason.length >= 10) {
                        onSubmit(
                            requestType,
                            reason,
                            requestedStatus,
                            requestedCheckIn.takeIf { it.isNotBlank() },
                            requestedCheckOut.takeIf { it.isNotBlank() },
                            leaveTypeId,
                            priority,
                            attachmentUri
                        )
                    } else {
                        showReasonError = reason.length < 10
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = requestType.isNotEmpty() && reason.length >= 10
            ) {
                Icon(Icons.Default.Send, "Submit")
                Spacer(Modifier.width(8.dp))
                Text("Submit Request")
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
