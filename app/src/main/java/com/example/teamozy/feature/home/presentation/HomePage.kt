@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.teamozy.feature.attendance.data.AttendanceRepository
import com.example.teamozy.feature.attendance.presentation.AttendanceViewModel
import com.example.teamozy.feature.face.presentation.FaceCaptureScreen
import kotlinx.coroutines.launch

@Composable
fun HomePage(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val vm = rememberAttendanceViewModel(context)
    val ui by vm.ui.collectAsState()

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showFaceScreen by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) } // "IN" | "OUT"
    var isProcessing by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    var showReasonField by remember { mutableStateOf(false) }
    var reasonMessage by remember { mutableStateOf<String?>(null) }

    fun openFaceFor(action: String) {
        pendingAction = action
        showFaceScreen = true
        isProcessing = false
        isSubmitting = false
        showReasonField = false
        reasonMessage = null
    }

    suspend fun autoPunchAfterVerify() {
        if (isProcessing) return
        isProcessing = true
        try {
            when (pendingAction) {
                "IN"  -> vm.checkIn(context)
                "OUT" -> vm.checkOut(context)
            }
        } finally {
            isProcessing = false
        }
    }

    LaunchedEffect(
        ui.showViolationSheet, ui.violationMessage,
        ui.successMessage, ui.errorMessage, ui.isLoading
    ) {
        if (!showFaceScreen) return@LaunchedEffect

        when {
            // Violation → stay on screen and show reason field
            ui.showViolationSheet -> {
                showReasonField = true
                reasonMessage = ui.violationMessage
            }

            // Success → close screen and refresh status
            !ui.isLoading && ui.successMessage != null -> {
                vm.refreshStatus()
                showFaceScreen = false
                pendingAction = null
                showReasonField = false
                reasonMessage = null
                isSubmitting = false
            }

            // Error (like "Maximum one punch in allow.") → stay on face screen
            !ui.isLoading && ui.errorMessage != null -> {
                // do nothing here; FaceCaptureScreen will display the message
            }
        }
    }

    fun onFinalSubmit(reason: String?) {
        if (!showReasonField) return
        isSubmitting = true
        scope.launch {
            try {
                vm.submitViolation(reason.orEmpty())
                vm.refreshStatus()
                showFaceScreen = false
                pendingAction = null
                showReasonField = false
                reasonMessage = null
            } finally {
                isSubmitting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teamozy", fontWeight = FontWeight.SemiBold) },
                actions = { TextButton(onClick = onLogout) { Text("Logout") } }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snack) }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Welcome 👋", style = MaterialTheme.typography.titleLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Can Check In now: ${ui.canCheckIn}")
                    Button(onClick = { vm.refreshStatus() }, enabled = !ui.isRefreshing) {
                        Text(if (ui.isRefreshing) "Refreshing…" else "Refresh")
                    }
                }
                ui.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                ui.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }

            val busy = ui.isLoading || showFaceScreen || isProcessing || isSubmitting
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (ui.canCheckIn) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        onClick = { openFaceFor("IN") }
                    ) { Text(if (busy) "Please wait…" else "Check In") }
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        onClick = { openFaceFor("OUT") }
                    ) { Text(if (busy) "Please wait…" else "Check Out") }
                }
            }
        }
    }

    if (showFaceScreen) {
        var hasAutoPunched by remember { mutableStateOf(false) }

        FaceCaptureScreen(
            onDismiss = {
                if (!isProcessing && !isSubmitting) {
                    showFaceScreen = false
                    pendingAction = null
                    showReasonField = false
                    reasonMessage = null
                }
            },
            // your temporary flow (keep as-is)
            onCaptured = { _ ->
                if (hasAutoPunched) return@FaceCaptureScreen
                hasAutoPunched = true
                scope.launch { autoPunchAfterVerify() }
            },
            onBitmapCaptured = { _ ->
                if (hasAutoPunched) return@FaceCaptureScreen
                hasAutoPunched = true
                scope.launch { autoPunchAfterVerify() }
            },
            showReasonField = showReasonField,
            reasonMessage = reasonMessage,
            isSubmitting = isSubmitting || ui.isSubmittingViolation,
            onSubmit = { reason -> onFinalSubmit(reason) },
            serverError = ui.errorMessage
        )
    }

    LaunchedEffect(ui.errorMessage) { ui.errorMessage?.let { snack.showSnackbar(it) } }
    LaunchedEffect(ui.successMessage) { ui.successMessage?.let { snack.showSnackbar(it) } }
}

@Composable
private fun rememberAttendanceViewModel(context: android.content.Context): AttendanceViewModel {
    val repo = remember(context) { AttendanceRepository(context) }
    return remember { AttendanceViewModel(repo) }
}
