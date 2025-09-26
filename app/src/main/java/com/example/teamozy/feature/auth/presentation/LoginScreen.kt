@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.teamozy.feature.auth.data.AuthOutcome
import com.example.teamozy.feature.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val useCase = remember { LoginUseCase(context) }
    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var useOtp by remember { mutableStateOf(true) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Login") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (useOtp) "Login with OTP" else "Login with Password", fontSize = 22.sp)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                label = { Text("Phone number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            if (!useOtp) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { useOtp = true }) { Text("Use OTP instead") }
            } else {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                    label = { Text("OTP (6 digits)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        enabled = phone.length == 10 && !loading && canResend,
                        onClick = {
                            scope.launch {
                                loading = true
                                val out = useCase.sendOtp(phone)
                                when (out) {
                                    is AuthOutcome.Success -> {
                                        snackbarHostState.showSnackbar(out.message)
                                        startResendTimer(30)
                                    }
                                    is AuthOutcome.Error -> {
                                        snackbarHostState.showSnackbar(out.message)
                                    }
                                }
                                loading = false
                            }
                        }
                    ) { Text(if (canResend) "Send OTP" else "Resend in ${secondsLeft}s") }

                    TextButton(onClick = { useOtp = false }) { Text("Use Password") }
                }
            }

            Spacer(Modifier.height(20.dp))

            val submitEnabled =
                phone.length == 10 &&
                        !loading &&
                        ((!useOtp && password.length >= 4) || (useOtp && otp.length >= 4))

            Button(
                enabled = submitEnabled,
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        val out = if (!useOtp) {
                            useCase.loginWithPassword(phone, password)
                        } else {
                            useCase.loginWithOtp(phone, otp)
                        }
                        when (out) {
                            is AuthOutcome.Success -> {
                                snackbarHostState.showSnackbar(out.message)
                                onLoginSuccess()
                            }
                            is AuthOutcome.Error -> {
                                error = out.message
                            }
                        }
                        loading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (loading) "Please wait…" else "Login")
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
