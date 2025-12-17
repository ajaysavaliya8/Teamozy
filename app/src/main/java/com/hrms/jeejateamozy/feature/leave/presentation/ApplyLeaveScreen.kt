package com.hrms.jeejateamozy.feature.leave.presentation

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.core.network.LeaveType
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * ✅ REWRITTEN: Apply Leave Screen
 *
 * Features:
 * - Comprehensive form validation
 * - Dynamic leave type loading
 * - Date range selection with validation
 * - File attachment support
 * - Task dependency tracking
 * - Real-time form state validation
 * - Proper error handling
 * - Material Design 3 UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLeaveScreen(
    viewModel: LeaveViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (Int) -> Unit
) {
    val uiState by viewModel.applyLeaveUiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ==========================================
    // Form State
    // ==========================================
    var selectedLeaveType by remember { mutableStateOf<LeaveType?>(null) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var leaveReason by remember { mutableStateOf("") }
    var alternateContact by remember { mutableStateOf("") }
    var taskDependedOnYou by remember { mutableStateOf(false) }
    var dependencyHandledBy by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    // ==========================================
    // Validation State
    // ==========================================
    var showLeaveTypeError by remember { mutableStateOf(false) }
    var showStartDateError by remember { mutableStateOf(false) }
    var showEndDateError by remember { mutableStateOf(false) }
    var showReasonError by remember { mutableStateOf(false) }
    var showDependencyError by remember { mutableStateOf(false) }
    var reasonErrorMessage by remember { mutableStateOf("") }
    var dateErrorMessage by remember { mutableStateOf("") }

    // ==========================================
    // Dialog State
    // ==========================================
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // ==========================================
    // BackHandler for gesture navigation
    // ==========================================
    BackHandler {
        onNavigateBack()
    }

    // ==========================================
    // File Picker Launcher
    // ==========================================
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = getFileName(context, it)
        }
    }

    // ==========================================
    // Load Leave Types
    // ==========================================
    LaunchedEffect(Unit) {
        viewModel.loadLeaveTypes()
    }

    // ==========================================
    // Handle Events
    // ==========================================
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LeaveEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is LeaveEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is LeaveEvent.NavigateToHistory -> {
                    // Small delay to show success message
                    kotlinx.coroutines.delay(500)
                    onNavigateToHistory(event.applicationId)
                }
            }
        }
    }

    // ==========================================
    // Validation Function
    // ==========================================
    fun validateForm(): Boolean {
        var isValid = true

        // Reset errors
        showLeaveTypeError = false
        showStartDateError = false
        showEndDateError = false
        showReasonError = false
        showDependencyError = false
        reasonErrorMessage = ""
        dateErrorMessage = ""

        // Validate leave type
        if (selectedLeaveType == null) {
            showLeaveTypeError = true
            isValid = false
        }

        // Validate start date
        if (startDate == null) {
            showStartDateError = true
            dateErrorMessage = "Start date is required"
            isValid = false
        } else if (startDate!! < LocalDate.now()) {
            showStartDateError = true
            dateErrorMessage = "Start date cannot be in the past"
            isValid = false
        }

        // Validate end date
        if (endDate == null) {
            showEndDateError = true
            dateErrorMessage = "End date is required"
            isValid = false
        } else if (startDate != null && endDate!! < startDate!!) {
            showEndDateError = true
            dateErrorMessage = "End date cannot be before start date"
            isValid = false
        }

        // Validate leave reason
        val trimmedReason = leaveReason.trim()
        when {
            trimmedReason.isEmpty() -> {
                showReasonError = true
                reasonErrorMessage = "Leave reason is required"
                isValid = false
            }
            trimmedReason.length < 10 -> {
                showReasonError = true
                reasonErrorMessage = "Reason must be at least 10 characters (currently ${trimmedReason.length})"
                isValid = false
            }
            trimmedReason.length > 500 -> {
                showReasonError = true
                reasonErrorMessage = "Reason must not exceed 500 characters (currently ${trimmedReason.length})"
                isValid = false
            }
        }

        // Validate dependency
        if (taskDependedOnYou && dependencyHandledBy.trim().isEmpty()) {
            showDependencyError = true
            isValid = false
        }

        return isValid
    }

    // ==========================================
    // Submit Handler
    // ==========================================
    fun submitLeaveApplication() {
        if (!validateForm()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Please fix all validation errors before submitting",
                    duration = SnackbarDuration.Short
                )
            }
            return
        }

        // Convert URI to File if selected
        val file = selectedFileUri?.let { uri ->
            copyUriToTempFile(context, uri)
        }

        // Submit the application
        viewModel.applyLeave(
            leaveTypeId = selectedLeaveType!!.id,
            startDate = startDate!!.format(DateTimeFormatter.ISO_DATE),
            endDate = endDate!!.format(DateTimeFormatter.ISO_DATE),
            leaveReason = leaveReason.trim(),
            alternateContact = alternateContact.trim().ifBlank { null },
            taskDependedOnYou = taskDependedOnYou,
            dependencyHandledBy = dependencyHandledBy.trim().ifBlank { null },
            supportingDocumentFile = file
        )
    }

    // ==========================================
    // UI Scaffold
    // ==========================================
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Apply for Leave") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
            // ==========================================
            // Show Empty State if no leave types available
            // ==========================================
            when {
                // Loading state
                uiState.isLoading && uiState.leaveTypes.isEmpty() -> {
                    LoadingState()
                }
                // Empty state - no leave types available
                !uiState.isLoading && uiState.leaveTypes.isEmpty() -> {
                    NoLeaveTypesAvailable(onNavigateBack = onNavigateBack)
                }
                // Show form when leave types are available
                else -> {
                    LeaveApplicationForm(
                        uiState = uiState,
                        selectedLeaveType = selectedLeaveType,
                        startDate = startDate,
                        endDate = endDate,
                        leaveReason = leaveReason,
                        alternateContact = alternateContact,
                        taskDependedOnYou = taskDependedOnYou,
                        dependencyHandledBy = dependencyHandledBy,
                        selectedFileName = selectedFileName,
                        showLeaveTypeError = showLeaveTypeError,
                        showStartDateError = showStartDateError,
                        showEndDateError = showEndDateError,
                        showReasonError = showReasonError,
                        showDependencyError = showDependencyError,
                        reasonErrorMessage = reasonErrorMessage,
                        dateErrorMessage = dateErrorMessage,
                        onLeaveTypeSelected = {
                            selectedLeaveType = it
                            showLeaveTypeError = false
                        },
                        onStartDateClick = { showStartDatePicker = true },
                        onEndDateClick = { showEndDatePicker = true },
                        onReasonChange = {
                            leaveReason = it
                            showReasonError = false
                        },
                        onContactChange = { alternateContact = it },
                        onTaskDependencyChange = {
                            taskDependedOnYou = it
                            showDependencyError = false
                        },
                        onDependencyHandledByChange = {
                            dependencyHandledBy = it
                            showDependencyError = false
                        },
                        onPickFile = { filePickerLauncher.launch("*/*") },
                        onRemoveFile = {
                            selectedFileUri = null
                            selectedFileName = null
                        },
                        onSubmit = { submitLeaveApplication() }
                    )
                }
            }
        }
    }

    // ==========================================
    // Date Picker Dialogs
    // ==========================================
    if (showStartDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                startDate = date
                showStartDatePicker = false
                showStartDateError = false
                // Auto-set end date if not set or invalid
                if (endDate == null || endDate!! < date) {
                    endDate = date
                }
            },
            onDismiss = { showStartDatePicker = false },
            minDate = LocalDate.now()
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                endDate = date
                showEndDatePicker = false
                showEndDateError = false
            },
            onDismiss = { showEndDatePicker = false },
            minDate = startDate ?: LocalDate.now()
        )
    }
}

