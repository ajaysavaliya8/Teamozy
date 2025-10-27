@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.profile.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // Facebook
            SocialMediaInputField(
                label = "Facebook",
                iconRes = R.drawable.ic_facebook,
                iconColor = Color(0xFF1877F2),
                value = facebook,
                onValueChange = { facebook = it },
                placeholder = "Type here"
            )

            Spacer(Modifier.height(16.dp))

            // LinkedIn
            SocialMediaInputField(
                label = "Linkedin",
                iconRes = R.drawable.ic_linkedin,
                iconColor = Color(0xFF0A66C2),
                value = linkedin,
                onValueChange = { linkedin = it },
                placeholder = "Type here"
            )

            Spacer(Modifier.height(16.dp))

            // X (Twitter)
            SocialMediaInputField(
                label = "X (Twitter)",
                iconRes = R.drawable.ic_x,
                iconColor = Color(0xFF000000),
                value = x,
                onValueChange = { x = it },
                placeholder = "Type here"
            )

            Spacer(Modifier.height(16.dp))

            // Instagram
            SocialMediaInputField(
                label = "Instagram",
                iconRes = R.drawable.ic_instagram,
                iconColor = Color(0xFFE4405F),
                value = instagram,
                onValueChange = { instagram = it },
                placeholder = "Type here"
            )

            Spacer(Modifier.height(16.dp))

            // Snapchat - Using WhatsApp style input
            SocialMediaInputFieldWithPhone(
                label = "Snapchat",
                iconRes = R.drawable.ic_snapchat,
                iconColor = Color(0xFFFFFC00),
                value = snapchat,
                onValueChange = { snapchat = it },
                placeholder = "Type here"
            )

            Spacer(Modifier.height(24.dp))

            // Error Message
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Success Message
            if (successMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = successMessage!!,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Save Button
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        successMessage = null

                        try {
                            val token = prefs.authToken
                            if (token.isNullOrBlank()) {
                                errorMessage = "Authentication token not found"
                                isLoading = false
                                return@launch
                            }

                            val response = NetworkModule.apiService.updateSocialMedia(
                                facebook = facebook.ifBlank { "" },
                                linkedin = linkedin.ifBlank { "" },
                                x = x.ifBlank { "" },
                                instagram = instagram.ifBlank { "" },
                                snapchat = snapchat.ifBlank { "" }
                            )

                            if (response.isSuccessful) {
                                val body = response.body()
                                if (body?.status == "success") {
                                    // Update PreferencesManager
                                    prefs.facebook = facebook.ifBlank { null }
                                    prefs.linkedin = linkedin.ifBlank { null }
                                    prefs.x = x.ifBlank { null }
                                    prefs.instagram = instagram.ifBlank { null }
                                    prefs.snapchat = snapchat.ifBlank { null }

                                    successMessage = "Social media links updated successfully!"

                                    // Navigate back after 1.5 seconds
                                    kotlinx.coroutines.delay(1500)
                                    onBack()
                                } else {
                                    errorMessage = body?.message ?: "Failed to update social media"
                                }
                            } else {
                                errorMessage = "Server error: ${response.code()}"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Network error: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "SAVE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Social Media Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Text Input
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun SocialMediaInputFieldWithPhone(
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Social Media Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Country Code
                Text(
                    text = "+91",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.width(8.dp))

                // Text Input
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            }
        }
    }
}