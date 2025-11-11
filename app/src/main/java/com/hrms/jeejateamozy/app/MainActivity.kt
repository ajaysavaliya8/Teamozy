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
import com.hrms.jeejateamozy.feature.permissions.presentation.PermissionDialog
import com.hrms.jeejateamozy.feature.permissions.presentation.arePermissionsGranted
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.di.authModule
import com.hrms.jeejateamozy.di.attendanceModule
import com.hrms.jeejateamozy.di.circularModule
import com.hrms.jeejateamozy.di.permissionsModule
import com.hrms.jeejateamozy.di.homeModule

private enum class AppScreen {
    SPLASH,
    LOGIN,
    HOME  // Permission popup shows here automatically if needed
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
                    homeModule,
                    circularModule
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
                    // User is logged in - go directly to home
                    // Permission popup will check and show automatically if needed
                    AppScreen.HOME
                } else {
                    // Not logged in, go to login
                    AppScreen.LOGIN
                }
            }
        )

        AppScreen.LOGIN -> LoginScreen(
            onLoginSuccess = {
                Log.d("MainActivity", "Login success")
                // After successful login, go directly to home
                // Permission popup will check and show automatically if needed
                current = AppScreen.HOME
            }
        )

        AppScreen.HOME -> {
            // Show home screen with automatic permission dialog if needed
            HomeWithPermissions(
                preferencesManager = prefs,
                onLogout = {
                    Log.d("MainActivity", "Logout, clearing preferences")
                    prefs.clearAll()
                    current = AppScreen.LOGIN
                }
            )
        }
    }
}

@Composable
private fun HomeWithPermissions(
    preferencesManager: PreferencesManager,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Check if we should show permission dialog
    LaunchedEffect(Unit) {
        // Only check on first time or if permissions not granted
        if (!preferencesManager.hasShownPermissions) {
            // Check if permissions are already granted
            if (arePermissionsGranted(context)) {
                // All permissions already granted - mark as handled
                preferencesManager.hasShownPermissions = true
            } else {
                // Some permissions missing - show dialog
                // The dialog will check again internally
                showPermissionDialog = true
            }
        }
    }

    // Main home screen
    MainScreen(onLogout = onLogout)

    // Show permission dialog overlay if needed
    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = {
                // User dismissed without granting all
                showPermissionDialog = false
                // Mark that we've shown the dialog
                preferencesManager.hasShownPermissions = true
                Log.d("MainActivity", "Permission dialog dismissed")
            },
            onPermissionsHandled = {
                // All permissions granted successfully
                showPermissionDialog = false
                // Mark that we've shown and handled permissions
                preferencesManager.hasShownPermissions = true
                Log.d("MainActivity", "All permissions granted")
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