package com.hrms.jeejateamozy.core.designsystem

import androidx.compose.ui.graphics.Color

/** Single source of truth for Teamozy colors, extracted from the Figma wireframes. */
object TeamozyColors {
    val Background = Color(0xFFF6F7FB)

    // App bar (logged-in screens) is violet; actions/active states are indigo.
    val AppBar = Color(0xFF7C3AED)
    val OnAppBar = Color(0xFFFFFFFF)
    val OnAppBarSecondary = Color(0xFFDDD6FE)

    val Primary = Color(0xFF6366F1)       // indigo-500 — actions, active, links
    val PrimaryGradientStart = Color(0xFF6366F1)
    val PrimaryGradientEnd = Color(0xFF8B5CF6)

    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)

    // Dark-on-tint container text (Tailwind 600/700 ramp)
    val SuccessDark = Color(0xFF059669)
    val WarningDark = Color(0xFFD97706)
    val ErrorDark = Color(0xFFDC2626)
    val PrimaryDark = Color(0xFF4338CA)

    val Heading = Color(0xFF111827)
    val HeadingStrong = Color(0xFF0F172A)
    val Label = Color(0xFF334155)
    val CardLabel = Color(0xFF374151)
    val Secondary = Color(0xFF6B7280)
    val SecondaryAlt = Color(0xFF64748B)
    val Tertiary = Color(0xFF9CA3AF)
    val Placeholder = Color(0xFF94A1B8)

    val Border = Color(0xFFE5E7EB)
    val BorderAlt = Color(0xFFE2E8F0)
    val FieldBg = Color(0xFFF8FAFC)
    val TrackBg = Color(0xFFF1F5FA)

    // Quick-access icon tiles
    val TileRose = Color(0xFFFFE4E6)
    val TileTeal = Color(0xFFCCFBF1)
    val TileAmber = Color(0xFFFEF3C7)

    // Auth header gradient
    val AuthHeaderStart = Color(0xFFEEF2FF)
    val AuthHeaderEnd = Color(0xFFF8FAFF)
}
