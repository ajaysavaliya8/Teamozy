package com.hrms.jeejateamozy.feature.profile.presentation.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hrms.jeejateamozy.core.utils.PreferencesManager

/**
 * Helper functions for ProfileScreen
 */

/**
 * Calculate profile completion percentage based on filled fields
 */
fun calculateProfileCompletion(prefs: PreferencesManager): Int {
    var filled = 0
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

/**
 * Format phone number with country code
 */
fun formatPhoneNumber(prefs: PreferencesManager): String {
    if (prefs.mobileNumber != 0L) {
        val s = prefs.mobileNumber.toString()
        return if (s.length == 10) "+91-$s" else s
    }
    val userId = prefs.userId
    if (!userId.isNullOrBlank()) return if (userId.length == 10) "+91-$userId" else userId
    return "Not Available"
}

/**
 * Open URL in browser
 */
fun openUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

/**
 * Open dialer with phone number
 */
fun dialPhoneNumber(context: Context, phoneNumber: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    }
}

/**
 * Open email client
 */
fun sendEmail(context: Context, email: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
        }
        context.startActivity(intent)
    }
}