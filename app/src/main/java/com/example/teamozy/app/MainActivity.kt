package com.example.teamozy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import com.example.teamozy.core.utils.PreferencesManager
import com.example.teamozy.feature.auth.presentation.LoginScreen
import com.example.teamozy.feature.permissions.presentation.PermissionScreen
import com.example.teamozy.feature.home.presentation.HomePage
import com.example.teamozy.splash.presentation.SplashScreen

import com.example.teamozy.ui.theme.TeamozyTheme

enum class AppScreen { SPLASH, LOGIN, PERMISSIONS, HOME }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeamozyTheme { AppRoot() }
        }
    }
}

@Composable
private fun AppRoot() {
    var current by remember { mutableStateOf(AppScreen.SPLASH) }
    val context = LocalContext.current
    val pm = remember { PreferencesManager.getInstance(context) }

    when (current) {
        AppScreen.SPLASH -> SplashScreen(onComplete = {
            current = if (pm.isLoggedIn()) AppScreen.PERMISSIONS else AppScreen.LOGIN
        })
        AppScreen.LOGIN -> LoginScreen(onLoginSuccess = { current = AppScreen.PERMISSIONS })
        AppScreen.PERMISSIONS -> PermissionScreen(
            onAllGranted = { current = AppScreen.HOME },
            onBackToLogin = {
                pm.clearAll()
                current = AppScreen.LOGIN
            }
        )
        AppScreen.HOME -> HomePage(
            onLogout = {
                pm.clearAll()
                current = AppScreen.LOGIN
            }
        )
    }
}
