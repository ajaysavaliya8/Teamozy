@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.home.presentation

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.teamozy.core.utils.PreferencesManager
import com.example.teamozy.feature.attendance.data.AttendanceRepository
import com.example.teamozy.feature.attendance.presentation.AttendanceViewModel
import com.example.teamozy.feature.face.data.EmbeddingExtractor
import com.example.teamozy.feature.face.data.FaceStore
import com.example.teamozy.feature.face.domain.FaceVerifier
import com.example.teamozy.feature.face.presentation.FaceCaptureScreen
import com.example.teamozy.feature.face.presentation.FaceRegistrationScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val VERIFY_THRESHOLD = 0.55f
private const val TAG = "HomePage"
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
    val prefs = remember { PreferencesManager.getInstance(context) }

    // Load initial status
    LaunchedEffect(Unit) {
        vm.refreshStatus()

        // Debug: Log stored embedding info (first 5 values only)
        val store = FaceStore.getInstance(context)
        if (store.hasEnrollment()) {
            val embedding = store.loadEmbedding()
            Log.d(TAG, "Stored embedding loaded: size=${embedding?.size}, first5=${embedding?.take(5)}")
        } else {
            Log.d(TAG, "No face enrollment found")
        }
    }

    var pendingAction by remember { mutableStateOf<PunchAction?>(null) }
    var showRegistration by remember { mutableStateOf(false) }
    var showVerify by remember { mutableStateOf(false) }
    var verifyBusy by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }

    fun proceedPunch() {
        when (pendingAction) {
            PunchAction.IN  -> {
                Log.d(TAG, "Proceeding with CHECK IN (face_verify=true)")
                vm.checkIn(context)
            }
            PunchAction.OUT -> {
                Log.d(TAG, "Proceeding with CHECK OUT (face_verify=true)")
                vm.checkOut(context)
            }
            null -> {
                Log.w(TAG, "proceedPunch() called with null action")
            }
        }
        vm.setFaceVerifyEnabled(false)
        pendingAction = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Filled.Home, "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Outlined.Person, "Attendance") },
                    label = { Text("Attendance") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Outlined.Settings, "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Company",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Teamozy",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { /* Profile */ },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Greeting
            Text(
                text = "Hello, ${prefs.userName ?: "User"}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Main Punch Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val buttonText = if (ui.canCheckIn) "Check In" else "Check Out"
                val buttonColor = if (ui.canCheckIn) Color(0xFF00C896) else Color(0xFFFF6B6B)

                Button(
                    onClick = {
                        // Determine action based on current state
                        pendingAction = if (ui.canCheckIn) PunchAction.IN else PunchAction.OUT
                        verifyError = null

                        Log.d(TAG, "Button clicked: action=$pendingAction")

                        val store = FaceStore.getInstance(context)
                        if (!store.hasEnrollment()) {
                            Log.d(TAG, "No enrollment - showing registration")
                            showRegistration = true
                        } else {
                            Log.d(TAG, "Enrollment exists - showing verification")
                            showVerify = true
                        }
                    },
                    enabled = !ui.isLoading && !ui.isRefreshing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        if (ui.canCheckIn) Icons.Filled.CheckCircle else Icons.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status indicator
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (ui.isLoading) "Processing..."
                    else if (ui.isRefreshing) "Refreshing status..."
                    else "Ready",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))

            Spacer(Modifier.height(24.dp))

            // Error/Success Messages
            if (!ui.errorMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = ui.errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (!ui.successMessage.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Text(
                        text = ui.successMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Debug: Clear face data button
            OutlinedButton(
                onClick = {
                    FaceStore.getInstance(context).clear()
                    vm.setFaceVerifyEnabled(false)
                    pendingAction = null
                    showRegistration = false
                    showVerify = false
                    verifyBusy = false
                    verifyError = null
                    scope.launch {
                        snack.showSnackbar("Face data cleared. Tap button to re-enroll.")
                    }
                    Log.d(TAG, "Face data cleared")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text("Clear Face Data")
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Face Registration Screen
    if (showRegistration) {
        FaceRegistrationScreen(
            onDismiss = {
                showRegistration = false
                pendingAction = null
                Log.d(TAG, "Registration dismissed")
            },
            onEnrolled = {
                showRegistration = false
                pendingAction = null
                vm.setFaceVerifyEnabled(false)
                scope.launch {
                    snack.showSnackbar("Face registered! Tap the button again to verify and punch.")
                }
                Log.d(TAG, "Registration completed successfully")
            }
        )
    }

    // Face Verification Screen
    if (showVerify) {
        FaceCaptureScreen(
            onDismiss = {
                showVerify = false
                verifyBusy = false
                verifyError = null
                pendingAction = null
                Log.d(TAG, "Verification dismissed")
            },
            onCaptured = { /* unused */ },
            onBitmapCaptured = { bmp: Bitmap ->
                if (verifyBusy) {
                    bmp.recycle()
                    return@FaceCaptureScreen
                }

                verifyBusy = true
                verifyError = null

                val store = FaceStore.getInstance(context)
                val stored = store.loadEmbedding()
                if (stored == null) {
                    verifyError = "Face data missing. Please enroll again."
                    verifyBusy = false
                    bmp.recycle()
                    Log.e(TAG, "Stored embedding is null")
                    return@FaceCaptureScreen
                }

                scope.launch {
                    try {
                        val extractor = EmbeddingExtractor.getInstance(
                            context = context,
                            numThreads = 4,
                            debugLogging = false
                        )

                        val live = withContext(Dispatchers.Default) {
                            extractor.extractNoRetry(bmp, 0)
                        }

                        val similarity = withContext(Dispatchers.Default) {
                            FaceVerifier.cosineSim(stored, live)
                        }

                        val matched = similarity >= VERIFY_THRESHOLD

                        Log.d(TAG, "Attempt result: similarity=${String.format("%.2f", similarity)}, matched=$matched")

                        if (matched) {
                            // SUCCESS - Close verification screen and proceed
                            vm.setFaceVerifyEnabled(true)
                            showVerify = false
                            verifyBusy = false

                            Log.d(TAG, "Face matched! Proceeding with punch...")
                            proceedPunch()
                        } else {
                            // NOT MATCHED - Update error but keep trying
                            verifyError = "Similarity: ${String.format("%.2f", similarity)} (need ≥ $VERIFY_THRESHOLD). Keep trying..."
                            verifyBusy = false
                            Log.d(TAG, "No match yet, will retry automatically")
                        }
                    } catch (e: IllegalStateException) {
                        // Face quality issue - update error but keep trying
                        verifyError = e.message ?: "Face quality issue - keep trying..."
                        verifyBusy = false
                        Log.d(TAG, "Quality issue: ${e.message}")
                    } catch (t: Throwable) {
                        verifyError = t.message ?: "Error - keep trying..."
                        verifyBusy = false
                        Log.e(TAG, "Verification error", t)
                    } finally {
                        bmp.recycle()
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

    LaunchedEffect(ui.errorMessage) {
        ui.errorMessage?.let {
            snack.showSnackbar(it)
            Log.d(TAG, "Error: $it")
        }
    }

    LaunchedEffect(ui.successMessage) {
        ui.successMessage?.let {
            snack.showSnackbar(it)
            Log.d(TAG, "Success: $it")
        }
    }
}

@Composable
private fun rememberAttendanceViewModel(context: android.content.Context): AttendanceViewModel {
    val repo = remember(context) { AttendanceRepository(context) }
    return remember { AttendanceViewModel(repo) }
}