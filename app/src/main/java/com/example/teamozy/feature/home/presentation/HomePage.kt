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
import com.example.teamozy.feature.face.data.FaceStore
import com.example.teamozy.feature.face.presentation.FaceRegistrationScreen
import com.example.teamozy.feature.profile.presentation.ProfileScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull

private const val TAG = "HomePage"
private enum class HomeScreen { HOME, ATTENDANCE, PROFILE }

@Composable
fun rememberAttendanceViewModel(context: android.content.Context): AttendanceViewModel {
    return remember {
        AttendanceViewModel(AttendanceRepository(context))
    }
}

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

    // Navigation state
    var currentScreen by remember { mutableStateOf(HomeScreen.HOME) }

    // Get dynamic threshold from server (saved during login)
    val verifyThreshold = remember { prefs.faceThreshold }

    LaunchedEffect(Unit) {
        // Refresh status when app opens
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

    var showRegistration by remember { mutableStateOf(false) }
    var registrationBusy by remember { mutableStateOf(false) }

    // Show error messages in snackbar
    LaunchedEffect(ui.errorMessage) {
        ui.errorMessage?.let { error ->
            snack.showSnackbar(error)
        }
    }

    // Show success messages in snackbar
    LaunchedEffect(ui.successMessage) {
        ui.successMessage?.let { success ->
            snack.showSnackbar(success)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == HomeScreen.HOME,
                    onClick = { currentScreen = HomeScreen.HOME },
                    icon = { Icon(if (currentScreen == HomeScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home, "Home") },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = currentScreen == HomeScreen.ATTENDANCE,
                    onClick = { currentScreen = HomeScreen.ATTENDANCE },
                    icon = { Icon(if (currentScreen == HomeScreen.ATTENDANCE) Icons.Filled.DateRange else Icons.Outlined.DateRange, "Attendance") },
                    label = { Text("Attendance") }
                )

                NavigationBarItem(
                    selected = currentScreen == HomeScreen.PROFILE,
                    onClick = { currentScreen = HomeScreen.PROFILE },
                    icon = { Icon(if (currentScreen == HomeScreen.PROFILE) Icons.Filled.Person else Icons.Outlined.Person, "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        when (currentScreen) {
            HomeScreen.HOME -> {
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

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Refresh button
                                IconButton(
                                    onClick = { vm.refreshStatus() },
                                    enabled = !ui.isRefreshing
                                ) {
                                    if (ui.isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Refresh,
                                            contentDescription = "Refresh Status",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { currentScreen = HomeScreen.PROFILE },
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
                    }

                    Spacer(Modifier.height(24.dp))

                    // Greeting
                    Text(
                        text = "Hello, ${prefs.userName ?: "User"}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // Status message from server
                    if (ui.statusMessage.isNotEmpty()) {
                        Text(
                            text = ui.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // Main Action Button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (ui.currentState) {
                            "CHECK_IN_NEEDED" -> {
                                // Show Check In button
                                Button(
                                    onClick = {
                                        // TODO: Implement check-in logic
                                        scope.launch {
                                            snack.showSnackbar("Check-in functionality coming soon")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00C896)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Check In",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            "CHECK_OUT_NEEDED" -> {
                                // Show Check Out button
                                Button(
                                    onClick = {
                                        // TODO: Implement check-out logic
                                        scope.launch {
                                            snack.showSnackbar("Check-out functionality coming soon")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF6B6B)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ExitToApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Check Out",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            "COMPLETED" -> {
                                // Show completed status
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = Color(0xFF00C896)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = "Attendance Complete",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "You've completed your attendance for today",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Face recognition info if enabled
                        if (ui.faceRecognitionEnabled) {
                            Spacer(Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Face,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Face Recognition Enabled",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Required for attendance",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    if (ui.faceVector != null) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "Enrolled",
                                            tint = Color(0xFF00C896),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Warning,
                                            contentDescription = "Not Enrolled",
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }

            HomeScreen.ATTENDANCE -> {
                // Placeholder for future Attendance screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Attendance Screen\n(Coming Soon)", textAlign = TextAlign.Center)
                }
            }

            HomeScreen.PROFILE -> ProfileScreen(
                onNavigateToFaceChange = {
                    Log.d(TAG, "📝 Profile -> Face Registration")
                    currentScreen = HomeScreen.HOME
                    showRegistration = true
                },
                onLogout = {
                    Log.d(TAG, "🚪 Logout - clearing all data")
                    scope.launch {
                        try {
                            // Clear face data
                            withContext(Dispatchers.IO) {
                                FaceStore.getInstance(context).clear()
                            }
                            Log.d(TAG, "✅ Face data cleared")

                            // Clear preferences
                            prefs.clearAll()
                            Log.d(TAG, "✅ Preferences cleared")

                            onLogout()
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error during logout cleanup", e)
                            onLogout()
                        }
                    }
                },
                onBack = { currentScreen = HomeScreen.HOME }
            )
        }
    }

    // Face Registration Screen
    if (showRegistration) {
        FaceRegistrationScreen(
            onDismiss = {
                showRegistration = false
                Log.d(TAG, "❌ Registration dismissed")
            },
            onEnrolled = { embedding, bitmap ->
                registrationBusy = true
                Log.d(TAG, "📸 Face registration complete - sending to API")

                scope.launch {
                    try {
                        val api = NetworkModule.apiService

                        // Convert bitmap to JPEG
                        val imageBytes = withContext(Dispatchers.Default) {
                            val outputStream = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                            outputStream.toByteArray()
                        }

                        // Convert embedding to JSON
                        val embeddingJson = com.google.gson.Gson().toJson(embedding.toList())

                        // Create multipart request
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
                            "Face registration from mobile app"
                        )

                        // Send to API
                        val response = withContext(Dispatchers.IO) {
                            api.registerFaceRecognition(
                                face_image = imagePart,
                                faceVector = faceDataPart,
                                priority = priorityPart,
                                reasonForChange = reasonPart
                            )
                        }

                        registrationBusy = false
                        showRegistration = false

                        if (response.isSuccessful) {
                            Log.d(TAG, "✅ Face registration successful!")
                            snack.showSnackbar("Face registered successfully!")

                            // Save locally after success
                            withContext(Dispatchers.IO) {
                                FaceStore.getInstance(context).saveEmbedding(embedding)
                            }

                            // Refresh status to get updated face_vector
                            vm.refreshStatus()

                            bitmap.recycle()
                        } else {
                            val message = try {
                                response.errorBody()?.string() ?: response.message()
                            } catch (e: Exception) {
                                "Failed to register face"
                            }
                            Log.e(TAG, "❌ Registration failed: $message")
                            snack.showSnackbar(message)
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Exception in face registration", e)
                        registrationBusy = false
                        showRegistration = false
                        bitmap.recycle()
                        snack.showSnackbar("Network error. Please try again.")
                    }
                }
            }
        )
    }
}