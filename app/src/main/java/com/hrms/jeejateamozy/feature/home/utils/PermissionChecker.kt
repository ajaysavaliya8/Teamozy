package com.hrms.jeejateamozy.feature.home.presentation.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.hrms.jeejateamozy.core.utils.PermissionHelper

/**
 * Permission Checker Composable
 * Checks for CAMERA and LOCATION permissions before attendance operations
 * Shows dialogs when permissions are missing
 */
@Composable
fun rememberPermissionChecker(
    context: Context,
    onPermissionsGranted: () -> Unit
): () -> Unit {
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true

        if (locationGranted && cameraGranted) {
            // Check GPS after permissions granted
            if (PermissionHelper.isLocationEnabled(context)) {
                onPermissionsGranted()
            } else {
                showGpsDialog = true
            }
        } else {
            showPermissionDialog = true
        }
    }

    // Permission Denied Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissions Required") },
            text = {
                Text("Camera and Location permissions are required for attendance tracking.\n\nPlease grant permissions to continue.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        PermissionHelper.openAppSettings(context)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // GPS Dialog
    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDialog = false },
            title = { Text("GPS Required") },
            text = {
                Text("Please enable GPS/Location services to use attendance features.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGpsDialog = false
                        PermissionHelper.openLocationSettings(context)
                    }
                ) {
                    Text("Enable GPS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    return remember {
        {
            // Check if all required permissions are granted
            val locationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

            val cameraGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            val gpsEnabled = PermissionHelper.isLocationEnabled(context)

            if (locationGranted && cameraGranted && gpsEnabled) {
                // All permissions granted - proceed
                onPermissionsGranted()
            } else if (!locationGranted || !cameraGranted) {
                // Request missing permissions
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA
                    )
                )
            } else if (!gpsEnabled) {
                // Permissions granted but GPS disabled
                showGpsDialog = true
            }
        }
    }
}