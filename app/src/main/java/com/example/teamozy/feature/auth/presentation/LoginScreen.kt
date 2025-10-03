@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.auth.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.teamozy.feature.auth.data.AuthOutcome
import com.example.teamozy.feature.auth.data.AuthRepository
import com.example.teamozy.feature.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val repo = remember(context) { AuthRepository(context) }
    val useCase = remember(repo) { LoginUseCase(repo) }
    val scope = rememberCoroutineScope()

    // --- Focus requesters for smart keyboard flow
    val phoneFR = remember { FocusRequester() }
    val otpFR = remember { FocusRequester() }
    val pwdFR = remember { FocusRequester() }

    // --- State (saveable for rotations)
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var useOtp by rememberSaveable { mutableStateOf(true) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var otpSent by rememberSaveable { mutableStateOf(false) }
    var canResend by rememberSaveable { mutableStateOf(true) }
    var secondsLeft by rememberSaveable { mutableStateOf(0) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val snack = remember { SnackbarHostState() }

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

    // Palette / motion
    val primaryGrad = Brush.verticalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
    val headerGrad = Brush.verticalGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF8FAFF)))
    val logoScale by animateFloatAsState(
        if (loading) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "logoScale"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        containerColor = Color(0xFFF6F7FB)
    ) { padding ->

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // Tap anywhere blank to dismiss keyboard
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focus.clearFocus()
                        keyboard?.hide()
                    })
                }
                .padding(padding)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGrad)
                    .padding(top = 36.dp, bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(primaryGrad)
                            .scale(logoScale),
                        contentAlignment = Alignment.Center
                    ) { Text("T", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                    Spacer(Modifier.height(14.dp))
                    Text("Welcome back", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sign in to continue to Teamozy",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Phone
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Mobile Number", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                val clean = it.filter(Char::isDigit).take(10)
                                error = null
                                phone = clean

                                // 👉 Auto-hide keyboard when 10 digits reached
                                if (clean.length == 10) {
                                    focus.clearFocus()
                                    keyboard?.hide()
                                }
                            },
                            placeholder = { Text("+91 98765 43210", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Filled.Phone, null, tint = Color(0xFF6366F1)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    // If user presses IME Next, jump to chosen auth input
                                    if (useOtp) {
                                        otpFR.requestFocus()
                                    } else {
                                        pwdFR.requestFocus()
                                    }
                                    keyboard?.show()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(phoneFR),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color.White
                            )
                        )
                        if (phone.isNotEmpty() && phone.length < 10) {
                            Text("${phone.length}/10 digits", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }

                    // 1) INPUT (OTP or Password)
                    AnimatedContent(
                        targetState = useOtp,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it / 3 })
                                .togetherWith(fadeOut() + slideOutVertically { -it / 3 })
                        },
                        label = "authModeInput"
                    ) { isOtp ->
                        if (isOtp) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Enter OTP", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))

                                OtpBoxes(
                                    value = otp,
                                    onValueChange = { input ->
                                        val clean = input.filter(Char::isDigit).take(4)
                                        error = null
                                        otp = clean
                                        // 👉 When 4 digits entered, auto-hide keyboard
                                        if (clean.length == 4) {
                                            focus.clearFocus()
                                            keyboard?.hide()
                                        }
                                    },
                                    boxCount = 4,
                                    focusRequester = otpFR
                                )

                                if (otpSent) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("OTP sent to +91 $phone", color = Color(0xFF10B981), fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Password", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { error = null; password = it },
                                    placeholder = { Text("Enter your password", color = Color(0xFF94A3B8)) },
                                    leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Color(0xFF6366F1)) },
                                    trailingIcon = {
                                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Text(if (passwordVisible) "HIDE" else "SHOW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (phone.length == 10 && password.isNotBlank() && !loading) {
                                                focus.clearFocus()
                                                keyboard?.hide()
                                                scope.launch {
                                                    loading = true; error = null
                                                    when (val out = useCase.loginWithPassword(phone, password)) {
                                                        is AuthOutcome.Success -> {
                                                            snack.showSnackbar(out.message)
                                                            onLoginSuccess()
                                                        }
                                                        is AuthOutcome.Error -> error = out.message
                                                    }
                                                    loading = false
                                                }
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(pwdFR),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color(0xFFE5E7EB),
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedContainerColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // 2) TOGGLE (between input and actions)
                    SegmentedTwoWay(
                        left = "Login with OTP",
                        right = "Password",
                        selectedLeft = useOtp,
                        onLeft = {
                            useOtp = true
                            password = ""
                            error = null
                            // 👉 Focus OTP field & open keyboard
                            otpFR.requestFocus()
                            keyboard?.show()
                        },
                        onRight = {
                            useOtp = false
                            otp = ""
                            otpSent = false
                            error = null
                            // 👉 Focus Password field & open keyboard
                            pwdFR.requestFocus()
                            keyboard?.show()
                        }
                    )

                    // 3) ACTION BUTTONS (below the toggle)
                    AnimatedContent(
                        targetState = useOtp,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it / 4 })
                                .togetherWith(fadeOut() + slideOutVertically { -it / 4 })
                        },
                        label = "authModeActions"
                    ) { isOtp ->
                        if (isOtp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    enabled = phone.length == 10 && !loading && canResend,
                                    onClick = {
                                        scope.launch {
                                            loading = true; error = null
                                            when (val out = useCase.sendOtp(phone)) {
                                                is AuthOutcome.Success -> {
                                                    otpSent = true
                                                    snack.showSnackbar(out.message)
                                                    startResendTimer(30)
                                                    // 👉 After sending OTP, focus OTP field & show keyboard
                                                    otpFR.requestFocus()
                                                    keyboard?.show()
                                                }
                                                is AuthOutcome.Error -> error = out.message
                                            }
                                            loading = false
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (loading && !canResend) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text(if (canResend) "Send OTP" else "${secondsLeft}s")
                                    }
                                }

                                Button(
                                    enabled = phone.length == 10 && otp.length == 4 && !loading,
                                    onClick = {
                                        focus.clearFocus()
                                        keyboard?.hide()
                                        scope.launch {
                                            loading = true; error = null
                                            when (val out = useCase.loginWithOtp(phone, otp)) {
                                                is AuthOutcome.Success -> {
                                                    snack.showSnackbar(out.message)
                                                    onLoginSuccess()
                                                }
                                                is AuthOutcome.Error -> error = out.message
                                            }
                                            loading = false
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                ) {
                                    if (loading && otp.length == 4) {
                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                    } else {
                                        Text("Verify", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            Button(
                                enabled = phone.length == 10 && password.isNotBlank() && !loading,
                                onClick = {
                                    focus.clearFocus()
                                    keyboard?.hide()
                                    scope.launch {
                                        loading = true; error = null
                                        when (val out = useCase.loginWithPassword(phone, password)) {
                                            is AuthOutcome.Success -> {
                                                snack.showSnackbar(out.message)
                                                onLoginSuccess()
                                            }
                                            is AuthOutcome.Error -> error = out.message
                                        }
                                        loading = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Signing in…")
                                } else {
                                    Text("Sign In", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Error
                    AnimatedVisibility(
                        visible = !error.isNullOrBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + androidx.compose.animation.shrinkVertically()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = error.orEmpty(),
                                color = Color(0xFF991B1B),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "By signing in, you agree to our Terms & Privacy Policy",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 8.dp)
            )
        }
    }
}

/* ---------------- UI bits ---------------- */

@Composable
private fun SegmentedTwoWay(
    left: String,
    right: String,
    selectedLeft: Boolean,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    val track = Color(0xFFF1F5F9)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(track)
            .padding(4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SegItem(text = left, selected = selectedLeft, onClick = onLeft, weight = 1f)
            SegItem(text = right, selected = !selectedLeft, onClick = onRight, weight = 1f)
        }
    }
}

@Composable
private fun RowScope.SegItem(text: String, selected: Boolean, onClick: () -> Unit, weight: Float) {
    Surface(
        modifier = Modifier
            .weight(weight)
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (selected) Color.White else Color.Transparent,
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) Color(0xFF6366F1) else Color(0xFF64748B)
            )
        }
    }
}

/** Single hidden input + pretty OTP boxes */
@Composable
private fun OtpBoxes(
    value: String,
    onValueChange: (String) -> Unit,
    boxCount: Int,
    focusRequester: FocusRequester
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var internal by remember(value) { mutableStateOf(value) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        contentAlignment = Alignment.Center
    ) {
        // Visual boxes
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(boxCount) { i ->
                val ch = internal.getOrNull(i)?.toString() ?: ""
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = if (ch.isNotEmpty()) 2.dp else 0.dp,
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (ch.isNotEmpty()) Color(0xFF6366F1) else Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier.size(width = 56.dp, height = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (ch.isBlank()) "•" else ch,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (ch.isBlank()) Color(0xFFCBD5E1) else Color(0xFF111827)
                        )
                    }
                }
            }
        }

        // Invisible field capturing input (on top)
        BasicTextField(
            value = internal,
            onValueChange = {
                val clean = it.filter(Char::isDigit).take(boxCount)
                internal = clean
                onValueChange(clean)
                if (clean.length == boxCount) {
                    focusManager.clearFocus()
                    keyboard?.hide()
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                keyboard?.hide()
            }),
            textStyle = TextStyle(color = Color.Transparent),
            modifier = Modifier
                .matchParentSize()
                .alpha(0f)
                .zIndex(1f)
                .focusRequester(focusRequester)
        )
    }
}
