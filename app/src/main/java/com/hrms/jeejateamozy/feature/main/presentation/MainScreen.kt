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

/**
 * Main Screen Component
 * Manages navigation between Home, Attendance, and Profile screens
 * Uses BottomNavigationBar for navigation
 *
 * ✅ CORRECTED STRUCTURE:
 * - HOME tab → HomePage (only home content)
 * - ATTENDANCE tab → AttendanceScreen (placeholder)
 * - PROFILE tab → ProfileScreen (directly)
 *
 * Note: Face Registration is now handled inside ProfileScreen
 */
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    // State to track current selected bottom navigation screen
    var currentNavigationScreen by remember { mutableStateOf(NavigationScreen.HOME) }

    // State to manage child screens
    var showEditSocialMedia by remember { mutableStateOf(false) }
    var showEditContactDetail by remember { mutableStateOf(false) }
    var showEditPersonalInfo by remember { mutableStateOf(false) }           // CHANGED: Edit instead of View
    var showViewEmploymentDetail by remember { mutableStateOf(false) }

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
                }
            )
        }
    ) { paddingValues ->
        // Handle child screens (full screen overlays)
        when {
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

            // CHANGED: EditPersonalInfoScreen (editable)
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

            else -> {
                // Main navigation screens
                when (currentNavigationScreen) {
                    NavigationScreen.HOME -> {
                        HomePage(
                            onLogout = onLogout,
                            onNavigateToProfile = {
                                currentNavigationScreen = NavigationScreen.PROFILE
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
                        // ✅ Directly show ProfileScreen
                        // Face Registration is handled inside ProfileScreen
                        ProfileScreen(
                            onNavigateToFaceChange = {
                                // No action needed - ProfileScreen handles it internally
                            },
                            onNavigateToEditSocialMedia = {
                                // Show EditSocialMedia screen
                                showEditSocialMedia = true
                            },
                            onNavigateToEditContactDetail = {
                                // Show EditContactDetail screen
                                showEditContactDetail = true
                            },
                            onNavigateToEditPersonalInfo = {                  // Keep name for compatibility
                                // Show EditPersonalInfo screen (editable)
                                showEditPersonalInfo = true
                            },
                            onNavigateToViewEmploymentDetail = {
                                // Show ViewEmploymentDetail screen
                                showViewEmploymentDetail = true
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