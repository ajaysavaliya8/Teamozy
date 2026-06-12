package com.hrms.jeejateamozy.feature.leave.presentation

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import com.hrms.jeejateamozy.core.network.LeaveType
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * âœ… IMPROVED: Apply Leave Screen with Debug Logging
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
    var emergencyContact by remember { mutableStateOf("") }
    var taskDependedOnYou by remember { mutableStateOf(false) }
    var dependencyHandledBy by remember { mutableStateOf("") }
    var handoverNotes by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    // Priority
    var selectedPriority by remember { mutableStateOf("normal") }

    // ==========================================
    // UI State
    // ==========================================
    var showLeaveTypeSheet by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    val leaveTypeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = null,
        initialSelectedEndDateMillis = null,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                return !date.isBefore(LocalDate.now())
            }
        }
    )

    // ==========================================
    // Validation State
    // ==========================================
    var showLeaveTypeError by remember { mutableStateOf(false) }
    var showDateError by remember { mutableStateOf(false) }
    var showReasonError by remember { mutableStateOf(false) }
    var showDependencyError by remember { mutableStateOf(false) }
    var dateErrorMessage by remember { mutableStateOf("") }
    var reasonErrorMessage by remember { mutableStateOf("") }

    // ==========================================
    // Calculated Values
    // ==========================================
    val numberOfDays = remember(startDate, endDate) {
        if (startDate != null && endDate != null) {
            ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        } else 0
    }

    val workingDays = remember(startDate, endDate) {
        if (startDate != null && endDate != null) {
            var count = 0
            var current = startDate!!
            while (!current.isAfter(endDate!!)) {
                if (current.dayOfWeek != DayOfWeek.SATURDAY && current.dayOfWeek != DayOfWeek.SUNDAY) {
                    count++
                }
                current = current.plusDays(1)
            }
            count
        } else 0
    }

    // Effective days is now same as numberOfDays (half-day removed from API)
    val effectiveDays = numberOfDays.toDouble()

    // ==========================================
    // File Picker
    // ==========================================
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = getFileName(context, it)
        }
    }

    // ==========================================
    // Handle gesture back
    // ==========================================
    BackHandler {
        onNavigateBack()
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
                    // Launch in separate coroutine to not block event collection
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "âŒ ${event.message}",
                            duration = SnackbarDuration.Long,
                            withDismissAction = true
                        )
                    }
                }
                is LeaveEvent.ShowSuccess -> {
                    // Don't show snackbar here - will be shown on history screen
                }
                is LeaveEvent.NavigateToHistory -> {
                    // Navigate to history screen immediately - success message shown there
                    onNavigateToHistory(1) // Pass 1 to indicate success
                }
            }
        }
    }

    // ==========================================
    // Validation Function (DEBUG VERSION)
    // ==========================================
    fun validateForm(): Boolean {
        var isValid = true
        val errorMessages = mutableListOf<String>()

        // Reset errors
        showLeaveTypeError = false
        showDateError = false
        showReasonError = false
        showDependencyError = false
        reasonErrorMessage = ""
        dateErrorMessage = ""

        // Validate leave type
        if (selectedLeaveType == null) {
            showLeaveTypeError = true
            errorMessages.add("Leave type is required")
            isValid = false
        } else {
        }

        // Validate dates
        if (startDate == null || endDate == null) {
            showDateError = true
            dateErrorMessage = "Please select leave dates"
            errorMessages.add("Leave dates are required")
            isValid = false
        } else {
        }

        // Validate reason
        if (leaveReason.isBlank()) {
            showReasonError = true
            reasonErrorMessage = "Please provide a reason for your leave"
            errorMessages.add("Leave reason is required")
            isValid = false
        } else if (leaveReason.length < 10) {
            showReasonError = true
            reasonErrorMessage = "Reason must be at least 10 characters"
            errorMessages.add("Leave reason is too short (min 10 characters)")
            isValid = false
        } else {
        }

        // Validate dependency handler if task is dependent
        if (taskDependedOnYou && dependencyHandledBy.isBlank()) {
            showDependencyError = true
            errorMessages.add("Please specify who will handle your tasks")
            isValid = false
        }

        // Validate document if required (MANDATORY means always required)
        val docReq = selectedLeaveType?.documentRequirement?.uppercase()
        if (docReq == "MANDATORY" && selectedFileName == null) {
            errorMessages.add("This leave type requires a supporting document")
            isValid = false
        }

        // Validate alternate contact format if provided
        if (alternateContact.isNotBlank() && alternateContact.length < 10) {
            errorMessages.add("Alternate contact must be at least 10 digits")
            isValid = false
        }

        // Validate emergency contact format if provided
        if (emergencyContact.isNotBlank() && emergencyContact.length < 10) {
            errorMessages.add("Emergency contact must be at least 10 digits")
            isValid = false
        }


        // Show error snackbar with all validation errors
        if (!isValid && errorMessages.isNotEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "âŒ ${errorMessages.first()}",
                    duration = SnackbarDuration.Long,
                    withDismissAction = true
                )
            }
        }

        return isValid
    }

    // ==========================================
    // Submit Handler (DEBUG VERSION)
    // ==========================================
    fun handleSubmit() {

        if (!validateForm()) {
            return
        }


        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        // Prepare file if selected
        var supportingDocumentFile: File? = null
        selectedFileUri?.let { uri ->
            supportingDocumentFile = createTempFileFromUri(context, uri)
        }


        viewModel.applyLeave(
            leaveTypeId = selectedLeaveType!!.id,
            startDate = startDate!!.format(dateFormatter),
            endDate = endDate!!.format(dateFormatter),
            leaveReason = leaveReason.trim(),
            alternateContact = alternateContact.ifBlank { null },
            emergencyContact = emergencyContact.ifBlank { null },
            taskDependedOnYou = taskDependedOnYou,
            dependencyHandledBy = dependencyHandledBy.ifBlank { null },
            handoverNotes = handoverNotes.ifBlank { null },
            priority = selectedPriority,
            supportingDocumentFile = supportingDocumentFile
        )

    }

    // ==========================================
    // Main UI
    // ==========================================
    Scaffold(
        containerColor = TeamozyColors.Background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 100.dp) // FIX: Add padding to show above nav bar
            ) { data ->
                val isSuccess = data.visuals.message.contains("âœ…")
                val isError = data.visuals.message.contains("âŒ")

                Snackbar(
                    snackbarData = data,
                    containerColor = when {
                        isSuccess -> Color(0xFF10B981)
                        isError -> Color(0xFFEF4444)
                        else -> MaterialTheme.colorScheme.inverseSurface
                    },
                    contentColor = Color.White,
                    actionColor = Color.White,
                    dismissActionContentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Apply for Leave",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                // Loading state
                uiState.isLoading && uiState.leaveTypes.isEmpty() -> {
                    LoadingState()
                }
                // No leave types available
                !uiState.isLoading && uiState.leaveTypes.isEmpty() -> {
                    NoLeaveTypesState(
                        message = uiState.errorMessage ?: uiState.leaveTypesMessage,
                        onNavigateBack = onNavigateBack
                    )
                }
                // Show form
                else -> {
                    LeaveApplicationContent(
                        uiState = uiState,
                        selectedLeaveType = selectedLeaveType,
                        startDate = startDate,
                        endDate = endDate,
                        numberOfDays = numberOfDays,
                        workingDays = workingDays,
                        effectiveDays = effectiveDays,
                        leaveReason = leaveReason,
                        alternateContact = alternateContact,
                        emergencyContact = emergencyContact,
                        taskDependedOnYou = taskDependedOnYou,
                        dependencyHandledBy = dependencyHandledBy,
                        handoverNotes = handoverNotes,
                        selectedFileName = selectedFileName,
                        selectedPriority = selectedPriority,
                        showLeaveTypeError = showLeaveTypeError,
                        showDateError = showDateError,
                        showReasonError = showReasonError,
                        showDependencyError = showDependencyError,
                        dateErrorMessage = dateErrorMessage,
                        reasonErrorMessage = reasonErrorMessage,
                        onLeaveTypeClick = {
                            showLeaveTypeSheet = true
                            showLeaveTypeError = false
                        },
                        onDateRangeClick = {
                            showDateRangePicker = true
                            showDateError = false
                        },
                        onReasonChange = {
                            leaveReason = it
                            showReasonError = false
                        },
                        onAlternateContactChange = { alternateContact = it },
                        onEmergencyContactChange = { emergencyContact = it },
                        onTaskDependencyChange = {
                            taskDependedOnYou = it
                            if (!it) {
                                dependencyHandledBy = ""
                                handoverNotes = ""
                                showDependencyError = false
                            }
                        },
                        onDependencyHandledByChange = {
                            dependencyHandledBy = it
                            showDependencyError = false
                        },
                        onHandoverNotesChange = { handoverNotes = it },
                        onPriorityChange = { selectedPriority = it },
                        onPickFile = { filePicker.launch("*/*") },
                        onRemoveFile = {
                            selectedFileUri = null
                            selectedFileName = null
                        },
                        onSubmit = ::handleSubmit
                    )
                }
            }

            // Loading overlay
            if (uiState.isLoading && uiState.leaveTypes.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Submitting leave application...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // Leave Type Bottom Sheet
    // ==========================================
    if (showLeaveTypeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLeaveTypeSheet = false },
            sheetState = leaveTypeSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            LeaveTypeBottomSheet(
                leaveTypes = uiState.leaveTypes,
                selectedLeaveType = selectedLeaveType,
                onLeaveTypeSelected = { leaveType ->
                    selectedLeaveType = leaveType
                    scope.launch {
                        leaveTypeSheetState.hide()
                        showLeaveTypeSheet = false
                    }
                }
            )
        }
    }

    // ==========================================
    // Date Range Picker Dialog
    // ==========================================
    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateRangePickerState.selectedStartDateMillis?.let { startMillis ->
                            startDate = Instant.ofEpochMilli(startMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        dateRangePickerState.selectedEndDateMillis?.let { endMillis ->
                            endDate = Instant.ofEpochMilli(endMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDateRangePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null &&
                            dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text("Confirm", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancel")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Select Leave Dates",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                headline = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val startDateText = dateRangePickerState.selectedStartDateMillis?.let {
                            Instant.ofEpochMilli(it)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                        } ?: "Start Date"

                        val endDateText = dateRangePickerState.selectedEndDateMillis?.let {
                            Instant.ofEpochMilli(it)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                        } ?: "End Date"

                        Text(
                            text = "$startDateText - $endDateText",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                showModeToggle = false,
                modifier = Modifier.height(500.dp)
            )
        }
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
            CircularProgressIndicator()
            Text(
                text = "Loading leave types...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==========================================
// NO LEAVE TYPES STATE
// ==========================================
@Composable
private fun NoLeaveTypesState(message: String, onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.EventBusy,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = TeamozyColors.Primary.copy(alpha = 0.6f)
            )
            Text(
                text = message.ifBlank { "No leave types are assigned to you. Please contact HR." },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onNavigateBack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Go Back")
            }
        }
    }
}

// ==========================================
// LEAVE APPLICATION CONTENT
// ==========================================
@Composable
private fun LeaveApplicationContent(
    uiState: ApplyLeaveUiState,
    selectedLeaveType: LeaveType?,
    startDate: LocalDate?,
    endDate: LocalDate?,
    numberOfDays: Int,
    workingDays: Int,
    effectiveDays: Double,
    leaveReason: String,
    alternateContact: String,
    emergencyContact: String,
    taskDependedOnYou: Boolean,
    dependencyHandledBy: String,
    handoverNotes: String,
    selectedFileName: String?,
    selectedPriority: String,
    showLeaveTypeError: Boolean,
    showDateError: Boolean,
    showReasonError: Boolean,
    showDependencyError: Boolean,
    dateErrorMessage: String,
    reasonErrorMessage: String,
    onLeaveTypeClick: () -> Unit,
    onDateRangeClick: () -> Unit,
    onReasonChange: (String) -> Unit,
    onAlternateContactChange: (String) -> Unit,
    onEmergencyContactChange: (String) -> Unit,
    onTaskDependencyChange: (Boolean) -> Unit,
    onDependencyHandledByChange: (String) -> Unit,
    onHandoverNotesChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
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
                top = 8.dp,
                bottom = 160.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Leave Type Selection
            item {
                FormSection(
                    title = "Leave Type",
                    icon = Icons.Outlined.Category,
                    isRequired = true,
                    hasError = showLeaveTypeError
                ) {
                    LeaveTypeSelector(
                        selectedLeaveType = selectedLeaveType,
                        hasError = showLeaveTypeError,
                        onClick = onLeaveTypeClick
                    )

                    AnimatedVisibility(
                        visible = showLeaveTypeError,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = "Please select a leave type",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
            }

            // Date Range Selection
            item {
                FormSection(
                    title = "Leave Period",
                    icon = Icons.Outlined.DateRange,
                    isRequired = true,
                    hasError = showDateError
                ) {
                    DateRangeSelector(
                        startDate = startDate,
                        endDate = endDate,
                        numberOfDays = numberOfDays,
                        workingDays = workingDays,
                        effectiveDays = effectiveDays,
                        hasError = showDateError,
                        onClick = onDateRangeClick
                    )

                    AnimatedVisibility(
                        visible = showDateError,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = dateErrorMessage.ifEmpty { "Please select leave dates" },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
            }

            // Priority Selection
            item {
                FormSection(
                    title = "Priority",
                    icon = Icons.Outlined.Flag,
                    isRequired = false
                ) {
                    PrioritySelector(
                        selectedPriority = selectedPriority,
                        onPriorityChange = onPriorityChange
                    )
                }
            }

            // Leave Reason
            item {
                FormSection(
                    title = "Reason for Leave",
                    icon = Icons.AutoMirrored.Outlined.Assignment,
                    isRequired = true,
                    hasError = showReasonError
                ) {
                    OutlinedTextField(
                        value = leaveReason,
                        onValueChange = onReasonChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Describe your reason for leave (min 10 characters)") },
                        minLines = 3,
                        maxLines = 5,
                        isError = showReasonError,
                        supportingText = if (showReasonError) {
                            { Text(reasonErrorMessage) }
                        } else {
                            { Text("${leaveReason.length}/10 characters minimum") }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Contact Information
            item {
                FormSection(
                    title = "Contact Information",
                    icon = Icons.Outlined.ContactPhone,
                    isRequired = false
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = alternateContact,
                            onValueChange = onAlternateContactChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Alternate Contact Number") },
                            placeholder = { Text("Enter contact number") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = emergencyContact,
                            onValueChange = onEmergencyContactChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Emergency Contact Number") },
                            placeholder = { Text("Enter emergency contact") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Task Handover Section
            item {
                FormSection(
                    title = "Task Handover",
                    icon = Icons.Outlined.SwapHoriz,
                    isRequired = false,
                    hasError = showDependencyError
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tasks depend on you?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = taskDependedOnYou,
                                onCheckedChange = onTaskDependencyChange
                            )
                        }

                        AnimatedVisibility(
                            visible = taskDependedOnYou,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = dependencyHandledBy,
                                    onValueChange = onDependencyHandledByChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Who will handle your tasks? *") },
                                    placeholder = { Text("Enter name of colleague") },
                                    isError = showDependencyError,
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = handoverNotes,
                                    onValueChange = onHandoverNotesChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Handover Notes") },
                                    placeholder = { Text("Any instructions for task handover") },
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Supporting Document
            item {
                FormSection(
                    title = "Supporting Document",
                    icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                    isRequired = selectedLeaveType?.documentRequirement?.uppercase() == "MANDATORY"
                ) {
                    if (selectedFileName == null) {
                        OutlinedCard(
                            onClick = onPickFile,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = TeamozyColors.Primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Select Media",
                                    color = TeamozyColors.Primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = TeamozyColors.Primary.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = TeamozyColors.Primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = selectedFileName!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
                    }
                }
            }
        }

        // Submit Button (Fixed at bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TeamozyColors.Background.copy(alpha = 0.9f),
                            TeamozyColors.Background
                        )
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp)
        ) {
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeamozyColors.Primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit Leave Request",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ==========================================
// FORM SECTION WRAPPER
// ==========================================
@Composable
private fun FormSection(
    title: String,
    icon: ImageVector,
    isRequired: Boolean,
    hasError: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasError)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (hasError)
                        MaterialTheme.colorScheme.error
                    else
                        TeamozyColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasError)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (isRequired) {
                    Text(
                        text = "*",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            content()
        }
    }
}

// ==========================================
// LEAVE TYPE SELECTOR
// ==========================================
@Composable
private fun LeaveTypeSelector(
    selectedLeaveType: LeaveType?,
    hasError: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = if (hasError) {
                    listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error)
                } else if (selectedLeaveType != null) {
                    listOf(TeamozyColors.Primary, TeamozyColors.Primary)
                } else {
                    listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant)
                }
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedLeaveType != null) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedLeaveType.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LeaveTypeChip(
                            text = selectedLeaveType.code,
                            color = TeamozyColors.Primary
                        )
                        LeaveTypeChip(
                            text = if (selectedLeaveType.isPaid) "Paid" else "Unpaid",
                            color = if (selectedLeaveType.isPaid)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                        if (selectedLeaveType.applicationMode != null) {
                            LeaveTypeChip(
                                text = selectedLeaveType.applicationMode,
                                color = TeamozyColors.Primary
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Select leave type",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==========================================
// DATE RANGE SELECTOR
// ==========================================
@Composable
private fun DateRangeSelector(
    startDate: LocalDate?,
    endDate: LocalDate?,
    numberOfDays: Int,
    workingDays: Int,
    effectiveDays: Double,
    hasError: Boolean,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = if (hasError) {
                    listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error)
                } else if (startDate != null && endDate != null) {
                    listOf(TeamozyColors.Primary, TeamozyColors.Primary)
                } else {
                    listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant)
                }
            )
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (startDate != null && endDate != null)
                TeamozyColors.Primary.copy(alpha = 0.05f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (startDate != null && endDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateDisplayBox(
                        label = "From",
                        date = startDate.format(dateFormatter),
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(32.dp)
                            .background(
                                TeamozyColors.Primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TeamozyColors.Primary
                        )
                    }

                    DateDisplayBox(
                        label = "To",
                        date = endDate.format(dateFormatter),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DaySummaryItem(
                        value = numberOfDays.toString(),
                        label = "Total Days",
                        color = TeamozyColors.Primary
                    )
                    DaySummaryItem(
                        value = workingDays.toString(),
                        label = "Working Days",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    DaySummaryItem(
                        value = String.format("%.1f", effectiveDays),
                        label = "Effective Days",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = TeamozyColors.Primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tap to select date range",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==========================================
// DATE DISPLAY BOX
// ==========================================
@Composable
private fun DateDisplayBox(
    label: String,
    date: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ==========================================
// DAY SUMMARY ITEM
// ==========================================
@Composable
private fun DaySummaryItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==========================================
// PRIORITY SELECTOR
// ==========================================
@Composable
private fun PrioritySelector(
    selectedPriority: String,
    onPriorityChange: (String) -> Unit
) {
    val priorities = listOf(
        Triple("normal", "Normal", Color(0xFF10B981)),
        Triple("high", "High", Color(0xFFF59E0B)),
        Triple("urgent", "Urgent", Color(0xFFEF4444))
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        priorities.forEach { (value, label, color) ->
            val isSelected = selectedPriority == value
            val backgroundColor = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
            val contentColor = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant

            OutlinedCard(
                onClick = { onPriorityChange(value) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = if (isSelected) {
                            listOf(color, color)
                        } else {
                            listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant)
                        }
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (value) {
                            "urgent" -> Icons.Default.PriorityHigh
                            "high" -> Icons.Default.Flag
                            else -> Icons.Outlined.Flag
                        },
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = contentColor
                    )
                }
            }
        }
    }
}

// ==========================================
// LEAVE TYPE CHIP
// ==========================================
@Composable
private fun LeaveTypeChip(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// LEAVE TYPE BOTTOM SHEET
// ==========================================
@Composable
private fun LeaveTypeBottomSheet(
    leaveTypes: List<LeaveType>,
    selectedLeaveType: LeaveType?,
    onLeaveTypeSelected: (LeaveType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Select Leave Type",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Text(
            text = "Choose the type of leave you want to apply for",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(leaveTypes) { leaveType ->
                LeaveTypeItem(
                    leaveType = leaveType,
                    isSelected = selectedLeaveType?.id == leaveType.id,
                    onClick = { onLeaveTypeSelected(leaveType) }
                )
            }
        }
    }
}

// ==========================================
// LEAVE TYPE ITEM
// ==========================================
@Composable
private fun LeaveTypeItem(
    leaveType: LeaveType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                TeamozyColors.Primary.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    listOf(TeamozyColors.Primary, TeamozyColors.Primary)
                )
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = leaveType.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeaveTypeChip(
                        text = leaveType.code,
                        color = TeamozyColors.Primary
                    )
                    LeaveTypeChip(
                        text = if (leaveType.isPaid) "Paid" else "Unpaid",
                        color = if (leaveType.isPaid)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = TeamozyColors.Primary
                )
            }
        }
    }
}

// ==========================================
// UTILITY FUNCTIONS
// ==========================================
private fun getFileName(context: Context, uri: Uri): String {
    var name = "document"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

private fun createTempFileFromUri(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = getFileName(context, uri)
        val tempFile = File(context.cacheDir, fileName)
        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        tempFile
    } catch (e: Exception) {
        null
    }
}
