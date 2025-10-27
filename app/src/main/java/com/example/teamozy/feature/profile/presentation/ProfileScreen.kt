@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.profile.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.teamozy.BuildConfig
import com.example.teamozy.R
import com.example.teamozy.core.utils.PreferencesManager

@Composable
fun ProfileScreen(
    onNavigateToFaceChange: () -> Unit,
    onNavigateToEditSocialMedia: () -> Unit,
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* TODO */ },
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
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Header
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!prefs.companyLogoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = prefs.companyLogoUrl,
                                    contentDescription = "Company Logo",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.width(12.dp))
                            }

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = prefs.companyName?.uppercase() ?: "COMPANY NAME",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                if (!prefs.companyWebsite.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = prefs.companyWebsite!!,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier.clickable {
                                            openUrl(
                                                context,
                                                if (prefs.companyWebsite!!.startsWith("http"))
                                                    prefs.companyWebsite!!
                                                else "https://${prefs.companyWebsite}"
                                            )
                                        }
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = prefs.companyAddress ?: "Company Address",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    // Profile
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier.size(120.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                if (!prefs.profileUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = prefs.profileUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .border(
                                                3.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                CircleShape
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .border(
                                                3.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                CircleShape
                                            )
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
                                }

                                // small add on avatar
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

                            // Name + completion
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 20.dp)
                            ) {
                                Text(
                                    text = prefs.fullName ?: prefs.userName ?: "User Name",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = listOfNotNull(prefs.branchName, prefs.departmentName)
                                        .joinToString(" • ")
                                        .ifBlank { "Department N/A" },
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))

                                val completion = calculateProfileCompletion(prefs)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = (completion / 100f).coerceIn(0f, 1f))
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Text(
                                        text = "$completion%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Phone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = formatPhoneNumber(prefs),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Social Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SocialMediaIconStatic(
                            iconRes = R.drawable.ic_facebook,
                            hasLink = !prefs.facebook.isNullOrBlank(),
                            onClick = { prefs.facebook?.let { openUrl(context, it) } }
                        )
                        SocialMediaIconStatic(
                            iconRes = R.drawable.ic_linkedin,
                            hasLink = !prefs.linkedin.isNullOrBlank(),
                            onClick = { prefs.linkedin?.let { openUrl(context, it) } }
                        )
                        SocialMediaIconStatic(
                            iconRes = R.drawable.ic_x,
                            hasLink = !prefs.x.isNullOrBlank(),
                            onClick = { prefs.x?.let { openUrl(context, it) } }
                        )
                        SocialMediaIconStatic(
                            iconRes = R.drawable.ic_instagram,
                            hasLink = !prefs.instagram.isNullOrBlank(),
                            onClick = { prefs.instagram?.let { openUrl(context, it) } }
                        )
                        SocialMediaIconStatic(
                            iconRes = R.drawable.ic_snapchat,
                            hasLink = !prefs.snapchat.isNullOrBlank(),
                            onClick = { prefs.snapchat?.let { openUrl(context, it) } }
                        )

                        // Edit button: IconButton for reliable clicks
                        IconButton(
                            onClick = onNavigateToEditSocialMedia,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Social Media",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val hasHRInfo = !prefs.hrEmail.isNullOrBlank() || !prefs.companyContact.isNullOrBlank()

            if (hasHRInfo) {
                SectionCard(title = "HR Contact") {
                    if (!prefs.companyContact.isNullOrBlank()) {
                        ContactItem(
                            icon = Icons.Default.Phone,
                            title = "HR Mobile",
                            value = prefs.companyContact!!,
                            onClick = { dialPhoneNumber(context, prefs.companyContact!!) }
                        )
                        if (!prefs.hrEmail.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                    if (!prefs.hrEmail.isNullOrBlank()) {
                        ContactItem(
                            icon = Icons.Default.Email,
                            title = "HR Email",
                            value = prefs.hrEmail!!,
                            onClick = { sendEmail(context, prefs.hrEmail!!) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            SectionCard(title = "Face Recognition") {
                ProfileMenuItem(
                    icon = Icons.Outlined.Face,
                    title = "Update Face",
                    subtitle = "Update your face for recognition",
                    onClick = onNavigateToFaceChange,
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Account") {
                ProfileMenuItem(
                    icon = Icons.Outlined.Person,
                    title = "Personal Information",
                    subtitle = "View and edit your personal details",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ProfileMenuItem(
                    icon = Icons.Outlined.Lock,
                    title = "Privacy & Security",
                    subtitle = "Manage your privacy settings",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ProfileMenuItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    subtitle = "Configure notification preferences",
                    onClick = { /* TODO */ }
                )
            }

            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Support") {
                val hasTechnicalInfo = !prefs.technicalSupportNumber.isNullOrBlank() ||
                        !prefs.technicalSupportEmail.isNullOrBlank()

                if (hasTechnicalInfo) {
                    if (!prefs.technicalSupportNumber.isNullOrBlank()) {
                        ContactItem(
                            icon = Icons.Default.Phone,
                            title = "Technical Support",
                            value = prefs.technicalSupportNumber!!,
                            onClick = { dialPhoneNumber(context, prefs.technicalSupportNumber!!) }
                        )
                        if (!prefs.technicalSupportEmail.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                    if (!prefs.technicalSupportEmail.isNullOrBlank()) {
                        ContactItem(
                            icon = Icons.Default.Email,
                            title = "Tech Support Email",
                            value = prefs.technicalSupportEmail!!,
                            onClick = { sendEmail(context, prefs.technicalSupportEmail!!) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }

                ProfileMenuItem(
                    icon = Icons.Outlined.Info,
                    title = "Help & Support",
                    subtitle = "Get help or contact support",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ProfileMenuItem(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    subtitle = "App version ${getAppVersion()}",
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ProfileMenuItem(
                    icon = Icons.Outlined.ExitToApp,
                    title = "Logout",
                    subtitle = "Sign out from your account",
                    onClick = { showLogoutDialog = true },
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(100.dp))
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                icon = {
                    Icon(
                        Icons.Outlined.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Logout", fontWeight = FontWeight.Bold) },
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
                    ) { Text("Logout") }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

/* ---------------- Helpers & UI components ---------------- */

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            content()
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(onClick = onClick, color = Color.Transparent) {
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
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = titleColor)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ContactItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Neutral chip so PNG colors remain intact; dims when link is missing. */
@Composable
private fun SocialMediaIconStatic(
    iconRes: Int,
    hasLink: Boolean,
    onClick: () -> Unit,
    iconSizeDp: Int = 22
) {
    val chipBg = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val alpha = if (hasLink) 1f else 0.45f
    val clickable = if (hasLink) Modifier.clickable { onClick() } else Modifier

    Box(
        modifier = Modifier
            .size(40.dp)
            .then(clickable)
            .alpha(alpha)
            .clip(CircleShape)
            .background(chipBg)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(iconSizeDp.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/* ---------------- Helpers ---------------- */

private fun calculateProfileCompletion(prefs: PreferencesManager): Int {
    var filled = 0
    // count: name, branch, dept, profile, facebook, linkedin, x, instagram, snapchat = 9
    val total = 9
    if (!prefs.fullName.isNullOrBlank() || !prefs.userName.isNullOrBlank()) filled++
    if (!prefs.branchName.isNullOrBlank()) filled++
    if (!prefs.departmentName.isNullOrBlank()) filled++
    if (!prefs.profileUrl.isNullOrBlank()) filled++
    if (!prefs.facebook.isNullOrBlank()) filled++
    if (!prefs.linkedin.isNullOrBlank()) filled++
    if (!prefs.x.isNullOrBlank()) filled++
    if (!prefs.instagram.isNullOrBlank()) filled++
    if (!prefs.snapchat.isNullOrBlank()) filled++
    return (filled * 100) / total
}

private fun formatPhoneNumber(prefs: PreferencesManager): String {
    if (prefs.mobileNumber != 0L) {
        val s = prefs.mobileNumber.toString()
        return if (s.length == 10) "+91-$s" else s
    }
    val userId = prefs.userId
    if (!userId.isNullOrBlank()) return if (userId.length == 10) "+91-$userId" else userId
    return "Not Available"
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

private fun dialPhoneNumber(context: android.content.Context, phoneNumber: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phoneNumber") }
        context.startActivity(intent)
    }
}

private fun sendEmail(context: android.content.Context, email: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:$email") }
        context.startActivity(intent)
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
        if (!company.isNullOrBlank()) append("Company: $company\n")
    }

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
}

private fun getAppVersion(): String =
    runCatching { BuildConfig.VERSION_NAME }.getOrDefault("1.0")
