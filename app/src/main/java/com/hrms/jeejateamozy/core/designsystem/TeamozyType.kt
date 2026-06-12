package com.hrms.jeejateamozy.core.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Inter-equivalent scale. Uses the platform default font family until Inter is bundled. */
object TeamozyType {
    val AuthTitle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = TeamozyColors.HeadingStrong)
    val Greeting = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TeamozyColors.Heading)
    val SectionHeader = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TeamozyColors.Heading)
    val AppBarName = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TeamozyColors.OnAppBar)
    val AppBarCompany = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, color = TeamozyColors.OnAppBarSecondary)
    val Body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val BodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val FieldLabel = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TeamozyColors.Label)
    val Caption = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = TeamozyColors.Secondary)
    val Meta = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = TeamozyColors.Tertiary)
    val Timer = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TeamozyColors.Success)
}
