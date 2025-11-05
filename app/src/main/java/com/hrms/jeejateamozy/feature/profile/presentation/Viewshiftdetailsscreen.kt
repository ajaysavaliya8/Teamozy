@file:OptIn(ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.feature.profile.data.DaySchedule
import com.hrms.jeejateamozy.feature.profile.data.ProfileRepository
import com.hrms.jeejateamozy.feature.profile.data.ShiftDetailsOutcome

/**
 * ViewShiftDetailsScreen - Display shift details (READ ONLY)
 * Shows shift timings, break schedules, policies, and weekly schedule
 */
@Composable
fun ViewShiftDetailsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context) }

    // State variables
    var shiftName by remember { mutableStateOf("") }
    var shiftCode by remember { mutableStateOf("") }
    var multiplePunchAllowed by remember { mutableStateOf(false) }
    var faceRecognitionEnabled by remember { mutableStateOf(false) }
    var fingerprintEnabled by remember { mutableStateOf(false) }
    var weekOffDays by remember { mutableStateOf<List<String>>(emptyList()) }
    var lateInMaxMinutes by remember { mutableStateOf<Int?>(null) }
    var earlyOutMaxMinutes by remember { mutableStateOf<Int?>(null) }
    var allowShortLeave by remember { mutableStateOf(false) }
    var shortLeaveMinutes by remember { mutableStateOf<Int?>(null) }
    var breakType by remember { mutableStateOf("") }
    var breakTimingType by remember { mutableStateOf("") }

    var mondaySchedule by remember { mutableStateOf<DaySchedule?>(null) }
    var tuesdaySchedule by remember { mutableStateOf<DaySchedule?>(null) }
    var wednesdaySchedule by remember { mutableStateOf<DaySchedule?>(null) }
    var thursdaySchedule by remember { mutableStateOf<DaySchedule?>(null) }
    var fridaySchedule by remember { mutableStateOf<DaySchedule?>(null) }
    var saturdaySchedule by remember { mutableStateOf<DaySchedule?>(null) }
    var sundaySchedule by remember { mutableStateOf<DaySchedule?>(null) }

    var isFetching by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch shift details on screen load
    LaunchedEffect(Unit) {
        isFetching = true
        when (val result = profileRepository.getShiftDetails()) {
            is ShiftDetailsOutcome.Success -> {
                result.shiftDetails?.let { data ->
                    shiftName = data.shift_name ?: ""
                    shiftCode = data.code ?: ""
                    multiplePunchAllowed = data.multiple_punch_allowed ?: false
                    faceRecognitionEnabled = data.face_recognition_enabled ?: false
                    fingerprintEnabled = data.fingerprint_recognition_enabled ?: false
                    weekOffDays = data.week_off_days ?: emptyList()
                    lateInMaxMinutes = data.late_in_max_minutes
                    earlyOutMaxMinutes = data.early_out_max_minutes
                    allowShortLeave = data.allow_short_leave ?: false
                    shortLeaveMinutes = data.short_leave_minutes_in_shift_time
                    breakType = data.break_type ?: ""
                    breakTimingType = data.break_timing_type ?: ""

                    // Weekly schedule
                    mondaySchedule = data.weekly_schedule?.monday
                    tuesdaySchedule = data.weekly_schedule?.tuesday
                    wednesdaySchedule = data.weekly_schedule?.wednesday
                    thursdaySchedule = data.weekly_schedule?.thursday
                    fridaySchedule = data.weekly_schedule?.friday
                    saturdaySchedule = data.weekly_schedule?.saturday
                    sundaySchedule = data.weekly_schedule?.sunday
                }
                isFetching = false
            }
            is ShiftDetailsOutcome.Error -> {
                errorMessage = result.message
                isFetching = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Shift Details",
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
                    // Loading State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    // Error State
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
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = errorMessage ?: "Failed to load shift details",
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
                    ) {
                        // Info Notice
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "This information is read-only and managed by HR",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Shift Overview Card
                        ShiftOverviewCard(
                            shiftName = shiftName,
                            shiftCode = shiftCode,
                            weekOffDays = weekOffDays
                        )

                        Spacer(Modifier.height(16.dp))

                        // Policies Card
                        PoliciesCard(
                            lateInMaxMinutes = lateInMaxMinutes,
                            earlyOutMaxMinutes = earlyOutMaxMinutes,
                            allowShortLeave = allowShortLeave,
                            shortLeaveMinutes = shortLeaveMinutes
                        )

                        Spacer(Modifier.height(16.dp))

                        // Settings Card
                        SettingsCard(
                            multiplePunchAllowed = multiplePunchAllowed,
                            faceRecognitionEnabled = faceRecognitionEnabled,
                            fingerprintEnabled = fingerprintEnabled,
                            breakType = breakType,
                            breakTimingType = breakTimingType
                        )

                        Spacer(Modifier.height(16.dp))

                        // Weekly Schedule Card
                        WeeklyScheduleCard(
                            monday = mondaySchedule,
                            tuesday = tuesdaySchedule,
                            wednesday = wednesdaySchedule,
                            thursday = thursdaySchedule,
                            friday = fridaySchedule,
                            saturday = saturdaySchedule,
                            sunday = sundaySchedule,
                            weekOffDays = weekOffDays
                        )

                        // Extra bottom padding
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

/**
 * Shift Overview Card
 */
@Composable
private fun ShiftOverviewCard(
    shiftName: String,
    shiftCode: String,
    weekOffDays: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Shift Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            InfoRow(label = "Shift Name", value = shiftName.ifEmpty { "Not Available" })
            Spacer(Modifier.height(12.dp))
            InfoRow(label = "Shift Code", value = shiftCode.ifEmpty { "Not Available" })
            Spacer(Modifier.height(12.dp))

            // Week Off Days
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Week Off Days",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                if (weekOffDays.isEmpty()) {
                    Text(
                        text = "No week offs",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        weekOffDays.forEach { day ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Policies Card
 */
@Composable
private fun PoliciesCard(
    lateInMaxMinutes: Int?,
    earlyOutMaxMinutes: Int?,
    allowShortLeave: Boolean,
    shortLeaveMinutes: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Attendance Policies",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            InfoRow(
                label = "Late In Allowed",
                value = if (lateInMaxMinutes != null) "$lateInMaxMinutes minutes" else "Not set"
            )
            Spacer(Modifier.height(12.dp))
            InfoRow(
                label = "Early Out Allowed",
                value = if (earlyOutMaxMinutes != null) "$earlyOutMaxMinutes minutes" else "Not set"
            )
            Spacer(Modifier.height(12.dp))
            InfoRow(
                label = "Short Leave",
                value = if (allowShortLeave) {
                    if (shortLeaveMinutes != null) "Allowed ($shortLeaveMinutes min)" else "Allowed"
                } else "Not Allowed"
            )
        }
    }
}

/**
 * Settings Card
 */
@Composable
private fun SettingsCard(
    multiplePunchAllowed: Boolean,
    faceRecognitionEnabled: Boolean,
    fingerprintEnabled: Boolean,
    breakType: String,
    breakTimingType: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Shift Settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            ToggleRow(label = "Multiple Punch", enabled = multiplePunchAllowed)
            Spacer(Modifier.height(12.dp))
            ToggleRow(label = "Face Recognition", enabled = faceRecognitionEnabled)
            Spacer(Modifier.height(12.dp))
            ToggleRow(label = "Fingerprint", enabled = fingerprintEnabled)
            Spacer(Modifier.height(12.dp))
            InfoRow(label = "Break Type", value = breakType.ifEmpty { "Not set" })
            Spacer(Modifier.height(12.dp))
            InfoRow(label = "Break Timing", value = breakTimingType.ifEmpty { "Not set" })
        }
    }
}

/**
 * Weekly Schedule Card
 */
@Composable
private fun WeeklyScheduleCard(
    monday: DaySchedule?,
    tuesday: DaySchedule?,
    wednesday: DaySchedule?,
    thursday: DaySchedule?,
    friday: DaySchedule?,
    saturday: DaySchedule?,
    sunday: DaySchedule?,
    weekOffDays: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Weekly Schedule",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            DayScheduleRow("Monday", monday, weekOffDays.contains("Monday"))
            Spacer(Modifier.height(12.dp))
            DayScheduleRow("Tuesday", tuesday, weekOffDays.contains("Tuesday"))
            Spacer(Modifier.height(12.dp))
            DayScheduleRow("Wednesday", wednesday, weekOffDays.contains("Wednesday"))
            Spacer(Modifier.height(12.dp))
            DayScheduleRow("Thursday", thursday, weekOffDays.contains("Thursday"))
            Spacer(Modifier.height(12.dp))
            DayScheduleRow("Friday", friday, weekOffDays.contains("Friday"))
            Spacer(Modifier.height(12.dp))
            DayScheduleRow("Saturday", saturday, weekOffDays.contains("Saturday"))
            Spacer(Modifier.height(12.dp))
            DayScheduleRow("Sunday", sunday, weekOffDays.contains("Sunday"))
        }
    }
}

