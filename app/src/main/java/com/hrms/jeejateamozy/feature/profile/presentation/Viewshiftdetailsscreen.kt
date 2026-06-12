@file:OptIn(ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import com.hrms.jeejateamozy.feature.profile.data.DaySchedule
import com.hrms.jeejateamozy.feature.profile.data.ShiftDetailsData
import com.hrms.jeejateamozy.feature.profile.data.ProfileRepository
import com.hrms.jeejateamozy.feature.profile.data.ShiftDetailsOutcome

/**
 * ViewShiftDetailsScreen - Display shift details (READ ONLY)
 * Shows shift overview, attendance policies, shift settings, and weekly schedule
 */
@Composable
fun ViewShiftDetailsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context) }

    // Single state variable for all shift data
    var shiftData by remember { mutableStateOf<ShiftDetailsData?>(null) }
    var isFetching by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch shift details on screen load
    LaunchedEffect(Unit) {
        isFetching = true
        when (val result = profileRepository.getShiftDetails()) {
            is ShiftDetailsOutcome.Success -> {
                shiftData = result.shiftDetails
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
                    val data = shiftData
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
                                containerColor = TeamozyColors.Primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = TeamozyColors.Primary,
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
                        ShiftOverviewCard(data)

                        Spacer(Modifier.height(16.dp))

                        // Attendance Policies Card
                        AttendancePoliciesCard(data)

                        Spacer(Modifier.height(16.dp))

                        // Shift Settings Card
                        ShiftSettingsCard(data)

                        Spacer(Modifier.height(16.dp))

                        // Weekly Schedule Card
                        WeeklyScheduleCard(data?.weekly_schedule)

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
private fun ShiftOverviewCard(data: ShiftDetailsData?) {
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
                    tint = TeamozyColors.Primary,
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

            InfoRow(
                label = "Shift Name",
                value = data?.shift_name?.ifEmpty { "Not Available" } ?: "Not Available"
            )
            Spacer(Modifier.height(12.dp))
            InfoRow(
                label = "Shift Code",
                value = data?.code?.ifEmpty { "Not Available" } ?: "Not Available"
            )
            Spacer(Modifier.height(12.dp))
            InfoRow(
                label = "Shift Type",
                value = data?.shift_type?.ifEmpty { "Not Available" } ?: "Not Available"
            )
            Spacer(Modifier.height(12.dp))
            InfoRow(
                label = "Attendance Calculation",
                value = data?.attendance_calc_mode?.ifEmpty { "Not Available" } ?: "Not Available"
            )
            Spacer(Modifier.height(12.dp))

            // Week Off Days
            val weekOffDays = data?.week_off_days ?: emptyList()
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
                                    containerColor = TeamozyColors.Primary.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TeamozyColors.PrimaryDark,
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
 * Attendance Policies Card (from policy object)
 */
@Composable
private fun AttendancePoliciesCard(data: ShiftDetailsData?) {
    val policy = data?.policy

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
                    tint = TeamozyColors.Primary,
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

            if (policy == null) {
                Text(
                    text = "No policy information available",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Late In Section
                Text(
                    text = "Late In",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeamozyColors.Primary
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = "Grace Minutes",
                    value = policy.late_in_grace_minutes?.let { "$it min" } ?: "Not set"
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = "Max Allowed",
                    value = policy.late_in_max_minutes?.let { "$it min" } ?: "Not set"
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = "Deduction Type",
                    value = policy.late_in_deduction_type?.ifEmpty { "Not set" } ?: "Not set"
                )
                Spacer(Modifier.height(4.dp))
                ToggleRow(
                    label = "Cover with Overtime",
                    enabled = policy.late_in_cover_with_overtime ?: false
                )

                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // Early Out Section
                Text(
                    text = "Early Out",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeamozyColors.Primary
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = "Grace Minutes",
                    value = policy.early_out_grace_minutes?.let { "$it min" } ?: "Not set"
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = "Max Allowed",
                    value = policy.early_out_max_minutes?.let { "$it min" } ?: "Not set"
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = "Deduction Type",
                    value = policy.early_out_deduction_type?.ifEmpty { "Not set" } ?: "Not set"
                )
                Spacer(Modifier.height(4.dp))
                ToggleRow(
                    label = "Cover with Early Arrival",
                    enabled = policy.early_out_cover_with_early_arrival ?: false
                )

                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // Overtime Section
                Text(
                    text = "Overtime",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeamozyColors.Primary
                )
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    label = "Overtime",
                    enabled = policy.overtime_enabled ?: false
                )
                if (policy.overtime_enabled == true) {
                    Spacer(Modifier.height(8.dp))
                    InfoRow(
                        label = "Threshold",
                        value = policy.overtime_threshold_minutes?.let { "$it min" } ?: "Not set"
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoRow(
                        label = "Max Per Day",
                        value = policy.max_overtime_hours_per_day?.let { "$it hrs" } ?: "Not set"
                    )
                }

                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // Short Leave Section
                Text(
                    text = "Short Leave",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeamozyColors.Primary
                )
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    label = "Short Leave",
                    enabled = policy.short_leave_enabled ?: false
                )
                if (policy.short_leave_enabled == true) {
                    Spacer(Modifier.height(8.dp))
                    InfoRow(
                        label = "Max Duration",
                        value = policy.short_leave_max_minutes?.let { "$it min" } ?: "Not set"
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoRow(
                        label = "Monthly Limit",
                        value = policy.short_leave_monthly_limit?.toString() ?: "Not set"
                    )
                }
            }
        }
    }
}

/**
 * Shift Settings Card
 */
@Composable
private fun ShiftSettingsCard(data: ShiftDetailsData?) {
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
                    tint = TeamozyColors.Primary,
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

            ToggleRow(label = "Multiple Check-in", enabled = data?.multiple_check_allowed ?: false)
            if (data?.multiple_check_allowed == true && data.max_checks_per_day != null) {
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = "Max Checks Per Day",
                    value = data.max_checks_per_day.toString()
                )
            }
            Spacer(Modifier.height(12.dp))
            ToggleRow(label = "Face Recognition", enabled = data?.face_recognition_enabled ?: false)
            Spacer(Modifier.height(12.dp))
            ToggleRow(label = "Fingerprint", enabled = data?.fingerprint_enabled ?: false)
            Spacer(Modifier.height(12.dp))
            ToggleRow(label = "GPS Tracking", enabled = data?.gps_tracking_enabled ?: false)
            Spacer(Modifier.height(12.dp))
            ToggleRow(label = "Geofence Required", enabled = data?.geofence_required ?: false)
            Spacer(Modifier.height(12.dp))
            InfoRow(
                label = "Break Type",
                value = data?.break_type?.ifEmpty { "Not set" } ?: "Not set"
            )
            Spacer(Modifier.height(12.dp))
            ToggleRow(label = "Break Check Required", enabled = data?.break_check_required ?: false)
        }
    }
}

