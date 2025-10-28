@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.permissions.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.teamozy.core.utils.PermissionHelper

@Composable
fun PermissionScreen(
    onAllGood: () -> Unit
) {
    val context = LocalContext.current

    var locationGranted by remember {
        mutableStateOf(
            isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
    var cameraGranted by remember {
        mutableStateOf(isGranted(context, Manifest.permission.CAMERA))
    }
    var gpsEnabled by remember {
        mutableStateOf(PermissionHelper.isLocationEnabled(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        cameraGranted = result[Manifest.permission.CAMERA] == true
        gpsEnabled = PermissionHelper.isLocationEnabled(context)
    }

    val allPermissionsGranted = locationGranted && cameraGranted && gpsEnabled

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Permissions") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Teamozy works better with these permissions:",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Location Status
            PermissionItem(
                icon = "📍",
                title = "Location",
                granted = locationGranted && gpsEnabled
            )

            Spacer(Modifier.height(16.dp))

            // Camera Status
            PermissionItem(
                icon = "📷",
                title = "Camera",
                granted = cameraGranted
            )

            Spacer(Modifier.height(48.dp))

            // Grant All Button
            if (!allPermissionsGranted) {
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        "Grant Permissions",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (locationGranted && cameraGranted && !gpsEnabled) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { PermissionHelper.openLocationSettings(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Enable GPS")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Skip Button - Allow entry without permissions
                TextButton(
                    onClick = onAllGood,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Skip for now",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Note: Some features may not work without permissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Button(
                    onClick = onAllGood,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        "Continue",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: String,
    title: String,
    granted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    icon,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                if (granted) "✅" else "❌",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

private fun isGranted(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED