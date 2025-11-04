@file:OptIn(ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.profile.presentation

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
import com.hrms.jeejateamozy.feature.profile.data.PersonalInfoOutcome
import com.hrms.jeejateamozy.feature.profile.data.ProfileRepository
import kotlinx.coroutines.launch

/**
 * EditPersonalInfoScreen - Edit personal information
 * Shows all fields but only allows editing: blood_group, marital_status, no_of_family_members, languages
 */
@Composable
fun EditPersonalInfoScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context) }
    val scope = rememberCoroutineScope()

    // Read-only fields (displayed but not editable)
    var aliasName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }

    // Editable fields
    var bloodGroup by remember { mutableStateOf("") }
    var maritalStatus by remember { mutableStateOf("") }
    var noOfFamilyMembers by remember { mutableStateOf("") }
    var languagesText by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Fetch personal info on screen load
    LaunchedEffect(Unit) {
        isFetching = true
        when (val result = profileRepository.getPersonalInfo()) {
            is PersonalInfoOutcome.Success -> {
                result.personalInfo?.let { data ->
                    // Read-only fields
                    aliasName = data.alias_name ?: ""
                    gender = data.gender ?: ""
                    birthDate = data.birth_date ?: ""
                    nationality = data.nationality ?: ""
                    fatherName = data.father_name ?: ""

                    // Editable fields
                    bloodGroup = data.blood_group ?: ""
                    maritalStatus = data.marital_status ?: ""
                    noOfFamilyMembers = data.no_of_family_members?.toString() ?: ""
                    languagesText = data.languages?.joinToString(", ") ?: ""
                }
            }
            is PersonalInfoOutcome.Error -> {
                errorMessage = result.message
            }
        }
        isFetching = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Information", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isFetching) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error/Success messages
                    errorMessage?.let { msg ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
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
                                    tint = Color(0xFFD32F2F)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(msg, color = Color(0xFFD32F2F))
                            }
                        }
                    }

                    successMessage?.let { msg ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
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
                                    tint = Color(0xFF388E3C)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(msg, color = Color(0xFF388E3C))
                            }
                        }
                    }

                    // Section: Read-Only Fields
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Basic Information (HR Managed)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF757575)
                            )
                            Spacer(Modifier.height(12.dp))

                            ReadOnlyField("Alias Name", aliasName)
                            ReadOnlyField("Gender", gender)
                            ReadOnlyField("Date of Birth", birthDate)
                            ReadOnlyField("Nationality", nationality)
                            ReadOnlyField("Father's Name", fatherName)
                        }
                    }

                    // Section: Editable Fields
                    Text(
                        "Editable Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6200EE)
                    )

                    // Blood Group
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        label = { Text("Blood Group") },
                        placeholder = { Text("e.g., O+, A+, B+, AB+") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6200EE),
                            focusedLabelColor = Color(0xFF6200EE)
                        )
                    )

                    // Marital Status
                    OutlinedTextField(
                        value = maritalStatus,
                        onValueChange = { maritalStatus = it },
                        label = { Text("Marital Status") },
                        placeholder = { Text("e.g., Single, Married, Divorced") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6200EE),
                            focusedLabelColor = Color(0xFF6200EE)
                        )
                    )

                    // Number of Family Members
                    OutlinedTextField(
                        value = noOfFamilyMembers,
                        onValueChange = {
                            // Only allow numbers
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                noOfFamilyMembers = it
                            }
                        },
                        label = { Text("Number of Family Members") },
                        placeholder = { Text("e.g., 4") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6200EE),
                            focusedLabelColor = Color(0xFF6200EE)
                        )
                    )

                    // Languages
                    OutlinedTextField(
                        value = languagesText,
                        onValueChange = { languagesText = it },
                        label = { Text("Languages") },
                        placeholder = { Text("e.g., English, Hindi, Gujarati") },
                        supportingText = {
                            Text(
                                "Separate multiple languages with commas",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6200EE),
                            focusedLabelColor = Color(0xFF6200EE)
                        ),
                        minLines = 2
                    )

                    Spacer(Modifier.height(8.dp))

                    // Update Button
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                successMessage = null

                                try {
                                    // Parse languages from comma-separated text
                                    val languagesList = if (languagesText.isBlank()) {
                                        emptyList()
                                    } else {
                                        languagesText.split(",")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }
                                    }

                                    // Parse number of family members
                                    val familyMembersInt = if (noOfFamilyMembers.isBlank()) {
                                        null
                                    } else {
                                        noOfFamilyMembers.toIntOrNull()
                                    }

                                    val result = profileRepository.updatePersonalInfo(
                                        bloodGroup = bloodGroup.ifBlank { null },
                                        maritalStatus = maritalStatus.ifBlank { null },
                                        noOfFamilyMembers = familyMembersInt,
                                        languages = if (languagesList.isEmpty()) null else languagesList
                                    )

                                    when (result) {
                                        is PersonalInfoOutcome.Success -> {
                                            successMessage = result.message
                                            // Update fields with returned data
                                            result.personalInfo?.let { data ->
                                                // Read-only fields
                                                aliasName = data.alias_name ?: ""
                                                gender = data.gender ?: ""
                                                birthDate = data.birth_date ?: ""
                                                nationality = data.nationality ?: ""
                                                fatherName = data.father_name ?: ""

                                                // Editable fields
                                                bloodGroup = data.blood_group ?: ""
                                                maritalStatus = data.marital_status ?: ""
                                                noOfFamilyMembers = data.no_of_family_members?.toString() ?: ""
                                                languagesText = data.languages?.joinToString(", ") ?: ""
                                            }
                                        }
                                        is PersonalInfoOutcome.Error -> {
                                            errorMessage = result.message
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "An error occurred"
                                }

                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EE)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Update Personal Information", fontSize = 16.sp)
                        }
                    }

                    // Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF8E1)
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
                                tint = Color(0xFFF57C00)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Only Blood Group, Marital Status, Number of Family Members, and Languages can be updated. Other fields are managed by HR.",
                                color = Color(0xFFF57C00),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyField(label: String, value: String) {
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