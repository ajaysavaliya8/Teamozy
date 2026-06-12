@file:OptIn(ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.profile.presentation

import androidx.compose.foundation.background
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
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import com.hrms.jeejateamozy.core.ui.LightStatusBarIcons
import com.hrms.jeejateamozy.feature.profile.data.EmploymentDetailOutcome
import com.hrms.jeejateamozy.feature.profile.data.ProfileRepository

/**
 * ViewEmploymentDetailScreen - Display employment details (read-only)
 * Shows all employment information from the API
 */
@Composable
fun ViewEmploymentDetailScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context) }

    // State variables for all employment detail fields
    var joiningDate by remember { mutableStateOf("") }
    var probationEndDate by remember { mutableStateOf("") }
    var confirmationDate by remember { mutableStateOf("") }
    var workLocation by remember { mutableStateOf("") }
    var employmentType by remember { mutableStateOf("") }
    var workMode by remember { mutableStateOf("") }
    var designationName by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }
    var departmentName by remember { mutableStateOf("") }
    var subDepartmentName by remember { mutableStateOf("") }
    var shiftName by remember { mutableStateOf("") }

    var isFetching by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch employment details on screen load
    LaunchedEffect(Unit) {
        isFetching = true
        when (val result = profileRepository.getEmploymentDetails()) {
            is EmploymentDetailOutcome.Success -> {
                result.employmentDetail?.let { data ->
                    joiningDate = data.joining_date ?: ""
                    probationEndDate = data.probation_end_date ?: ""
                    confirmationDate = data.confirmation_date ?: ""
                    workLocation = data.work_location ?: ""
                    employmentType = data.employment_type ?: ""
                    workMode = data.work_mode ?: ""
                    designationName = data.designation_name ?: ""
                    branchName = data.branch_name ?: ""
                    departmentName = data.department_name ?: ""
                    subDepartmentName = data.sub_department_name ?: ""
                    shiftName = data.shift_name ?: ""
                }
            }
            is EmploymentDetailOutcome.Error -> {
                errorMessage = result.message
            }
        }
        isFetching = false
    }

    LightStatusBarIcons()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employment Details", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TeamozyColors.AppBar,
                    titleContentColor = TeamozyColors.OnAppBar,
                    navigationIconContentColor = TeamozyColors.OnAppBar
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TeamozyColors.Background)
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
                        .padding(bottom = 80.dp),  // Extra padding for nav bar
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error message
                    errorMessage?.let { msg ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
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
                                    tint = Color(0xFFEF4444)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(msg, color = Color(0xFFEF4444))
                            }
                        }
                    }

                    // Employment Details Card
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
                                "Employment Information",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TeamozyColors.Primary
                            )

                            HorizontalDivider()

                            DetailRow("Joining Date", joiningDate)
                            DetailRow("Probation End Date", probationEndDate)
                            DetailRow("Confirmation Date", confirmationDate)
                            DetailRow("Work Location", workLocation)
                            DetailRow("Employment Type", formatEmploymentType(employmentType))
                            DetailRow("Work Mode", formatEmploymentType(workMode))
                            DetailRow("Designation", designationName)
                            DetailRow("Branch", branchName)
                            DetailRow("Department", departmentName)
                            DetailRow("Sub Department", subDepartmentName)
                            DetailRow("Shift", shiftName)
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
                                "Employment details are managed by HR and cannot be edited.",
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

/**
 * Format employment type for display
 * Converts "FULL_TIME" to "Full Time"
 */
private fun formatEmploymentType(type: String): String {
    return when (type.uppercase()) {
        "FULL_TIME" -> "Full Time"
        "PART_TIME" -> "Part Time"
        "CONTRACT" -> "Contract"
        "INTERN" -> "Intern"
        "TEMPORARY" -> "Temporary"
        else -> type.replace("_", " ").lowercase()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}