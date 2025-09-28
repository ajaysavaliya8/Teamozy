@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.home.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.teamozy.feature.attendance.data.AttendanceRepository
import com.example.teamozy.feature.attendance.presentation.AttendanceViewModel
import com.example.teamozy.feature.face.presentation.FaceCaptureScreen
import com.example.teamozy.feature.face.presentation.FaceRegistrationScreen
import com.example.teamozy.feature.face.data.EmbeddingExtractor
import com.example.teamozy.feature.face.data.FaceStore
import com.example.teamozy.feature.face.domain.FaceVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val VERIFY_THRESHOLD = 0.55f
private enum class PunchAction { IN, OUT }

@Composable
fun HomePage(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val vm = rememberAttendanceViewModel(context)
    val ui = vm.ui.collectAsState().value
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    // Load initial Check in/out status from backend
    LaunchedEffect(Unit) { vm.refreshStatus() }

    var pendingAction by remember { mutableStateOf<PunchAction?>(null) }
    var showRegistration by remember { mutableStateOf(false) }
    var showVerify by remember { mutableStateOf(false) }
    var verifyBusy by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }

    fun proceedPunch() {
        when (pendingAction) {
            PunchAction.IN  -> vm.checkIn(context)
            PunchAction.OUT -> vm.checkOut(context)
            null -> Unit
        }
        // Never leak the flag to the next attempt
        vm.setFaceVerifyEnabled(false)
        pendingAction = null
    }

    Scaffold(snackbarHost = { SnackbarHost(snack) }) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            val buttonText = if (ui.canCheckIn) "Check in" else "Check out"
            Button(
                onClick = {
                    pendingAction = if (ui.canCheckIn) PunchAction.IN else PunchAction.OUT
                    verifyError = null

                    val store = FaceStore.getInstance(context)
                    if (!store.hasEnrollment()) {
                        // First-time: open Registration (no API call after this)
                        showRegistration = true
                    } else {
                        // Already enrolled: open Verify → only on match we call API
                        showVerify = true
                    }
                },
                enabled = !ui.isLoading,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(buttonText, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(10.dp))

            // TEMP helper for testing re-enrollment
            OutlinedButton(
                onClick = {
                    FaceStore.getInstance(context).clear()
                    vm.setFaceVerifyEnabled(false)
                    pendingAction = null
                    showRegistration = false
                    showVerify = false
                    verifyBusy = false
                    verifyError = null
                    scope.launch { snack.showSnackbar("Face data cleared. Tap the main button to re-register.") }
                },
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(44.dp)
            ) { Text("Clear face data (temp)") }

            if (!ui.errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    ui.errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }

    // === First-time registration (NO API CALL here) ===
    if (showRegistration) {
        FaceRegistrationScreen(
            onDismiss = {
                showRegistration = false
                pendingAction = null
            },
            onEnrolled = {
                // ✅ Only finish registration. DO NOT punch or set face_verify here.
                showRegistration = false
                pendingAction = null
                vm.setFaceVerifyEnabled(false)

                // Optional: confirm we really saved the vector
                FaceStore.getInstance(context).loadEmbedding()?.let {
                    android.util.Log.d("FaceEnroll", "embedding512=" + it.joinToString(prefix = "[", postfix = "]"))
                }

                // Tell user to tap again to proceed (then Verify → API)
                scope.launch { snack.showSnackbar("Face registered. Tap the button again to continue.") }
            }
        )
    }

    // === Verification for enrolled users (API only on match) ===
    if (showVerify) {
        FaceCaptureScreen(
            onDismiss = {
                showVerify = false
                verifyBusy = false
                verifyError = null
                pendingAction = null
            },
            onCaptured = { /* unused */ },
            onBitmapCaptured = { bmp: Bitmap ->
                if (verifyBusy) return@FaceCaptureScreen
                verifyBusy = true
                verifyError = null

                val store = FaceStore.getInstance(context)
                val stored = store.loadEmbedding()
                if (stored == null) {
                    verifyError = "Face data missing. Please enroll again."
                    verifyBusy = false
                    return@FaceCaptureScreen
                }

                // Use the top-level scope (rememberCoroutineScope)
                scope.launch {
                    try {
                        val extractor = EmbeddingExtractor.getInstance(context)
                        val live = withContext(Dispatchers.Default) { extractor.extract(bmp) }
                        android.util.Log.d("FaceVerify", "live512=" + live.joinToString(prefix = "[", postfix = "]"))

                        val matched = withContext(Dispatchers.Default) {
                            FaceVerifier.isMatch(stored, live, VERIFY_THRESHOLD)
                        }
                        android.util.Log.d("FaceVerify", "matched=$matched thr=$VERIFY_THRESHOLD")

                        if (matched) {
                            // ✅ Only now we mark and perform API call
                            vm.setFaceVerifyEnabled(true)
                            showVerify = false
                            verifyBusy = false
                            proceedPunch()
                        } else {
                            verifyError = "Face didn’t match. Try again in good light."
                            verifyBusy = false
                        }
                    } catch (t: Throwable) {
                        verifyError = t.message ?: "Verification failed"
                        verifyBusy = false
                    }
                }
            },
            showReasonField = false,
            reasonMessage = null,
            isSubmitting = verifyBusy,
            onSubmit = { /* unused */ },
            serverError = verifyError
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
