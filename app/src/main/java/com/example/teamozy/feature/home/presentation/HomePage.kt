@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.teamozy.feature.home.presentation

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.teamozy.core.network.NetworkModule
import com.example.teamozy.core.utils.PreferencesManager
import com.example.teamozy.feature.attendance.data.AttendanceRepository
import com.example.teamozy.feature.attendance.presentation.AttendanceViewModel
import com.example.teamozy.feature.face.data.EmbeddingExtractor
import com.example.teamozy.feature.face.presentation.FaceCaptureScreen
import com.example.teamozy.feature.face.presentation.FaceRegistrationScreen
import com.example.teamozy.feature.profile.presentation.ProfileScreen
import com.example.teamozy.feature.profile.presentation.EditSocialMediaScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.teamozy.feature.face.util.FaceVectorUtil

private const val TAG = "HomePage"
private const val PREF_FACE_EMBEDDING = "face_embedding"

private enum class HomeScreen { HOME, ATTENDANCE, PROFILE, EDIT_SOCIAL_MEDIA }

/**
 * Simple face storage using SharedPreferences
 */
private object SimpleFaceStore {
    private const val PREFS_NAME = "face_data"

    fun saveEmbedding(context: Context, embedding: FloatArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(embedding.toList())
        prefs.edit().putString(PREF_FACE_EMBEDDING, json).apply()
        Log.d(TAG, "✅ Face embedding saved: size=${embedding.size}")
    }

    fun loadEmbedding(context: Context): FloatArray? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_FACE_EMBEDDING, null) ?: return null

        return try {
            val type = object : TypeToken<List<Float>>() {}.type
            val list = Gson().fromJson<List<Float>>(json, type)
            list.toFloatArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading embedding", e)
            null
        }
    }

    fun hasEnrollment(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(PREF_FACE_EMBEDDING)
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d(TAG, "✅ Face data cleared")
    }
}

/**
 * Calculate elapsed seconds from last check-in time to now
 * @param lastCheckInTime Format: "2025-10-26 17:45:00"
 * @return Elapsed seconds, or 0 if parsing fails
 */
private fun calculateElapsedSeconds(lastCheckInTime: String?): Int {
    if (lastCheckInTime.isNullOrBlank()) return 0

    try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val checkInDate = formatter.parse(lastCheckInTime) ?: return 0
        val now = java.util.Date()
        val elapsedMillis = now.time - checkInDate.time
        val elapsedSeconds = (elapsedMillis / 1000).toInt()

        // Return elapsed seconds, minimum 0
        return if (elapsedSeconds >= 0) elapsedSeconds else 0
    } catch (e: Exception) {
        android.util.Log.e(TAG, "Error parsing last_check_in_time: $lastCheckInTime", e)
        return 0
    }
}

/**
 * Calculate cosine similarity between two embeddings
 * Returns a value between 0 and 1, where 1 means identical faces
 */
