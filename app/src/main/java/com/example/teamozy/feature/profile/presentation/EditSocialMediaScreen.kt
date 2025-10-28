@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.profile.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamozy.R
import com.example.teamozy.core.network.NetworkModule
import com.example.teamozy.core.utils.PreferencesManager
import kotlinx.coroutines.launch

@Composable
fun EditSocialMediaScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var facebook by remember { mutableStateOf(prefs.facebook ?: "") }
    var linkedin by remember { mutableStateOf(prefs.linkedin ?: "") }
    var x by remember { mutableStateOf(prefs.x ?: "") }
    var instagram by remember { mutableStateOf(prefs.instagram ?: "") }
    var snapchat by remember { mutableStateOf(prefs.snapchat ?: "") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Social Links",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // Facebook
            SocialMediaInputField(
                label = "Facebook",
                iconRes = R.drawable.ic_facebook,
                iconColor = Color(0xFF1877F2),
                value = facebook,
                onValueChange = { facebook = it },
                placeholder = "Type here"
            )

            Spacer(Modifier.height(8.dp))

            // LinkedIn
            SocialMediaInputField(
                label = "Linkedin",
                iconRes = R.drawable.ic_linkedin,
                iconColor = Color(0xFF0A66C2),
                value = linkedin,
                onValueChange = { linkedin = it },
                placeholder = "Type here"
            )

            Spacer(Modifier.height(8.dp))

            // X (Twitter)
            SocialMediaInputField(
                label = "X (Twitter)",
                iconRes = R.drawable.ic_x,
                iconColor = Color(0xFF000000),
                value = x,
                onValueChange = { x = it },
                placeholder = "Type here"
            )

            Spacer(Modifier.height(8.dp))

            // Instagram
            SocialMediaInputField(
                label = "Instagram",
                iconRes = R.drawable.ic_instagram,
                iconColor = Color(0xFFE4405F),
                value = instagram,
                onValueChange = { instagram = it },
                placeholder = "Type here"
            )

            Spacer(Modifier.height(8.dp))

            // Snapchat
            SocialMediaInputField(
                label = "Snapchat",
                iconRes = R.drawable.ic_snapchat,
                iconColor = Color(0xFFFFFC00),
                value = snapchat,
                onValueChange = { snapchat = it },
                placeholder = "Type here"
            )

            Spacer(Modifier.height(16.dp))

            // Error Message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Success Message
            successMessage?.let { success ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // SAVE Button - After Snapchat
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        successMessage = null

                        try {
                            val response = NetworkModule.apiService.updateSocialMedia(
                                facebook = facebook.ifBlank { null },
                                linkedin = linkedin.ifBlank { null },
                                x = x.ifBlank { null },
                                instagram = instagram.ifBlank { null },
                                snapchat = snapchat.ifBlank { null }
                            )

                            if (response.isSuccessful) {
                                val body = response.body()
                                if (body?.status == "success") {
                                    body.social_media?.let { socialMedia ->
                                        prefs.facebook = socialMedia.facebook
                                        prefs.linkedin = socialMedia.linkedin
                                        prefs.x = socialMedia.x
                                        prefs.instagram = socialMedia.instagram
                                        prefs.snapchat = socialMedia.snapchat
                                    }

                                    successMessage = body.message ?: "Social media links updated successfully!"
                                    kotlinx.coroutines.delay(1500)
                                    onBack()
                                } else {
                                    errorMessage = body?.message ?: "Failed to update social media"
                                }
                            } else {
                                val errorBody = response.errorBody()?.string()
                                errorMessage = if (errorBody != null) {
                                    try {
                                        val json = org.json.JSONObject(errorBody)
                                        json.optString("message", "Server error: ${response.code()}")
                                    } catch (e: Exception) {
                                        "Server error: ${response.code()}"
                                    }
                                } else {
                                    "Server error: ${response.code()}"
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Network error: ${e.localizedMessage ?: "Unknown error"}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "SAVE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(80.dp)) // Extra space for bottom navigation
        }
    }
}

@Composable
private fun SocialMediaInputField(
    label: String,
    iconRes: Int,
    iconColor: Color,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF212121),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFFBDBDBD),
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFF212121)
                        ),
                        singleLine = true
                    )
                }
            }
        }
    }
}