@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.teamozy.feature.permissions.presentation

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.teamozy.core.utils.PermissionHelper
import kotlinx.coroutines.launch
import androidx.compose.material3.TopAppBar
@Composable
fun PermissionScreen(
    onAllGranted: () -> Unit,
    onBackToLogin: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as Activity

    val requiredPermissions = remember {
        buildList<String> {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CAMERA)
            // Background location optional:
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }.toTypedArray()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Early auto-advance if already granted
    LaunchedEffect(Unit) {
        if (PermissionHelper.areAllGranted(context, requiredPermissions)) {
            onAllGranted()
        }
    }

    var permanentlyDenied by remember { mutableStateOf(false) }
    var justRequested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        justRequested = true
        val allGranted = grantMap.values.all { it }
        permanentlyDenied = PermissionHelper.anyPermanentlyDenied(activity, requiredPermissions)

        if (allGranted) {
            scope.launch { snackbarHostState.showSnackbar("All permissions granted.") }
            onAllGranted()
        } else {
            scope.launch {
                if (permanentlyDenied) {
                    snackbarHostState.showSnackbar("Permission permanently denied. Open App Settings.")
                } else {
                    snackbarHostState.showSnackbar("Some permissions were denied. Try again.")
                }
            }
        }
    }

    fun requestNow() {
        permanentlyDenied = false
        launcher.launch(requiredPermissions)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar( // ⬅️ changed
                title = { Text("Permissions") },
                navigationIcon = {
                    onBackToLogin?.let {
                        TextButton(onClick = it) { Text("Back") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("We need a couple of permissions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Location: to verify you're at the work location.\nCamera: for face verification or evidence when needed.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tip: Allow “While using the app” for smooth check-ins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(24.dp))

            if (permanentlyDenied) {
                Button(
                    onClick = { PermissionHelper.openAppSettings(context) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Open App Settings") }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        if (PermissionHelper.areAllGranted(context, requiredPermissions)) {
                            scope.launch { snackbarHostState.showSnackbar("Thanks! Permissions enabled.") }
                            onAllGranted()
                        } else {
                            requestNow()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("I’ve enabled them, continue") }
            } else {
                Button(
                    onClick = { requestNow() },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text(if (justRequested) "Try Again" else "Grant Permissions") }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        if (PermissionHelper.areAllGranted(context, requiredPermissions)) {
                            scope.launch { snackbarHostState.showSnackbar("All set!") }
                            onAllGranted()
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Still missing permissions.") }
                            justRequested = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Check Again") }
            }
        }
    }
}
