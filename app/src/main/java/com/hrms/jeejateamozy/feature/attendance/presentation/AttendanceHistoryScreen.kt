package com.hrms.jeejateamozy.feature.attendance.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import com.hrms.jeejateamozy.core.network.CalendarDay
import com.hrms.jeejateamozy.core.network.MonthSummary
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    viewModel: AttendanceHistoryViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDayDetail: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.historyUiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AttendanceHistoryEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is AttendanceHistoryEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is AttendanceHistoryEvent.NavigateToDayDetail -> {
                    onNavigateToDayDetail(event.date)
                }
            }
        }
    }

    Scaffold(
        containerColor = TeamozyColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Attendance History",
                            fontWeight = FontWeight.Bold,
                            color = TeamozyColors.Heading
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TeamozyColors.Heading)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = TeamozyColors.Heading)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                )
            )
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TeamozyColors.Background)
                .padding(scaffoldPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 16.dp)
            ) {
                item {
                    CompactMonthNavigationBar(
                        monthName = uiState.monthName,
                        year = uiState.currentYear,
                        onPreviousMonth = { viewModel.loadPreviousMonth() },
                        onNextMonth = { viewModel.loadNextMonth() }
                    )
                }

                item {
                    if (uiState.calendarDays.isNotEmpty()) {
                        CalendarGrid(
                            days = uiState.calendarDays,
                            onDayClick = { day ->
                                onNavigateToDayDetail(day.date)
                            }
                        )
                    } else if (!uiState.isLoading) {
                        EmptyCalendarState()
                    }
                }

                item {
                    uiState.summary?.let { summary ->
                        MonthSummaryCard(summary = summary)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun CompactMonthNavigationBar(
    monthName: String,
    year: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = TeamozyColors.Primary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TeamozyColors.Heading
                )
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TeamozyColors.Secondary
                )
            }

            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = TeamozyColors.Primary
                )
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(summary: MonthSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryStatItem(
                value = summary.presentDays.toString(),
                label = "Present",
                valueColor = Color(0xFF10B981)
            )
            VerticalDivider(modifier = Modifier.height(48.dp))
            SummaryStatItem(
                value = summary.irregularDays.toString(),
                label = "Irregular",
                valueColor = Color(0xFFEF4444)
            )
            VerticalDivider(modifier = Modifier.height(48.dp))
            SummaryStatItem(
                value = summary.totalTime,
                label = "Total Time",
                valueColor = TeamozyColors.Primary
            )
        }
    }
}

@Composable
private fun SummaryStatItem(
    value: String,
    label: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarGrid(
    days: List<CalendarDay>,
    onDayClick: (CalendarDay) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            items(days) { day ->
                CalendarDayItem(
                    day = day,
                    onClick = { onDayClick(day) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CalendarLegend(days = days)
    }
}

@Composable
private fun CalendarDayItem(
    day: CalendarDay,
    onClick: () -> Unit
) {
    if (day.day == 0) {
        Box(modifier = Modifier.aspectRatio(1f))
        return
    }

    val parsedColor = remember(day.color) { parseHexColor(day.color) }
    val hasColor = parsedColor != Color.Transparent

    val backgroundColor = if (hasColor) parsedColor.copy(alpha = 0.75f) else Color.Transparent
    val borderColor = if (hasColor) parsedColor.copy(alpha = 0.9f) else Color(0xFFE0E0E0)
    val textColor = if (hasColor) Color.White else Color.Black

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.day.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (hasColor) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )

            // Show correction pending indicator
            if (day.isCorrectionRequestPending) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFFF59E0B), shape = RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarLegend(days: List<CalendarDay>) {
    // Build legend dynamically from actual calendar data
    val legendItems = remember(days) {
        days.filter { it.day > 0 && it.color.isNotBlank() }
            .map { it.status.replace("_", " ").lowercase()
                .replaceFirstChar { c -> c.uppercase() } to it.color }
            .distinctBy { it.second }
            .sortedBy { it.first }
    }

    if (legendItems.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            legendItems.forEach { (label, colorCode) ->
                LegendItem(color = parseHexColor(colorCode), label = label)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyCalendarState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No attendance records for this month",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        Color(colorInt)
    } catch (e: Exception) {
        Color.Transparent
    }
}

