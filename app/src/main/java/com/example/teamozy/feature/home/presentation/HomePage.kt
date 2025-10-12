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
import com.example.teamozy.core.network.NetworkModule
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject

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

    // Get dynamic threshold from server (saved during login)
    val verifyThreshold = remember { prefs.faceThreshold }

    LaunchedEffect(Unit) {
        vm.refreshStatus()

        val store = FaceStore.getInstance(context)
        if (store.hasEnrollment()) {
            val embedding = store.loadEmbedding()
            Log.d(TAG, "Stored embedding loaded: size=${embedding?.size}, first5=${embedding?.take(5)}")
            Log.d(TAG, "Face accuracy threshold: $verifyThreshold (from server)")
        } else {
            Log.d(TAG, "No face enrollment found")
        }
    }

    var pendingAction by remember { mutableStateOf<PunchAction?>(null) }
    var showRegistration by remember { mutableStateOf(false) }
    var showVerify by remember { mutableStateOf(false) }
    var verifyBusy by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }
    var registrationBusy by remember { mutableStateOf(false) }

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
                    enabled = !ui.isLoading && !ui.isRefreshing && !registrationBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    if (registrationBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Registering face...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            if (ui.canCheckIn) Icons.Filled.CheckCircle else Icons.Filled.Close,
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
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (registrationBusy) "Registering face with server..."
                    else if (ui.isLoading) "Processing..."
                    else if (ui.isRefreshing) "Refreshing status..."
                    else "Ready",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show current threshold
                Text(
                    text = "Face threshold: ${String.format("%.2f", verifyThreshold)}",
                    style = MaterialTheme.typography.bodySmall,
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

            Spacer(Modifier.height(24.dp))
        }
    }

    // Face Registration Screen
    if (showRegistration) {
        FaceRegistrationScreen(
            onDismiss = {
                showRegistration = false
                pendingAction = null
                Log.d(TAG, "❌ Registration dismissed")
            },
            onEnrolled = { embedding, bitmap ->
                // Don't close screen yet - send to API first
                registrationBusy = true

                scope.launch {
                    try {
                        Log.d(TAG, "📤 Sending face vector and image to API...")

                        val api = NetworkModule.apiService

                        // Convert bitmap to JPEG bytes
                        val imageBytes = withContext(Dispatchers.Default) {
                            val outputStream = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                            outputStream.toByteArray()
                        }

                        // Convert embedding to JSON string
                        val embeddingJson = com.google.gson.Gson().toJson(embedding.toList())

                        // Create multipart request parts
                        val imagePart = okhttp3.MultipartBody.Part.createFormData(
                            "face_image",
                            "face_${System.currentTimeMillis()}.jpg",
                            okhttp3.RequestBody.create(
                                "image/jpeg".toMediaTypeOrNull(),
                                imageBytes
                            )
                        )

                        val faceDataPart = okhttp3.RequestBody.create(
                            "text/plain".toMediaTypeOrNull(),
                            embeddingJson
                        )

                        val priorityPart = okhttp3.RequestBody.create(
                            "text/plain".toMediaTypeOrNull(),
                            "normal"
                        )

                        val reasonPart = okhttp3.RequestBody.create(
                            "text/plain".toMediaTypeOrNull(),
                            "Initial face registration"
                        )

                        val response = withContext(Dispatchers.IO) {
                            api.registerFaceRecognition(
                                face_image = imagePart,
                                faceRecognitionData = faceDataPart,
                                priority = priorityPart,
                                reasonForChange = reasonPart
                            )
                        }

                        Log.d(TAG, "📥 API Response: code=${response.code()}")

                        registrationBusy = false
                        showRegistration = false
                        pendingAction = null

                        // Clean up bitmap
                        bitmap.recycle()

                        when {
                            response.isSuccessful && response.code() == 200 -> {
                                // Success - now save to local storage
                                withContext(Dispatchers.IO) {
                                    FaceStore.getInstance(context).saveEmbedding(embedding)
                                }

                                val message = response.body()?.message
                                    ?: "Face registered successfully! Your face recognition is now active."

                                Log.d(TAG, "✅ Face registered successfully on server and saved locally")

                                vm.setFaceVerifyEnabled(false)
                                snack.showSnackbar(message)
                            }

                            response.code() == 403 -> {
                                // Permission denied
                                val errorBody = response.errorBody()?.string()
                                val message = extractErrorMessage(errorBody)
                                    ?: "Face registration not enabled for your account. Contact HR/Admin."

                                Log.e(TAG, "❌ Registration forbidden: $message")
                                snack.showSnackbar(message)
                            }

                            response.code() == 409 -> {
                                // Pending request exists
                                val errorBody = response.errorBody()?.string()
                                val message = extractErrorMessage(errorBody)
                                    ?: "You have a pending face recognition request."

                                Log.w(TAG, "⚠️ Pending request: $message")
                                snack.showSnackbar(message)
                            }

                            response.code() == 404 -> {
                                // Employee not found
                                val errorBody = response.errorBody()?.string()
                                val message = extractErrorMessage(errorBody)
                                    ?: "Employee not found."

                                Log.e(TAG, "❌ Employee not found: $message")
                                snack.showSnackbar(message)
                            }

                            response.code() == 400 -> {
                                // Bad request - invalid data
                                val errorBody = response.errorBody()?.string()
                                val message = extractErrorMessage(errorBody)
                                    ?: "Invalid face data. Please try again."

                                Log.e(TAG, "❌ Bad request: $message")
                                snack.showSnackbar(message)
                            }

                            else -> {
                                // Other errors
                                val errorBody = response.errorBody()?.string()
                                val message = extractErrorMessage(errorBody)
                                    ?: "Failed to register face. Please try again."

                                Log.e(TAG, "❌ Registration failed: code=${response.code()}, message=$message")
                                snack.showSnackbar(message)
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error registering face", e)
                        registrationBusy = false
                        showRegistration = false
                        pendingAction = null

                        // Clean up bitmap on error
                        try { bitmap.recycle() } catch (_: Exception) {}

                        snack.showSnackbar("Network error. Please check your connection and try again.")
                    }
                }
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
                Log.d(TAG, "❌ Verification dismissed")
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
                    Log.e(TAG, "❌ Stored embedding is null - enrollment data missing!")
                    return@FaceCaptureScreen
                }

                Log.d(TAG, "✅ Stored embedding loaded successfully (size: ${stored.size})")

                scope.launch {
                    try {
                        Log.d(TAG, "🔍 Starting face verification with threshold: $verifyThreshold")

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

                        // Use dynamic threshold from server
                        val matched = similarity >= verifyThreshold

                        Log.d(TAG, "📊 Verification result: similarity=${String.format("%.3f", similarity)}, threshold=${String.format("%.3f", verifyThreshold)}, matched=$matched")

                        if (matched) {
                            // SUCCESS - Close verification screen and proceed
                            vm.setFaceVerifyEnabled(true)
                            showVerify = false
                            verifyBusy = false

                            Log.d(TAG, "✅ Face matched! Proceeding with punch...")
                            proceedPunch()
                        } else {
                            // NOT MATCHED - Update error but keep trying
                            verifyError = "Similarity: ${String.format("%.2f", similarity)} (need ≥ ${String.format("%.2f", verifyThreshold)}). Keep trying..."
                            verifyBusy = false
                            Log.d(TAG, "❌ No match yet, will retry automatically")
                        }
                    } catch (e: IllegalStateException) {
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

// Helper function to extract error message from response
private fun extractErrorMessage(errorBody: String?): String? {
    return try {
        if (errorBody.isNullOrBlank()) return null
        val json = JSONObject(errorBody)
        json.optString("message").ifBlank {
            json.optString("detail").ifBlank { null }
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun rememberAttendanceViewModel(context: android.content.Context): AttendanceViewModel {
    val repo = remember(context) { AttendanceRepository(context) }
    return remember { AttendanceViewModel(repo) }
}