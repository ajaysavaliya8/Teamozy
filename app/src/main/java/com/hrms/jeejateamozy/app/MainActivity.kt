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
import androidx.lifecycle.lifecycleScope
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
import com.hrms.jeejateamozy.di.leaveModule
import com.hrms.jeejateamozy.di.attendanceHistoryModule

// OneSignal imports
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

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

        // ============================================
        // INITIALIZE ONESIGNAL
        // ============================================
        initializeOneSignal()

        // Start Koin here (since there's no Application class now)
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(application)
                modules(
                    authModule,
                    attendanceModule,
                    permissionsModule,
                    homeModule,
                    circularModule,
                    leaveModule,
                    attendanceHistoryModule
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

    /**
     * Initialize OneSignal Push Notifications
     */
    private fun initializeOneSignal() {
        try {
            // Enable verbose logging for debugging (disable in production)
            OneSignal.Debug.logLevel = LogLevel.VERBOSE

            // Initialize OneSignal with your App ID
            OneSignal.initWithContext(this, "d28eccbe-be33-48f3-a12d-fc5d75b4ac69")


            // Request notification permission (Android 13+) - using lifecycleScope
            lifecycleScope.launch {
                try {
                    OneSignal.Notifications.requestPermission(true)
                    Log.d("OneSignal", "✅ Notification permission requested")
                } catch (e: Exception) {
                    Log.e("OneSignal", "Failed to request permission", e)
                }
            }

            // Log the player ID when available
            OneSignal.User.pushSubscription.id?.let { playerId ->
                Log.d("OneSignal", "✅ Player ID: $playerId")
            }

            Log.d("OneSignal", "✅ OneSignal initialized successfully")
        } catch (e: Exception) {
            Log.e("OneSignal", "❌ Failed to initialize OneSignal", e)
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
    val scope = rememberCoroutineScope()
    val repo: AuthRepository = koinInject()
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Check if we should show permission dialog
    LaunchedEffect(Unit) {
        // Only check on first time or if permissions not granted
        if (!preferencesManager.hasShownPermissions) {
            // Check if permissions are already granted
            val permissionsGranted = arePermissionsGranted(context)
            if (!permissionsGranted) {
                showPermissionDialog = true
            } else {
                // If permissions are already granted, mark as shown
                preferencesManager.hasShownPermissions = true
            }
        }
    }

    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = {
                showPermissionDialog = false
                preferencesManager.hasShownPermissions = true
            },
            onPermissionsHandled = {
                showPermissionDialog = false
                preferencesManager.hasShownPermissions = true
            }
        )
    }

    MainScreen(
        onLogout = {
            // Call logout API
            scope.launch {
                Log.d("MainActivity", "Logout requested")

                when (val outcome = repo.logout(clearPushToken = true)) {
                    is AuthOutcome.Success -> {
                        Log.d("MainActivity", "✅ Logout successful: ${outcome.message}")
                        onLogout()
                    }
                    is AuthOutcome.Error -> {
                        Log.e("MainActivity", "❌ Logout error: ${outcome.message}")
                        // Still logout locally
                        onLogout()
                    }
                    else -> {
                        onLogout()
                    }
                }
            }
        }
    )
}

@Composable
private fun InlineSplash(
    authRepository: AuthRepository,
    preferencesManager: PreferencesManager,
    onComplete: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(1000)  // Show splash for 1 second

        val token = preferencesManager.authToken
        if (token.isNullOrBlank()) {
            onComplete(false)
        } else {
            when (authRepository.verifyToken()) {
                is AuthOutcome.Success -> onComplete(true)
                is AuthOutcome.Error -> {
                    preferencesManager.clearAll()
                    onComplete(false)
                }
                is AuthOutcome.DeviceNotRegistered -> {
                    preferencesManager.clearAll()
                    onComplete(false)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}