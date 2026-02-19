@file:OptIn(ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 *
 * Read-only fields (HR Managed): full_name, first_name, last_name, gender,
 *   date_of_birth, nationality, father_name, mother_name
 *
 * Editable fields: blood_group, marital_status, spouse_name, no_of_children,
 *   languages (comma-separated), religion
 */
@Composable
fun EditPersonalInfoScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context) }
    val scope = rememberCoroutineScope()

    // Read-only fields (displayed but not editable)
    var fullName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }

    // Editable fields
    var bloodGroup by remember { mutableStateOf("") }
    var maritalStatus by remember { mutableStateOf("") }
    var spouseName by remember { mutableStateOf("") }
    var noOfChildren by remember { mutableStateOf("") }
    var languagesText by remember { mutableStateOf("") }
    var religion by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Helper to populate fields from PersonalInfoData
    fun populateFields(result: PersonalInfoOutcome.Success) {
        result.personalInfo?.let { data ->
            // Read-only fields
            fullName = data.full_name ?: ""
            firstName = data.first_name ?: ""
            lastName = data.last_name ?: ""
            gender = data.gender ?: ""
            dateOfBirth = data.date_of_birth ?: ""
            nationality = data.nationality ?: ""
            fatherName = data.father_name ?: ""
            motherName = data.mother_name ?: ""

            // Editable fields
            bloodGroup = data.blood_group ?: ""
            maritalStatus = data.marital_status ?: ""
            spouseName = data.spouse_name ?: ""
            noOfChildren = data.no_of_children?.toString() ?: ""
            languagesText = data.languages?.joinToString(", ") ?: ""
            religion = data.religion ?: ""
        }
    }

    // Fetch personal info on screen load
    LaunchedEffect(Unit) {
        isFetching = true
        when (val result = profileRepository.getPersonalInfo()) {
            is PersonalInfoOutcome.Success -> {
                populateFields(result)
            }
            is PersonalInfoOutcome.Error -> {
                errorMessage = result.message
            }
        }
        isFetching = false
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedContainerColor = MaterialTheme.colorScheme.surface
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Edit Personal Info",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isFetching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null && fullName.isEmpty() -> {
                    // Full-screen error state (no data loaded)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = errorMessage ?: "Failed to load personal information",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                else -> {
                    // Content State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                            .padding(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Error message (inline, after data loaded)
                        errorMessage?.let { msg ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
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
                                        msg,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Success message
                        successMessage?.let { msg ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        msg,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Section: Read-Only Fields (HR Managed)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Basic Information",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(16.dp))

                                InfoRow("Full Name", fullName.ifEmpty { "Not provided" })
                                Spacer(Modifier.height(12.dp))
                                InfoRow("First Name", firstName.ifEmpty { "Not provided" })
                                Spacer(Modifier.height(12.dp))
                                InfoRow("Last Name", lastName.ifEmpty { "Not provided" })
                                Spacer(Modifier.height(12.dp))
                                InfoRow("Gender", gender.ifEmpty { "Not provided" })
                                Spacer(Modifier.height(12.dp))
                                InfoRow("Date of Birth", dateOfBirth.ifEmpty { "Not provided" })
                                Spacer(Modifier.height(12.dp))
                                InfoRow("Nationality", nationality.ifEmpty { "Not provided" })
                                Spacer(Modifier.height(12.dp))
                                InfoRow("Father's Name", fatherName.ifEmpty { "Not provided" })
                                Spacer(Modifier.height(12.dp))
                                InfoRow("Mother's Name", motherName.ifEmpty { "Not provided" })

                                Spacer(Modifier.height(12.dp))

                                // Managed by HR notice
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Managed by HR",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Section: Editable Fields
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Update Information",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Blood Group
                                OutlinedTextField(
                                    value = bloodGroup,
                                    onValueChange = { bloodGroup = it },
                                    label = { Text("Blood Group") },
                                    placeholder = { Text("e.g., O+, A+, B+, AB+") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Marital Status
                                OutlinedTextField(
                                    value = maritalStatus,
                                    onValueChange = { maritalStatus = it },
                                    label = { Text("Marital Status") },
                                    placeholder = { Text("e.g., Single, Married, Divorced") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Spouse Name
                                OutlinedTextField(
                                    value = spouseName,
                                    onValueChange = { spouseName = it },
                                    label = { Text("Spouse Name") },
                                    placeholder = { Text("Enter spouse name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Number of Children
                                OutlinedTextField(
                                    value = noOfChildren,
                                    onValueChange = {
                                        // Only allow digits
                                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                            noOfChildren = it
                                        }
                                    },
                                    label = { Text("Number of Children") },
                                    placeholder = { Text("e.g., 2") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(12.dp)
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 2
                                )

                                // Religion
                                OutlinedTextField(
                                    value = religion,
                                    onValueChange = { religion = it },
                                    label = { Text("Religion") },
                                    placeholder = { Text("e.g., Hindu, Muslim, Christian") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

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
                                            null
                                        } else {
                                            languagesText.split(",")
                                                .map { it.trim() }
                                                .filter { it.isNotEmpty() }
                                                .ifEmpty { null }
                                        }

                                        // Parse number of children
                                        val childrenInt = if (noOfChildren.isBlank()) {
                                            null
                                        } else {
                                            noOfChildren.toIntOrNull()
                                        }

                                        val result = profileRepository.updatePersonalInfo(
                                            bloodGroup = bloodGroup.ifBlank { null },
                                            maritalStatus = maritalStatus.ifBlank { null },
                                            spouseName = spouseName.ifBlank { null },
                                            noOfChildren = childrenInt,
                                            languages = languagesList,
                                            religion = religion.ifBlank { null }
                                        )

                                        when (result) {
                                            is PersonalInfoOutcome.Success -> {
                                                successMessage = result.message

                                                // API doesn't return data on update, so re-fetch
                                                when (val fetchResult = profileRepository.getPersonalInfo()) {
                                                    is PersonalInfoOutcome.Success -> {
                                                        populateFields(fetchResult)
                                                    }
                                                    is PersonalInfoOutcome.Error -> {
                                                        // Silently ignore re-fetch error;
                                                        // the update was successful
                                                    }
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
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text("Update Personal Information", fontSize = 16.sp)
                            }
                        }

                        // Bottom Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Only Blood Group, Marital Status, Spouse Name, Number of Children, Languages, and Religion can be updated. Other fields are managed by HR.",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
