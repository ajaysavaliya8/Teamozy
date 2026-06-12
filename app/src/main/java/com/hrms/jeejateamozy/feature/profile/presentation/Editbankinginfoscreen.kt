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
import com.hrms.jeejateamozy.feature.profile.data.BankingInfoOutcome
import com.hrms.jeejateamozy.feature.profile.data.ProfileRepository

/**
 * EditBankingInfoScreen - View banking information (read-only)
 */
@Composable
fun EditBankingInfoScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context) }

    var accountHolderName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankAccountNumber by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var bankVerified by remember { mutableStateOf<Boolean?>(null) }

    var isFetching by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isFetching = true
        when (val result = profileRepository.getBankingInfo()) {
            is BankingInfoOutcome.Success -> {
                result.bankingInfo?.let { data ->
                    accountHolderName = data.account_holder_name ?: ""
                    bankName = data.bank_name ?: ""
                    bankAccountNumber = data.bank_account_number ?: ""
                    accountType = data.account_type ?: ""
                    ifscCode = data.ifsc_code ?: ""
                    branchName = data.branch_name ?: ""
                    upiId = data.upi_id ?: ""
                    bankVerified = data.bank_verified
                }
            }
            is BankingInfoOutcome.Error -> {
                errorMessage = result.message
            }
        }
        isFetching = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Banking Information", fontSize = 18.sp) },
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
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                                "Banking Information",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TeamozyColors.Primary
                            )

                            HorizontalDivider()

                            BankingDetailRow("Account Holder Name", accountHolderName)
                            BankingDetailRow("Bank Name", bankName)
                            BankingDetailRow("Bank Account Number", bankAccountNumber)
                            BankingDetailRow("Account Type", accountType)
                            BankingDetailRow("IFSC Code", ifscCode)
                            BankingDetailRow("Branch Name", branchName)
                            BankingDetailRow("UPI ID", upiId)
                        }
                    }

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
                                "Banking details are managed by HR and cannot be edited.",
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
private fun BankingDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value.ifBlank { "-" },
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Normal
        )
    }
}
