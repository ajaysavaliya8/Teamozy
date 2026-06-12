package com.hrms.jeejateamozy.feature.profile.presentation.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import com.hrms.jeejateamozy.core.image.CoilImageLoader
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.core.utils.PrivateFileUrl
import com.hrms.jeejateamozy.feature.profile.presentation.utils.openUrl

/**
 * Company Card Component
 * Displays company header with logo, name, website, address
 * and contains the ProfileHeader component
 */
@Composable
fun CompanyCard(
    context: Context,
    prefs: PreferencesManager,
    profileCompletion: Int,
    isUpdatingPicture: Boolean,
    onProfilePictureClick: () -> Unit,
    onNavigateToEditSocialMedia: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Company Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                TeamozyColors.AppBar,
                                TeamozyColors.AppBar.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Company Logo
                    val logoUrl = PrivateFileUrl.build(NetworkModule.BASE_URL, prefs.companyLogoUrl)
                    if (logoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(logoUrl)
                                .crossfade(true)
                                .build(),
                            imageLoader = CoilImageLoader.get(context),
                            contentDescription = "Company Logo",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    // Company Info
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = prefs.companyName?.uppercase() ?: "COMPANY NAME",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeamozyColors.OnAppBar
                        )
                        if (!prefs.companyWebsite.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = prefs.companyWebsite!!,
                                fontSize = 12.sp,
                                color = TeamozyColors.OnAppBar.copy(alpha = 0.9f),
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
                            color = TeamozyColors.OnAppBar.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Profile Header Section
            ProfileHeader(
                context = context,
                prefs = prefs,
                profileCompletion = profileCompletion,
                isUpdatingPicture = isUpdatingPicture,
                onProfilePictureClick = onProfilePictureClick,
                onNavigateToEditSocialMedia = onNavigateToEditSocialMedia
            )
        }
    }
}