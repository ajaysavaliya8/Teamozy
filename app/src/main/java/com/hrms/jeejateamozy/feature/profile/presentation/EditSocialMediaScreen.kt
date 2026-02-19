@file:OptIn(ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.profile.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.R
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
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
                title = { Text("Edit Social Links", fontSize = 18.sp) },
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
                iconRes = R.drawable.ic_facebook,          // full-color asset
                contentDesc = "Facebook",
                brandFocusColor = Color(0xFF1877F2),
                value = facebook,
                onValueChange = { facebook = it },
                placeholder = "https://facebook.com/your.username",
                imeAction = ImeAction.Next
            )

            Spacer(Modifier.height(12.dp))

            // LinkedIn
            SocialMediaInputField(
                iconRes = R.drawable.ic_linkedin,
                contentDesc = "LinkedIn",
                brandFocusColor = Color(0xFF0A66C2),
                value = linkedin,
                onValueChange = { linkedin = it },
                placeholder = "https://linkedin.com/in/your-id",
                imeAction = ImeAction.Next
            )

            Spacer(Modifier.height(12.dp))

            // X (Twitter)
            SocialMediaInputField(
                iconRes = R.drawable.ic_x,
                contentDesc = "X (Twitter)",
                brandFocusColor = Color(0xFF000000),
                value = x,
                onValueChange = { x = it },
                placeholder = "https://x.com/your.handle",
                imeAction = ImeAction.Next
            )

            Spacer(Modifier.height(12.dp))

            // Instagram
            SocialMediaInputField(
                iconRes = R.drawable.ic_instagram,
                contentDesc = "Instagram",
                brandFocusColor = Color(0xFFE4405F),
                value = instagram,
                onValueChange = { instagram = it },
                placeholder = "https://instagram.com/your.username",
                imeAction = ImeAction.Next
            )

            Spacer(Modifier.height(12.dp))

            // Snapchat (ID or link)
            SocialMediaInputField(
                iconRes = R.drawable.ic_snapchat,
                contentDesc = "Snapchat",
                brandFocusColor = Color(0xFFFFFC00),
                value = snapchat,
                onValueChange = { snapchat = it },
                placeholder = "Your Snapchat ID or link",
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            )

            Spacer(Modifier.height(16.dp))

            // Error
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
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
                LaunchedEffect(error) {
                    kotlinx.coroutines.delay(3000)
                    errorMessage = null
                }
                Spacer(Modifier.height(12.dp))
            }

            // Success
            successMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(text = msg, color = Color.White, fontSize = 14.sp)
                    }
                }
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3000)
                    successMessage = null
                }
                Spacer(Modifier.height(12.dp))
            }

            // Save
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        successMessage = null

                        try {
                            val res = NetworkModule.apiService.updateSocialMedia(
                                facebookUrl = facebook.ifBlank { null },
                                linkedinUrl = linkedin.ifBlank { null },
                                xUrl = x.ifBlank { null },
                                instagramUrl = instagram.ifBlank { null },
                                snapchatUrl = snapchat.ifBlank { null }
                            )

                            if (res.isSuccessful && res.body()?.success == true) {
                                // persist locally
                                prefs.facebook = facebook.ifBlank { null }
                                prefs.linkedin = linkedin.ifBlank { null }
                                prefs.x = x.ifBlank { null }
                                prefs.instagram = instagram.ifBlank { null }
                                prefs.snapchat = snapchat.ifBlank { null }

                                successMessage = res.body()?.message ?: "Social media links updated"
                            } else {
                                val errorBody = res.errorBody()?.string().orEmpty()
                                val msg = try {
                                    org.json.JSONObject(errorBody).optString("message")
                                        .ifBlank { "Failed to update" }
                                } catch (_: Exception) {
                                    "Failed to update social media links"
                                }
                                errorMessage = msg
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Unknown error"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("SAVE")
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SocialMediaInputField(
    iconRes: Int,
    contentDesc: String,
    brandFocusColor: Color,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    imeAction: ImeAction,
    keyboardType: KeyboardType = KeyboardType.Uri
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            // Use the original, full-color asset — no tint
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = contentDesc,
                modifier = Modifier.size(22.dp)
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            // Neutral when not focused; brand accent on focus
            unfocusedBorderColor = Color(0xFFE2E8F0),
            focusedBorderColor = brandFocusColor,
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White
        )
    )
}
