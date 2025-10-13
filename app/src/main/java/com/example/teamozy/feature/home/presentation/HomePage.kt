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
import com.example.teamozy.feature.profile.presentation.ProfileScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject

private const val TAG = "HomePage"
private enum class PunchAction { IN, OUT }
private enum class HomeScreen { HOME, ATTENDANCE, PROFILE }

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
        vm.refreshStatus()

        val store = FaceStore.getInstance(context)
        if (store.hasEnrollment()) {
            val embedding = store.loadEmbedding()
            Log.d(TAG, "Stored embedding loaded: size=${embedding?.size}, first5=${embedding?.take(5)}")
            Log.d(TAG, "Face accuracy threshold: $verifyThreshold (from server)")
        } else {
            Log.d(TAG, "No face enrollment found")
        }

        // Log face verification requirements
        Log.d(TAG, "🎭 require_face_checkin: ${prefs.requireFaceCheckin}")
        Log.d(TAG, "🎭 require_face_break: ${prefs.requireFaceBreak}")
    }

    var pendingAction by remember { mutableStateOf<PunchAction?>(null) }
    var showRegistration by remember { mutableStateOf(false) }
    var showVerify by remember { mutableStateOf(false) }
    var verifyBusy by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }
    var registrationBusy by remember { mutableStateOf(false) }
    var checkingFaceData by remember { mutableStateOf(false) }

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

    // Function to check face data from server
    suspend fun checkFaceDataFromServer(): Boolean? {
        Log.d(TAG, "🔍 checkFaceDataFromServer() - Starting...")
        return try {
            val api = NetworkModule.apiService
            Log.d(TAG, "🌐 Calling GET /employees/face-recognition...")

            val response = withContext(Dispatchers.IO) {
                api.getFaceRecognitionData()
            }

            Log.d(TAG, "📥 GET response received: code=${response.code()}, isSuccessful=${response.isSuccessful}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val body = response.body()
                    Log.d(TAG, "📦 Response body: status=${body?.status}, message=${body?.message}")
                    Log.d(TAG, "📦 face_vector present: ${body?.face_vector != null}, length: ${body?.face_vector?.length ?: 0}")
                    Log.d(TAG, "📦 threshold: ${body?.minimum_face_recognition_quality_score}")
                    Log.d(TAG, "📦 require_face_checkin: ${body?.require_face_checkin}")
                    Log.d(TAG, "📦 require_face_break: ${body?.require_face_break}")

                    // Update threshold if provided
                    body?.minimum_face_recognition_quality_score?.let { threshold ->
                        prefs.faceThreshold = threshold
                        Log.d(TAG, "✅ Updated face threshold from server: $threshold")
                    }

                    // Update face verification requirements if provided
                    body?.require_face_checkin?.let { requireCheckin ->
                        prefs.requireFaceCheckin = requireCheckin
                        Log.d(TAG, "✅ Updated require_face_checkin from server: $requireCheckin")
                    }

                    body?.require_face_break?.let { requireBreak ->
                        prefs.requireFaceBreak = requireBreak
                        Log.d(TAG, "✅ Updated require_face_break from server: $requireBreak")
                    }

                    // Check if face_vector exists
                    val faceVector = body?.face_vector
                    if (!faceVector.isNullOrBlank()) {
                        Log.d(TAG, "✅ face_vector exists on server - attempting to parse and save...")
                        // Parse and save face vector locally
                        try {
                            Log.d(TAG, "📝 Parsing JSON array (length: ${faceVector.length})...")
                            val jsonArray = org.json.JSONArray(faceVector)
                            Log.d(TAG, "📝 JSON array parsed successfully, array length: ${jsonArray.length()}")

                            val embedding = FloatArray(jsonArray.length()) { i ->
                                jsonArray.getDouble(i).toFloat()
                            }
                            Log.d(TAG, "📝 FloatArray created: size=${embedding.size}, first 5 values: ${embedding.take(5)}")

                            // Save to local storage
                            Log.d(TAG, "💾 Saving embedding to local storage...")
                            withContext(Dispatchers.IO) {
                                FaceStore.getInstance(context).saveEmbedding(embedding)
                            }

                            Log.d(TAG, "✅ Face vector saved locally from server (size: ${embedding.size})")
                            snack.showSnackbar("Face data synced from server")

                            // Face data exists - proceed to verification
                            Log.d(TAG, "✅ Returning TRUE - will proceed to VERIFICATION")
                            true
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to parse face vector from server", e)
                            Log.e(TAG, "❌ Error type: ${e.javaClass.simpleName}, message: ${e.message}")
                            snack.showSnackbar("Failed to sync face data")
                            false
                        }
                    } else {
                        // Face data doesn't exist - need registration
                        Log.d(TAG, "⚠️ face_vector is null or blank on server")
                        Log.d(TAG, "✅ Returning FALSE - will proceed to REGISTRATION")
                        false
                    }
                }

                response.code() == 403 -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ 403 Forbidden - errorBody: $errorBody")
                    val message = extractErrorMessage(errorBody)
                        ?: "Face recognition is not enabled for your account. Contact HR/Admin."

                    Log.e(TAG, "❌ Face data access forbidden: $message")
                    snack.showSnackbar(message)
                    Log.d(TAG, "⚠️ Returning NULL - will stop flow")
                    null
                }

                response.code() == 404 -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ 404 Not Found - errorBody: $errorBody")
                    val message = extractErrorMessage(errorBody) ?: "Employee not found."

                    Log.e(TAG, "❌ Employee not found: $message")
                    snack.showSnackbar(message)
                    Log.d(TAG, "⚠️ Returning NULL - will stop flow")
                    null
                }

                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ Unexpected response code: ${response.code()}")
                    Log.e(TAG, "❌ errorBody: $errorBody")
                    val message = extractErrorMessage(errorBody)
                        ?: "Failed to check face data. Please try again."

                    Log.e(TAG, "❌ Get face data failed: code=${response.code()}, message=$message")
                    snack.showSnackbar(message)
                    Log.d(TAG, "⚠️ Returning NULL - will stop flow")
                    null
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in checkFaceDataFromServer", e)
            Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}, message: ${e.message}")
            e.printStackTrace()
            snack.showSnackbar("Network error. Please check your connection.")
            Log.d(TAG, "⚠️ Returning NULL - will stop flow")
            null
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

                                Log.d(TAG, "🔘 Check In/Out button clicked")
                                Log.d(TAG, "📍 Pending action: $pendingAction")

                                // Check if face verification is required
                                val requireFaceCheckin = prefs.requireFaceCheckin
                                Log.d(TAG, "🎭 require_face_checkin: $requireFaceCheckin")

                                if (!requireFaceCheckin) {
                                    // Face verification NOT required - proceed directly to check-in/check-out
                                    Log.d(TAG, "✅ Face verification NOT required - proceeding directly to punch")
                                    vm.setFaceVerifyEnabled(false)
                                    proceedPunch()
                                    return@Button
                                }

                                // Face verification IS required - check face data
                                Log.d(TAG, "🎭 Face verification REQUIRED - checking face data...")
                                checkingFaceData = true

                                scope.launch {
                                    try {
                                        val store = FaceStore.getInstance(context)

                                        // Check if face vector exists locally
                                        val hasLocalEnrollment = store.hasEnrollment()
                                        Log.d(TAG, "💾 Local enrollment exists: $hasLocalEnrollment")

                                        if (!hasLocalEnrollment) {
                                            Log.d(TAG, "⚠️ No local face data - checking server...")

                                            // Call GET API to check server
                                            val hasServerData = checkFaceDataFromServer()

                                            checkingFaceData = false
                                            Log.d(TAG, "🔄 Server check result: $hasServerData")

                                            when (hasServerData) {
                                                true -> {
                                                    // Server has face data and it's now saved locally
                                                    // Proceed to verification
                                                    Log.d(TAG, "✅ Face data synced from server - showing VERIFICATION screen")
                                                    showVerify = true
                                                }
                                                false -> {
                                                    // Server confirmed no face data exists
                                                    // Proceed to registration
                                                    Log.d(TAG, "📝 No face data on server - showing REGISTRATION screen")
                                                    showRegistration = true
                                                }
                                                null -> {
                                                    // Error occurred (already shown to user via snackbar)
                                                    Log.d(TAG, "❌ Error checking face data - STOPPING flow")
                                                    pendingAction = null
                                                }
                                            }
                                        } else {
                                            // Local face data exists - proceed to verification
                                            Log.d(TAG, "✅ Local face data exists - showing VERIFICATION screen")
                                            checkingFaceData = false
                                            showVerify = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Error in button click handler", e)
                                        Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}, message: ${e.message}")
                                        e.printStackTrace()
                                        checkingFaceData = false
                                        snack.showSnackbar("An error occurred. Please try again.")
                                        pendingAction = null
                                    }
                                }
                            },
                            enabled = !ui.isLoading && !ui.isRefreshing && !registrationBusy && !checkingFaceData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            if (checkingFaceData) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Checking face data...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (registrationBusy) {
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
                            text = if (checkingFaceData) "Checking face data..."
                            else if (registrationBusy) "Registering face with server..."
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

                        // Show face verification requirement status
                        val requireFace = prefs.requireFaceCheckin
                        Text(
                            text = "Face verification: ${if (requireFace) "Required" else "Not required"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (requireFace) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                    // When coming from Profile, ALWAYS show registration (not verification)
                    Log.d(TAG, "📝 Profile -> Face Registration - showing REGISTRATION screen")
                    currentScreen = HomeScreen.HOME
                    pendingAction = null
                    showRegistration = true
                },
                onLogout = {
                    // Clear all data including face data
                    Log.d(TAG, "🚪 Logout - clearing all data including face data")
                    scope.launch {
                        try {
                            // Clear face data
                            withContext(Dispatchers.IO) {
                                FaceStore.getInstance(context).clear()
                            }
                            Log.d(TAG, "✅ Face data cleared")

                            // Clear preferences (includes auth token, etc.)
                            prefs.clearAll()
                            Log.d(TAG, "✅ Preferences cleared")

                            // Call the logout callback
                            onLogout()
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error during logout cleanup", e)
                            // Still proceed with logout even if cleanup fails
                            onLogout()
                        }
                    }
                },
                onBack = { currentScreen = HomeScreen.HOME }
            )
        }
    }

    // Face Registration Screen - ONLY for registration (not verification)
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
                Log.d(TAG, "📸 Face registration complete - preparing to send to API")
                Log.d(TAG, "📊 Embedding size: ${embedding.size}, bitmap: ${bitmap.width}x${bitmap.height}")

                scope.launch {
                    try {
                        Log.d(TAG, "📤 Starting API upload process...")

                        val api = NetworkModule.apiService

                        // Convert bitmap to JPEG bytes
                        Log.d(TAG, "🖼️ Converting bitmap to JPEG...")
                        val imageBytes = withContext(Dispatchers.Default) {
                            val outputStream = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                            outputStream.toByteArray()
                        }
                        Log.d(TAG, "✅ JPEG conversion complete: ${imageBytes.size} bytes")

                        // Convert embedding to JSON string
                        Log.d(TAG, "📝 Converting embedding to JSON...")
                        val embeddingJson = com.google.gson.Gson().toJson(embedding.toList())
                        Log.d(TAG, "✅ JSON conversion complete: length=${embeddingJson.length}")

                        // Create multipart request parts
                        Log.d(TAG, "📦 Creating multipart request...")
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
                            "Face registration update"
                        )

                        Log.d(TAG, "🌐 Sending POST /employees/face-recognition...")
                        val response = withContext(Dispatchers.IO) {
                            api.registerFaceRecognition(
                                face_image = imagePart,
                                faceVector = faceDataPart,
                                priority = priorityPart,
                                reasonForChange = reasonPart
                            )
                        }

                        Log.d(TAG, "📥 POST response received: code=${response.code()}, isSuccessful=${response.isSuccessful}")

                        registrationBusy = false
                        showRegistration = false
                        pendingAction = null

                        // Clean up bitmap
                        bitmap.recycle()
                        Log.d(TAG, "🧹 Bitmap recycled")

                        when {
                            response.isSuccessful && response.code() == 200 -> {
                                Log.d(TAG, "✅ Face registration API SUCCESS")

                                // Success - now save to local storage
                                Log.d(TAG, "💾 Saving embedding to local storage...")
                                withContext(Dispatchers.IO) {
                                    FaceStore.getInstance(context).saveEmbedding(embedding)
                                }
                                Log.d(TAG, "✅ Embedding saved locally")

                                val message = response.body()?.message
                                    ?: "Face registered successfully! Your face recognition is now active."

                                Log.d(TAG, "✅ Registration complete: $message")

                                vm.setFaceVerifyEnabled(false)
                                snack.showSnackbar(message)
                            }

                            response.code() == 403 -> {
                                val errorBody = response.errorBody()?.string()
                                Log.e(TAG, "❌ 403 Forbidden - errorBody: $errorBody")
                                val message = extractErrorMessage(errorBody)
                                    ?: "Face registration not enabled for your account. Contact HR/Admin."

                                Log.e(TAG, "❌ Registration forbidden: $message")
                                snack.showSnackbar(message)
                            }

                            response.code() == 409 -> {
                                val errorBody = response.errorBody()?.string()
                                Log.w(TAG, "⚠️ 409 Conflict - errorBody: $errorBody")
                                val message = extractErrorMessage(errorBody)
                                    ?: "You have a pending face recognition request."

                                Log.w(TAG, "⚠️ Pending request: $message")
                                snack.showSnackbar(message)
                            }

                            response.code() == 404 -> {
                                val errorBody = response.errorBody()?.string()
                                Log.e(TAG, "❌ 404 Not Found - errorBody: $errorBody")
                                val message = extractErrorMessage(errorBody)
                                    ?: "Employee not found."

                                Log.e(TAG, "❌ Employee not found: $message")
                                snack.showSnackbar(message)
                            }

                            response.code() == 400 -> {
                                val errorBody = response.errorBody()?.string()
                                Log.e(TAG, "❌ 400 Bad Request - errorBody: $errorBody")
                                val message = extractErrorMessage(errorBody)
                                    ?: "Invalid face data. Please try again."

                                Log.e(TAG, "❌ Bad request: $message")
                                snack.showSnackbar(message)
                            }

                            else -> {
                                val errorBody = response.errorBody()?.string()
                                Log.e(TAG, "❌ Unexpected response code: ${response.code()}")
                                Log.e(TAG, "❌ errorBody: $errorBody")
                                val message = extractErrorMessage(errorBody)
                                    ?: "Failed to register face. Please try again."

                                Log.e(TAG, "❌ Registration failed: code=${response.code()}, message=$message")
                                snack.showSnackbar(message)
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Exception in face registration", e)
                        Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}, message: ${e.message}")
                        e.printStackTrace()
                        registrationBusy = false
                        showRegistration = false
                        pendingAction = null

                        // Clean up bitmap on error
                        try {
                            bitmap.recycle()
                            Log.d(TAG, "🧹 Bitmap recycled after error")
                        } catch (_: Exception) {}

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