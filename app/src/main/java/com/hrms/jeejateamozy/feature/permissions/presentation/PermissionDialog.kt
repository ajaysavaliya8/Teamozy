package com.hrms.jeejateamozy.feature.permissions.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.hrms.jeejateamozy.core.utils.PermissionHelper

/**
 * Permission Dialog - Shows a simple popup for permissions on first launch
 * Intelligently checks permissions before showing and handles denials
 */
@Composable
fun PermissionDialog(
    onDismiss: () -> Unit,
    onPermissionsHandled: () -> Unit
) {
    val context = LocalContext.current

    // Check current permission states
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var gpsEnabled by remember {
        mutableStateOf(PermissionHelper.isLocationEnabled(context))
    }

    var showInitialDialog by remember { mutableStateOf(false) }
    var showDenialDialog by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    var deniedPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Check permissions on first composition
    LaunchedEffect(Unit) {
        // Check if all permissions are already granted
        val allGranted = locationGranted && cameraGranted && gpsEnabled

        if (allGranted) {
            // All permissions already granted - no need to show dialog
            onPermissionsHandled()
        } else {
            // Some permissions missing - show initial dialog
            showInitialDialog = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Update permission states
        locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        cameraGranted = permissions[Manifest.permission.CAMERA] == true
        gpsEnabled = PermissionHelper.isLocationEnabled(context)

        // Build list of denied permissions
        val denied = mutableListOf<String>()
        if (!locationGranted) denied.add("Location")
        if (!cameraGranted) denied.add("Camera")

        if (denied.isEmpty()) {
            // All runtime permissions granted - check GPS
            if (!gpsEnabled) {
                showGpsDialog = true
            } else {
                // Everything is granted
                onPermissionsHandled()
            }
        } else {
            // Some permissions were denied - show denial dialog
            deniedPermissions = denied
            showDenialDialog = true
        }
    }

    // Initial Permission Request Dialog
    if (showInitialDialog) {
        AlertDialog(
            onDismissRequest = {
                showInitialDialog = false
                onDismiss()
            },
            title = { Text("Enable App Features") },
            text = {
                val missingPermissions = buildString {
                    append("Teamozy needs the following permissions to work properly:\n\n")
                    if (!locationGranted) {
                        append("📍 Location - For attendance tracking\n")
                    }
                    if (!cameraGranted) {
                        append("📷 Camera - For face verification\n")
                    }
                    if (!gpsEnabled) {
                        append("🌐 GPS - For location services\n")
                    }
                    append("\nThese help ensure accurate attendance records.")
                }
                Text(missingPermissions)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInitialDialog = false
                        // Request permissions
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                        )
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showInitialDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Not Now")
                }
            }
        )
    }

    // Denial Dialog (if user denied some permissions)
    if (showDenialDialog) {
        AlertDialog(
            onDismissRequest = {
                showDenialDialog = false
                onDismiss()
            },
            title = { Text("Permissions Required") },
            text = {
                val denialMessage = buildString {
                    append("The following permissions are required:\n\n")
                    deniedPermissions.forEach { permission ->
                        when (permission) {
                            "Location" -> append("📍 Location - Required for attendance tracking\n")
                            "Camera" -> append("📷 Camera - Required for face verification\n")
                        }
                    }
                    append("\nWithout these permissions, some features won't work properly.\n\n")
                    append("Please enable them in app settings.")
                }
                Text(denialMessage)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDenialDialog = false
                        PermissionHelper.openAppSettings(context)
                        onDismiss()
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDenialDialog = false
                        // Try again
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                        )
                    }
                ) {
                    Text("Try Again")
                }
            }
        )
    }

    // GPS Dialog
    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = {
                showGpsDialog = false
                onDismiss()
            },
            title = { Text("Enable Location Services") },
            text = {
                Text("GPS/Location services are currently disabled.\n\nPlease enable them to use attendance features.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGpsDialog = false
                        PermissionHelper.openLocationSettings(context)
                        onDismiss()
                    }
                ) {
                    Text("Enable GPS")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGpsDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Skip")
                }
            }
        )
    }
}

/**
 * Helper function to check if all permissions are granted
 */
fun arePermissionsGranted(context: android.content.Context): Boolean {
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

    return locationGranted && cameraGranted && gpsEnabled
}