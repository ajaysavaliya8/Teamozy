package com.hrms.jeejateamozy.feature.home.presentation.dialogs

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.hrms.jeejateamozy.core.network.WorkReportQuestion
import com.hrms.jeejateamozy.core.network.WorkReportQuestionOption
import com.hrms.jeejateamozy.core.network.WorkReportQuestionSet
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkReportBottomSheet(
    questionSet: WorkReportQuestionSet?,
    priorityOptions: List<String>,
    isEarly: Boolean = false,
    isOutOfRange: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (workReport: String?, fileUri: Uri?, priority: String?, answers: String?) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Free-text mode state (when no questionSet)
    var workReport by remember { mutableStateOf("") }

    // Question answers: question_id -> answer string
    val answers = remember { mutableStateMapOf<Int, String>() }

    // Pre-populate DATE questions with today's date
    LaunchedEffect(questionSet) {
        questionSet?.questions?.forEach { q ->
            if (q.question_type == "DATE" && !answers.containsKey(q.id)) {
                answers[q.id] = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            }
        }
    }

    // Multi-choice selections: question_id -> set of selected options
    val multiChoiceSelections = remember { mutableStateMapOf<Int, MutableSet<String>>() }

    // Priority selection
    var selectedPriority by remember(priorityOptions) {
        mutableStateOf(
            priorityOptions.firstOrNull { it.equals("NORMAL", ignoreCase = true) }
                ?: priorityOptions.firstOrNull()
        )
    }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val cameraPhotoUri = remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = getFileName(context, it)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri.value != null) {
            selectedFileUri = cameraPhotoUri.value
            selectedFileName = "Photo_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Work Report",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Describe your work for today",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isEarly || isOutOfRange) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isEarly) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFF8E1)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFF59E0B)
                                )
                                Text(
                                    text = "Leaving Early",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                    if (isOutOfRange) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFEF4444)
                                )
                                Text(
                                    text = "Out of Range",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Priority selector
            if (priorityOptions.isNotEmpty()) {
                Text(
                    text = "Priority",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorityOptions.forEach { option ->
                        val isSelected = selectedPriority == option
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPriority = option },
                            label = {
                                Text(
                                    text = option.replace("_", " "),
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Question set OR free-text field
            if (questionSet != null) {
                questionSet.questions.forEach { question ->
                    QuestionField(
                        question = question,
                        value = answers[question.id] ?: "",
                        multiSelected = multiChoiceSelections.getOrElse(question.id) { mutableSetOf() },
                        onValueChange = { answers[question.id] = it },
                        onMultiSelectChange = { option, checked ->
                            val updated = (multiChoiceSelections[question.id] ?: emptySet()).toMutableSet()
                            if (checked) updated.add(option) else updated.remove(option)
                            multiChoiceSelections[question.id] = updated
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                // Free-text work description (legacy / no question set)
                OutlinedTextField(
                    value = workReport,
                    onValueChange = {
                        workReport = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = "Work Description *",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    placeholder = {
                        Text(
                            text = "What did you work on today?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    supportingText = {
                        Text(
                            text = "${workReport.length}/500 characters (minimum 10)",
                            fontSize = 12.sp,
                            color = if (workReport.length >= 10) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    },
                    isError = showError,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    minLines = 4,
                    maxLines = 8
                )
            }

            // Error Message
            if (showError) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Attachment Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Attach Document/Photo (Optional)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val photoFile = createImageFile(context)
                                val photoUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    photoFile
                                )
                                cameraPhotoUri.value = photoUri
                                cameraLauncher.launch(photoUri)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Camera", fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Files", fontSize = 14.sp)
                        }
                    }

                    if (selectedFileName != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedFileName?.startsWith("Photo_") == true)
                                    Icons.Default.CameraAlt
                                else
                                    Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = selectedFileName ?: "",
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = {
                                    selectedFileUri = null
                                    selectedFileName = null
                                    cameraPhotoUri.value = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove file",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Submit button
            Button(
                onClick = {
                    showError = false
                    errorMessage = ""

                    if (questionSet != null) {
                        // Validate required questions.
                        // BOOLEAN is always considered answered (Switch defaults to false).
                        val missingRequired = questionSet.questions.firstOrNull { q ->
                            q.is_required && q.question_type != "BOOLEAN" && when (q.question_type) {
                                "MULTI_CHOICE" -> multiChoiceSelections[q.id].isNullOrEmpty()
                                else -> answers[q.id].isNullOrBlank()
                            }
                        }
                        if (missingRequired != null) {
                            showError = true
                            errorMessage = "Please answer: ${missingRequired.question_text}"
                            return@Button
                        }

                        // Build answers JSON using JsonElement API to guarantee correct types:
                        // NUMBER → bare number, BOOLEAN → bare bool, MULTI_CHOICE → JSON array.
                        val jsonArray = JsonArray()
                        questionSet.questions.forEach { q ->
                            val obj = JsonObject()
                            obj.addProperty("question_id", q.id)
                            val included = when (q.question_type) {
                                "BOOLEAN" -> {
                                    val v = answers[q.id]?.equals("true", ignoreCase = true) ?: false
                                    obj.addProperty("answer_value", v)
                                    true
                                }
                                "NUMBER" -> {
                                    val raw = answers[q.id]?.takeIf { it.isNotBlank() }
                                    val num = raw?.toDoubleOrNull()
                                    if (num != null) {
                                        obj.addProperty("answer_value", num)
                                        true
                                    } else {
                                        raw?.let { obj.addProperty("answer_value", it); true } ?: false
                                    }
                                }
                                "MULTI_CHOICE" -> {
                                    val selected = multiChoiceSelections[q.id]
                                    if (!selected.isNullOrEmpty()) {
                                        val arr = JsonArray()
                                        selected.forEach { arr.add(it) }
                                        obj.add("answer_value", arr)
                                        true
                                    } else false
                                }
                                else -> {
                                    answers[q.id]?.takeIf { it.isNotBlank() }?.let {
                                        obj.addProperty("answer_value", it)
                                        true
                                    } ?: false
                                }
                            }
                            if (included) jsonArray.add(obj)
                        }
                        val answersJson = Gson().toJson(jsonArray)
                        onSubmit(null, selectedFileUri, selectedPriority, answersJson)
                    } else {
                        val trimmed = workReport.trim()
                        when {
                            trimmed.isEmpty() -> {
                                showError = true
                                errorMessage = "Work description cannot be empty"
                            }
                            trimmed.length < 10 -> {
                                showError = true
                                errorMessage = "Work description must be at least 10 characters (currently ${trimmed.length})"
                            }
                            else -> onSubmit(trimmed, selectedFileUri, selectedPriority, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = if (questionSet != null) true else workReport.trim().isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(
                    text = "Submit Work Report",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionField(
    question: WorkReportQuestion,
    value: String,
    multiSelected: Set<String>,
    onValueChange: (String) -> Unit,
    onMultiSelectChange: (option: String, checked: Boolean) -> Unit
) {
    val label = if (question.is_required) "${question.question_text} *" else question.question_text

    when (question.question_type) {
        "SHORT_TEXT" -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
        "LONG_TEXT" -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp)
            )
        }
        "NUMBER" -> {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' || c == '-' }) onValueChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )
        }
        "BOOLEAN" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = value == "true",
                    onCheckedChange = { onValueChange(it.toString()) }
                )
            }
        }
        "DATE" -> {
            var showDatePicker by remember { mutableStateOf(false) }
            val initialMillis = remember {
                val today = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                if (value.isNotBlank()) {
                    try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            .apply { timeZone = TimeZone.getTimeZone("UTC") }
                            .parse(value)?.time ?: today
                    } catch (_: Exception) { today }
                } else today
            }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                    placeholder = {
                        Text(
                            "Select date",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                onValueChange(
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                        .apply { timeZone = TimeZone.getTimeZone("UTC") }
                                        .format(Date(millis))
                                )
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
        "TIME" -> {
            var showTimePicker by remember { mutableStateOf(false) }
            val (initHour, initMinute) = remember {
                val cal = Calendar.getInstance()
                Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            }
            val timePickerState = rememberTimePickerState(
                initialHour = initHour,
                initialMinute = initMinute,
                is24Hour = true
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                    placeholder = {
                        Text(
                            "Select time",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.matchParentSize().clickable { showTimePicker = true })
            }

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text("Select Time", style = MaterialTheme.typography.titleMedium) },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TimePicker(state = timePickerState)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            onValueChange(
                                "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                            )
                            showTimePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                    }
                )
            }
        }
        "SINGLE_CHOICE" -> {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            question.options?.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = value == option.value,
                            onClick = { onValueChange(option.value) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = value == option.value,
                        onClick = { onValueChange(option.value) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(option.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        "MULTI_CHOICE" -> {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            question.options?.forEach { option ->
                val isChecked = option.value in multiSelected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMultiSelectChange(option.value, !isChecked) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked -> onMultiSelectChange(option.value, checked) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(option.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        else -> {
            // Fallback: treat as SHORT_TEXT
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "WORK_REPORT_${timeStamp}_",
        ".jpg",
        storageDir
    )
}

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
