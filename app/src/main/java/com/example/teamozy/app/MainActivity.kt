package com.example.teamozy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import com.example.teamozy.core.network.NetworkModule
import com.example.teamozy.core.utils.PreferencesManager
import com.example.teamozy.feature.auth.presentation.LoginScreen
import com.example.teamozy.feature.auth.data.AuthRepository
import com.example.teamozy.feature.auth.data.AuthOutcome
import com.example.teamozy.feature.home.presentation.HomePage
import com.example.teamozy.feature.permissions.presentation.PermissionScreen
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.compose.koinInject
import com.example.teamozy.di.authModule
import com.example.teamozy.di.attendanceModule
import com.example.teamozy.di.permissionsModule
import com.example.teamozy.di.homeModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log

private enum class AppScreen { SPLASH, PERMISSIONS, LOGIN, HOME }

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
    var showPermissionsOnce by remember { mutableStateOf(false) }

    when (current) {
        AppScreen.SPLASH -> InlineSplash(
            authRepository = authRepo,
            preferencesManager = prefs,
            onComplete = { isAuthorized ->
                current = if (isAuthorized) {
                    // User is logged in
                    // Show permissions screen only on first launch after login
                    if (!showPermissionsOnce) {
                        showPermissionsOnce = true
                        AppScreen.PERMISSIONS
                    } else {
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
                current = AppScreen.HOME
            }
        )

        AppScreen.LOGIN -> LoginScreen(
            onLoginSuccess = {
                Log.d("MainActivity", "Login success")
                // After login, show permissions once
                showPermissionsOnce = false
                current = AppScreen.PERMISSIONS
            }
        )

        AppScreen.HOME -> HomePage(
            onLogout = {
                Log.d("MainActivity", "Logout, clearing preferences")
                prefs.clearAll()
                showPermissionsOnce = false
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

            Log.d("InlineSplash", "isLoggedIn: $isLoggedIn")

            if (!isLoggedIn) {
                Log.d("InlineSplash", "No token found, going to login")
                onComplete(false)
                return@launch
            }

            // User has token, try to verify it
            Log.d("InlineSplash", "Token found, verifying with server...")

            var isAuthorized = true // Assume authorized by default (offline mode)

            try {
                when (val result = authRepository.verifyToken()) {
                    is AuthOutcome.Success -> {
                        Log.d("InlineSplash", "✅ Token verified successfully")
                        isAuthorized = true
                    }

                    is AuthOutcome.Error -> {
                        val errorMsg = result.message.lowercase()

                        // Only clear token if it's explicitly invalid
                        if (errorMsg.contains("invalid") ||
                            errorMsg.contains("expired") ||
                            errorMsg.contains("unauthorized")) {
                            Log.w("InlineSplash", "❌ Token is invalid: ${result.message}")
                            preferencesManager.clearAll()
                            isAuthorized = false
                        } else {
                            // Network error or other issue - allow offline access
                            Log.w("InlineSplash", "⚠️ Token verification failed (network?): ${result.message}")
                            Log.d("InlineSplash", "Allowing offline access with existing token")
                            isAuthorized = true
                        }
                    }

                    is AuthOutcome.DeviceNotRegistered -> {
                        Log.w("InlineSplash", "❌ Device not registered: ${result.message}")
                        preferencesManager.clearAll()
                        isAuthorized = false
                    }
                }
            } catch (e: Exception) {
                // Any exception during verification - allow offline access
                Log.e("InlineSplash", "⚠️ Exception during token verification: ${e.message}")
                Log.d("InlineSplash", "Allowing offline access with existing token")
                isAuthorized = true
            }

            onComplete(isAuthorized)
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
            CircularProgressIndicator()
            Text(
                "Loading Teamozy...",
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}