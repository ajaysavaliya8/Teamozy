@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.teamozy.core.utils.NetworkMonitor
import com.example.teamozy.core.utils.PermissionHelper
import com.example.teamozy.feature.attendance.data.AttendanceRepository
import com.example.teamozy.feature.attendance.presentation.AttendanceViewModel

@Composable
fun HomePage(
    onLogout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val vm = rememberAttendanceViewModel(context)
    val ui by vm.ui.collectAsState()

    // Online/offline state
    val isOnline by remember { NetworkMonitor.isOnlineFlow(context) }
        .collectAsState(initial = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teamozy") },
                actions = {
                    TextButton(onClick = { vm.refreshStatus() }, enabled = !ui.isRefreshing) {
                        Text(if (ui.isRefreshing) "Refreshing…" else "Refresh")
                    }
                    TextButton(onClick = { onLogout?.invoke() }) { Text("Logout") }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Offline banner
                if (!isOnline) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("You’re offline", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Check your internet connection.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Card {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (ui.canCheckIn) "You can Check In" else "You can Check Out",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Face verify toggle (flag only; no capture)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verify with Face (flag only)")
                            Switch(
                                checked = ui.faceVerifyEnabled,
                                onCheckedChange = { vm.setFaceVerifyEnabled(it) },
                                enabled = !ui.isLoading
                            )
                        }

                        // Action Button (disabled when offline)
                        Button(
                            onClick = {
                                if (ui.canCheckIn) vm.checkIn(context) else vm.checkOut(context)
                            },
                            enabled = !ui.isLoading && isOnline
                        ) {
                            Text(
                                when {
                                    !isOnline -> "No Internet"
                                    ui.isLoading && ui.canCheckIn -> "Checking In…"
                                    ui.isLoading && !ui.canCheckIn -> "Checking Out…"
                                    ui.canCheckIn -> "Check In"
                                    else -> "Check Out"
                                }
                            )
                        }

                        // Last accuracy (if any)
                        ui.lastAccuracyMeters?.let { acc ->
                            Text(
                                "Last accuracy: ${acc.toInt()} m",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Messages
                ui.errorMessage?.let { msg ->
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Error", style = MaterialTheme.typography.titleSmall)
                            Text(msg)
                            if (msg.contains("GPS is disabled", ignoreCase = true)) {
                                OutlinedButton(
                                    onClick = { PermissionHelper.openLocationSettings(context) }
                                ) { Text("Enable GPS") }
                            }
                            OutlinedButton(onClick = { vm.clearMessages() }) { Text("Dismiss") }
                        }
                    }
                }

                ui.successMessage?.let { msg ->
                    ElevatedCard {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Success", style = MaterialTheme.typography.titleSmall)
                            Text(msg)
                            OutlinedButton(onClick = { vm.clearMessages() }) { Text("OK") }
                        }
                    }
                }
            }

            // Violation bottom sheet
            if (ui.showViolationSheet) {
                ViolationSheet(
                    message = ui.violationMessage.orEmpty(),
                    isSubmitting = ui.isSubmittingViolation,
                    onSubmit = { reason -> vm.submitViolation(reason) },
                    onDismiss = { vm.dismissViolationSheet() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViolationSheet(
    message: String,
    isSubmitting: Boolean,
    onSubmit: (reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var reason by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Additional details required", style = MaterialTheme.typography.titleMedium)
            if (message.isNotBlank()) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason") },
                singleLine = false,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSubmit(reason.trim()) },
                    enabled = reason.isNotBlank() && !isSubmitting
                ) {
                    Text(if (isSubmitting) "Submitting…" else "Submit")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Lightweight factory without DI */
@Composable
private fun rememberAttendanceViewModel(context: android.content.Context): AttendanceViewModel {
    val repo = remember(context) { AttendanceRepository(context) }
    return remember { AttendanceViewModel(repo) }
}
