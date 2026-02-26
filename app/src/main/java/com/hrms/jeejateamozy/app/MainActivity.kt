package com.hrms.jeejateamozy.app

import android.Manifest
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
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
import com.hrms.jeejateamozy.feature.auth.presentation.dialogs.AppVersionUpdateDialog
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
import com.hrms.jeejateamozy.di.locationModule
import com.hrms.jeejateamozy.di.notificationModule
import com.hrms.jeejateamozy.feature.notification.utils.NotificationHelper
import com.hrms.jeejateamozy.core.state.AppEvent
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.navigation.DeepLink
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

private enum class AppScreen {
    SPLASH,
    LOGIN,
    HOME
}

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Deep link from notification - supports all screen types
    private var pendingDeepLink by mutableStateOf<DeepLink?>(null)

    // Permission launcher for notification permission (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("FCM", "✅ Notification permission granted")
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

        // Handle notification deep link (fast, no I/O)
        handleNotificationIntent(intent)

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
                    attendanceHistoryModule,
                    locationModule,
                    notificationModule
                )
            }
        }

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    AppRoot(
                        initialDeepLink = pendingDeepLink,
                        onDeepLinkConsumed = { pendingDeepLink = null }
                    )
                }
            }
        }

        // Defer Firebase & notification channel init to after UI is shown
        lifecycleScope.launch {
            initializeFirebase()
            NotificationHelper.createNotificationChannels(this@MainActivity)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    /**
     * Handle notification deep link intent
     * Supports all screen types via DeepLink sealed class
     */
    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let { it ->
            try {
                // DEBUG: Log ALL intent extras
                Log.d(TAG, "========================================")
                Log.d(TAG, "📨 INTENT RECEIVED")
                Log.d(TAG, "   Action: ${it.action}")
                Log.d(TAG, "   Data: ${it.data}")
                it.extras?.let { bundle ->
                    Log.d(TAG, "   Extras:")
                    for (key in bundle.keySet()) {
                        Log.d(TAG, "      - $key: ${bundle.get(key)}")
                    }
                }
                Log.d(TAG, "========================================")

                // Read notification extras (support both old and new payload field names)
                val screen = it.getStringExtra(NotificationHelper.EXTRA_SCREEN)
                    ?: it.getStringExtra("screen")
                    ?: it.getStringExtra("mobile_screen")

                val notificationType = it.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_TYPE)
                    ?: it.getStringExtra("type")
                    ?: "general"

                Log.d(TAG, "📋 Notification: screen=$screen, type=$notificationType")

                // Only navigate if screen is explicitly provided
                if (!screen.isNullOrEmpty()) {
                    val extras = mapOf(
                        "circular_id" to it.getStringExtra("circular_id"),
                        "leave_id" to it.getStringExtra("leave_id"),
                        "application_id" to it.getStringExtra("application_id"),
                        "date" to it.getStringExtra("date"),
                        "post_shift_data" to it.getStringExtra("post_shift_data")
                    )
                    val deepLink = DeepLink.fromFcmData(screen, extras)
                    if (deepLink != null) {
                        pendingDeepLink = deepLink
                        Log.d(TAG, "✅ Deep link set: $deepLink")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling notification intent", e)
            }
        }
    }

    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Log.d("FCM", "✅ Firebase initialized successfully")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        Log.d("FCM", "✅ Notification permission already granted")
                        lifecycleScope.launch {
                            getFCMToken()
                        }
                    }
                    else -> {
                        Log.d("FCM", "📋 Requesting notification permission")
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                Log.d("FCM", "✅ Notification permission not required for this Android version")
                lifecycleScope.launch {
                    getFCMToken()
                }
            }

        } catch (e: Exception) {
            Log.e("FCM", "❌ Failed to initialize Firebase", e)
        }
    }

    private suspend fun getFCMToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()

            if (token.isNotBlank()) {
                Log.d("FCM", "✅ FCM Token retrieved: ${token.take(30)}...")

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
private fun AppRoot(
    initialDeepLink: DeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val authRepo: AuthRepository = koinInject()
    var current by remember { mutableStateOf(AppScreen.SPLASH) }
    var deepLinkForNavigation by remember { mutableStateOf(initialDeepLink) }

    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink != null) {
            deepLinkForNavigation = initialDeepLink
        }
    }

    // Global listener: logout on 401 or "device not registered" 403
    LaunchedEffect(Unit) {
        AppStateManager.events.collect { event ->
            when (event) {
                is AppEvent.Unauthorized -> {
                    Log.d("MainActivity", "Global unauthorized event - logging out")
                    prefs.clearAll()
                    current = AppScreen.LOGIN
                }
            }
        }
    }

    when (current) {
        AppScreen.SPLASH -> InlineSplash(
            authRepository = authRepo,
            preferencesManager = prefs,
            onComplete = { isAuthorized ->
                current = if (isAuthorized) AppScreen.HOME else AppScreen.LOGIN
            }
        )

        AppScreen.LOGIN -> LoginScreen(
            onLoginSuccess = {
                Log.d("MainActivity", "Login success")
                current = AppScreen.HOME
            }
        )

        AppScreen.HOME -> {
            HomeWithPermissions(
                preferencesManager = prefs,
                initialDeepLink = deepLinkForNavigation,
                onLogout = {
                    Log.d("MainActivity", "Logout, clearing preferences")
                    prefs.clearAll()
                    current = AppScreen.LOGIN
                }
            )

            LaunchedEffect(deepLinkForNavigation) {
                if (deepLinkForNavigation != null) {
                    delay(500)
                    deepLinkForNavigation = null
                    onDeepLinkConsumed()
                }
            }
        }
    }
}

