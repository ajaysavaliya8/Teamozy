package com.hrms.jeejateamozy.feature.workreport.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkReportScreen(
    viewModel: WorkReportViewModel,
    onNavigateBack: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()

    // ✅ FIX: Handle gesture back navigation
    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Work Report",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TeamozyColors.Background)
                .padding(paddingValues)
        ) {
            // Tab switcher
            TabSwitcher(
                selectedTab = uiState.selectedTab,
                onTabSelected = viewModel::onTabSelected
            )

            Spacer(Modifier.height(16.dp))

            // Date selector (for Add New) or Month selector (for History)
            when (uiState.selectedTab) {
                WorkReportTab.ADD_NEW -> {
                    DateSelector(
                        selectedDate = uiState.selectedDate,
                        onDateSelected = viewModel::onDateSelected
                    )
                }
                WorkReportTab.HISTORY -> {
                    MonthSelector(
                        selectedMonth = uiState.selectedMonth,
                        onPreviousMonth = viewModel::onPreviousMonth,
                        onNextMonth = viewModel::onNextMonth
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tab content
            when (uiState.selectedTab) {
                WorkReportTab.ADD_NEW -> {
                    AddNewWorkReportTab(
                        viewModel = viewModel,
                        uiState = uiState,
                        bottomPadding = bottomPadding
                    )
                }
                WorkReportTab.HISTORY -> {
                    WorkReportHistoryTab(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }
            }
        }

        // Success/Error messages
        if (uiState.submitSuccess) {
            LaunchedEffect(uiState.submitSuccess) {
                kotlinx.coroutines.delay(2000)
                viewModel.onDismissSubmitSuccess()
            }
        }

        // Show report detail bottom sheet
        if (uiState.showReportDetail && uiState.selectedReport != null) {
            WorkReportDetailBottomSheet(
                report = uiState.selectedReport!!,
                onDismiss = viewModel::onDismissReportDetail
            )
        }
    }
}

@Composable
private fun DateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateSelected(selectedDate.minusDays(1)) }) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous day"
            )
        }

        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM, yyyy")),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        IconButton(onClick = { onDateSelected(selectedDate.plusDays(1)) }) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next day"
            )
        }
    }
}

@Composable
private fun MonthSelector(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous month"
            )
        }

        Text(
            text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM, yyyy")),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next month"
            )
        }
    }
}

@Composable
private fun TabSwitcher(
    selectedTab: WorkReportTab,
    onTabSelected: (WorkReportTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        TabButton(
            text = "Add New",
            isSelected = selectedTab == WorkReportTab.ADD_NEW,
            onClick = { onTabSelected(WorkReportTab.ADD_NEW) },
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(16.dp))

        TabButton(
            text = "History",
            isSelected = selectedTab == WorkReportTab.HISTORY,
            onClick = { onTabSelected(WorkReportTab.HISTORY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                TeamozyColors.Primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isSelected) {
                androidx.compose.ui.graphics.Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}