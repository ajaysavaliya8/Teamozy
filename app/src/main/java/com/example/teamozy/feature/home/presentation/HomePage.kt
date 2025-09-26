@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.teamozy.feature.home.presentation

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamozy.feature.attendance.data.AttendanceRepository
import com.example.teamozy.feature.attendance.presentation.AttendanceViewModel
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun HomePage(
    onLogout: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val vm = rememberAttendanceViewModel(context)
    val ui = vm.ui.collectAsState().value

    // Keep reason outside the sheet Composable so it doesn't reset on recomposition
    var reason by rememberSaveable { mutableStateOf("") }

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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status + messages
                if (ui.errorMessage != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text(ui.errorMessage ?: "") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (ui.successMessage != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text(ui.successMessage ?: "") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Face verify toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Verify with Face", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "When enabled, the server will verify face for this action.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = ui.faceVerifyEnabled,
                        onCheckedChange = { vm.setFaceVerifyEnabled(it) },
                        enabled = !ui.isLoading && !ui.isRefreshing
                    )
                }

                // Last accuracy (if available)
                ui.lastAccuracyMeters?.let {
                    Text(
                        text = "Location accuracy: %.0f m".format(it),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Big action button
                val buttonText = when {
                    ui.isLoading -> "Please wait…"
                    ui.canCheckIn -> "Check In"
                    else -> "Check Out"
                }

                Button(
                    onClick = {
                        if (ui.canCheckIn) vm.checkIn(context) else vm.checkOut(context)
                    },
                    enabled = !ui.isLoading && !ui.isRefreshing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(buttonText, fontSize = 18.sp, textAlign = TextAlign.Center)
                }
            }

            // --- Violation Bottom Sheet ---
            if (ui.showViolationSheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = { vm.dismissViolationSheet() },
                    sheetState = sheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Violation required", style = MaterialTheme.typography.titleLarge)

                        ui.violationMessage?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Reason (required)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    reason = ""
                                    vm.dismissViolationSheet()
                                },
                                enabled = !ui.isSubmittingViolation,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) { Text("Cancel") }

                            Button(
                                onClick = { vm.submitViolation(reason.trim()) },
                                enabled = reason.isNotBlank() && !ui.isSubmittingViolation,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(if (ui.isSubmittingViolation) "Submitting…" else "Submit")
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/** Lightweight factory without DI */
@Composable
private fun rememberAttendanceViewModel(context: Context): AttendanceViewModel {
    val repo = remember(context) { AttendanceRepository(context) }
    return remember { AttendanceViewModel(repo) }
}
