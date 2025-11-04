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

    // State to manage EditSocialMedia screen
    var showEditSocialMedia by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentNavigationScreen,
                onScreenSelected = { screen ->
                    currentNavigationScreen = screen
                    // Reset child navigation when switching tabs
                    showEditSocialMedia = false
                }
            )
        }
    ) { paddingValues ->
        // Handle EditSocialMedia screen (full screen overlay)
        when {
            showEditSocialMedia -> {
                EditSocialMediaScreen(
                    onBack = {
                        showEditSocialMedia = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }
            else -> {
                // Main navigation screens
                when (currentNavigationScreen) {
                    NavigationScreen.HOME -> {
                        // ✅ HomePage shows ONLY home content
                        HomePage(
                            onLogout = onLogout,
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