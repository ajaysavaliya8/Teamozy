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
import androidx.compose.ui.text.font.FontWeight
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

    // Read-only fields
    var personalMobile by remember { mutableStateOf("") }
    var personalEmail by remember { mutableStateOf("") }
    var companyEmail by remember { mutableStateOf("") }

    // Editable phone fields
    var alternateMobile by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }

    // Emergency contact fields
    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactRelationship by remember { mutableStateOf("") }
    var emergencyContactNumber by remember { mutableStateOf("") }

    // Current address fields
    var currentAddressLine by remember { mutableStateOf("") }
    var currentCity by remember { mutableStateOf("") }
    var currentState by remember { mutableStateOf("") }
    var currentCountry by remember { mutableStateOf("") }
    var currentPostalCode by remember { mutableStateOf("") }

    // Same as current toggle
    var sameAsCurrent by remember { mutableStateOf(false) }

    // Permanent address fields
    var permanentAddressLine by remember { mutableStateOf("") }
    var permanentCity by remember { mutableStateOf("") }
    var permanentState by remember { mutableStateOf("") }
    var permanentCountry by remember { mutableStateOf("") }
    var permanentPostalCode by remember { mutableStateOf("") }

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
                    // Read-only fields
                    personalMobile = data.personal_mobile ?: ""
                    personalEmail = data.personal_email ?: ""
                    companyEmail = data.company_email ?: ""

                    // Editable phone fields
                    alternateMobile = data.alternate_mobile ?: ""
                    whatsappNumber = data.whatsapp_number ?: ""

                    // Emergency contact
                    emergencyContactName = data.emergency_contact_name ?: ""
                    emergencyContactRelationship = data.emergency_contact_relationship ?: ""
                    emergencyContactNumber = data.emergency_contact_number ?: ""

                    // Current address
                    data.current_address?.let { addr ->
                        currentAddressLine = addr.address_line ?: ""
                        currentCity = addr.city ?: ""
                        currentState = addr.state ?: ""
                        currentCountry = addr.country ?: ""
                        currentPostalCode = addr.postal_code ?: ""
                    }

                    // Same as current
                    sameAsCurrent = data.same_as_current ?: false

                    // Permanent address
                    data.permanent_address?.let { addr ->
                        permanentAddressLine = addr.address_line ?: ""
                        permanentCity = addr.city ?: ""
                        permanentState = addr.state ?: ""
                        permanentCountry = addr.country ?: ""
                        permanentPostalCode = addr.postal_code ?: ""
                    }
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
                            text = "Fields marked as read-only are managed by HR and cannot be changed here.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Read-Only Fields Section ──
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Contact Info (HR Managed)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF757575)
                        )
                        Spacer(Modifier.height(12.dp))

                        ReadOnlyContactField("Personal Mobile", personalMobile)
                        ReadOnlyContactField("Personal Email", personalEmail)
                        ReadOnlyContactField("Company Email", companyEmail)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Phone Numbers Section ──
                SectionHeader("Phone Numbers")

                Spacer(Modifier.height(8.dp))

                ContactInputField(
                    label = "Alternate Mobile",
                    value = alternateMobile,
                    onValueChange = { alternateMobile = it },
                    placeholder = "Enter alternate mobile number",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(12.dp))

                ContactInputField(
                    label = "WhatsApp Number",
                    value = whatsappNumber,
                    onValueChange = { whatsappNumber = it },
                    placeholder = "Enter WhatsApp number",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(20.dp))

                // ── Emergency Contact Section ──
                SectionHeader("Emergency Contact")

                Spacer(Modifier.height(8.dp))

                ContactInputField(
                    label = "Contact Name",
                    value = emergencyContactName,
                    onValueChange = { emergencyContactName = it },
                    placeholder = "Enter emergency contact name",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(12.dp))

                ContactInputField(
                    label = "Relationship",
                    value = emergencyContactRelationship,
                    onValueChange = { emergencyContactRelationship = it },
                    placeholder = "e.g., Father, Mother, Spouse",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(12.dp))

                ContactInputField(
                    label = "Contact Number",
                    value = emergencyContactNumber,
                    onValueChange = { emergencyContactNumber = it },
                    placeholder = "Enter emergency contact number",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(20.dp))

                // ── Current Address Section ──
                SectionHeader("Current Address")

                Spacer(Modifier.height(8.dp))

                ContactInputField(
                    label = "Address Line",
                    value = currentAddressLine,
                    onValueChange = { currentAddressLine = it },
                    placeholder = "Enter street address",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    singleLine = false,
                    maxLines = 2
                )

                Spacer(Modifier.height(12.dp))

                ContactInputField(
                    label = "City",
                    value = currentCity,
                    onValueChange = { currentCity = it },
                    placeholder = "Enter city",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(12.dp))

                ContactInputField(
                    label = "State",
                    value = currentState,
                    onValueChange = { currentState = it },
                    placeholder = "Enter state",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(12.dp))

                ContactInputField(
                    label = "Country",
                    value = currentCountry,
                    onValueChange = { currentCountry = it },
                    placeholder = "Enter country",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(12.dp))

                ContactInputField(
                    label = "Postal Code",
                    value = currentPostalCode,
                    onValueChange = { currentPostalCode = it },
                    placeholder = "Enter postal code",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(16.dp))

                // ── Same as Current Address Toggle ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Permanent address same as current",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = sameAsCurrent,
                        onCheckedChange = { sameAsCurrent = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = Color.White
                        )
                    )
                }

                // ── Permanent Address Section (hidden when sameAsCurrent is true) ──
                if (!sameAsCurrent) {
                    Spacer(Modifier.height(20.dp))

                    SectionHeader("Permanent Address")

                    Spacer(Modifier.height(8.dp))

                    ContactInputField(
                        label = "Address Line",
                        value = permanentAddressLine,
                        onValueChange = { permanentAddressLine = it },
                        placeholder = "Enter street address",
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        singleLine = false,
                        maxLines = 2
                    )

                    Spacer(Modifier.height(12.dp))

                    ContactInputField(
                        label = "City",
                        value = permanentCity,
                        onValueChange = { permanentCity = it },
                        placeholder = "Enter city",
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )

                    Spacer(Modifier.height(12.dp))

                    ContactInputField(
                        label = "State",
                        value = permanentState,
                        onValueChange = { permanentState = it },
                        placeholder = "Enter state",
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )

                    Spacer(Modifier.height(12.dp))

                    ContactInputField(
                        label = "Country",
                        value = permanentCountry,
                        onValueChange = { permanentCountry = it },
                        placeholder = "Enter country",
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )

                    Spacer(Modifier.height(12.dp))

                    ContactInputField(
                        label = "Postal Code",
                        value = permanentPostalCode,
                        onValueChange = { permanentPostalCode = it },
                        placeholder = "Enter postal code",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                }

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
                                val result = profileRepository.updateContactInfo(
                                    alternateMobile = alternateMobile.ifBlank { null },
                                    whatsappNumber = whatsappNumber.ifBlank { null },
                                    emergencyContactName = emergencyContactName.ifBlank { null },
                                    emergencyContactRelationship = emergencyContactRelationship.ifBlank { null },
                                    emergencyContactNumber = emergencyContactNumber.ifBlank { null },
                                    currentAddressLine = currentAddressLine.ifBlank { null },
                                    currentCity = currentCity.ifBlank { null },
                                    currentState = currentState.ifBlank { null },
                                    currentCountry = currentCountry.ifBlank { null },
                                    currentPostalCode = currentPostalCode.ifBlank { null },
                                    sameAsCurrent = sameAsCurrent,
                                    permanentAddressLine = if (sameAsCurrent) null else permanentAddressLine.ifBlank { null },
                                    permanentCity = if (sameAsCurrent) null else permanentCity.ifBlank { null },
                                    permanentState = if (sameAsCurrent) null else permanentState.ifBlank { null },
                                    permanentCountry = if (sameAsCurrent) null else permanentCountry.ifBlank { null },
                                    permanentPostalCode = if (sameAsCurrent) null else permanentPostalCode.ifBlank { null }
                                )

                                when (result) {
                                    is ContactInfoOutcome.Success -> {
                                        successMessage = result.message
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun ReadOnlyContactField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value.ifEmpty { "Not provided" },
            fontSize = 15.sp,
            color = if (value.isEmpty()) Color.Gray else Color(0xFF424242)
        )
        Spacer(Modifier.height(8.dp))
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
            fontWeight = FontWeight.Medium,
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