// ==========================================
// LOADING STATE
// ==========================================
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Loading leave types...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==========================================
// NO LEAVE TYPES AVAILABLE - EMPTY STATE
// ==========================================
@Composable
private fun NoLeaveTypesAvailable(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            modifier = Modifier.size(120.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "No Leave Types Available",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text(
            text = "There are currently no leave types configured for your account. Please contact your HR department or administrator for assistance.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Go Back")
            }

            OutlinedButton(
                onClick = {
                    // You can add intent to open email or contact HR
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contact HR")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Why am I seeing this?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Leave types are configured by your organization. This could be due to your role, department, or employment status.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==========================================
// LEAVE APPLICATION FORM
// ==========================================
@Composable
private fun LeaveApplicationForm(
    uiState: ApplyLeaveUiState,
    selectedLeaveType: LeaveType?,
    startDate: LocalDate?,
    endDate: LocalDate?,
    leaveReason: String,
    alternateContact: String,
    taskDependedOnYou: Boolean,
    dependencyHandledBy: String,
    selectedFileName: String?,
    showLeaveTypeError: Boolean,
    showStartDateError: Boolean,
    showEndDateError: Boolean,
    showReasonError: Boolean,
    showDependencyError: Boolean,
    reasonErrorMessage: String,
    dateErrorMessage: String,
    onLeaveTypeSelected: (LeaveType) -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onReasonChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onTaskDependencyChange: (Boolean) -> Unit,
    onDependencyHandledByChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    onSubmit: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 70.dp  // ✅ UPDATED: 70dp space from bottom
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ==========================================
            // Section: Leave Type Selection
            // ==========================================
            item {
                LeaveTypeSection(
                    leaveTypes = uiState.leaveTypes,
                    selectedLeaveType = selectedLeaveType,
                    onLeaveTypeSelected = onLeaveTypeSelected,
                    showError = showLeaveTypeError,
                    isLoading = false // Already filtered out in parent
                )
            }

            // ==========================================
            // Section: Date Range Selection
            // ==========================================
            item {
                DateRangeSection(
                    startDate = startDate,
                    endDate = endDate,
                    onStartDateClick = onStartDateClick,
                    onEndDateClick = onEndDateClick,
                    showError = showStartDateError || showEndDateError,
                    errorMessage = dateErrorMessage
                )
            }

            // ==========================================
            // Section: Leave Reason
            // ==========================================
            item {
                LeaveReasonSection(
                    leaveReason = leaveReason,
                    onReasonChange = onReasonChange,
                    showError = showReasonError,
                    errorMessage = reasonErrorMessage
                )
            }

            // ==========================================
            // Section: Alternate Contact
            // ==========================================
            item {
                AlternateContactSection(
                    alternateContact = alternateContact,
                    onContactChange = onContactChange
                )
            }

            // ==========================================
            // Section: Task Dependency
            // ==========================================
            item {
                TaskDependencySection(
                    taskDependedOnYou = taskDependedOnYou,
                    dependencyHandledBy = dependencyHandledBy,
                    onTaskDependencyChange = onTaskDependencyChange,
                    onDependencyHandledByChange = onDependencyHandledByChange,
                    showError = showDependencyError
                )
            }

            // ==========================================
            // Section: Supporting Document
            // ==========================================
            item {
                SupportingDocumentSection(
                    selectedFileName = selectedFileName,
                    onPickFile = onPickFile,
                    onRemoveFile = onRemoveFile
                )
            }

            // ==========================================
            // Submit Button
            // ==========================================
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Submitting...", fontSize = 16.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Submit Leave Application",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ==========================================
        // Loading Overlay
        // ==========================================
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Processing your application...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION: Leave Type Selection (Dropdown)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveTypeSection(
    leaveTypes: List<LeaveType>,
    selectedLeaveType: LeaveType?,
    onLeaveTypeSelected: (LeaveType) -> Unit,
    showError: Boolean,
    isLoading: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Leave Type *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            leaveTypes.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "No leave types available",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            else -> {
                // Dropdown Menu
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedLeaveType?.leaveTypeName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Leave Type") },
                        placeholder = { Text("Choose from available options") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        isError = showError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = if (selectedLeaveType != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        leaveTypes.forEach { leaveType ->
                            DropdownMenuItem(
                                text = {
                                    LeaveTypeDropdownItem(leaveType = leaveType)
                                },
                                onClick = {
                                    onLeaveTypeSelected(leaveType)
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (leaveType.isPaid)
                                            Icons.Default.CheckCircle
                                        else
                                            Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (leaveType.isPaid)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Show selected leave type details
                if (selectedLeaveType != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectedLeaveTypeCard(leaveType = selectedLeaveType)
                }
            }
        }

        if (showError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Please select a leave type",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LeaveTypeDropdownItem(leaveType: LeaveType) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = leaveType.leaveTypeName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (!leaveType.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = leaveType.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(
                text = if (leaveType.isPaid) "Paid" else "Unpaid",
                color = if (leaveType.isPaid)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                icon = if (leaveType.isPaid)
                    Icons.Default.CheckCircle
                else
                    Icons.Default.Cancel
            )

            if (leaveType.requiresApproval) {
                StatusChip(
                    text = "Approval Required",
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Default.AccountCircle
                )
            }
        }
    }
}

@Composable
private fun SelectedLeaveTypeCard(leaveType: LeaveType) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Selected: ${leaveType.leaveTypeName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!leaveType.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = leaveType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(
                        text = if (leaveType.isPaid) "Paid" else "Unpaid",
                        color = if (leaveType.isPaid)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                        icon = if (leaveType.isPaid)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.Cancel
                    )

                    if (leaveType.requiresApproval) {
                        StatusChip(
                            text = "Approval Required",
                            color = MaterialTheme.colorScheme.tertiary,
                            icon = Icons.Default.AccountCircle
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==========================================
// SECTION: Date Range Selection
// ==========================================
@Composable
private fun DateRangeSection(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    showError: Boolean,
    errorMessage: String
) {
    Column {
        Text(
            text = "Leave Duration *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DateField(
                label = "Start Date",
                date = startDate,
                onClick = onStartDateClick,
                modifier = Modifier.weight(1f),
                showError = showError
            )
            DateField(
                label = "End Date",
                date = endDate,
                onClick = onEndDateClick,
                modifier = Modifier.weight(1f),
                showError = showError
            )
        }

        // Show number of days if both dates selected
        if (startDate != null && endDate != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Days:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    val days = ChronoUnit.DAYS.between(startDate, endDate) + 1
                    Text(
                        text = "$days ${if (days == 1L) "day" else "days"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showError: Boolean = false
) {
    OutlinedCard(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (showError)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = if (date != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = date?.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    ?: "Select date",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (date != null) FontWeight.Medium else FontWeight.Normal,
                color = if (date != null)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==========================================
// SECTION: Leave Reason
// ==========================================
@Composable
private fun LeaveReasonSection(
    leaveReason: String,
    onReasonChange: (String) -> Unit,
    showError: Boolean,
    errorMessage: String
) {
    Column {
        Text(
            text = "Leave Reason *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = leaveReason,
            onValueChange = onReasonChange,
            label = { Text("Describe your reason for leave") },
            placeholder = { Text("Enter detailed reason (min 10 characters)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(12.dp),
            isError = showError,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (showError) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Minimum 10 characters required",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${leaveReason.length}/500",
                        color = if (leaveReason.length > 500)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

// ==========================================
// SECTION: Alternate Contact
// ==========================================
@Composable
private fun AlternateContactSection(
    alternateContact: String,
    onContactChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Alternate Contact",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = alternateContact,
            onValueChange = onContactChange,
            label = { Text("Phone Number or Email") },
            placeholder = { Text("Optional - for urgent matters") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null
                )
            }
        )
    }
}

// ==========================================
// SECTION: Task Dependency
// ==========================================
@Composable
private fun TaskDependencySection(
    taskDependedOnYou: Boolean,
    dependencyHandledBy: String,
    onTaskDependencyChange: (Boolean) -> Unit,
    onDependencyHandledByChange: (String) -> Unit,
    showError: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (taskDependedOnYou)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Any task dependent on you?",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Will someone cover your responsibilities?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = taskDependedOnYou,
                    onCheckedChange = onTaskDependencyChange
                )
            }

            if (taskDependedOnYou) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = dependencyHandledBy,
                    onValueChange = onDependencyHandledByChange,
                    label = { Text("Who will handle it? *") },
                    placeholder = { Text("Enter name or department") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("This field is required when tasks are dependent on you") }
                    } else null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

// ==========================================
// SECTION: Supporting Document
// ==========================================
@Composable
private fun SupportingDocumentSection(
    selectedFileName: String?,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit
) {
    Column {
        Text(
            text = "Supporting Document",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (selectedFileName != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedFileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Attached",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onRemoveFile) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove file",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = onPickFile,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Attach Supporting Document",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Optional - Medical certificate, etc.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==========================================
// DATE PICKER DIALOG
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column {
                DatePicker(
                    state = datePickerState,
                    title = {
                        Text(
                            text = "Select Date",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = java.time.Instant
                                    .ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()

                                // Validate min date
                                if (minDate == null || date >= minDate) {
                                    onDateSelected(date)
                                }
                            }
                        },
                        enabled = datePickerState.selectedDateMillis != null
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

// ==========================================
// HELPER FUNCTIONS
// ==========================================

/**
 * Get file name from URI
 */
private fun getFileName(context: Context, uri: Uri): String {
    var name = "Unknown"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name
}

/**
 * Copy URI to temporary file for upload
 */
private fun copyUriToTempFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = getFileName(context, uri)
        val tempFile = File(context.cacheDir, fileName)

        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()

        tempFile
    } catch (e: Exception) {
        null
    }
}