/**
 * Weekly Schedule Card
 */
@Composable
private fun WeeklyScheduleCard(weeklySchedule: Map<String, DaySchedule>?) {
    // Ordered days of the week
    val dayOrder = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")

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
                    tint = TeamozyColors.Primary,
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

            if (weeklySchedule.isNullOrEmpty()) {
                Text(
                    text = "No schedule information available",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                dayOrder.forEachIndexed { index, dayKey ->
                    val schedule = weeklySchedule[dayKey]
                    val displayName = dayKey.replaceFirstChar { it.uppercase() }
                    DayScheduleRow(
                        dayName = displayName,
                        schedule = schedule
                    )
                    if (index < dayOrder.lastIndex) {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * Day Schedule Row
 */
@Composable
private fun DayScheduleRow(dayName: String, schedule: DaySchedule?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = dayName,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))

        if (schedule == null) {
            Text(
                text = "No schedule",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (schedule.is_working_day == false) {
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
        } else {
            Column {
                // Shift times
                if (schedule.shift_start_time != null && schedule.shift_end_time != null) {
                    Text(
                        text = "${formatTime(schedule.shift_start_time)} - ${formatTime(schedule.shift_end_time)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Full day and half day hours
                if (schedule.min_full_day_hours != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Full Day: ${schedule.min_full_day_hours} hrs",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (schedule.min_half_day_hours != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Half Day: ${schedule.min_half_day_hours} hrs",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                    TeamozyColors.Primary.copy(alpha = 0.15f)
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
                    TeamozyColors.PrimaryDark
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
