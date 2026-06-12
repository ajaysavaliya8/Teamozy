package com.hrms.jeejateamozy.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GreetingSection(
    userName: String?,
    streakDays: Int = 0,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    val (greetingWord, emoji) = when {
        hour < 12 -> "Morning" to "☀️"
        hour < 17 -> "Afternoon" to "☀️"
        else -> "Evening" to "🌙"
    }

    val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
    val currentDate = dateFormat.format(calendar.time)
    val firstName = userName?.split(" ")?.firstOrNull() ?: "there"

    val greetingText = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TeamozyColors.Heading)) {
            append("Good ")
        }
        withStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
                fontSize = 21.sp,
                color = TeamozyColors.PrimaryDark
            )
        ) {
            append(greetingWord)
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TeamozyColors.Heading)) {
            append(", $firstName! $emoji")
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = greetingText)

            if (streakDays > 0) {
                Row(
                    modifier = Modifier
                        .background(TeamozyColors.StreakBg, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "🔥", fontSize = 11.sp)
                    Text(
                        text = "$streakDays-day streak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TeamozyColors.StreakText
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = currentDate,
            fontSize = 13.sp,
            color = TeamozyColors.Secondary
        )
    }
}
