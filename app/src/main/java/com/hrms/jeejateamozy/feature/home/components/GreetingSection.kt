package com.hrms.jeejateamozy.feature.home.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Greeting Section Component
 * Shows time-based greeting with current date
 */
@Composable
fun GreetingSection(
    userName: String?,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    val (greeting, emoji) = when {
        hour < 12 -> "Good Morning" to "\uD83C\uDF1E"  // Sun emoji
        hour < 17 -> "Good Afternoon" to "☀\uFE0F"
        else -> "Good Evening" to "\uD83C\uDF19"  // Moon emoji
    }

    val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
    val currentDate = dateFormat.format(calendar.time)

    val firstName = userName?.split(" ")?.firstOrNull() ?: "there"

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$greeting, $firstName!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = com.hrms.jeejateamozy.core.designsystem.TeamozyColors.Heading
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = emoji,
                fontSize = 18.sp
            )
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = currentDate,
            fontSize = 13.sp,
            color = com.hrms.jeejateamozy.core.designsystem.TeamozyColors.Secondary
        )
    }
}
