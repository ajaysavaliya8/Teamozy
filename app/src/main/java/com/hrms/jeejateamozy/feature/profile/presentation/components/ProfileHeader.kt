package com.hrms.jeejateamozy.feature.profile.presentation.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hrms.jeejateamozy.R
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import com.hrms.jeejateamozy.core.image.CoilImageLoader
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.core.utils.PrivateFileUrl
import com.hrms.jeejateamozy.feature.profile.presentation.utils.formatPhoneNumber
import com.hrms.jeejateamozy.feature.profile.presentation.utils.openUrl

/**
 * Profile Header Section
 * Contains avatar, name, branch, department, profile completion, contact info, and social media
 */
@Composable
fun ProfileHeader(
    context: Context,
    prefs: PreferencesManager,
    profileCompletion: Int,
    isUpdatingPicture: Boolean,
    onProfilePictureClick: () -> Unit,
    onNavigateToEditSocialMedia: () -> Unit
) {
    Column(Modifier.padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Profile Avatar with Edit Button
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                // Avatar Image — always fetch from server; fall back to gender-aware default when no picture uploaded
                val avatarPath = when {
                    !prefs.profileUrl.isNullOrBlank() -> prefs.profileUrl
                    prefs.gender == "FEMALE" -> "private/profile/default_female.webp"
                    else -> "private/profile/default_male.webp"
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(PrivateFileUrl.build(NetworkModule.BASE_URL, avatarPath))
                        .crossfade(true)
                        .build(),
                    imageLoader = CoilImageLoader.get(context),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(
                            3.dp,
                            TeamozyColors.Primary.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )

                // Edit/Add Button
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onProfilePictureClick() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isUpdatingPicture) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                if (prefs.profileUrl.isNullOrBlank())
                                    Icons.Default.Add
                                else
                                    Icons.Default.Edit,
                                contentDescription = "Edit Profile Picture",
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            }

            // Name, Branch, Department & Profile Completion
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp)
            ) {
                Text(
                    text = prefs.fullName ?: prefs.userName ?: "Employee Name",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${prefs.branchName ?: "Branch"} • ${prefs.departmentName ?: "Department"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!prefs.shiftName.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = prefs.shiftName!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Profile Completion Progress
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Profile Completion",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "$profileCompletion%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeamozyColors.Primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { profileCompletion / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
                        color = TeamozyColors.Primary,
                        trackColor = TeamozyColors.Primary.copy(alpha = 0.2f),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(20.dp))

        // Contact Phone Number
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                Icons.Default.Phone,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TeamozyColors.Primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatPhoneNumber(prefs),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Social Media Row
        Row(
            modifier = Modifier.fillMaxWidth(),
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

            // Edit Social Media Button
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