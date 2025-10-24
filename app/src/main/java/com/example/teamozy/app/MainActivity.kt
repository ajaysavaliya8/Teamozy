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

private enum class AppScreen { SPLASH, LOGIN, HOME }

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
                current = if (isAuthorized) AppScreen.HOME else AppScreen.LOGIN
            }
        )

        AppScreen.LOGIN -> LoginScreen(
            onLoginSuccess = { current = AppScreen.HOME }
        )

        AppScreen.HOME -> HomePage(
            onLogout = {
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
    onComplete: (Boolean) -> Unit,
    minimumDurationMillis: Long = 1200L
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()

        // Check if token exists
        val hasToken = !preferencesManager.authToken.isNullOrBlank()

        var isAuthorized = false

        if (hasToken) {
            // Verify token with backend
            Log.d("SPLASH", "Token found, verifying with server...")
            when (val result = authRepository.verifyToken()) {
                is AuthOutcome.Success -> {
                    Log.d("SPLASH", "Token verified successfully")
                    isAuthorized = true
                }
                is AuthOutcome.Error -> {
                    Log.d("SPLASH", "Token verification failed: ${result.message}")
                    // Clear invalid token
                    preferencesManager.clearAll()
                    isAuthorized = false
                }

                is AuthOutcome.DeviceNotRegistered -> {
                    Log.d("SPLASH", "Device not registered: ${result.message}")
                    // Clear token for unregistered device
                    preferencesManager.clearAll()
                    isAuthorized = false
                }
            }
        } else {
            Log.d("SPLASH", "No token found, redirecting to login")
            isAuthorized = false
        }

        // Ensure minimum splash duration
        val elapsed = System.currentTimeMillis() - startTime
        val remaining = minimumDurationMillis - elapsed
        if (remaining > 0) {
            delay(remaining)
        }

        onComplete(isAuthorized)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Teamozy",
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Verifying…",
                textAlign = TextAlign.Center
            )
        }
    }
}
