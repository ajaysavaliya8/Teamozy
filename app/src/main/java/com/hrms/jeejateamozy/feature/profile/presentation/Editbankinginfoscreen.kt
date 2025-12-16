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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.feature.profile.data.BankingInfoOutcome
import com.hrms.jeejateamozy.feature.profile.data.ProfileRepository
import kotlinx.coroutines.launch

/**
 * EditBankingInfoScreen - Edit banking information
 * All 5 fields are editable: account_holder_name, bank_name, bank_account_number, account_type, ifsc_code
 */
@Composable
fun EditBankingInfoScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context) }
    val scope = rememberCoroutineScope()

    // State variables for all banking info fields
    var accountHolderName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankAccountNumber by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Dropdown state
    var showAccountTypeMenu by remember { mutableStateOf(false) }

    // Fetch banking info on screen load
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

                    // Account Holder Name
                    OutlinedTextField(
                        value = accountHolderName,
                        onValueChange = { accountHolderName = it },
                        label = { Text("Account Holder Name") },
                        placeholder = { Text("Enter full name as per bank") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6200EE),
                            focusedLabelColor = Color(0xFF6200EE)
                        )
                    )

                    // Bank Name
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name") },
                        placeholder = { Text("e.g., State Bank of India") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6200EE),
                            focusedLabelColor = Color(0xFF6200EE)
                        )
                    )

                    // Bank Account Number
                    OutlinedTextField(
                        value = bankAccountNumber,
                        onValueChange = {
                            // Only allow numbers
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                bankAccountNumber = it
                            }
                        },
                        label = { Text("Bank Account Number") },
                        placeholder = { Text("Enter account number") },
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

                    // Account Type (Dropdown)
                    ExposedDropdownMenuBox(
                        expanded = showAccountTypeMenu,
                        onExpandedChange = { showAccountTypeMenu = it }
                    ) {
                        OutlinedTextField(
                            value = accountType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Account Type") },
                            placeholder = { Text("Select account type") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountTypeMenu)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6200EE),
                                focusedLabelColor = Color(0xFF6200EE)
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = showAccountTypeMenu,
                            onDismissRequest = { showAccountTypeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("SAVING") },
                                onClick = {
                                    accountType = "SAVING"
                                    showAccountTypeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("CURRENT") },
                                onClick = {
                                    accountType = "CURRENT"
                                    showAccountTypeMenu = false
                                }
                            )
                        }
                    }

                    // IFSC Code
                    OutlinedTextField(
                        value = ifscCode,
                        onValueChange = {
                            // Convert to uppercase and allow only alphanumeric
                            ifscCode = it.uppercase().filter { char -> char.isLetterOrDigit() }
                        },
                        label = { Text("IFSC Code") },
                        placeholder = { Text("e.g., SBIN0001234") },
                        supportingText = {
                            Text(
                                "11 characters (4 letters + 7 digits)",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6200EE),
                            focusedLabelColor = Color(0xFF6200EE)
                        )
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
                                    val result = profileRepository.updateBankingInfo(
                                        accountHolderName = accountHolderName.ifBlank { null },
                                        bankName = bankName.ifBlank { null },
                                        bankAccountNumber = bankAccountNumber.ifBlank { null },
                                        accountType = accountType.ifBlank { null },
                                        ifscCode = ifscCode.ifBlank { null }
                                    )

                                    when (result) {
                                        is BankingInfoOutcome.Success -> {
                                            successMessage = result.message
                                            // Update fields with returned data
                                            result.bankingInfo?.let { data ->
                                                accountHolderName = data.account_holder_name ?: ""
                                                bankName = data.bank_name ?: ""
                                                bankAccountNumber = data.bank_account_number ?: ""
                                                accountType = data.account_type ?: ""
                                                ifscCode = data.ifsc_code ?: ""
                                            }
                                        }
                                        is BankingInfoOutcome.Error -> {
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
                            Text("Update Banking Information", fontSize = 16.sp)
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
                                "Keep your banking information up to date for salary processing and reimbursements.",
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