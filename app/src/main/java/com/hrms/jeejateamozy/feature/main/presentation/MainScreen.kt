package com.hrms.jeejateamozy.feature.main.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.hrms.jeejateamozy.feature.home.presentation.HomePage
import com.hrms.jeejateamozy.feature.profile.presentation.ProfileScreen
import com.hrms.jeejateamozy.feature.profile.presentation.EditSocialMediaScreen
import com.hrms.jeejateamozy.feature.profile.presentation.EditContactDetailScreen
import com.hrms.jeejateamozy.feature.profile.presentation.EditPersonalInfoScreen
import com.hrms.jeejateamozy.feature.profile.presentation.ViewEmploymentDetailScreen
import com.hrms.jeejateamozy.feature.profile.presentation.EditBankingInfoScreen
import com.hrms.jeejateamozy.feature.profile.presentation.ViewEmploymentIdentityScreen
import com.hrms.jeejateamozy.feature.profile.presentation.ViewShiftDetailsScreen
import com.hrms.jeejateamozy.feature.workreport.presentation.WorkReportScreen
import com.hrms.jeejateamozy.feature.workreport.presentation.WorkReportViewModel
import com.hrms.jeejateamozy.feature.circular.presentation.CircularListScreen
import com.hrms.jeejateamozy.feature.circular.presentation.CircularDetailScreen
import com.hrms.jeejateamozy.feature.circular.presentation.CircularViewModel
import com.hrms.jeejateamozy.feature.leave.presentation.LeaveHistoryScreen
import org.koin.androidx.compose.koinViewModel
import com.hrms.jeejateamozy.feature.leave.presentation.ApplyLeaveScreen
import com.hrms.jeejateamozy.feature.leave.presentation.LeaveHistoryScreen
import com.hrms.jeejateamozy.feature.leave.presentation.LeaveViewModel
var showAttendanceHistory by remember { mutableStateOf(false) }
var selectedAttendanceDate by remember { mutableStateOf<String?>(null) }


