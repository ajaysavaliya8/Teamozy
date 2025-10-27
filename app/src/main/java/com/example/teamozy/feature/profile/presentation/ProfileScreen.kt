@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.profile.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Settings Icon
                    IconButton(
                        onClick = { /* TODO: Navigate to settings */ },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Share Icon
                    IconButton(
                        onClick = { shareProfile(context, prefs) },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Logout Icon
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(
                            Icons.Outlined.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
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
            Spacer(Modifier.height(16.dp))

            // Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Company Header with Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = prefs.companyName?.uppercase() ?: "COMPANY NAME",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = prefs.companyAddress ?: "Company Address",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Profile Content
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Profile Image with Camera Icon
                            Box(
                                modifier = Modifier.size(120.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                // Profile Image Placeholder
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.size(60.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Camera Icon
                                Surface(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { /* TODO: Change profile picture */ },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondary
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Edit Profile Picture",
                                        modifier = Modifier.padding(8.dp),
                                        tint = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }

                            // User Info
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 20.dp)
                            ) {
                                Text(
                                    text = prefs.fullName ?: prefs.userName ?: "User Name",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(Modifier.height(4.dp))

                                if (!prefs.shiftName.isNullOrBlank()) {
                                    Text(
                                        text = prefs.shiftName!!.uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }

                                // Department Info (Branch - Department)
                                val departmentInfo = buildString {
                                    if (!prefs.branchName.isNullOrBlank()) {
                                        append(prefs.branchName!!.uppercase())
                                    }
                                    if (!prefs.departmentName.isNullOrBlank()) {
                                        if (isNotEmpty()) append(" - ")
                                        append(prefs.departmentName!!.uppercase())
                                    }
                                }

                                if (departmentInfo.isNotEmpty()) {
                                    Text(
                                        text = departmentInfo,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Profile Completion Section
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Calculate profile completion
                            val completion = calculateProfileCompletion(prefs)

                            // Completion Badge
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$completion%",
                                    fontSize = 14.sp, // Made smaller as requested
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = completion / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Profile Completion",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Divider
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Phone Number Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val phone = prefs.mobileNumber
                                if (phone != 0L) {
                                    dialPhoneNumber(context, phone.toString())
                                } else if (!prefs.userId.isNullOrBlank()) {
                                    dialPhoneNumber(context, prefs.userId!!)
                                }
                            }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = formatPhoneNumber(prefs),
                            fontSize = 16.sp, // Made smaller as requested
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Social Media Section (Under Phone Number)
                    val hasSocialMedia = !prefs.facebook.isNullOrBlank() ||
                            !prefs.linkedin.isNullOrBlank() ||
                            !prefs.x.isNullOrBlank() ||
                            !prefs.instagram.isNullOrBlank() ||
                            !prefs.snapchat.isNullOrBlank()

                    if (hasSocialMedia) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Facebook
                            if (!prefs.facebook.isNullOrBlank()) {
                                SocialMediaIcon(
                                    icon = Icons.Default.Face,
                                    contentDescription = "Facebook",
                                    onClick = { openUrl(context, prefs.facebook!!) }
                                )
                            }

                            // LinkedIn
                            if (!prefs.linkedin.isNullOrBlank()) {
                                SocialMediaIcon(
                                    icon = Icons.Default.AccountCircle,
                                    contentDescription = "LinkedIn",
                                    onClick = { openUrl(context, prefs.linkedin!!) }
                                )
                            }

                            // X (Twitter)
                            if (!prefs.x.isNullOrBlank()) {
                                SocialMediaIcon(
                                    icon = Icons.Default.Star,
                                    contentDescription = "X",
                                    onClick = { openUrl(context, prefs.x!!) }
                                )
                            }

                            // Instagram
                            if (!prefs.instagram.isNullOrBlank()) {
                                SocialMediaIcon(
                                    icon = Icons.Default.Favorite,
                                    contentDescription = "Instagram",
                                    onClick = { openUrl(context, prefs.instagram!!) }
                                )
                            }

                            // Snapchat/WhatsApp
                            if (!prefs.snapchat.isNullOrBlank()) {
                                SocialMediaIcon(
                                    icon = Icons.Outlined.Email,
                                    contentDescription = "WhatsApp",
                                    onClick = { openUrl(context, prefs.snapchat!!) }
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            // Edit Social Media Button
                            IconButton(
                                onClick = { /* TODO: Edit social media */ },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Social Media",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Company Information Card
            if (!prefs.companyEmail.isNullOrBlank() ||
                !prefs.companyContact.isNullOrBlank() ||
                !prefs.companyWebsite.isNullOrBlank()) {

                SectionCard(title = "Company Information") {
                    // Company Email
                    if (!prefs.companyEmail.isNullOrBlank()) {
                        ContactInfoItem(
                            icon = Icons.Default.Email,
                            title = "Company Email",
                            value = prefs.companyEmail!!,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${prefs.companyEmail}")
                                }
                                context.startActivity(intent)
                            }
                        )
                        if (!prefs.companyContact.isNullOrBlank() || !prefs.companyWebsite.isNullOrBlank()) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    // Company Contact
                    if (!prefs.companyContact.isNullOrBlank()) {
                        ContactInfoItem(
                            icon = Icons.Default.Phone,
                            title = "Company Phone",
                            value = prefs.companyContact!!,
                            onClick = { dialPhoneNumber(context, prefs.companyContact!!) }
                        )
                        if (!prefs.companyWebsite.isNullOrBlank()) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    // Company Website
                    if (!prefs.companyWebsite.isNullOrBlank()) {
                        ContactInfoItem(
                            icon = Icons.Default.Info,
                            title = "Company Website",
                            value = prefs.companyWebsite!!,
                            onClick = { openUrl(context, prefs.companyWebsite!!) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // HR Contact Card
            if (!prefs.hrEmail.isNullOrBlank()) {
                SectionCard(title = "HR Contact") {
                    ContactInfoItem(
                        icon = Icons.Outlined.Person,
                        title = "HR Email",
                        value = prefs.hrEmail!!,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${prefs.hrEmail}")
                                putExtra(Intent.EXTRA_SUBJECT, "HR Inquiry")
                            }
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            // Technical Support Card
            if (!prefs.technicalSupportNumber.isNullOrBlank() || !prefs.technicalSupportEmail.isNullOrBlank()) {
                SectionCard(title = "Technical Support") {
                    // Tech Support Number
                    if (!prefs.technicalSupportNumber.isNullOrBlank()) {
                        ContactInfoItem(
                            icon = Icons.Default.Phone,
                            title = "Support Phone",
                            value = prefs.technicalSupportNumber!!,
                            onClick = { dialPhoneNumber(context, prefs.technicalSupportNumber!!) }
                        )
                        if (!prefs.technicalSupportEmail.isNullOrBlank()) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    // Tech Support Email
                    if (!prefs.technicalSupportEmail.isNullOrBlank()) {
                        ContactInfoItem(
                            icon = Icons.Default.Email,
                            title = "Support Email",
                            value = prefs.technicalSupportEmail!!,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${prefs.technicalSupportEmail}")
                                    putExtra(Intent.EXTRA_SUBJECT, "Technical Support Request")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

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

                HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                ProfileMenuItem(
                    icon = Icons.Outlined.Star,
                    title = "Rate App",
                    subtitle = "Share your feedback",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=${context.packageName}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            intent.data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            context.startActivity(intent)
                        }
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
private fun ContactInfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
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
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SocialMediaIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(end = 8.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
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
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
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

            if (showChevron) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper Functions
private fun calculateProfileCompletion(prefs: PreferencesManager): Int {
    var totalFields = 10
    var filledFields = 0

    if (!prefs.fullName.isNullOrBlank() || !prefs.userName.isNullOrBlank()) filledFields++
    if (!prefs.profileUrl.isNullOrBlank()) filledFields++
    if (!prefs.branchName.isNullOrBlank()) filledFields++
    if (!prefs.departmentName.isNullOrBlank()) filledFields++
    if (!prefs.shiftName.isNullOrBlank()) filledFields++
    if (!prefs.facebook.isNullOrBlank()) filledFields++
    if (!prefs.linkedin.isNullOrBlank()) filledFields++
    if (!prefs.x.isNullOrBlank()) filledFields++
    if (!prefs.instagram.isNullOrBlank()) filledFields++
    if (!prefs.snapchat.isNullOrBlank()) filledFields++

    return (filledFields * 100) / totalFields
}

private fun formatPhoneNumber(prefs: PreferencesManager): String {
    // Try new mobile number field first
    if (prefs.mobileNumber != 0L) {
        val numStr = prefs.mobileNumber.toString()
        return if (numStr.length == 10) {
            "+91-$numStr"
        } else {
            numStr
        }
    }

    // Fall back to old userId field
    val userId = prefs.userId
    if (!userId.isNullOrBlank()) {
        return if (userId.length == 10) {
            "+91-$userId"
        } else {
            userId
        }
    }

    return "Not Available"
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun dialPhoneNumber(context: android.content.Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun shareProfile(context: android.content.Context, prefs: PreferencesManager) {
    val name = prefs.fullName ?: prefs.userName ?: "N/A"
    val phone = formatPhoneNumber(prefs)
    val branch = prefs.branchName
    val dept = prefs.departmentName
    val company = prefs.companyName

    val shareText = buildString {
        append("Name: $name\n")
        append("Mobile: $phone\n")

        if (!branch.isNullOrBlank() || !dept.isNullOrBlank()) {
            append("Department: ")
            if (!branch.isNullOrBlank()) append("$branch ")
            if (!dept.isNullOrBlank()) append("- $dept")
            append("\n")
        }

        if (!company.isNullOrBlank()) {
            append("Company: $company\n")
        }
    }

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
}

private fun getAppVersion(): String {
    return try {
        BuildConfig.VERSION_NAME
    } catch (e: Exception) {
        "1.0"
    }
}