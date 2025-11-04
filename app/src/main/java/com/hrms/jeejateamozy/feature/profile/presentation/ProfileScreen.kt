@file:OptIn(ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.profile.presentation

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.BuildConfig
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.face.presentation.FaceRegistrationScreen
import com.hrms.jeejateamozy.feature.profile.data.ProfileRepository
import com.hrms.jeejateamozy.feature.profile.data.ProfilePictureOutcome
import com.hrms.jeejateamozy.feature.profile.presentation.components.*
import com.hrms.jeejateamozy.feature.profile.presentation.utils.calculateProfileCompletion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull

private const val TAG = "ProfileScreen"

/**
 * ProfileScreen - Main screen logic only
 *
 * This file only handles:
 * - State management
 * - Face Registration logic
 * - Profile picture update
 * - Navigation and callbacks
 *
 * All UI components are in separate files!
 */
@Composable
fun ProfileScreen(
    onNavigateToFaceChange: () -> Unit,
    onNavigateToEditSocialMedia: () -> Unit,
    onNavigateToEditContactDetail: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val profileRepository = remember { ProfileRepository(context) }
    val scope = rememberCoroutineScope()

    // Dialog States
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showProfilePictureOptions by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }

    // Loading & Message States
    var isUpdatingPicture by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Face Registration States
    var showFaceRegistration by remember { mutableStateOf(false) }
    var registrationBusy by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUpdatingPicture = true
                errorMessage = null
                successMessage = null

                when (val result = profileRepository.updateProfilePicture(it)) {
                    is ProfilePictureOutcome.Success -> {
                        successMessage = result.message
                        prefs.profileUrl = result.profileUrl
                    }
                    is ProfilePictureOutcome.Error -> {
                        errorMessage = result.message
                    }
                }

                isUpdatingPicture = false
            }
        }
    }

    // Calculate profile completion
    val profileCompletion = remember(prefs) {
        calculateProfileCompletion(prefs)
    }

    // ✅ Face Registration Screen (Full Screen Overlay)
    if (showFaceRegistration) {
        FaceRegistrationScreen(
            onDismiss = {
                showFaceRegistration = false
                registrationBusy = false
            },
            onEnrolled = { embedding: FloatArray, bitmap: Bitmap ->
                registrationBusy = true
                scope.launch {
                    try {
                        val api = NetworkModule.apiService

                        // Convert bitmap to bytes
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

                        // Call API
                        val response = withContext(Dispatchers.IO) {
                            api.registerFaceRecognition(
                                face_image = imagePart,
                                faceVector = faceDataPart,
                                priority = priorityPart,
                                reasonForChange = reasonPart
                            )
                        }

                        registrationBusy = false
                        showFaceRegistration = false

                        if (response.isSuccessful) {
                            successMessage = "Face registered successfully!"
                            bitmap.recycle()
                        } else {
                            val message = try {
                                response.errorBody()?.string() ?: response.message()
                            } catch (e: Exception) {
                                "Failed to register face"
                            }
                            errorMessage = message
                            bitmap.recycle()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Registration error", e)
                        registrationBusy = false
                        showFaceRegistration = false
                        bitmap.recycle()
                        errorMessage = "Network error. Please try again."
                    }
                }
            }
        )
        return  // Exit early to show only FaceRegistrationScreen
    }

    // Main Profile Screen
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(16.dp))

                // Company Card (contains ProfileHeader inside)
                CompanyCard(
                    context = context,
                    prefs = prefs,
                    profileCompletion = profileCompletion,
                    isUpdatingPicture = isUpdatingPicture,
                    onProfilePictureClick = { showProfilePictureOptions = true },
                    onNavigateToEditSocialMedia = onNavigateToEditSocialMedia
                )

                Spacer(Modifier.height(16.dp))

                QuickAccessGrid(
                    onContactDetailClick = onNavigateToEditContactDetail
                )




                Spacer(Modifier.height(16.dp))

                // HR Contact Section
                HRContactSection(
                    context = context,
                    prefs = prefs
                )

                Spacer(Modifier.height(16.dp))

                // Face Recognition Section
                FaceRecognitionSection(
                    onUpdateFaceClick = { showFaceRegistration = true }
                )

                Spacer(Modifier.height(16.dp))

                // Support Section
                SupportSection(
                    context = context,
                    prefs = prefs
                )

                Spacer(Modifier.height(32.dp))

                // App Version
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "App Version ${BuildConfig.VERSION_NAME}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // Success/Error Messages
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                errorMessage?.let { error ->
                    MessageCard(
                        message = error,
                        isError = true,
                        onDismiss = { errorMessage = null }
                    )
                }

                successMessage?.let { success ->
                    MessageCard(
                        message = success,
                        isError = false,
                        onDismiss = { successMessage = null }
                    )
                }
            }
        }

        // Profile Picture Options Bottom Sheet
        if (showProfilePictureOptions) {
            ProfilePictureOptionsSheet(
                hasProfilePicture = !prefs.profileUrl.isNullOrBlank(),
                onDismiss = { showProfilePictureOptions = false },
                onGalleryClick = {
                    imagePickerLauncher.launch("image/*")
                },
                onCameraClick = {
                    // TODO: Implement camera
                },
                onRemoveClick = {
                    showRemoveConfirmation = true
                }
            )
        }

        // Remove Profile Picture Confirmation
        if (showRemoveConfirmation) {
            RemoveProfilePictureDialog(
                onDismiss = { showRemoveConfirmation = false },
                onConfirm = {
                    showRemoveConfirmation = false
                    scope.launch {
                        isUpdatingPicture = true
                        errorMessage = null
                        successMessage = null

                        when (val result = profileRepository.removeProfilePicture()) {
                            is ProfilePictureOutcome.Success -> {
                                successMessage = result.message
                                prefs.profileUrl = null
                            }
                            is ProfilePictureOutcome.Error -> {
                                errorMessage = result.message
                            }
                        }

                        isUpdatingPicture = false
                    }
                }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            LogoutDialog(
                onDismiss = { showLogoutDialog = false },
                onConfirm = {
                    showLogoutDialog = false
                    onLogout()
                }
            )
        }
    }
}