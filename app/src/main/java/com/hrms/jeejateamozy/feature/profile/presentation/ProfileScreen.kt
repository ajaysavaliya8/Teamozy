@file:OptIn(ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.profile.presentation

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hrms.jeejateamozy.BuildConfig
import com.hrms.jeejateamozy.R
import com.hrms.jeejateamozy.core.image.CoilImageLoader
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.face.presentation.FaceRegistrationScreen
import com.hrms.jeejateamozy.feature.profile.data.ProfileRepository
import com.hrms.jeejateamozy.feature.profile.data.ProfilePictureOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull


private const val TAG = "ProfileScreen"

/**
 * ProfileScreen with Face Registration
 *
 * ✅ ADDED: Face Registration logic (moved from HomePage)
 * - User can update their face from Profile screen
 * - Face registration handled completely here
 */
@Composable
fun ProfileScreen(
    onNavigateToFaceChange: () -> Unit,
    onNavigateToEditSocialMedia: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val profileRepository = remember { ProfileRepository(context) }
    val scope = rememberCoroutineScope()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showProfilePictureOptions by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    var isUpdatingPicture by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // ✅ ADDED: Face Registration state
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

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // TODO: Launch camera
        }
    }

    // ✅ ADDED: Show Face Registration Screen
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(16.dp))

                // Company Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!prefs.companyLogoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(prefs.companyLogoUrl)
                                            .crossfade(true)
                                            .build(),
                                        imageLoader = CoilImageLoader.get(context),
                                        contentDescription = "Company Logo",
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = prefs.companyName?.uppercase() ?: "COMPANY NAME",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    if (!prefs.companyWebsite.isNullOrBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = prefs.companyWebsite!!,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                            textDecoration = TextDecoration.Underline,
                                            modifier = Modifier.clickable {
                                                openUrl(
                                                    context,
                                                    if (prefs.companyWebsite!!.startsWith("http"))
                                                        prefs.companyWebsite!!
                                                    else "https://${prefs.companyWebsite}"
                                                )
                                            }
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = prefs.companyAddress ?: "Company Address",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        // Profile
                        Column(Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Avatar with Edit Button
                                Box(
                                    modifier = Modifier.size(120.dp),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    if (!prefs.profileUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(prefs.profileUrl)
                                                .crossfade(true)
                                                .build(),
                                            imageLoader = CoilImageLoader.get(context),
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    3.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                    CircleShape
                                                ),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    3.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                    CircleShape
                                                )
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = "Profile Picture",
                                                modifier = Modifier.size(60.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Edit/Add button
                                    Surface(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { showProfilePictureOptions = true },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondary
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isUpdatingPicture) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = MaterialTheme.colorScheme.onSecondary,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    if (prefs.profileUrl.isNullOrBlank()) Icons.Default.Add else Icons.Default.Edit,
                                                    contentDescription = "Edit Profile Picture",
                                                    modifier = Modifier.padding(8.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                // Name + completion
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 20.dp)
                                ) {
                                    Text(
                                        text = prefs.fullName ?: prefs.userName ?: "Employee Name",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "${prefs.branchName ?: "Branch"} • ${prefs.departmentName ?: "Department"}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(12.dp))

                                    // Profile completion
                                    val completion = calculateProfileCompletion(prefs)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Profile Completion",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "$completion%",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { completion / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(20.dp))

                            // Contact Info
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = formatPhoneNumber(prefs),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Social Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 0.dp, end = 0.dp, bottom = 0.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SocialMediaIconStatic(
                                    iconRes = R.drawable.ic_facebook,
                                    hasLink = !prefs.facebook.isNullOrBlank(),
                                    onClick = { prefs.facebook?.let { openUrl(context, it) } }
                                )
                                SocialMediaIconStatic(
                                    iconRes = R.drawable.ic_linkedin,
                                    hasLink = !prefs.linkedin.isNullOrBlank(),
                                    onClick = { prefs.linkedin?.let { openUrl(context, it) } }
                                )
                                SocialMediaIconStatic(
                                    iconRes = R.drawable.ic_x,
                                    hasLink = !prefs.x.isNullOrBlank(),
                                    onClick = { prefs.x?.let { openUrl(context, it) } }
                                )
                                SocialMediaIconStatic(
                                    iconRes = R.drawable.ic_instagram,
                                    hasLink = !prefs.instagram.isNullOrBlank(),
                                    onClick = { prefs.instagram?.let { openUrl(context, it) } }
                                )
                                SocialMediaIconStatic(
                                    iconRes = R.drawable.ic_snapchat,
                                    hasLink = !prefs.snapchat.isNullOrBlank(),
                                    onClick = { prefs.snapchat?.let { openUrl(context, it) } }
                                )

                                // Edit button
                                IconButton(
                                    onClick = onNavigateToEditSocialMedia,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Social Media",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Quick Access Section
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Quick Access",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Row 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                QuickAccessItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.ContactPage,
                                    title = "Contact\nDetail",
                                    onClick = { /* TODO */ }
                                )
                                QuickAccessItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.Person,
                                    title = "Personal\nInfo",
                                    onClick = { /* TODO */ }
                                )
                                QuickAccessItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.Work,
                                    title = "Employment\nDetail",
                                    onClick = { /* TODO */ }
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Row 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                QuickAccessItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.WorkHistory,
                                    title = "Past\nExperience",
                                    onClick = { /* TODO */ }
                                )
                                QuickAccessItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.School,
                                    title = "Achievements\n& Education",
                                    onClick = { /* TODO */ }
                                )
                                QuickAccessItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.AccessTime,
                                    title = "Shift\nDetails",
                                    onClick = { /* TODO */ }
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Row 3
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                QuickAccessItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.Timeline,
                                    title = "My\nTimeline",
                                    onClick = { /* TODO */ }
                                )
                                QuickAccessItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.Notifications,
                                    title = "Notification\nSettings",
                                    onClick = { /* TODO */ }
                                )
                                QuickAccessItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.Group,
                                    title = "Nominees",
                                    onClick = { /* TODO */ }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // HR Contact Section
                val hasHRInfo = !prefs.hrEmail.isNullOrBlank() || !prefs.companyContact.isNullOrBlank()

                if (hasHRInfo) {
                    SectionCard(title = "HR Contact") {
                        if (!prefs.companyContact.isNullOrBlank()) {
                            ContactItem(
                                icon = Icons.Default.Phone,
                                title = "HR Mobile",
                                value = prefs.companyContact!!,
                                onClick = { dialPhoneNumber(context, prefs.companyContact!!) }
                            )
                            if (!prefs.hrEmail.isNullOrBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                        if (!prefs.hrEmail.isNullOrBlank()) {
                            ContactItem(
                                icon = Icons.Default.Email,
                                title = "HR Email",
                                value = prefs.hrEmail!!,
                                onClick = { sendEmail(context, prefs.hrEmail!!) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ✅ Face Recognition Section - Now triggers internal FaceRegistrationScreen
                SectionCard(title = "Face Recognition") {
                    ProfileMenuItem(
                        icon = Icons.Outlined.Face,
                        title = "Update Face",
                        subtitle = "Update your face for recognition",
                        onClick = { showFaceRegistration = true },  // ✅ Show directly here!
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Account Section
                SectionCard(title = "Account") {
                    ProfileMenuItem(
                        icon = Icons.Outlined.Person,
                        title = "Personal Information",
                        subtitle = "View and edit your personal details",
                        onClick = { /* TODO */ }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ProfileMenuItem(
                        icon = Icons.Outlined.Lock,
                        title = "Privacy & Security",
                        subtitle = "Manage your privacy settings",
                        onClick = { /* TODO */ }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ProfileMenuItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        subtitle = "Configure notification preferences",
                        onClick = { /* TODO */ }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Support Section
                SectionCard(title = "Support") {
                    val hasTechnicalInfo = !prefs.technicalSupportNumber.isNullOrBlank() ||
                            !prefs.technicalSupportEmail.isNullOrBlank()

                    if (hasTechnicalInfo) {
                        if (!prefs.technicalSupportNumber.isNullOrBlank()) {
                            ContactItem(
                                icon = Icons.Default.Phone,
                                title = "Technical Support",
                                value = prefs.technicalSupportNumber!!,
                                onClick = { dialPhoneNumber(context, prefs.technicalSupportNumber!!) }
                            )
                            if (!prefs.technicalSupportEmail.isNullOrBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                        if (!prefs.technicalSupportEmail.isNullOrBlank()) {
                            ContactItem(
                                icon = Icons.Default.Email,
                                title = "Technical Support",
                                value = prefs.technicalSupportEmail!!,
                                onClick = { sendEmail(context, prefs.technicalSupportEmail!!) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // About
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
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                        }
                    }
                    LaunchedEffect(error) {
                        kotlinx.coroutines.delay(4000)
                        errorMessage = null
                    }
                }

                successMessage?.let { success ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = success,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                    LaunchedEffect(success) {
                        kotlinx.coroutines.delay(4000)
                        successMessage = null
                    }
                }
            }
        }

        // Profile Picture Options Bottom Sheet
        if (showProfilePictureOptions) {
            ModalBottomSheet(
                onDismissRequest = { showProfilePictureOptions = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Profile Picture",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    ProfilePictureOption(
                        icon = Icons.Filled.Add,
                        title = "Choose from Gallery",
                        onClick = {
                            showProfilePictureOptions = false
                            imagePickerLauncher.launch("image/*")
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    ProfilePictureOption(
                        icon = Icons.Default.Add,
                        title = "Take Photo",
                        subtitle = "Coming soon",
                        onClick = {
                            showProfilePictureOptions = false
                        },
                        enabled = false
                    )

                    if (!prefs.profileUrl.isNullOrBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        ProfilePictureOption(
                            icon = Icons.Default.Delete,
                            title = "Remove Picture",
                            iconTint = MaterialTheme.colorScheme.error,
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                showProfilePictureOptions = false
                                showRemoveConfirmation = true
                            }
                        )
                    }
                }
            }
        }

        // Remove Confirmation Dialog
        if (showRemoveConfirmation) {
            AlertDialog(
                onDismissRequest = { showRemoveConfirmation = false },
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                title = { Text("Remove Profile Picture?") },
                text = { Text("Are you sure you want to remove your profile picture? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
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
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ==================== UI COMPONENTS ====================
// (Keep all your existing UI components from the original ProfileScreen)

@Composable
private fun ProfilePictureOption(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ContactItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconTint
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SocialMediaIconStatic(
    iconRes: Int,
    hasLink: Boolean,
    onClick: () -> Unit,
    iconSizeDp: Int = 22
) {
    val chipBg = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val alpha = if (hasLink) 1f else 0.45f
    val clickable = if (hasLink) Modifier.clickable { onClick() } else Modifier

    Box(
        modifier = Modifier
            .size(40.dp)
            .then(clickable)
            .alpha(alpha)
            .clip(CircleShape)
            .background(chipBg)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(iconSizeDp.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun QuickAccessItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

/* ---------------- Helpers ---------------- */

private fun calculateProfileCompletion(prefs: PreferencesManager): Int {
    var filled = 0
    val total = 9
    if (!prefs.fullName.isNullOrBlank() || !prefs.userName.isNullOrBlank()) filled++
    if (!prefs.branchName.isNullOrBlank()) filled++
    if (!prefs.departmentName.isNullOrBlank()) filled++
    if (!prefs.profileUrl.isNullOrBlank()) filled++
    if (!prefs.facebook.isNullOrBlank()) filled++
    if (!prefs.linkedin.isNullOrBlank()) filled++
    if (!prefs.x.isNullOrBlank()) filled++
    if (!prefs.instagram.isNullOrBlank()) filled++
    if (!prefs.snapchat.isNullOrBlank()) filled++
    return (filled * 100) / total
}

private fun formatPhoneNumber(prefs: PreferencesManager): String {
    if (prefs.mobileNumber != 0L) {
        val s = prefs.mobileNumber.toString()
        return if (s.length == 10) "+91-$s" else s
    }
    val userId = prefs.userId
    if (!userId.isNullOrBlank()) return if (userId.length == 10) "+91-$userId" else userId
    return "Not Available"
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

private fun dialPhoneNumber(context: android.content.Context, phoneNumber: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phoneNumber") }
        context.startActivity(intent)
    }
}

private fun sendEmail(context: android.content.Context, email: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:$email") }
        context.startActivity(intent)
    }
}