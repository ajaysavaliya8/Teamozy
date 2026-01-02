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
 * ✅ IMPROVED: Apply Leave Screen
 *
 * Enhancements:
 * - Bottom Sheet for leave type selection (faster than dropdown)
 * - Single Date Range Picker with highlighted range
 * - Animated form sections
 * - Leave days calculation with visual display
 * - Better error feedback with inline validation
 * - Smooth transitions and micro-interactions
 * - Material Design 3 with custom styling
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

    // Half-day support
    var isHalfDayStart by remember { mutableStateOf(false) }
    var isHalfDayEnd by remember { mutableStateOf(false) }
    var halfDayType by remember { mutableStateOf<String?>(null) }

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
                // Allow today and future dates
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

    // Effective days (accounting for half days)
    val effectiveDays = remember(numberOfDays, isHalfDayStart, isHalfDayEnd) {
        var days = numberOfDays.toDouble()
        if (isHalfDayStart) days -= 0.5
        if (isHalfDayEnd) days -= 0.5
        days
    }

    // Check if half-day is applicable (same day selection)
    val isSingleDay = startDate != null && endDate != null && startDate == endDate

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
                    // Show error message prominently
                    snackbarHostState.showSnackbar(
                        message = "❌ ${event.message}",
                        duration = SnackbarDuration.Long,
                        withDismissAction = true
                    )
                }
                is LeaveEvent.ShowSuccess -> {
                    // Show success message
                    snackbarHostState.showSnackbar(
                        message = "✅ ${event.message}",
                        duration = SnackbarDuration.Short
                    )
                }
                is LeaveEvent.NavigateToHistory -> {
                    // Show success and navigate back
                    snackbarHostState.showSnackbar(
                        message = "✅ Leave application submitted successfully!",
                        duration = SnackbarDuration.Short
                    )
                    // Small delay to let user see the success message
                    kotlinx.coroutines.delay(800)
                    onNavigateBack()
                }
                is LeaveEvent.DraftSaved -> {
                    snackbarHostState.showSnackbar(
                        message = "📝 ${event.message}",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // ==========================================
    // Validation Function
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
        }

        // Validate dates
        if (startDate == null || endDate == null) {
            showDateError = true
            dateErrorMessage = "Please select leave dates"
            errorMessages.add("Leave dates are required")
            isValid = false
        }

        // Validate half-day type when half-day is selected
        if ((isHalfDayStart || isHalfDayEnd) && halfDayType == null) {
            showDateError = true
            dateErrorMessage = "Please select half day type (First Half or Second Half)"
            errorMessages.add("Half day type is required")
            isValid = false
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
        }

        // Validate dependency handler if task is dependent
        if (taskDependedOnYou && dependencyHandledBy.isBlank()) {
            showDependencyError = true
            errorMessages.add("Please specify who will handle your tasks")
            isValid = false
        }

        // Validate document if required
        if (selectedLeaveType?.requiresDocument == true && selectedFileName == null) {
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
                    message = "❌ ${errorMessages.first()}",
                    duration = SnackbarDuration.Long,
                    withDismissAction = true
                )
            }
        }

        return isValid
    }

    // ==========================================
    // Submit Handler
    // ==========================================
    fun handleSubmit() {
        if (!validateForm()) return

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
            isHalfDayStart = isHalfDayStart,
            isHalfDayEnd = isHalfDayEnd,
            halfDayType = halfDayType,
            alternateContact = alternateContact.ifBlank { null },
            emergencyContact = emergencyContact.ifBlank { null },
            taskDependedOnYou = taskDependedOnYou,
            dependencyHandledBy = dependencyHandledBy.ifBlank { null },
            handoverNotes = handoverNotes.ifBlank { null },
            supportingDocumentFile = supportingDocumentFile
        )
    }

    // ==========================================
    // Main UI
    // ==========================================
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val isSuccess = data.visuals.message.contains("✅")
                val isError = data.visuals.message.contains("❌")
                val isDraft = data.visuals.message.contains("📝")

                Snackbar(
                    snackbarData = data,
                    containerColor = when {
                        isSuccess -> Color(0xFF4CAF50)  // Green for success
                        isError -> Color(0xFFF44336)    // Red for error
                        isDraft -> Color(0xFF2196F3)    // Blue for draft
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
                    NoLeaveTypesState(onNavigateBack = onNavigateBack)
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
                        isSingleDay = isSingleDay,
                        isHalfDayStart = isHalfDayStart,
                        isHalfDayEnd = isHalfDayEnd,
                        halfDayType = halfDayType,
                        leaveReason = leaveReason,
                        alternateContact = alternateContact,
                        emergencyContact = emergencyContact,
                        taskDependedOnYou = taskDependedOnYou,
                        dependencyHandledBy = dependencyHandledBy,
                        handoverNotes = handoverNotes,
                        selectedFileName = selectedFileName,
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
                        onHalfDayStartChange = { isHalfDayStart = it },
                        onHalfDayEndChange = { isHalfDayEnd = it },
                        onHalfDayTypeChange = { halfDayType = it },
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

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "From",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = startDateText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.CenterVertically),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "To",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = endDateText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                showModeToggle = false,
                modifier = Modifier.height(500.dp),
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                    dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
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
    isSingleDay: Boolean,
    isHalfDayStart: Boolean,
    isHalfDayEnd: Boolean,
    halfDayType: String?,
    leaveReason: String,
    alternateContact: String,
    emergencyContact: String,
    taskDependedOnYou: Boolean,
    dependencyHandledBy: String,
    handoverNotes: String,
    selectedFileName: String?,
    showLeaveTypeError: Boolean,
    showDateError: Boolean,
    showReasonError: Boolean,
    showDependencyError: Boolean,
    dateErrorMessage: String,
    reasonErrorMessage: String,
    onLeaveTypeClick: () -> Unit,
    onDateRangeClick: () -> Unit,
    onHalfDayStartChange: (Boolean) -> Unit,
    onHalfDayEndChange: (Boolean) -> Unit,
    onHalfDayTypeChange: (String?) -> Unit,
    onReasonChange: (String) -> Unit,
    onAlternateContactChange: (String) -> Unit,
    onEmergencyContactChange: (String) -> Unit,
    onTaskDependencyChange: (Boolean) -> Unit,
    onDependencyHandledByChange: (String) -> Unit,
    onHandoverNotesChange: (String) -> Unit,
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
                bottom = 160.dp  // Increased to account for submit button + bottom nav bar
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // Leave Type Selection
            // ==========================================
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

            // ==========================================
            // Date Range Selection
            // ==========================================
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

            // ==========================================
            // Half Day Selection (visible when dates selected)
            // ==========================================
            if (startDate != null && endDate != null) {
                item {
                    FormSection(
                        title = "Half Day Options",
                        icon = Icons.Outlined.Timelapse,
                        isRequired = false
                    ) {
                        HalfDaySection(
                            isSingleDay = isSingleDay,
                            isHalfDayStart = isHalfDayStart,
                            isHalfDayEnd = isHalfDayEnd,
                            halfDayType = halfDayType,
                            onHalfDayStartChange = onHalfDayStartChange,
                            onHalfDayEndChange = onHalfDayEndChange,
                            onHalfDayTypeChange = onHalfDayTypeChange
                        )
                    }
                }
            }

            // ==========================================
            // Leave Reason
            // ==========================================
            item {
                FormSection(
                    title = "Reason for Leave",
                    icon = Icons.Outlined.Description,
                    isRequired = true,
                    hasError = showReasonError
                ) {
                    OutlinedTextField(
                        value = leaveReason,
                        onValueChange = onReasonChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Describe the reason for your leave request...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        isError = showReasonError,
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AnimatedVisibility(visible = showReasonError) {
                                    Text(
                                        text = reasonErrorMessage,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${leaveReason.length}/500",
                                    color = if (leaveReason.length > 500)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // ==========================================
            // Contact Information
            // ==========================================
            item {
                FormSection(
                    title = "Contact Information",
                    icon = Icons.Outlined.Phone,
                    isRequired = false
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Alternate Contact
                        OutlinedTextField(
                            value = alternateContact,
                            onValueChange = { newValue ->
                                // Only allow digits
                                if (newValue.all { it.isDigit() }) {
                                    onAlternateContactChange(newValue)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Alternate Contact") },
                            placeholder = {
                                Text(
                                    text = "Phone number during leave",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            singleLine = true,
                            leadingIcon = {
                                Text(
                                    text = "+91",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // Emergency Contact
                        OutlinedTextField(
                            value = emergencyContact,
                            onValueChange = { newValue ->
                                // Only allow digits
                                if (newValue.all { it.isDigit() }) {
                                    onEmergencyContactChange(newValue)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Emergency Contact") },
                            placeholder = {
                                Text(
                                    text = "Emergency phone number",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            singleLine = true,
                            leadingIcon = {
                                Text(
                                    text = "+91",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }

            // ==========================================
            // Task Dependency & Handover
            // ==========================================
            item {
                FormSection(
                    title = "Task Handover",
                    icon = Icons.Outlined.Assignment,
                    isRequired = false
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tasks depend on you?",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Enable if someone needs to handle your tasks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = taskDependedOnYou,
                                    onCheckedChange = onTaskDependencyChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }

                            AnimatedVisibility(
                                visible = taskDependedOnYou,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = dependencyHandledBy,
                                        onValueChange = onDependencyHandledByChange,
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Who will handle your tasks? *") },
                                        placeholder = { Text("Enter colleague's name") },
                                        singleLine = true,
                                        isError = showDependencyError,
                                        supportingText = if (showDependencyError) {
                                            { Text("Please specify who will handle your tasks") }
                                        } else null,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )

                                    OutlinedTextField(
                                        value = handoverNotes,
                                        onValueChange = onHandoverNotesChange,
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Handover Notes") },
                                        placeholder = { Text("Any specific instructions for task handover...") },
                                        minLines = 2,
                                        maxLines = 4,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // Supporting Document
            // ==========================================
            item {
                FormSection(
                    title = "Supporting Document",
                    icon = Icons.Outlined.AttachFile,
                    isRequired = false
                ) {
                    if (selectedFileName == null) {
                        OutlinedCard(
                            onClick = onPickFile,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.outlineVariant,
                                        MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Upload Document",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "PDF, Image, or Document (Max 5MB)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.InsertDriveFile,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
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

        // ==========================================
        // Submit Button (Fixed at bottom)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp)  // Added bottom padding for nav bar
        ) {
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (hasError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (hasError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
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
                    listOf(
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.error
                    )
                } else if (selectedLeaveType != null) {
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary
                    )
                } else {
                    listOf(
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                }
            )
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selectedLeaveType != null)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedLeaveType.code.take(2).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = selectedLeaveType.leaveTypeName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LeaveTypeChip(
                                text = if (selectedLeaveType.isPaid) "Paid" else "Unpaid",
                                color = if (selectedLeaveType.isPaid)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                            if (selectedLeaveType.requiresApproval) {
                                LeaveTypeChip(
                                    text = "Requires Approval",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
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
                    listOf(
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.error
                    )
                } else if (startDate != null && endDate != null) {
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary
                    )
                } else {
                    listOf(
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                }
            )
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (startDate != null && endDate != null)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
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
                    // Start Date
                    DateDisplayBox(
                        label = "From",
                        date = startDate.format(dateFormatter),
                        modifier = Modifier.weight(1f)
                    )

                    // Arrow
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // End Date
                    DateDisplayBox(
                        label = "To",
                        date = endDate.format(dateFormatter),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Days summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DaySummaryItem(
                        value = numberOfDays.toString(),
                        label = "Total Days",
                        icon = Icons.Outlined.CalendarMonth
                    )
                    DaySummaryItem(
                        value = if (effectiveDays == effectiveDays.toLong().toDouble())
                            effectiveDays.toLong().toString()
                        else
                            String.format("%.1f", effectiveDays),
                        label = "Effective",
                        icon = Icons.Outlined.EventAvailable,
                        highlight = true
                    )
                    DaySummaryItem(
                        value = workingDays.toString(),
                        label = "Working",
                        icon = Icons.Outlined.WorkOutline
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Tap to select leave dates",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

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
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = date,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DaySummaryItem(
    value: String,
    label: String,
    icon: ImageVector,
    highlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (highlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==========================================
// HALF DAY SECTION
// ==========================================
@Composable
private fun HalfDaySection(
    isSingleDay: Boolean,
    isHalfDayStart: Boolean,
    isHalfDayEnd: Boolean,
    halfDayType: String?,
    onHalfDayStartChange: (Boolean) -> Unit,
    onHalfDayEndChange: (Boolean) -> Unit,
    onHalfDayTypeChange: (String?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Info text
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = if (isSingleDay)
                        "Take half day leave for this date"
                    else
                        "You can take half day on start or end date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (isSingleDay) {
                // Single day - just one half day option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Half Day Leave",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isHalfDayStart,
                        onCheckedChange = {
                            onHalfDayStartChange(it)
                            if (!it) onHalfDayTypeChange(null)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            } else {
                // Multiple days - start and end half day options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Half Day on Start Date",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isHalfDayStart,
                        onCheckedChange = {
                            onHalfDayStartChange(it)
                            if (!it && !isHalfDayEnd) onHalfDayTypeChange(null)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Half Day on End Date",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isHalfDayEnd,
                        onCheckedChange = {
                            onHalfDayEndChange(it)
                            if (!it && !isHalfDayStart) onHalfDayTypeChange(null)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }

            // Half day type selection (visible when any half day is selected)
            AnimatedVisibility(
                visible = isHalfDayStart || isHalfDayEnd,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "Select Half Day Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // First Half
                        HalfDayTypeChip(
                            text = "First Half",
                            subtext = "Morning",
                            isSelected = halfDayType == "FIRST_HALF",
                            onClick = { onHalfDayTypeChange("FIRST_HALF") },
                            modifier = Modifier.weight(1f)
                        )

                        // Second Half
                        HalfDayTypeChip(
                            text = "Second Half",
                            subtext = "Afternoon",
                            isSelected = halfDayType == "SECOND_HALF",
                            onClick = { onHalfDayTypeChange("SECOND_HALF") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HalfDayTypeChip(
    text: String,
    subtext: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.secondary
        else
            MaterialTheme.colorScheme.surface,
        label = "chipBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onSecondary
        else
            MaterialTheme.colorScheme.onSurface,
        label = "chipContent"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (!isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
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
        // Header
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

        // Leave types list
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

@Composable
private fun LeaveTypeItem(
    leaveType: LeaveType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        label = "background"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outlineVariant,
        label = "border"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon/Code box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = leaveType.code.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = leaveType.leaveTypeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Tags row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        LeaveTypeChip(
                            text = if (leaveType.isPaid) "Paid" else "Unpaid",
                            color = if (leaveType.isPaid)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (leaveType.requiresApproval) {
                        item {
                            LeaveTypeChip(
                                text = "Approval Required",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    leaveType.applicableFor?.let { applicable ->
                        item {
                            LeaveTypeChip(
                                text = applicable.replaceFirstChar { it.uppercase() },
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Description
                leaveType.description?.let { desc ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
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
private fun NoLeaveTypesState(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "No Leave Types Available",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Leave types are configured by your organization. This could be due to your role, department, or employment status.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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