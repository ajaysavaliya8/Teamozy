package com.hrms.jeejateamozy.core.designsystem

import androidx.compose.ui.graphics.Color

/** Single source of truth for Teamozy colors, matched exactly to Figma wireframes (node 118:3). */
object TeamozyColors {
    val Background = Color(0xFFFBF7F0)          // warm cream

    // App bar (logged-in screens): dark forest green
    val AppBar = Color(0xFF062E1E)
    val OnAppBar = Color(0xFFFFFFFF)
    val OnAppBarSecondary = Color(0xB8FFFFFF)    // rgba(255,255,255,0.72)

    // Primary action / active states: medium green
    val Primary = Color(0xFF00875A)
    val PrimaryDark = Color(0xFF006A48)          // darker green, greeting italic accent
    val PrimaryGradientStart = Color(0xFF00875A)
    val PrimaryGradientEnd = Color(0xFF006A48)

    // Semantic
    val Success = Color(0xFF00875A)
    val Warning = Color(0xFFE0A93A)
    val Error = Color(0xFFEF4444)
    val SuccessDark = Color(0xFF006A48)
    val WarningDark = Color(0xFFB8862A)
    val ErrorDark = Color(0xFFDC2626)

    // Text
    val Heading = Color(0xFF1A1A1A)
    val HeadingStrong = Color(0xFF0F172A)
    val Label = Color(0xFF334155)
    val CardLabel = Color(0xFF4A4A4A)
    val Secondary = Color(0xFF8A8A8A)
    val SecondaryAlt = Color(0xFF64748B)
    val Tertiary = Color(0xFF9CA3AF)
    val Placeholder = Color(0xFF94A1B8)

    // Surfaces / borders
    val Border = Color(0x0F000000)              // rgba(0,0,0,0.06)
    val BorderAlt = Color(0xFFECECEC)           // bottom-nav divider
    val FieldBg = Color(0xFFF8FAFC)
    val TrackBg = Color(0xFFF1F5FA)

    // Quick-access icon tiles (Figma exact)
    val TileLeaves = Color(0xFFD7F0E6)          // light green
    val TileCirculars = Color(0xFFE5F1FF)        // light blue
    val TileReports = Color(0xFFF8EDD3)          // warm amber/cream
    val TileIconLeaves = Color(0xFF00875A)        // green icon
    val TileIconCirculars = Color(0xFF3B82F6)     // blue icon
    val TileIconReports = Color(0xFFB8862A)       // amber icon

    // Streak badge
    val StreakBg = Color(0xFFF8EDD3)
    val StreakText = Color(0xFFB8862A)

    // Shift badge
    val ShiftBadgeBg = Color(0xFFE0A93A)
    val ShiftBadgeText = Color(0xFF062E1E)

    // Auth header gradient
    val AuthHeaderStart = Color(0xFFEEF2FF)
    val AuthHeaderEnd = Color(0xFFF8FAFF)

    // Check-in ring outer fill
    val CheckInRing = Color(0xFFD7F0E6)
}
