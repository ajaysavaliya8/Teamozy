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
import com.hrms.jeejateamozy.feature.notification.data.NotificationRepository

// Firebase imports
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private enum class AppScreen {
    SPLASH,
    LOGIN,
    HOME
}

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Deep link circular ID from notification
    private var deepLinkCircularId by mutableStateOf<Int?>(null)

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

        // ============================================
        // INITIALIZE FIREBASE & NOTIFICATION CHANNELS
        // ============================================
        initializeFirebase()
        NotificationHelper.createNotificationChannels(this)

        // Handle notification deep link
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
                ConfigureSystemBars()

                Surface {
                    AppRoot(
                        initialCircularId = deepLinkCircularId,
                        onCircularIdConsumed = { deepLinkCircularId = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    /**
     * Handle notification deep link intent
     * ✅ FIXED: Extract ALL FCM data from intent extras
     * When app is in background, Android passes FCM data payload as intent extras
     */
    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let { it ->
            try {
                // ✅ DEBUG: Log ALL intent extras to see what FCM passes
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

                // ✅ Read from multiple possible key names
                // FCM uses original keys, our code uses different keys
                val notificationType = it.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_TYPE)
                    ?: it.getStringExtra("type")
                    ?: "circular_new"  // Default to circular_new

                val notificationId = it.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_ID)
                    ?: it.getStringExtra("notification_id")
                    ?: "notif_${UUID.randomUUID().toString().take(12)}"

                // Read circular_id from multiple sources
                val circularIdString = it.getStringExtra(NotificationHelper.EXTRA_CIRCULAR_ID)
                    ?: it.getStringExtra("circular_id")
                val circularId = circularIdString?.toIntOrNull() ?: 0

                // Read other FCM data
                val title = it.getStringExtra("title") ?: ""
                val message = it.getStringExtra("message")
                    ?: it.getStringExtra("body")  // FCM notification body
                    ?: ""
                val priority = it.getStringExtra("priority") ?: "normal"
                val circularType = it.getStringExtra("circular_type") ?: "general"
                val createdAt = it.getStringExtra("created_at") ?: getCurrentIsoTime()

                Log.d(TAG, "📋 Parsed notification data:")
                Log.d(TAG, "   - type: $notificationType")
                Log.d(TAG, "   - circularId: $circularId")
                Log.d(TAG, "   - notificationId: $notificationId")
                Log.d(TAG, "   - title: $title")
                Log.d(TAG, "   - message: $message")

                if (circularId > 0) {
                    deepLinkCircularId = circularId

                    // ✅ ALWAYS save notification to database when clicking from system notification
                    // This handles the case when app was in background and onMessageReceived wasn't called
                    lifecycleScope.launch {
                        try {
                            val repo = NotificationRepository.getInstance(applicationContext)

                            // Check if notification already exists
                            val exists = repo.exists(notificationId)

                            if (!exists) {
                                // Save to database with whatever data we have
                                val saved = repo.saveFromFcmData(
                                    notificationId = notificationId,
                                    type = notificationType,
                                    circularId = circularId,
                                    title = title.ifEmpty { "Circular #$circularId" },
                                    message = message.ifEmpty { "New notification" },
                                    priority = priority,
                                    circularType = circularType,
                                    createdAt = createdAt
                                )

                                if (saved) {
                                    Log.d(TAG, "✅ Notification SAVED from deep link: $notificationId")
                                }
                            } else {
                                Log.d(TAG, "📋 Notification already exists: $notificationId")
                            }

                            // Mark as read
                            repo.markAsReadByNotificationId(notificationId)
                            Log.d(TAG, "✅ Notification marked as read: $notificationId")

                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error handling notification", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling notification intent", e)
            }
        }
    }

    private fun getCurrentIsoTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        return sdf.format(Date())
    }

    @Composable
    private fun ConfigureSystemBars() {
        val view = LocalView.current
        val primaryColor = MaterialTheme.colorScheme.primary

        DisposableEffect(primaryColor) {
            val window = window
            window.statusBarColor = primaryColor.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
            }
            onDispose { }
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
    initialCircularId: Int? = null,
    onCircularIdConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val authRepo: AuthRepository = koinInject()
    var current by remember { mutableStateOf(AppScreen.SPLASH) }
    var circularIdForDeepLink by remember { mutableStateOf(initialCircularId) }

    LaunchedEffect(initialCircularId) {
        if (initialCircularId != null) {
            circularIdForDeepLink = initialCircularId
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
                initialCircularId = circularIdForDeepLink,
                onLogout = {
                    Log.d("MainActivity", "Logout, clearing preferences")
                    prefs.clearAll()
                    current = AppScreen.LOGIN
                }
            )

            LaunchedEffect(circularIdForDeepLink) {
                if (circularIdForDeepLink != null) {
                    delay(500)
                    circularIdForDeepLink = null
                    onCircularIdConsumed()
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
    var status by remember { mutableStateOf("Initializing...") }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }

    if (showUpdateDialog) {
        AppVersionUpdateDialog(
            message = updateMessage,
            onDismiss = null
        )
        return
    }

    LaunchedEffect(Unit) {
        delay(1000)
        status = "Verifying session..."
        delay(500)

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
    initialCircularId: Int? = null,
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
        initialCircularId = initialCircularId,
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