@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    // State to track current selected bottom navigation screen
    var currentNavigationScreen by remember { mutableStateOf(NavigationScreen.HOME) }

    // State to manage child screens
    var showEditSocialMedia by remember { mutableStateOf(false) }
    var showEditContactDetail by remember { mutableStateOf(false) }
    var showEditPersonalInfo by remember { mutableStateOf(false) }
    var showViewEmploymentDetail by remember { mutableStateOf(false) }
    var showEditBankingInfo by remember { mutableStateOf(false) }
    var showViewEmploymentIdentity by remember { mutableStateOf(false) }
    var showViewShiftDetails by remember { mutableStateOf(false) }
    var showWorkReport by remember { mutableStateOf(false) }

    var showCircularList by remember { mutableStateOf(false) }
    var showCircularDetail by remember { mutableStateOf<Int?>(null) }

    var showApplyLeave by remember { mutableStateOf(false) }
    var showLeaveHistory by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentNavigationScreen,
                onScreenSelected = { screen ->
                    currentNavigationScreen = screen

                    // Reset child navigation when switching tabs
                    showEditSocialMedia = false
                    showEditContactDetail = false
                    showEditPersonalInfo = false
                    showViewEmploymentDetail = false
                    showEditBankingInfo = false
                    showViewEmploymentIdentity = false
                    showViewShiftDetails = false
                    showWorkReport = false
                    // ⭐ NEW: Reset circular navigation
                    showCircularList = false
                    showCircularDetail = null
                    showApplyLeave = false
                    showLeaveHistory = false
                }
            )
        }
    ) { paddingValues ->
        // Handle child screens (full screen overlays)
        when {
            // Leave History Screen
            showLeaveHistory -> {
                val leaveViewModel: LeaveViewModel = koinViewModel()

                LeaveHistoryScreen(
                    viewModel = leaveViewModel,
                    onNavigateBack = {
                        showLeaveHistory = false
                        currentNavigationScreen = NavigationScreen.HOME
                    },
                    onNavigateToApplyLeave = {
                        showLeaveHistory = false
                        showApplyLeave = true
                    }
                )
            }

            // Apply Leave Screen
            showApplyLeave -> {
                val leaveViewModel: LeaveViewModel = koinViewModel()

                ApplyLeaveScreen(
                    viewModel = leaveViewModel,
                    onNavigateBack = {
                        showApplyLeave = false
                        currentNavigationScreen = NavigationScreen.HOME
                    },
                    onNavigateToHistory = { applicationId ->
                        showApplyLeave = false
                        showLeaveHistory = true
                    }
                )
            }
            // ⭐ NEW: Circular Detail Screen
            showCircularDetail != null -> {
                val circularViewModel: CircularViewModel = koinViewModel()

                CircularDetailScreen(
                    viewModel = circularViewModel,
                    circularId = showCircularDetail!!,
                    onNavigateBack = {
                        showCircularDetail = null
                        showCircularList = true
                    }
                )
            }

            // ⭐ NEW: Circular List Screen
            showCircularList -> {
                val circularViewModel: CircularViewModel = koinViewModel()

                CircularListScreen(
                    viewModel = circularViewModel,
                    onNavigateToDetail = { circularId ->
                        showCircularDetail = circularId
                        showCircularList = false
                    },
                    onNavigateBack = {
                        showCircularList = false
                        currentNavigationScreen = NavigationScreen.HOME
                    }
                )
            }

            showEditSocialMedia -> {
                EditSocialMediaScreen(
                    onBack = {
                        showEditSocialMedia = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showEditContactDetail -> {
                EditContactDetailScreen(
                    onBack = {
                        showEditContactDetail = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showEditPersonalInfo -> {
                EditPersonalInfoScreen(
                    onBack = {
                        showEditPersonalInfo = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showViewEmploymentDetail -> {
                ViewEmploymentDetailScreen(
                    onBack = {
                        showViewEmploymentDetail = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showEditBankingInfo -> {
                EditBankingInfoScreen(
                    onBack = {
                        showEditBankingInfo = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showViewEmploymentIdentity -> {
                ViewEmploymentIdentityScreen(
                    onBack = {
                        showViewEmploymentIdentity = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showViewShiftDetails -> {
                ViewShiftDetailsScreen(
                    onBack = {
                        showViewShiftDetails = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            // ⭐ WORK REPORT SCREEN ⭐
            showWorkReport -> {
                val workReportViewModel: WorkReportViewModel = koinViewModel()

                WorkReportScreen(
                    viewModel = workReportViewModel,
                    onNavigateBack = {
                        showWorkReport = false
                        currentNavigationScreen = NavigationScreen.HOME
                    }
                )
            }

            else -> {
                // Main navigation screens
                when (currentNavigationScreen) {
                    NavigationScreen.HOME -> {
                        HomePage(
                            onLogout = onLogout,
                            onNavigateToProfile = {
                                currentNavigationScreen = NavigationScreen.PROFILE
                            },
                            onNavigateToWorkReport = {
                                showWorkReport = true
                            },
                            // ⭐ NEW: Circular navigation callback
                            onNavigateToCircular = {
                                showCircularList = true
                            },
                            onNavigateToApplyLeaves = {
                                showApplyLeave = true
                            },
                            paddingValues = paddingValues
                        )
                    }

                    NavigationScreen.ATTENDANCE -> {
                        // Attendance screen placeholder (coming soon)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Attendance Screen\n(Coming Soon)",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    NavigationScreen.PROFILE -> {
                        // ✅ ProfileScreen with ALL required parameters
                        ProfileScreen(
                            onNavigateToFaceChange = {
                                // No action needed - ProfileScreen handles it internally
                            },
                            onNavigateToEditSocialMedia = {
                                showEditSocialMedia = true
                            },
                            onNavigateToEditContactDetail = {
                                showEditContactDetail = true
                            },
                            onNavigateToEditPersonalInfo = {
                                showEditPersonalInfo = true
                            },
                            onNavigateToViewEmploymentDetail = {
                                showViewEmploymentDetail = true
                            },
                            onNavigateToEditBankingInfo = {
                                showEditBankingInfo = true
                            },
                            onNavigateToViewEmploymentIdentity = {
                                showViewEmploymentIdentity = true
                            },
                            onNavigateToViewShiftDetails = {
                                showViewShiftDetails = true
                            },
                            onLogout = onLogout,
                            onBack = {
                                // Go back to HOME tab
                                currentNavigationScreen = NavigationScreen.HOME
                            }
                        )
                    }
                }
            }
        }
    }
}