@Composable
private fun InlineSplash(
    authRepository: AuthRepository,
    preferencesManager: PreferencesManager,
    onComplete: (isAuthorized: Boolean) -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Initializing...") }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var showNoInternetDialog by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    if (showUpdateDialog) {
        AppVersionUpdateDialog(
            message = updateMessage,
            onDismiss = null
        )
        return
    }

    if (showNoInternetDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Text("📡", style = MaterialTheme.typography.displaySmall)
            },
            title = {
                Text(
                    text = "No Internet Connection",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Text(
                    text = "Please check your Wi-Fi or mobile data and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNoInternetDialog = false
                        status = "Retrying..."
                        retryTrigger++
                    }
                ) {
                    Text("Retry")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        (context as? ComponentActivity)?.finish()
                    }
                ) {
                    Text("Exit")
                }
            }
        )
    }

    LaunchedEffect(retryTrigger) {
        status = "Verifying session..."

        when (val outcome = authRepository.verifyToken()) {
            is AuthOutcome.Success -> {
                Log.d("SplashScreen", "Token valid - navigating to home")
                onComplete(true)
            }
            is AuthOutcome.UpdateRequired -> {
                Log.d("SplashScreen", "⚠️ Update required: ${outcome.message}")
                updateMessage = outcome.message
                showUpdateDialog = true
            }
            is AuthOutcome.NetworkError -> {
                val hasToken = !preferencesManager.authToken.isNullOrEmpty()
                Log.d("SplashScreen", "Network error (hasToken=$hasToken): ${outcome.message}")
                showNoInternetDialog = true
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
    initialDeepLink: DeepLink? = null,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo: AuthRepository = koinInject()
    var showPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        if (!preferencesManager.hasShownPermissions) {
            val permissionsGranted = arePermissionsGranted(context)
            if (!permissionsGranted) {
                showPermissionDialog = true
            } else {
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
        initialDeepLink = initialDeepLink,
        onLogout = {
            scope.launch {
                Log.d("MainActivity", "Logout requested")

                when (val outcome = repo.logout(clearPushToken = true)) {
                    is AuthOutcome.Success -> {
                        Log.d("MainActivity", "✅ Logout successful: ${outcome.message}")
                        onLogout()
                    }
                    is AuthOutcome.Error -> {
                        Log.e("MainActivity", "❌ Logout error: ${outcome.message}")
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