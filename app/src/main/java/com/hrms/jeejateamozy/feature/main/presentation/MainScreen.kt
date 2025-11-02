package com.hrms.jeejateamozy.feature.main.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.hrms.jeejateamozy.feature.home.presentation.HomePage

/**
 * Main Screen Component
 * Manages navigation between Home, Attendance, and Profile screens
 * Uses BottomNavigationBar for navigation
 *
 * This is the main container that:
 * 1. Displays the BottomNavigationBar
 * 2. Routes to HomePage (which handles Home, Profile, and Edit Social Media internally)
 * 3. Provides placeholder for Attendance screen
 */
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    // State to track current selected bottom navigation screen
    var currentNavigationScreen by remember { mutableStateOf(NavigationScreen.HOME) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentNavigationScreen,
                onScreenSelected = { screen ->
                    currentNavigationScreen = screen
                }
            )
        }
    ) { paddingValues ->
        // Screen content based on current bottom navigation selection
        when (currentNavigationScreen) {
            NavigationScreen.HOME -> {
                // HomePage handles: Home content, Profile screen, Edit Social Media screen
                // It manages its own internal navigation between these screens
                HomePage(
                    onLogout = onLogout,
                    paddingValues = paddingValues  // Pass padding for bottom navigation
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
                // Profile is handled by HomePage, so navigate to HOME and let HomePage show Profile
                LaunchedEffect(Unit) {
                    currentNavigationScreen = NavigationScreen.HOME
                }
                // Temporarily show HomePage while switching
                HomePage(
                    onLogout = onLogout,
                    paddingValues = paddingValues,
                    initialScreen = "PROFILE"  // Tell HomePage to start with Profile screen
                )
            }
        }
    }
}