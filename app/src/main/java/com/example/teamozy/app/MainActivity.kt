package com.example.teamozy.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import com.example.teamozy.core.state.AppEvent
import com.example.teamozy.core.state.AppStateManager
import com.example.teamozy.core.utils.PermissionHelper
import com.example.teamozy.core.utils.PreferencesManager
import com.example.teamozy.feature.auth.presentation.LoginScreen
import com.example.teamozy.feature.home.presentation.HomePage

import com.example.teamozy.feature.permissions.presentation.PermissionScreen
import com.example.teamozy.feature.splash.presentation.SplashScreen
import com.example.teamozy.ui.theme.TeamozyTheme
import kotlinx.coroutines.flow.collectLatest



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeamozyTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val pm = remember { PreferencesManager.getInstance(context) }
    var current by rememberSaveable { mutableStateOf(AppScreen.SPLASH) }

    // Global 401 -> Login
    LaunchedEffect(Unit) {
        AppStateManager.events.collectLatest { ev ->
            when (ev) {
                is AppEvent.Unauthorized -> {
                    pm.clearAll()
                    current = AppScreen.LOGIN
                }
            }
        }
    }

    when (current) {
        AppScreen.SPLASH -> SplashScreen {
            // Decide next screen after splash
            val hasLocationPerm = PermissionHelper.areAllGranted(
                context,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            val gpsEnabled = PermissionHelper.isLocationEnabled(context)
            val cameraGranted = PermissionHelper.areAllGranted(
                context,
                arrayOf(Manifest.permission.CAMERA)
            )

            val needsPermissions = !hasLocationPerm || !gpsEnabled || !cameraGranted
            current = when {
                needsPermissions -> AppScreen.PERMISSIONS
                pm.authToken != null -> AppScreen.HOME
                else -> AppScreen.LOGIN
            }
        }

        AppScreen.PERMISSIONS -> PermissionScreen(
            onAllGood = {
                // After permissions, go to Home if already logged in; otherwise Login
                current = if (pm.authToken != null) AppScreen.HOME else AppScreen.LOGIN
            }
        )

        AppScreen.LOGIN -> {
            // Move to Home when LoginScreen saves a token
            LaunchedEffect(Unit) {
                snapshotFlow { pm.authToken }.collectLatest { tok ->
                    if (!tok.isNullOrEmpty()) current = AppScreen.HOME
                }
            }
            LoginScreen(
                onLoginSuccess = {
                    // In case LoginScreen doesn’t set token immediately,
                    // still navigate to Home on callback.
                    current = AppScreen.HOME
                }
            )
        }

        AppScreen.HOME -> HomePage(
            onLogout = {
                pm.clearAll()
                current = AppScreen.LOGIN
            }
        )
    }
}