private fun calculateCosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
    if (embedding1.size != embedding2.size) {
        throw IllegalArgumentException("Embeddings must have same size")
    }

    var dotProduct = 0.0
    var norm1 = 0.0
    var norm2 = 0.0

    for (i in embedding1.indices) {
        dotProduct += embedding1[i] * embedding2[i]
        norm1 += embedding1[i] * embedding1[i]
        norm2 += embedding2[i] * embedding2[i]
    }

    val similarity = dotProduct / (kotlin.math.sqrt(norm1) * kotlin.math.sqrt(norm2))
    return similarity.toFloat()
}

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
    var showRegistration by remember { mutableStateOf(false) }
    var registrationBusy by remember { mutableStateOf(false) }

    // ✨ Face verification state with generation counter
    var faceVerifyBusy by remember { mutableStateOf(false) }
    var faceVerifyError by remember { mutableStateOf<String?>(null) }
    var faceVerificationGeneration by remember { mutableIntStateOf(0) }

    // ✨ Timer state for tracking work hours
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // ✅ Permission checkers for Check In and Check Out
    val checkInPermissionChecker = rememberPermissionChecker(
        context = context,
        onPermissionsGranted = {
            Log.d(TAG, "✅ Permissions granted for Check In")
            vm.startCheckIn(context)
        }
    )

    val checkOutPermissionChecker = rememberPermissionChecker(
        context = context,
        onPermissionsGranted = {
            Log.d(TAG, "✅ Permissions granted for Check Out")
            vm.startCheckOut(context)
        }
    )

    // ✨ Start/Continue timer based on check-in state
    LaunchedEffect(ui.currentState) {
        if (ui.currentState == "CHECK_OUT_NEEDED") {
            if (!isTimerRunning) {
                // Try to get last check-in time if field exists
                val lastCheckInTime = try {
                    // Use reflection to check if field exists
                    ui::class.java.getDeclaredField("lastCheckInTime").let { field ->
                        field.isAccessible = true
                        field.get(ui) as? String
                    }
                } catch (e: Exception) {
                    null
                }

                // Calculate elapsed time from last check-in if available
                val elapsed = calculateElapsedSeconds(lastCheckInTime)
                elapsedSeconds = elapsed
                isTimerRunning = true

                if (lastCheckInTime != null) {
                    Log.d(TAG, "✅ Continuing timer from last_check_in_time: $lastCheckInTime")
                    Log.d(TAG, "⏱️ Elapsed time: ${elapsed}s (${elapsed/3600}h ${(elapsed%3600)/60}m)")
                } else {
                    Log.d(TAG, "✅ Timer started (lastCheckInTime not available yet)")
                }
            }
        } else {
            // Stop timer when not in CHECK_OUT_NEEDED state
            if (isTimerRunning) {
                isTimerRunning = false
                elapsedSeconds = 0
                Log.d(TAG, "⏹️ Timer stopped")
            }
        }
    }

    // ✨ Timer tick effect - runs every second when active
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000)
            elapsedSeconds++
        }
    }

    LaunchedEffect(Unit) {
        vm.refreshStatus()
        if (SimpleFaceStore.hasEnrollment(context)) {
            val embedding = SimpleFaceStore.loadEmbedding(context)
            Log.d(TAG, "Stored embedding loaded: size=${embedding?.size}")
        } else {
            Log.d(TAG, "No face enrollment found")
        }
    }

    LaunchedEffect(ui.errorMessage) {
        ui.errorMessage?.let { error ->
            snack.showSnackbar(error)
        }
    }

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
                    HomeTopBar(
                        context = context,
                        companyName = prefs.companyName,
                        userName = prefs.userName,
                        profileUrl = prefs.profileUrl,
                        isRefreshing = ui.isRefreshing,
                        onRefreshClick = { vm.refreshStatus() },
                        onProfileClick = { currentScreen = HomeScreen.PROFILE }
                    )

                    Spacer(Modifier.height(24.dp))

                    if (ui.checkInMessage != null || ui.checkOutMessage != null) {
                        val message = ui.checkInMessage ?: ui.checkOutMessage
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3CD)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFF856404)
                                )
                                Text(
                                    text = message ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF856404)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(130.dp)
                            ) {
                                // ✨ Calculate progress based on 8-hour workday (28800 seconds)
                                val workdaySeconds = 8 * 60 * 60 // 8 hours
                                val progress = if (isTimerRunning) {
                                    (elapsedSeconds.toFloat() / workdaySeconds).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }

                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(130.dp),
                                    color = if (isTimerRunning) Color(0xFF00C896) else Color(0xFF4DD0B8),
                                    strokeWidth = 10.dp,
                                    trackColor = Color(0xFFE0F2F1),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // ✨ Display actual elapsed time
                                    val hours = elapsedSeconds / 3600
                                    val minutes = (elapsedSeconds % 3600) / 60
                                    val seconds = elapsedSeconds % 60

                                    Text(
                                        text = "%02d:%02d:%02d".format(hours, minutes, seconds),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTimerRunning) {
                                            Color(0xFF00C896)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }

                            when (ui.currentState) {
                                "CHECK_IN_NEEDED" -> {
                                    Button(
                                        onClick = {
                                            Log.d(TAG, "CHECK IN BUTTON CLICKED")
                                            faceVerificationGeneration++
                                            Log.d(TAG, "faceVerificationGeneration: $faceVerificationGeneration")
                                            checkInPermissionChecker()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp),
                                        enabled = !ui.isLoading && !faceVerifyBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00C896)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        if (ui.isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                "Check In",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                "CHECK_OUT_NEEDED" -> {
                                    Button(
                                        onClick = {
                                            Log.d(TAG, "CHECK OUT BUTTON CLICKED")
                                            faceVerificationGeneration++
                                            Log.d(TAG, "faceVerificationGeneration: $faceVerificationGeneration")
                                            checkOutPermissionChecker()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp),
                                        enabled = !ui.isLoading && !faceVerifyBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFF6B6B)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        if (ui.isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                "Check Out",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                "COMPLETED" -> {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = Color(0xFF00C896)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "Completed",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00C896)
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
                    Log.d(TAG, "Profile -> Face Registration")
                    currentScreen = HomeScreen.HOME
                    showRegistration = true
                },
                onNavigateToEditSocialMedia = {
                    Log.d(TAG, "Navigate to Edit Social Media")
                    currentScreen = HomeScreen.EDIT_SOCIAL_MEDIA
                },
                onLogout = {
                    Log.d(TAG, "Logout - clearing all data")
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                SimpleFaceStore.clear(context)
                            }
                            prefs.clearAll()
                            onLogout()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during logout", e)
                            onLogout()
                        }
                    }
                },
                onBack = {
                    currentScreen = HomeScreen.HOME
                }
            )

            HomeScreen.EDIT_SOCIAL_MEDIA -> EditSocialMediaScreen(
                onBack = {
                    Log.d(TAG, "Edit Social Media -> Back to Profile")
                    currentScreen = HomeScreen.PROFILE
                }
            )
        }
    }

    // ✨ Reset busy state when screens are dismissed
    LaunchedEffect(ui.showFaceVerification, ui.showReasonDialog) {
        // When face verification screen is dismissed
        if (!ui.showFaceVerification && !ui.showReasonDialog && faceVerifyBusy) {
            Log.d(TAG, "✅ Screens dismissed, resetting faceVerifyBusy = false")
            faceVerifyBusy = false
            faceVerifyError = null
        }
    }

    // ✨ Face Verification Screen
    if (ui.showFaceVerification) {
        key(faceVerificationGeneration) {
            FaceCaptureScreen(
                generation = faceVerificationGeneration,
                onDismiss = {
                    Log.d(TAG, "🔙 Face capture dismissed (generation=$faceVerificationGeneration)")
                    // ✨ Reset state IMMEDIATELY - no delay
                    vm.onFaceVerificationCancelled()
                    faceVerifyBusy = false
                    faceVerifyError = null
                    Log.d(TAG, "✅ Button re-enabled: faceVerifyBusy = false")
                },
                onCaptured = { /* unused */ },
                onBitmapCaptured = { bitmap ->
                    if (faceVerifyBusy) {
                        bitmap.recycle()
                        return@FaceCaptureScreen
                    }

                    faceVerifyBusy = true
                    faceVerifyError = null

                    scope.launch {
                        try {
                            // Extract live face embedding
                            val extractor = EmbeddingExtractor.getInstance(context)
                            val liveEmbedding = withContext(Dispatchers.Default) {
                                extractor.extractOrNull(bitmap)
                            }
                            bitmap.recycle()

                            if (liveEmbedding == null) {
                                Log.e(TAG, "No face detected")
                                faceVerifyError = "No face detected. Please try again."
                                faceVerifyBusy = false
                                return@launch
                            }

                            // ⭐ Get face_vector from API (not local storage!)
                            val storedFaceVector = ui.checkInFaceVector ?: ui.checkOutFaceVector

                            if (storedFaceVector == null) {
                                Log.e(TAG, "❌ No stored face vector from API")
                                faceVerifyError = "Face verification data not available. Please contact support."
                                faceVerifyBusy = false
                                return@launch
                            }

                            // Validate stored face vector
                            if (!FaceVectorUtil.isValidFaceVector(storedFaceVector)) {
                                Log.e(TAG, "❌ Invalid stored face vector from API")
                                faceVerifyError = "Invalid face data. Please contact support."
                                faceVerifyBusy = false
                                return@launch
                            }

                            Log.d(TAG, "✅ Stored face vector loaded from API: ${storedFaceVector.size} dimensions")

                            // ⭐ Calculate similarity using FaceVectorUtil
                            val similarity = FaceVectorUtil.calculateSimilarity(liveEmbedding, storedFaceVector)



                            Log.d(TAG, "📊 Face similarity score: $similarity")

                            // ✨ Get minimum threshold from server
                            val minimumThreshold = ui.checkInMinimumQualityScore ?: ui.checkOutMinimumQualityScore ?: 0.55f

                            // ✨ Verify if similarity meets threshold
                            val isVerified = similarity >= minimumThreshold

                            if (isVerified) {
                                Log.d(TAG, "✅ Face VERIFIED! Similarity: $similarity >= $minimumThreshold")
                                vm.onFaceVerificationComplete(
                                    qualityScore = similarity,
                                    verified = true
                                )
                            } else {
                                Log.e(TAG, "❌ Face NOT verified! Similarity: $similarity < $minimumThreshold")
                                faceVerifyError = "Face does not match (${String.format("%.2f", similarity * 100)}% match). Please try again."
                                faceVerifyBusy = false
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Face capture error: ${e.message}", e)
                            faceVerifyError = "Face capture failed: ${e.message}"
                            faceVerifyBusy = false
                            bitmap.recycle()
                        }
                    }
                },
                showReasonField = false,
                reasonMessage = null,
                isSubmitting = faceVerifyBusy,
                onSubmit = { },
                serverError = faceVerifyError
            )
        }
    }

    if (ui.showReasonDialog) {
        val isCheckIn = ui.checkInTToken != null
        val isLateOrEarly = if (isCheckIn) ui.checkInIsLate else ui.checkOutIsEarly
        val isOutOfRange = if (isCheckIn) ui.checkInIsOutOfRange else ui.checkOutIsOutOfRange
        val lateOrEarlyReasonRequired = if (isCheckIn) ui.checkInLateReasonRequired else ui.checkOutEarlyReasonRequired
        val outOfRangeReasonRequired = if (isCheckIn) ui.checkInOutOfRangeReasonRequired else ui.checkOutOutOfRangeReasonRequired

        ReasonDialog(
            isCheckIn = isCheckIn,
            isLateOrEarly = isLateOrEarly,
            isOutOfRange = isOutOfRange,
            lateOrEarlyReasonRequired = lateOrEarlyReasonRequired,
            outOfRangeReasonRequired = outOfRangeReasonRequired,
            onDismiss = { vm.onReasonDialogDismissed() },
            onSubmit = { lateOrEarlyReason, outOfRangeReason ->
                if (isCheckIn) {
                    vm.completeCheckIn(lateOrEarlyReason, outOfRangeReason)
                } else {
                    vm.completeCheckOut(lateOrEarlyReason, outOfRangeReason)
                }
            }
        )
    }

    if (showRegistration) {
        FaceRegistrationScreen(
            onDismiss = { showRegistration = false },
            onEnrolled = { embedding: FloatArray, bitmap: Bitmap ->
                registrationBusy = true
                scope.launch {
                    try {
                        val api = NetworkModule.apiService
                        val imageBytes = withContext(Dispatchers.Default) {
                            val outputStream = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                            outputStream.toByteArray()
                        }
                        val embeddingJson = com.google.gson.Gson().toJson(embedding.toList())

                        val imagePart = okhttp3.MultipartBody.Part.createFormData(
                            "face_image",
                            "face_${System.currentTimeMillis()}.jpg",
                            okhttp3.RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageBytes)
                        )
                        val faceDataPart = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), embeddingJson)
                        val priorityPart = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), "normal")
                        val reasonPart = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), "Face registration from mobile app")

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
                            snack.showSnackbar("Face registered successfully!")
                            withContext(Dispatchers.IO) {
                                SimpleFaceStore.saveEmbedding(context, embedding)
                            }
                            vm.refreshStatus()
                            bitmap.recycle()
                        } else {
                            val message = try {
                                response.errorBody()?.string() ?: response.message()
                            } catch (e: Exception) {
                                "Failed to register face"
                            }
                            snack.showSnackbar(message)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Registration error", e)
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

@Composable
fun ReasonDialog(
    isCheckIn: Boolean,
    isLateOrEarly: Boolean,
    isOutOfRange: Boolean,
    lateOrEarlyReasonRequired: Boolean,
    outOfRangeReasonRequired: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (lateOrEarlyReason: String?, outOfRangeReason: String?) -> Unit
) {
    var lateOrEarlyReason by remember { mutableStateOf("") }
    var outOfRangeReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attendance Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isLateOrEarly) {
                    Text(
                        text = if (isCheckIn) "You are checking in late" else "You are checking out early",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFF9800)
                    )
                }
                if (isOutOfRange) {
                    Text(
                        text = "You are outside the designated area",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFF9800)
                    )
                }
                if (lateOrEarlyReasonRequired) {
                    OutlinedTextField(
                        value = lateOrEarlyReason,
                        onValueChange = { lateOrEarlyReason = it },
                        label = { Text(if (isCheckIn) "Late Reason *" else "Early Check-Out Reason *") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
                if (outOfRangeReasonRequired) {
                    OutlinedTextField(
                        value = outOfRangeReason,
                        onValueChange = { outOfRangeReason = it },
                        label = { Text("Out of Range Reason *") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
                if (!lateOrEarlyReasonRequired && !outOfRangeReasonRequired) {
                    Text(
                        text = if (isCheckIn) "Tap Continue to complete check-in" else "Tap Continue to complete check-out",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lateOrEarly = if (lateOrEarlyReasonRequired && lateOrEarlyReason.isNotBlank()) lateOrEarlyReason else null
                    val outRange = if (outOfRangeReasonRequired && outOfRangeReason.isNotBlank()) outOfRangeReason else null
                    if (lateOrEarlyReasonRequired && lateOrEarlyReason.isBlank()) return@Button
                    if (outOfRangeReasonRequired && outOfRangeReason.isBlank()) return@Button
                    onSubmit(lateOrEarly, outRange)
                },
                enabled = (!lateOrEarlyReasonRequired || lateOrEarlyReason.isNotBlank()) &&
                        (!outOfRangeReasonRequired || outOfRangeReason.isNotBlank())
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}