@file:OptIn(ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.feature.profile.data.PersonalInfoOutcome
import com.hrms.jeejateamozy.feature.profile.data.ProfileRepository

/**
 * ViewPersonalInfoScreen - Display personal information (read-only)
 * Shows all personal info fields from the API
 */
@Composable
fun ViewPersonalInfoScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context) }

    // State variables for all personal info fields
    var aliasName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var maritalStatus by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var noOfFamilyMembers by remember { mutableStateOf("") }
    var languages by remember { mutableStateOf<List<String>>(emptyList()) }

    var isFetching by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch personal info on screen load
    LaunchedEffect(Unit) {
        isFetching = true
        when (val result = profileRepository.getPersonalInfo()) {
            is PersonalInfoOutcome.Success -> {
                result.personalInfo?.let { data ->
                    aliasName = data.alias_name ?: ""
                    gender = data.gender ?: ""
                    birthDate = data.birth_date ?: ""
                    nationality = data.nationality ?: ""
                    bloodGroup = data.blood_group ?: ""
                    maritalStatus = data.marital_status ?: ""
                    fatherName = data.father_name ?: ""
                    noOfFamilyMembers = data.no_of_family_members?.toString() ?: ""
                    languages = data.languages ?: emptyList()
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
                        // ✅ FIX: Use navigationBarsPadding() instead of hardcoded padding(bottom = 80.dp)
                        // This adapts automatically to both gesture navigation and 3-button navigation
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error message
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

                    // Personal Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Personal Details",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6200EE)
                            )

                            HorizontalDivider()

                            DetailRow("Alias Name", aliasName)
                            DetailRow("Gender", gender)
                            DetailRow("Date of Birth", birthDate)
                            DetailRow("Nationality", nationality)
                            DetailRow("Blood Group", bloodGroup)
                            DetailRow("Marital Status", maritalStatus)
                            DetailRow("Father's Name", fatherName)
                            DetailRow("Number of Family Members", noOfFamilyMembers)

                            // Languages
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Languages",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(4.dp))
                                if (languages.isEmpty()) {
                                    Text(
                                        text = "Not provided",
                                        fontSize = 16.sp,
                                        color = Color.Gray
                                    )
                                } else {
                                    Text(
                                        text = languages.joinToString(", "),
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }

                    // Info note
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
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
                                tint = Color(0xFF757575)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Most personal details are managed by HR. Only specific fields can be updated.",
                                color = Color(0xFF757575),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
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
            fontSize = 16.sp,
            color = if (value.isEmpty()) Color.Gray else Color.Black
        )
    }
}