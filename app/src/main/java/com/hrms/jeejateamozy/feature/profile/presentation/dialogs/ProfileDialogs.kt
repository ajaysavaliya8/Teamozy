package com.hrms.jeejateamozy.feature.profile.presentation.components

import com.hrms.jeejateamozy.core.designsystem.TeamozyColors

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Profile Picture Options Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePictureOptionsSheet(
    hasProfilePicture: Boolean,
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Profile Picture",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ProfilePictureOption(
                icon = Icons.Filled.Image,
                title = "Choose from Gallery",
                onClick = {
                    onDismiss()
                    onGalleryClick()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ProfilePictureOption(
                icon = Icons.Filled.PhotoCamera,
                title = "Take Photo",
                subtitle = "Coming soon",
                onClick = {
                    onDismiss()
                    onCameraClick()
                },
                enabled = false
            )

            if (hasProfilePicture) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ProfilePictureOption(
                    icon = Icons.Default.Delete,
                    title = "Remove Picture",
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDismiss()
                        onRemoveClick()
                    }
                )
            }
        }
    }
}

/**
 * Profile Picture Option Item (for bottom sheet)
 */
@Composable
private fun ProfilePictureOption(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Remove Profile Picture Confirmation Dialog
 */
@Composable
fun RemoveProfilePictureDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = { Text("Remove Profile Picture?") },
        text = {
            Text("Are you sure you want to remove your profile picture? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Logout Confirmation Dialog
 */
@Composable
fun LogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
        title = { Text("Logout") },
        text = { Text("Are you sure you want to logout?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Success/Error Message Card
 */
@Composable
fun MessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(message) {
        kotlinx.coroutines.delay(4000)
        onDismiss()
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError)
                MaterialTheme.colorScheme.errorContainer
            else
                TeamozyColors.Success
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isError) Icons.Default.Info else Icons.Default.Check,
                contentDescription = null,
                tint = if (isError)
                    MaterialTheme.colorScheme.error
                else
                    androidx.compose.ui.graphics.Color.White
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                color = if (isError)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    androidx.compose.ui.graphics.Color.White,
                fontSize = 13.sp
            )
        }
    }
}