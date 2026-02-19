@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.auth.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.hrms.jeejateamozy.feature.auth.data.AuthOutcome
import com.hrms.jeejateamozy.feature.auth.data.ResetOutcome
import com.hrms.jeejateamozy.feature.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ForgotPasswordScreen(
    initialPhone: String = "",
    onBack: () -> Unit,
    onResetSuccess: () -> Unit
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val useCase: LoginUseCase = koinInject()
    val scope = rememberCoroutineScope()

    val phoneFR = remember { FocusRequester() }
    val otpFR = remember { FocusRequester() }
    val newPwdFR = remember { FocusRequester() }
    val confirmPwdFR = remember { FocusRequester() }

    // Step: 1=phone, 2=otp, 3=new password
    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    // Step 1
    var phone by rememberSaveable { mutableStateOf(initialPhone) }
    var canResend by rememberSaveable { mutableStateOf(true) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(0) }
    var otpSent by rememberSaveable { mutableStateOf(false) }

    // Step 2
    var otp by rememberSaveable { mutableStateOf("") }
    var resetToken by rememberSaveable { mutableStateOf("") }

    // Step 3
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

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

    val headerGrad = Brush.verticalGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF8FAFF)))
    val primaryGrad = Brush.verticalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))

    val stepTitle = when (currentStep) {
        1 -> "Forgot Password"
        2 -> "Verify OTP"
        3 -> "Set New Password"
        else -> ""
    }
    val stepSubtitle = when (currentStep) {
        1 -> "Enter your registered mobile number to receive an OTP"
        2 -> "Enter the OTP sent to +91 $phone"
        3 -> "Create a new password for your account"
        else -> ""
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        containerColor = Color(0xFFF6F7FB)
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                // Back button
                IconButton(
                    onClick = {
                        if (currentStep > 1 && !loading) {
                            when (currentStep) {
                                2 -> {
                                    currentStep = 1
                                    otp = ""
                                    error = null
                                }
                                3 -> {
                                    currentStep = 2
                                    newPassword = ""
                                    confirmPassword = ""
                                    error = null
                                }
                            }
                        } else if (!loading) {
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 0.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF6366F1)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(primaryGrad),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(stepTitle, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stepSubtitle,
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            // Step indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    val step = index + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (step <= currentStep) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                            )
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
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it / 3 })
                                .togetherWith(fadeOut() + slideOutVertically { -it / 3 })
                        },
                        label = "forgotStep"
                    ) { step ->
                        when (step) {
                            // ====== STEP 1: Phone ======
                            1 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Mobile Number", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = {
                                            val clean = it.filter(Char::isDigit).take(10)
                                            error = null
                                            phone = clean
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
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                focus.clearFocus()
                                                keyboard?.hide()
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

                                if (otpSent) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("OTP sent to +91 $phone", color = Color(0xFF10B981), fontSize = 12.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        enabled = phone.length == 10 && !loading && canResend,
                                        onClick = {
                                            focus.clearFocus()
                                            keyboard?.hide()
                                            scope.launch {
                                                loading = true; error = null
                                                when (val outcome = useCase.forgotPassword(phone)) {
                                                    is AuthOutcome.Success -> {
                                                        otpSent = true
                                                        startResendTimer(30)
                                                        snack.showSnackbar(outcome.message)
                                                    }
                                                    is AuthOutcome.Error -> error = outcome.message
                                                    else -> {}
                                                }
                                                loading = false
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (loading) {
                                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text(
                                                if (canResend) {
                                                    if (otpSent) "Resend OTP" else "Send OTP"
                                                } else "${secondsLeft}s"
                                            )
                                        }
                                    }

                                    if (otpSent) {
                                        Button(
                                            enabled = !loading,
                                            onClick = {
                                                currentStep = 2
                                                error = null
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                        ) {
                                            Text("Next", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            // ====== STEP 2: OTP ======
                            2 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Enter OTP", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                    ForgotOtpBoxes(
                                        value = otp,
                                        onValueChange = { input ->
                                            val clean = input.filter(Char::isDigit).take(4)
                                            error = null
                                            otp = clean
                                            if (clean.length == 4) {
                                                focus.clearFocus()
                                                keyboard?.hide()
                                            }
                                        },
                                        boxCount = 4,
                                        focusRequester = otpFR
                                    )
                                }

                                // Resend row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!canResend && secondsLeft > 0) {
                                        Text("Resend in ${secondsLeft}s", fontSize = 12.sp, color = Color(0xFF6366F1))
                                    } else {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    loading = true; error = null
                                                    when (val outcome = useCase.forgotPassword(phone)) {
                                                        is AuthOutcome.Success -> {
                                                            startResendTimer(30)
                                                            snack.showSnackbar(outcome.message)
                                                        }
                                                        is AuthOutcome.Error -> error = outcome.message
                                                        else -> {}
                                                    }
                                                    loading = false
                                                }
                                            },
                                            enabled = !loading
                                        ) {
                                            Text("Resend OTP", fontWeight = FontWeight.SemiBold, color = Color(0xFF6366F1))
                                        }
                                    }
                                }

                                Button(
                                    enabled = otp.length == 4 && !loading,
                                    onClick = {
                                        focus.clearFocus()
                                        keyboard?.hide()
                                        scope.launch {
                                            loading = true; error = null
                                            when (val outcome = useCase.verifyResetOtp(phone, otp)) {
                                                is ResetOutcome.TokenReceived -> {
                                                    resetToken = outcome.resetToken
                                                    currentStep = 3
                                                    snack.showSnackbar(outcome.message)
                                                }
                                                is ResetOutcome.Error -> error = outcome.message
                                                is ResetOutcome.Success -> {
                                                    snack.showSnackbar(outcome.message)
                                                }
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
                                        Text("Verifying...")
                                    } else {
                                        Text("Verify OTP", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // ====== STEP 3: New Password ======
                            3 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("New Password", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { error = null; newPassword = it },
                                        placeholder = { Text("Enter new password", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Color(0xFF6366F1)) },
                                        trailingIcon = {
                                            TextButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                                Text(if (newPasswordVisible) "HIDE" else "SHOW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        },
                                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { confirmPwdFR.requestFocus() }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(newPwdFR),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = Color(0xFFE5E7EB),
                                            focusedBorderColor = Color(0xFF6366F1),
                                            unfocusedContainerColor = Color(0xFFF8FAFC),
                                            focusedContainerColor = Color.White
                                        )
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Confirm Password", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { error = null; confirmPassword = it },
                                        placeholder = { Text("Confirm new password", color = Color(0xFF94A3B8)) },
                                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Color(0xFF6366F1)) },
                                        trailingIcon = {
                                            TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                                Text(if (confirmPasswordVisible) "HIDE" else "SHOW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        },
                                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                focus.clearFocus()
                                                keyboard?.hide()
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(confirmPwdFR),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = Color(0xFFE5E7EB),
                                            focusedBorderColor = Color(0xFF6366F1),
                                            unfocusedContainerColor = Color(0xFFF8FAFC),
                                            focusedContainerColor = Color.White
                                        )
                                    )
                                }

                                if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                                    Text("Passwords do not match", color = Color(0xFFEF4444), fontSize = 12.sp)
                                }

                                Button(
                                    enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank() && newPassword == confirmPassword && !loading,
                                    onClick = {
                                        focus.clearFocus()
                                        keyboard?.hide()
                                        scope.launch {
                                            loading = true; error = null
                                            when (val outcome = useCase.resetPassword(resetToken, newPassword, confirmPassword)) {
                                                is AuthOutcome.Success -> {
                                                    successMessage = outcome.message
                                                    snack.showSnackbar(outcome.message)
                                                    delay(1500)
                                                    onResetSuccess()
                                                }
                                                is AuthOutcome.Error -> error = outcome.message
                                                else -> {}
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
                                        Text("Resetting...")
                                    } else {
                                        Text("Reset Password", fontWeight = FontWeight.SemiBold)
                                    }
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

                    // Success
                    AnimatedVisibility(
                        visible = !successMessage.isNullOrBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + androidx.compose.animation.shrinkVertically()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                                Text(
                                    text = successMessage.orEmpty(),
                                    color = Color(0xFF065F46),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "Back to Sign In",
                    fontSize = 14.sp,
                    color = Color(0xFF6366F1),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ForgotOtpBoxes(
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
                            if (ch.isBlank()) "\u2022" else ch,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (ch.isBlank()) Color(0xFFCBD5E1) else Color(0xFF111827)
                        )
                    }
                }
            }
        }

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
