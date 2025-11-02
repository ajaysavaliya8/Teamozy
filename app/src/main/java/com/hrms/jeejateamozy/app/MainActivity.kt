package com.hrms.jeejateamozy.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.feature.auth.data.AuthRepository
import com.hrms.jeejateamozy.feature.auth.data.AuthOutcome
import com.hrms.jeejateamozy.feature.auth.presentation.LoginScreen
import com.hrms.jeejateamozy.feature.main.presentation.MainScreen
import com.hrms.jeejateamozy.feature.permissions.presentation.PermissionScreen
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.di.authModule
import com.hrms.jeejateamozy.di.attendanceModule
import com.hrms.jeejateamozy.di.permissionsModule
import com.hrms.jeejateamozy.di.homeModule

private enum class AppScreen {
    SPLASH,
    LOGIN,
    PERMISSIONS,
    HOME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize NetworkModule with context for auth interceptor
        NetworkModule.initialize(applicationContext)

        // Start Koin here (since there's no Application class now)
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(application)
                modules(
                    authModule,
                    attendanceModule,
                    permissionsModule,
                    homeModule
                )
            }
        }

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface { AppRoot() }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val authRepo: AuthRepository = koinInject()
    var current by remember { mutableStateOf(AppScreen.SPLASH) }

    when (current) {
        AppScreen.SPLASH -> InlineSplash(
            authRepository = authRepo,
            preferencesManager = prefs,
            onComplete = { isAuthorized ->
                current = if (isAuthorized) {
                    // User is logged in
                    // Check if permissions screen has been shown before
                    if (!prefs.hasShownPermissions) {
                        // First time after login - show permissions
                        AppScreen.PERMISSIONS
                    } else {
                        // Already shown before - go directly to home
                        AppScreen.HOME
                    }
                } else {
                    // Not logged in, go to login
                    AppScreen.LOGIN
                }
            }
        )

        AppScreen.PERMISSIONS -> PermissionScreen(
            onAllGood = {
                Log.d("MainActivity", "Permission screen completed (granted or skipped)")
                // Mark that permissions screen has been shown
                prefs.hasShownPermissions = true
                current = AppScreen.HOME
            }
        )

        AppScreen.LOGIN -> LoginScreen(
            onLoginSuccess = {
                Log.d("MainActivity", "Login success")
                // After successful login, reset the flag to show permissions
                prefs.hasShownPermissions = false
                current = AppScreen.PERMISSIONS
            }
        )

        AppScreen.HOME -> MainScreen(
            onLogout = {
                Log.d("MainActivity", "Logout, clearing preferences")
                prefs.clearAll()
                current = AppScreen.LOGIN
            }
        )
    }
}

@Composable
private fun InlineSplash(
    authRepository: AuthRepository,
    preferencesManager: PreferencesManager,
    onComplete: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(1500) // Short splash delay

        scope.launch {
            // Check if user is logged in (has token)
            val isLoggedIn = preferencesManager.isLoggedIn()

            if (isLoggedIn) {
                // For now, assume token is valid if it exists
                // You can add token validation logic here if needed
                onComplete(true)
            } else {
                // Not logged in
                onComplete(false)
            }
        }
    }

    // Simple splash screen UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Teamozy",
                style = MaterialTheme.typography.headlineLarge
            )
            CircularProgressIndicator()
        }
    }
}