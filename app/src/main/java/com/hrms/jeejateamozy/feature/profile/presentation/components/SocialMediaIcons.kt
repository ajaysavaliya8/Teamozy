package com.hrms.jeejateamozy.feature.profile.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Social Media Icon (Static - no edit mode)
 * Used in Profile screen to display social media links
 */
@Composable
fun SocialMediaIconStatic(
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