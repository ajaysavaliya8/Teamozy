package com.example.teamozy

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.teamozy.ui.HomePage
import com.example.teamozy.ui.theme.TeamozyTheme

class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TeamozyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }

    @Composable
    private fun AppContent() {
        var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }

        when (currentScreen) {
            AppScreen.SPLASH -> {
                SplashScreen {
                    // Check if user is logged in
                    val prefs = getSharedPreferences("teamozy", MODE_PRIVATE)
                    val token = prefs.getString("token", "")
                    currentScreen = if (!token.isNullOrEmpty()) {
                        AppScreen.PERMISSIONS
                    } else {
                        AppScreen.LOGIN
                    }
                }
            }

            AppScreen.LOGIN -> {
                LoginScreen {
                    currentScreen = AppScreen.PERMISSIONS
                }
            }

            AppScreen.PERMISSIONS -> {
                if (PermissionHelper.checkAllPermissions(this@MainActivity, requiredPermissions)) {
                    currentScreen = AppScreen.HOME
                } else {
                    PermissionScreen(
                        requiredPermissions = requiredPermissions,
                        onPermissionsGranted = {
                            currentScreen = AppScreen.HOME
                        }
                    )
                }
            }

            AppScreen.HOME -> {
                HomePage()
            }
        }
    }

    enum class AppScreen {
        SPLASH, LOGIN, PERMISSIONS, HOME
    }
}