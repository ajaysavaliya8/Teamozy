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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.teamozy.core.utils.PermissionHelper

@Composable
fun PermissionScreen(
    onAllGood: () -> Unit
) {
    val context = LocalContext.current

    var fineGranted by remember { mutableStateOf(isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var coarseGranted by remember { mutableStateOf(isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    var cameraGranted by remember { mutableStateOf(isGranted(context, Manifest.permission.CAMERA)) }
    var gpsEnabled by remember { mutableStateOf(PermissionHelper.isLocationEnabled(context)) }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        result[Manifest.permission.ACCESS_FINE_LOCATION]?.let { fineGranted = it }
        result[Manifest.permission.ACCESS_COARSE_LOCATION]?.let { coarseGranted = it }
        result[Manifest.permission.CAMERA]?.let { cameraGranted = it }
    }

    val locationOk = fineGranted || coarseGranted
    val everythingOk = locationOk && gpsEnabled   // camera NOT required

    Scaffold(
        topBar = { TopAppBar(title = { Text("Permissions") }) }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("We need location before you continue:", fontWeight = FontWeight.SemiBold)

            PermissionCard(
                title = "Location",
                description = "Required for accurate Check In / Check Out.",
                statusLines = listOf(
                    "Permission: " + (if (locationOk) "GRANTED" else "NOT GRANTED"),
                    "GPS: " + (if (gpsEnabled) "ENABLED" else "DISABLED")
                ),
                primaryButton = {
                    if (!locationOk) {
                        Button(onClick = {
                            multiPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }) { Text("Grant Location") }
                    }
                },
                secondaryButton = {
                    if (!gpsEnabled) {
                        OutlinedButton(onClick = { PermissionHelper.openLocationSettings(context) }) {
                            Text("Enable GPS")
                        }
                    } else {
                        OutlinedButton(onClick = {
                            gpsEnabled = PermissionHelper.isLocationEnabled(context)
                            fineGranted = isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            coarseGranted = isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        }) { Text("Refresh") }
                    }
                }
            )

            PermissionCard(
                title = "Camera (optional)",
                description = "Only needed if we later add selfie verification upload. Safe to skip now.",
                statusLines = listOf("Permission: " + (if (cameraGranted) "GRANTED" else "NOT GRANTED")),
                primaryButton = {
                    if (!cameraGranted) {
                        OutlinedButton(onClick = {
                            multiPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        }) { Text("Grant Camera") }
                    }
                },
                secondaryButton = {
                    if (!cameraGranted) {
                        TextButton(onClick = { PermissionHelper.openAppSettings(context) }) {
                            Text("Open App Settings")
                        }
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onAllGood,
                enabled = everythingOk,
                modifier = Modifier.align(Alignment.End)
            ) { Text("Continue") }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    statusLines: List<String>,
    primaryButton: (@Composable () -> Unit)? = null,
    secondaryButton: (@Composable () -> Unit)? = null
) {
    ElevatedCard {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
            statusLines.forEach { line ->
                Text("• $line", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                primaryButton?.invoke()
                secondaryButton?.invoke()
            }
        }
    }
}

private fun isGranted(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
