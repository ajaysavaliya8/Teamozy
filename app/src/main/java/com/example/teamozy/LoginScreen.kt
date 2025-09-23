package com.example.teamozy

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamozy.network.NetworkModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isOtpMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00BCD4),
                        Color(0xFF009688),
                        Color(0xFF004D40)
                    ),
                    radius = 1200f
                )
            )
    ) {
        // Background decoration
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset((-100).dp, (-100).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(250.dp, 600.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Main Login Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // App Logo/Icon
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = Color(0xFF00BCD4).copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            tint = Color(0xFF00BCD4)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Welcome Text
                    Text(
                        text = "Welcome!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF263238),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Sign in to get started",
                        fontSize = 16.sp,
                        color = Color(0xFF78909C),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp, top = 8.dp)
                    )

                    // Phone Number Field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                phoneNumber = it
                                if (statusMessage.isNotEmpty()) {
                                    statusMessage = ""
                                    isError = false
                                }
                            }
                        },
                        label = { Text("Phone Number", color = Color(0xFF78909C)) },
                        placeholder = { Text("Enter 10 digit number") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF00BCD4)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00BCD4),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            cursorColor = Color(0xFF00BCD4)
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dynamic Input Field
                    if (isOtpMode) {
                        OutlinedTextField(
                            value = otp,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    otp = it
                                    if (statusMessage.isNotEmpty()) {
                                        statusMessage = ""
                                        isError = false
                                    }
                                }
                            },
                            label = { Text("OTP", color = Color(0xFF78909C)) },
                            placeholder = { Text("Enter 4 digit OTP") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF00BCD4)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00BCD4),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                cursorColor = Color(0xFF00BCD4)
                            )
                        )
                    } else {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (statusMessage.isNotEmpty()) {
                                    statusMessage = ""
                                    isError = false
                                }
                            },
                            label = { Text("Password", color = Color(0xFF78909C)) },
                            placeholder = { Text("Enter your password") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF00BCD4)
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00BCD4),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                cursorColor = Color(0xFF00BCD4)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Get OTP Button (only in OTP mode)
                    if (isOtpMode) {
                        ElevatedButton(
                            onClick = {
                                if (phoneNumber.length == 10) {
                                    scope.launch {
                                        isLoading = true
                                        requestOtp(phoneNumber, getDeviceId(context)) { success, message ->
                                            isLoading = false
                                            statusMessage = message
                                            isError = !success
                                        }
                                    }
                                } else {
                                    statusMessage = "Please enter a valid 10-digit phone number"
                                    isError = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isLoading && phoneNumber.length == 10,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Send OTP", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Main Login Button
                    Button(
                        onClick = {
                            val validationResult = validateInputs(phoneNumber, password, otp, isOtpMode)
                            if (validationResult.first) {
                                scope.launch {
                                    isLoading = true
                                    performLogin(
                                        phoneNumber,
                                        if (!isOtpMode) password else null,
                                        if (isOtpMode) otp.toIntOrNull() else null,
                                        getDeviceId(context),
                                        context
                                    ) { success, message ->
                                        isLoading = false
                                        if (success) {
                                            onLoginSuccess()
                                        } else {
                                            statusMessage = message
                                            isError = true
                                        }
                                    }
                                }
                            } else {
                                statusMessage = validationResult.second
                                isError = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BCD4),
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Sign In",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mode Toggle
                    TextButton(
                        onClick = {
                            isOtpMode = !isOtpMode
                            password = ""
                            otp = ""
                            statusMessage = ""
                            isError = false
                        }
                    ) {
                        Text(
                            text = if (isOtpMode) "Use Password Instead" else "Use OTP Instead",
                            color = Color(0xFF00BCD4),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Status Message
                    if (statusMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isError)
                                Color(0xFFFFEBEE) else Color(0xFFE8F5E8)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isError) Color(0xFFE53935) else Color(0xFF43A047),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = statusMessage,
                                    color = if (isError) Color(0xFFE53935) else Color(0xFF43A047),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Input validation helper
fun validateInputs(phone: String, password: String, otp: String, isOtpMode: Boolean): Pair<Boolean, String> {
    return when {
        phone.length != 10 -> Pair(false, "Please enter a valid 10-digit phone number")
        isOtpMode && otp.length != 4 -> Pair(false, "Please enter a valid 4-digit OTP")
        !isOtpMode && password.length < 3 -> Pair(false, "Password must be at least 3 characters")
        else -> Pair(true, "")
    }
}

// Device ID helper
fun getDeviceId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}

// OTP Request with better error handling
suspend fun requestOtp(
    mobileNumber: String,
    deviceId: String,
    callback: (Boolean, String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val response = NetworkModule.apiService.sendOtp(mobileNumber.toLong(), deviceId)

            withContext(Dispatchers.Main) {
                val body = response.body()
                val bodyMessage = body?.message

                if (bodyMessage != null && bodyMessage.isNotEmpty()) {
                    val success = response.isSuccessful && body.status == "success"
                    callback(success, bodyMessage)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null && errorBody.isNotEmpty()) {
                        try {
                            val jsonError = JSONObject(errorBody)
                            jsonError.getString("message")
                        } catch (e: Exception) {
                            "Server error occurred"
                        }
                    } else {
                        if (response.isSuccessful) "OTP sent successfully"
                        else "Failed to send OTP"
                    }

                    callback(response.isSuccessful, errorMessage)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback(false, "Network connection failed. Please try again.")
            }
        }
    }
}

suspend fun performLogin(
    mobileNumber: String,
    password: String?,
    otp: Int?,
    deviceId: String,
    context: Context,
    callback: (Boolean, String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val response = NetworkModule.apiService.verifyLogin(
                mobileNumber = mobileNumber.toLong(),
                deviceId = deviceId,
                password = password,
                otp = otp
            )

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null && body.status == "success") {
                        val token = body.token

                        if (token.isNotEmpty()) {
                            try {
                                // Save to the correct SharedPreferences - "teamozy" not "teamozy_prefs"
                                context.getSharedPreferences("teamozy", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("token", token)
                                    .putString("device_id", deviceId)
                                    .apply()

                                println("Token successfully saved: ${token.take(20)}...")

                            } catch (e: Exception) {
                                println("Error saving token: ${e.message}")
                            }
                        }

                        callback(true, body.message)
                    } else {
                        callback(false, body?.message ?: "Invalid response")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null && errorBody.isNotEmpty()) {
                        try {
                            val jsonError = JSONObject(errorBody)
                            jsonError.getString("message")
                        } catch (e: Exception) {
                            "Authentication failed"
                        }
                    } else {
                        "Login failed"
                    }

                    callback(false, errorMessage)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback(false, "Network connection failed. Please check your internet.")
            }
        }
    }
}