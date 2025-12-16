package com.hrms.jeejateamozy.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.android.ext.koin.androidContext
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.feature.auth.data.AuthRepository
import com.hrms.jeejateamozy.feature.auth.data.AuthOutcome
import com.hrms.jeejateamozy.feature.auth.presentation.LoginScreen
import com.hrms.jeejateamozy.feature.auth.presentation.dialogs.AppVersionUpdateDialog  // ⚡ NEW IMPORT
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

// Firebase imports
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.hrms.jeejateamozy.di.locationModule

private enum class AppScreen {
    SPLASH,
    LOGIN,
    HOME  // Permission popup shows here automatically if needed
}

class MainActivity : ComponentActivity() {

    // Permission launcher for notification permission (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("FCM", "✅ Notification permission granted")
            // Get FCM token after permission is granted
            lifecycleScope.launch {
                getFCMToken()
            }
        } else {
            Log.w("FCM", "⚠️ Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize NetworkModule with context for auth interceptor
        NetworkModule.initialize(applicationContext)

        // ============================================
        // INITIALIZE FIREBASE
        // ============================================
        initializeFirebase()

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
                    attendanceHistoryModule ,
                    locationModule
                )
            }
        }

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                // ✅ GLOBAL STATUS BAR CONFIGURATION - APPLIES TO ALL SCREENS
                ConfigureSystemBars()

                Surface {
                    AppRoot()
                }
            }
        }
    }

    /**
     * ✅ Configure System Bars (Status Bar) Globally for All Screens
     * This ensures consistent status bar color across the entire app
     */
    @Composable
    private fun ConfigureSystemBars() {
        val view = LocalView.current
        val primaryColor = MaterialTheme.colorScheme.primary

        DisposableEffect(primaryColor) {
            val window = window

            // Set status bar color to match TopAppBar (primary color)
            window.statusBarColor = primaryColor.toArgb()

            // Set status bar icons to light (white) for visibility on dark background
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false  // false = light/white icons
            }

            onDispose {
                // No cleanup needed - status bar stays consistent
            }
        }
    }

    /**
     * Initialize Firebase and get FCM token
     */
    private fun initializeFirebase() {
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            Log.d("FCM", "✅ Firebase initialized successfully")

            // Request notification permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        Log.d("FCM", "✅ Notification permission already granted")
                        // Get FCM token
                        lifecycleScope.launch {
                            getFCMToken()
                        }
                    }
                    else -> {
                        // Request permission
                        Log.d("FCM", "📋 Requesting notification permission")
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                // For Android < 13, notification permission is granted by default
                Log.d("FCM", "✅ Notification permission not required for this Android version")
                lifecycleScope.launch {
                    getFCMToken()
                }
            }

        } catch (e: Exception) {
            Log.e("FCM", "❌ Failed to initialize Firebase", e)
        }
    }

    /**
     * Get FCM Token and save to PreferencesManager
     */
    private suspend fun getFCMToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()

            if (token.isNotBlank()) {
                Log.d("FCM", "✅ FCM Token retrieved: ${token.take(30)}...")

                // Save token to PreferencesManager
                val prefsManager = PreferencesManager.getInstance(applicationContext)
                prefsManager.fcmToken = token

                Log.d("FCM", "✅ FCM token saved to PreferencesManager")
            } else {
                Log.w("FCM", "⚠️ FCM token is blank")
            }
        } catch (e: Exception) {
            Log.e("FCM", "❌ Failed to get FCM token", e)
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

// ⚡ UPDATED: Added 426 handling
@Composable
private fun InlineSplash(
    authRepository: AuthRepository,
    preferencesManager: PreferencesManager,
    onComplete: (isAuthorized: Boolean) -> Unit
) {
    var status by remember { mutableStateOf("Initializing...") }

    // ⚡ NEW: State for update dialog
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }

    // ⚡ NEW: Show update dialog if needed - blocks entire UI
    if (showUpdateDialog) {
        AppVersionUpdateDialog(
            message = updateMessage,
            onDismiss = null // Cannot be dismissed - user must update
        )
        // Don't render the rest of the splash screen when update is required
        return
    }

    LaunchedEffect(Unit) {
        delay(1000)
        status = "Verifying session..."
        delay(500)

        // ⚡ UPDATED: Handle all auth outcomes including UpdateRequired
        when (val outcome = authRepository.verifyToken()) {
            is AuthOutcome.Success -> {
                Log.d("SplashScreen", "Token valid - navigating to home")
                onComplete(true)
            }
            is AuthOutcome.UpdateRequired -> {
                // ⚡ NEW: Show update dialog and block navigation
                Log.d("SplashScreen", "⚠️ Update required: ${outcome.message}")
                updateMessage = outcome.message
                showUpdateDialog = true
                // Don't call onComplete - stay on splash with update dialog
            }
            else -> {
                Log.d("SplashScreen", "Token invalid or missing - navigating to login")
                onComplete(false)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Teamozy",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "HR Management System",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(48.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(text = status, textAlign = TextAlign.Center)
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
        delay(300)
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
                        // Clear local data even if API fails
                        onLogout()
                    }
                    else -> {
                        Log.w("MainActivity", "⚠️ Unexpected logout outcome")
                        onLogout()
                    }
                }
            }
        }
    )
}