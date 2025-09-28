@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.teamozy.feature.auth.data.AuthOutcome
import com.example.teamozy.feature.auth.data.AuthRepository
import com.example.teamozy.feature.auth.domain.usecase.LoginUseCase

// ✅ Keyboard imports for older Compose versions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    // ✅ Build repo from context, then pass repo to use case
    val repo = remember(context) { AuthRepository(context) }
    val useCase = remember(repo) { LoginUseCase(repo) }

    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var useOtp by remember { mutableStateOf(true) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }

    // resend flow
    var canResend by remember { mutableStateOf(true) }
    var secondsLeft by remember { mutableStateOf(0) }
    fun startResendTimer(seconds: Int = 30) {
        canResend = false
        secondsLeft = seconds
        scope.launch {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
            canResend = true
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome", fontSize = 26.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Sign in with your registered mobile number.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    error = null
                    phone = it.filter(Char::isDigit).take(10)
                },
                label = { Text("Mobile number") },
                supportingText = {
                    if (phone.length in 1..9) Text("Enter 10-digit number")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Auth method toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = useOtp,
                    onClick = { useOtp = true; password = ""; error = null },
                    label = { Text("Use OTP") }
                )
                FilterChip(
                    selected = !useOtp,
                    onClick = { useOtp = false; otp = ""; error = null },
                    label = { Text("Use Password") }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (useOtp) {
                OutlinedTextField(
                    value = otp,
                    onValueChange = {
                        error = null
                        otp = it.filter(Char::isDigit).take(4) // backend sends 4-digit OTP (0000)
                    },
                    label = { Text("OTP (4 digits)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        enabled = phone.length == 10 && !loading && canResend,
                        onClick = {
                            scope.launch {
                                loading = true; error = null
                                when (val out = useCase.sendOtp(phone)) {
                                    is AuthOutcome.Success -> {
                                        snack.showSnackbar(out.message)
                                        startResendTimer(30)
                                    }
                                    is AuthOutcome.Error -> {
                                        snack.showSnackbar(out.message)
                                        error = out.message
                                    }
                                }
                                loading = false
                            }
                        }
                    ) {
                        Text(if (canResend) "Send OTP" else "Resend in ${secondsLeft}s")
                    }

                    Button(
                        enabled = phone.length == 10 && otp.length == 4 && !loading,
                        onClick = {
                            scope.launch {
                                loading = true; error = null
                                when (val out = useCase.loginWithOtp(phone, otp)) {
                                    is AuthOutcome.Success -> {
                                        snack.showSnackbar(out.message)
                                        onLoginSuccess()
                                    }
                                    is AuthOutcome.Error -> {
                                        error = out.message
                                        snack.showSnackbar(out.message)
                                    }
                                }
                                loading = false
                            }
                        }
                    ) {
                        Text(if (loading) "Please wait…" else "Verify & Login")
                    }
                }
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = { error = null; password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = phone.length == 10 && password.isNotBlank() && !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    onClick = {
                        scope.launch {
                            loading = true; error = null
                            when (val out = useCase.loginWithPassword(phone, password)) {
                                is AuthOutcome.Success -> {
                                    snack.showSnackbar(out.message)
                                    onLoginSuccess()
                                }
                                is AuthOutcome.Error -> {
                                    error = out.message
                                    snack.showSnackbar(out.message)
                                }
                            }
                            loading = false
                        }
                    }
                ) { Text(if (loading) "Please wait…" else "Login") }

                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { useOtp = true; password = ""; error = null }) {
                    Text("Use OTP instead")
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
