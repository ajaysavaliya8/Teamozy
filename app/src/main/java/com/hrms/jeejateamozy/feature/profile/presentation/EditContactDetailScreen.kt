@file:OptIn(ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.feature.profile.data.ContactInfoOutcome
import com.hrms.jeejateamozy.feature.profile.data.ProfileRepository
import kotlinx.coroutines.launch

@Composable
fun EditContactDetailScreen(
    onBack: () -> Unit
    ) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context) }
    val scope = rememberCoroutineScope()

    // State variables
    var countryCode by remember { mutableStateOf("") }
    var alternatePhone by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var currentAddress by remember { mutableStateOf("") }
    var permanentAddress by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Fetch contact info on screen load
    LaunchedEffect(Unit) {
        isFetching = true
        when (val result = profileRepository.getContactInfo()) {
            is ContactInfoOutcome.Success -> {
                result.contactInfo?.let { data ->
                    countryCode = data.country_code?.toString() ?: ""
                    alternatePhone = data.alternate_phone_number?.toString() ?: ""
                    emergencyPhone = data.emergency_phone_number?.toString() ?: ""
                    whatsappNumber = data.whatsapp_number?.toString() ?: ""
                    companyPhone = data.company_phone_number?.toString() ?: ""
                    currentAddress = data.current_address ?: ""
                    permanentAddress = data.permanent_address ?: ""
                }
            }
            is ContactInfoOutcome.Error -> {
                errorMessage = result.message
            }
        }
        isFetching = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Contact Details", fontSize = 18.sp) },
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
        if (isFetching) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(16.dp))

                // Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Enter 0 or leave blank to clear a field",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Country Code
                ContactInputField(
                    label = "Country Code",
                    value = countryCode,
                    onValueChange = { countryCode = it },
                    placeholder = "91",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(12.dp))

                // Alternate Phone Number
                ContactInputField(
                    label = "Alternate Phone Number",
                    value = alternatePhone,
                    onValueChange = { alternatePhone = it },
                    placeholder = "10-digit phone number",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(12.dp))

                // Emergency Phone Number
                ContactInputField(
                    label = "Emergency Phone Number",
                    value = emergencyPhone,
                    onValueChange = { emergencyPhone = it },
                    placeholder = "10-digit phone number",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(12.dp))

                // WhatsApp Number
                ContactInputField(
                    label = "WhatsApp Number",
                    value = whatsappNumber,
                    onValueChange = { whatsappNumber = it },
                    placeholder = "10-digit phone number",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(12.dp))

                // Company Phone Number
                ContactInputField(
                    label = "Company Phone Number",
                    value = companyPhone,
                    onValueChange = { companyPhone = it },
                    placeholder = "10-digit phone number",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(12.dp))

                // Current Address
                ContactInputField(
                    label = "Current Address",
                    value = currentAddress,
                    onValueChange = { currentAddress = it },
                    placeholder = "Enter your current address",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(Modifier.height(12.dp))

                // Permanent Address
                ContactInputField(
                    label = "Permanent Address",
                    value = permanentAddress,
                    onValueChange = { permanentAddress = it },
                    placeholder = "Enter your permanent address",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    singleLine = false,
                    maxLines = 3
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
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Success Message
                successMessage?.let { success ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = success,
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFF2E7D32),
                            fontSize = 13.sp
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
                                // Convert string values to appropriate types
                                val countryCodeValue = countryCode.toIntOrNull()
                                val alternatePhoneValue = when {
                                    alternatePhone.isBlank() -> null
                                    alternatePhone == "0" -> 0L
                                    else -> alternatePhone.toLongOrNull()
                                }
                                val emergencyPhoneValue = emergencyPhone.toLongOrNull()
                                val whatsappValue = whatsappNumber.toLongOrNull()
                                val companyPhoneValue = companyPhone.toLongOrNull()

                                val result = profileRepository.updateContactInfo(
                                    countryCode = countryCodeValue,
                                    alternatePhone = alternatePhoneValue,
                                    emergencyPhone = emergencyPhoneValue,
                                    whatsappNumber = whatsappValue,
                                    companyPhone = companyPhoneValue,
                                    currentAddress = currentAddress.ifBlank { null },
                                    permanentAddress = permanentAddress.ifBlank { null }
                                )

                                when (result) {
                                    is ContactInfoOutcome.Success -> {
                                        successMessage = result.message
                                        // Update fields with returned data
                                        result.contactInfo?.let { data ->
                                            countryCode = data.country_code?.toString() ?: ""
                                            alternatePhone = data.alternate_phone_number?.toString() ?: ""
                                            emergencyPhone = data.emergency_phone_number?.toString() ?: ""
                                            whatsappNumber = data.whatsapp_number?.toString() ?: ""
                                            companyPhone = data.company_phone_number?.toString() ?: ""
                                            currentAddress = data.current_address ?: ""
                                            permanentAddress = data.permanent_address ?: ""
                                        }
                                    }
                                    is ContactInfoOutcome.Error -> {
                                        errorMessage = result.message
                                    }
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
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
}

@Composable
private fun ContactInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontSize = 14.sp) },
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )
    }
}