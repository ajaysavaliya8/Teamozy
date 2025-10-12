@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.profile.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamozy.BuildConfig
import com.example.teamozy.core.utils.PreferencesManager

@Composable
fun ProfileScreen(
    onNavigateToFaceChange: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            // Profile Header
            ProfileHeader(
                userName = prefs.userName ?: "User",
                mobileNumber = prefs.userId ?: "Not Available"
            )

            Spacer(Modifier.height(16.dp))

            // Account Section
            SectionCard(title = "Account") {
                ProfileMenuItem(
                    icon = Icons.Filled.Face,
                    title = "Face Recognition",
                    subtitle = "Manage face verification",
                    onClick = onNavigateToFaceChange
                )
            }

            Spacer(Modifier.height(12.dp))

            // App Section
            SectionCard(title = "About") {
                ProfileMenuItem(
                    icon = Icons.Outlined.Info,
                    title = "App Version",
                    subtitle = getAppVersion(),
                    onClick = { /* No action */ },
                    showChevron = false
                )

                Divider(Modifier.padding(horizontal = 16.dp))

                ProfileMenuItem(
                    icon = Icons.Outlined.Star,
                    title = "Rate App",
                    subtitle = "Share your feedback",
                    onClick = {
                        // Open Play Store rating
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=${context.packageName}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to web browser
                            intent.data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            context.startActivity(intent)
                        }
                    }
                )

                Divider(Modifier.padding(horizontal = 16.dp))

                ProfileMenuItem(
                    icon = Icons.Outlined.Email,
                    title = "App Support",
                    subtitle = "Get help and support",
                    onClick = {
                        // Open email client
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@teamozy.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Teamozy App Support Request")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Logout Section
            SectionCard(title = "Session") {
                ProfileMenuItem(
                    icon = Icons.Outlined.ExitToApp,
                    title = "Logout",
                    subtitle = "Sign out of your account",
                    onClick = { showLogoutDialog = true },
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(24.dp))

            // Future Features (Commented out for now)
            /*
            // Personal Information Section
            SectionCard(title = "Personal Information") {
                ProfileMenuItem(
                    icon = Icons.Outlined.Person,
                    title = "Contact Details",
                    subtitle = "Email, phone, address",
                    onClick = { }
                )
                Divider(Modifier.padding(horizontal = 16.dp))
                ProfileMenuItem(
                    icon = Icons.Outlined.Badge,
                    title = "Personal Info",
                    subtitle = "Date of birth, gender, etc.",
                    onClick = { }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Work Information Section
            SectionCard(title = "Work Information") {
                ProfileMenuItem(
                    icon = Icons.Outlined.Work,
                    title = "Employee Details",
                    subtitle = "Job title, department, ID",
                    onClick = { }
                )
                Divider(Modifier.padding(horizontal = 16.dp))
                ProfileMenuItem(
                    icon = Icons.Outlined.Schedule,
                    title = "Shift Details",
                    subtitle = "Work schedule and timings",
                    onClick = { }
                )
                Divider(Modifier.padding(horizontal = 16.dp))
                ProfileMenuItem(
                    icon = Icons.Outlined.WorkHistory,
                    title = "Past Experience",
                    subtitle = "Previous work history",
                    onClick = { }
                )
                Divider(Modifier.padding(horizontal = 16.dp))
                ProfileMenuItem(
                    icon = Icons.Outlined.EmojiEvents,
                    title = "Achievements",
                    subtitle = "Awards and recognitions",
                    onClick = { }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Emergency Section
            SectionCard(title = "Emergency") {
                ProfileMenuItem(
                    icon = Icons.Outlined.Contacts,
                    title = "Nominees",
                    subtitle = "Emergency contacts",
                    onClick = { }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Social Section
            SectionCard(title = "Connect") {
                ProfileMenuItem(
                    icon = Icons.Outlined.Share,
                    title = "Social Media",
                    subtitle = "LinkedIn, Twitter, etc.",
                    onClick = { }
                )
            }
            */

            Spacer(Modifier.height(80.dp))
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Outlined.ExitToApp, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout from your account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
    mobileNumber: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture with Edit Icon
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                // Profile Picture Placeholder
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(50.dp),
                        tint = Color.White
                    )
                }

                // Edit Icon
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { /* TODO: Handle profile picture update */ },
                    color = MaterialTheme.colorScheme.secondary,
                    tonalElevation = 4.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit Profile Picture",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // User Name
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(4.dp))

            // Mobile Number
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = mobileNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Title and Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chevron
            if (showChevron) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getAppVersion(): String {
    return try {
        BuildConfig.VERSION_NAME
    } catch (e: Exception) {
        "1.0"
    }
}