/**
 * Day Schedule Row
 */
@Composable
private fun DayScheduleRow(dayName: String, schedule: DaySchedule?, isWeekOff: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = dayName,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))

        if (isWeekOff) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Week Off",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else if (schedule != null) {
            Column {
                if (schedule.shift_start_time != null && schedule.shift_end_time != null) {
                    Text(
                        text = "🕐 ${formatTime(schedule.shift_start_time)} - ${formatTime(schedule.shift_end_time)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (schedule.lunch_start_time != null && schedule.lunch_end_time != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "🍽️ Lunch: ${formatTime(schedule.lunch_start_time)} - ${formatTime(schedule.lunch_end_time)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (schedule.tea_start_time != null && schedule.tea_end_time != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "☕ Tea: ${formatTime(schedule.tea_start_time)} - ${formatTime(schedule.tea_end_time)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "No schedule",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Info Row Component
 */
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

/**
 * Toggle Row Component
 */
@Composable
private fun ToggleRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (enabled)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = if (enabled) "Enabled" else "Disabled",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Helper function to format time (removes seconds if present)
 */
private fun formatTime(time: String?): String {
    if (time == null) return ""
    // If time format is HH:MM:SS, return HH:MM
    return if (time.length > 5) time.substring(0, 5) else time
}