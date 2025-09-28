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
import com.example.teamozy.core.utils.PreferencesManager
import com.example.teamozy.feature.auth.presentation.LoginScreen
import com.example.teamozy.feature.home.presentation.HomePage
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import com.example.teamozy.di.authModule
import com.example.teamozy.di.attendanceModule
import com.example.teamozy.di.permissionsModule
import com.example.teamozy.di.homeModule
import kotlinx.coroutines.delay

private enum class AppScreen { SPLASH, LOGIN, HOME }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    var current by remember { mutableStateOf(AppScreen.SPLASH) }

    when (current) {
        AppScreen.SPLASH -> InlineSplash(
            onComplete = {
                val hasToken = !prefs.authToken.isNullOrBlank()
                current = if (hasToken) AppScreen.HOME else AppScreen.LOGIN
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
private fun InlineSplash(onComplete: () -> Unit, durationMillis: Long = 1200L) {
    LaunchedEffect(Unit) {
        delay(durationMillis)
        onComplete()
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
                text = "Loading…",
                textAlign = TextAlign.Center
            )
        }
    }
}
