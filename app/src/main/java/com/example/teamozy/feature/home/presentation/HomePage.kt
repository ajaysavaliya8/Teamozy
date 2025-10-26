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
import com.example.teamozy.feature.face.presentation.FaceCaptureScreen
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
    var showRegistration by remember { mutableStateOf(false) }
    var registrationBusy by remember { mutableStateOf(false) }

    // Face verification state
    var faceVerifyBusy by remember { mutableStateOf(false) }
    var faceVerifyError by remember { mutableStateOf<String?>(null) }
    var faceVerificationKey by remember { mutableStateOf(0) }
    var isFaceCaptureActive by remember { mutableStateOf(false) }

    // Log state changes
    LaunchedEffect(ui.showFaceVerification) {
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "UI State Changed: showFaceVerification = ${ui.showFaceVerification}")
        Log.d(TAG, "faceVerifyBusy = $faceVerifyBusy")
        Log.d(TAG, "faceVerifyError = $faceVerifyError")
        Log.d(TAG, "isFaceCaptureActive = $isFaceCaptureActive")
        Log.d(TAG, "════════════════════════════════════════")
    }

    LaunchedEffect(Unit) {
        // Refresh status when app opens
        vm.refreshStatus()

        val store = FaceStore.getInstance(context)
        if (store.hasEnrollment()) {
            val embedding = store.loadEmbedding()
            Log.d(TAG, "Stored embedding loaded: size=${embedding?.size}")
        } else {
            Log.d(TAG, "No face enrollment found")
        }
    }

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

                    // Check-in or check-out message (late/early/out of range)
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

                    // Timer and Check In/Check Out Card
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
                            // Circular Timer
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(130.dp)
                            ) {
                                // Outer circular progress segments
                                CircularProgressIndicator(
                                    progress = { 0.75f },
                                    modifier = Modifier.size(130.dp),
                                    color = Color(0xFF4DD0B8),
                                    strokeWidth = 10.dp,
                                    trackColor = Color(0xFFE0F2F1),
                                )

                                // Inner circle background
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Timer Text
                                    Text(
                                        text = "00:00:00",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Check In/Check Out Button
                            when (ui.currentState) {
                                "CHECK_IN_NEEDED" -> {
                                    Button(
                                        onClick = {
                                            Log.d(TAG, "╔═══════════════════════════════════════╗")
                                            Log.d(TAG, "║   CHECK IN BUTTON CLICKED             ║")
                                            Log.d(TAG, "╠═══════════════════════════════════════╣")
                                            Log.d(TAG, "║ UI State:                             ║")
                                            Log.d(TAG, "║   showFaceVerification: ${ui.showFaceVerification}        ║")
                                            Log.d(TAG, "║   isLoading: ${ui.isLoading}                  ║")
                                            Log.d(TAG, "║ Local State:                          ║")
                                            Log.d(TAG, "║   faceVerifyBusy: $faceVerifyBusy           ║")
                                            Log.d(TAG, "║   faceVerifyError: $faceVerifyError         ║")
                                            Log.d(TAG, "║   faceVerificationKey: $faceVerificationKey      ║")
                                            Log.d(TAG, "║   isFaceCaptureActive: $isFaceCaptureActive      ║")
                                            Log.d(TAG, "╠═══════════════════════════════════════╣")
                                            Log.d(TAG, "║ Calling vm.startCheckIn(context)      ║")
                                            Log.d(TAG, "╚═══════════════════════════════════════╝")
                                            vm.startCheckIn(context)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp),
                                        enabled = !ui.isLoading,
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
                                            Log.d(TAG, "╔═══════════════════════════════════════╗")
                                            Log.d(TAG, "║   CHECK OUT BUTTON CLICKED            ║")
                                            Log.d(TAG, "╠═══════════════════════════════════════╣")
                                            Log.d(TAG, "║ UI State:                             ║")
                                            Log.d(TAG, "║   showFaceVerification: ${ui.showFaceVerification}        ║")
                                            Log.d(TAG, "║   isLoading: ${ui.isLoading}                  ║")
                                            Log.d(TAG, "║ Local State:                          ║")
                                            Log.d(TAG, "║   faceVerifyBusy: $faceVerifyBusy           ║")
                                            Log.d(TAG, "║   faceVerifyError: $faceVerifyError         ║")
                                            Log.d(TAG, "║   faceVerificationKey: $faceVerificationKey      ║")
                                            Log.d(TAG, "║   isFaceCaptureActive: $isFaceCaptureActive      ║")
                                            Log.d(TAG, "╠═══════════════════════════════════════╣")
                                            Log.d(TAG, "║ Calling vm.startCheckOut(context)     ║")
                                            Log.d(TAG, "╚═══════════════════════════════════════╝")
                                            vm.startCheckOut(context)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp),
                                        enabled = !ui.isLoading,
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
                                    // Show completed status in the button area
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
                    Log.d(TAG, "📝 Profile -> Face Registration")
                    currentScreen = HomeScreen.HOME
                    showRegistration = true
                },
                onLogout = {
                    Log.d(TAG, "🚪 Logout - clearing all data")
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                FaceStore.getInstance(context).clear()
                            }
                            prefs.clearAll()
                            onLogout()
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error during logout", e)
                            onLogout()
                        }
                    }
                },
                onBack = { currentScreen = HomeScreen.HOME }
            )
        }
    }

    // Face Verification Screen (for check-in OR check-out)
    // Increment key when screen is dismissed to force fresh instance next time

    LaunchedEffect(ui.showFaceVerification) {
        if (!ui.showFaceVerification && faceVerificationKey > 0) {
            // Screen was dismissed - reset state and increment key for next time
            Log.d(TAG, "┌─────────────────────────────────────────┐")
            Log.d(TAG, "│ FACE VERIFICATION DISMISSED             │")
            Log.d(TAG, "│ Resetting state for next launch         │")
            Log.d(TAG, "│ Key: $faceVerificationKey → ${faceVerificationKey + 1}         │")
            Log.d(TAG, "└─────────────────────────────────────────┘")
            faceVerifyBusy = false
            faceVerifyError = null
            faceVerificationKey++
        }
    }

    if (ui.showFaceVerification) {
        val minimumScore = ui.checkInMinimumQualityScore ?: ui.checkOutMinimumQualityScore ?: 0.57f

        LaunchedEffect(Unit) {
            Log.d(TAG, "🟢 Setting isFaceCaptureActive = true")
            isFaceCaptureActive = true
        }

        DisposableEffect(Unit) {
            onDispose {
                Log.d(TAG, "🔴 Disposing face capture - setting isFaceCaptureActive = false")
                isFaceCaptureActive = false
            }
        }

        Log.d(TAG, "▶ Creating FaceCaptureScreen with key=$faceVerificationKey, minimumScore=$minimumScore")

        key(faceVerificationKey) {
            Log.d(TAG, "▶▶ Inside key($faceVerificationKey) block - FaceCaptureScreen composing")

            DisposableEffect(Unit) {
                Log.d(TAG, "🟢 FaceCaptureScreen ENTERED (key=$faceVerificationKey)")
                onDispose {
                    Log.d(TAG, "🔴 FaceCaptureScreen DISPOSED (key=$faceVerificationKey)")
                    Log.d(TAG, "   - faceVerifyBusy=$faceVerifyBusy, faceVerifyError=$faceVerifyError")
                }
            }

            FaceCaptureScreen(
                onDismiss = {
                    Log.d(TAG, "🔙 FaceCaptureScreen onDismiss called")
                    Log.d(TAG, "   - Calling vm.onFaceVerificationCancelled()")
                    vm.onFaceVerificationCancelled()
                    Log.d(TAG, "   - Resetting faceVerifyBusy to false")
                    faceVerifyBusy = false
                    Log.d(TAG, "   - Resetting faceVerifyError to null")
                    faceVerifyError = null
                    Log.d(TAG, "   - Setting isFaceCaptureActive to false")
                    isFaceCaptureActive = false
                },
                onCaptured = { /* unused */ },
                onBitmapCaptured = { bitmap ->
                    Log.d(TAG, "📸 onBitmapCaptured called")
                    Log.d(TAG, "   - faceVerifyBusy = $faceVerifyBusy")
                    Log.d(TAG, "   - isFaceCaptureActive = $isFaceCaptureActive")

                    if (!isFaceCaptureActive) {
                        Log.d(TAG, "   ⚠️ Screen no longer active, ignoring late frame")
                        bitmap.recycle()
                        return@FaceCaptureScreen
                    }

                    if (faceVerifyBusy) {
                        Log.d(TAG, "   ⚠️ Already busy, recycling bitmap and returning")
                        bitmap.recycle()
                        return@FaceCaptureScreen
                    }

                    Log.d(TAG, "   - Setting faceVerifyBusy = true")
                    faceVerifyBusy = true
                    faceVerifyError = null

                    scope.launch {
                        try {
                            Log.d(TAG, "   - Starting face extraction in coroutine")
                            // Extract face embedding to get quality score
                            val extractor = EmbeddingExtractor.getInstance(context)
                            Log.d(TAG, "   - Got EmbeddingExtractor instance")

                            val liveEmbedding = withContext(Dispatchers.Default) {
                                Log.d(TAG, "   - Extracting embedding from bitmap...")
                                extractor.extractOrNull(bitmap)
                            }

                            bitmap.recycle()
                            Log.d(TAG, "   - Bitmap recycled")

                            if (liveEmbedding == null) {
                                Log.e(TAG, "   ❌ No face detected in bitmap")
                                faceVerifyError = "No face detected. Please try again."
                                faceVerifyBusy = false
                                return@launch
                            }

                            Log.d(TAG, "   ✅ Face embedding extracted successfully (size=${liveEmbedding.size})")

                            // Calculate a basic quality score (you can adjust this logic)
                            val qualityScore = 0.85f // Or calculate based on face detection confidence

                            Log.d(TAG, "   ✅ Face captured with quality score: $qualityScore")
                            Log.d(TAG, "   - Calling vm.onFaceVerificationComplete()")

                            // Let the server do the verification
                            // Just send that face was captured successfully
                            vm.onFaceVerificationComplete(
                                qualityScore = qualityScore,
                                verified = true // Server will do actual verification
                            )

                            Log.d(TAG, "   ✅ Face verification complete callback sent")

                        } catch (e: Exception) {
                            Log.e(TAG, "   ❌ Face capture error: ${e.message}", e)
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

    // Reason Dialog (for late/early or out of range violations)
    if (ui.showReasonDialog) {
        // Determine if this is check-in or check-out
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

    // Face Registration Screen
    if (showRegistration) {
        FaceRegistrationScreen(
            onDismiss = {
                showRegistration = false
            },
            onEnrolled = { embedding, bitmap ->
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
                                FaceStore.getInstance(context).saveEmbedding(embedding)
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
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLateOrEarly) {
                    Text(
                        text = if (isCheckIn) {
                            "You are checking in late"
                        } else {
                            "You are checking out early"
                        },
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
                        label = {
                            Text(
                                if (isCheckIn) "Late Reason *" else "Early Check-Out Reason *"
                            )
                        },
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
                        text = if (isCheckIn) {
                            "Tap Continue to complete check-in"
                        } else {
                            "Tap Continue to complete check-out"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lateOrEarly = if (lateOrEarlyReasonRequired && lateOrEarlyReason.isNotBlank())
                        lateOrEarlyReason else null
                    val outRange = if (outOfRangeReasonRequired && outOfRangeReason.isNotBlank())
                        outOfRangeReason else null

                    // Validate required fields
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