package com.hrms.jeejateamozy.feature.profile.presentation.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
 * Open URL in browser or app
 * If specific app (like Facebook) is installed, it opens there.
 * Otherwise, it opens in the default browser.
 */
fun openUrl(context: Context, url: String) {
    try {
        // Ensure URL has proper protocol
        val formattedUrl = when {
            url.startsWith("http://", ignoreCase = true) ||
                    url.startsWith("https://", ignoreCase = true) -> url
            else -> "https://$url"
        }

        // Create intent - Android will decide whether to use app or browser
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
            // Add flags to ensure it opens externally
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Just start the activity - Android handles the rest
        context.startActivity(intent)

    } catch (e: Exception) {
        // Only catch actual errors (like malformed URLs)
        Toast.makeText(
            context,
            "Unable to open link. Please check the URL format.",
            Toast.LENGTH_SHORT
        ).show()
        android.util.Log.e("ProfileHelpers", "Error opening URL: $url", e)
    }
}

/**
 * Open dialer with phone number
 */
fun dialPhoneNumber(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to open dialer", Toast.LENGTH_SHORT).show()
        android.util.Log.e("ProfileHelpers", "Error opening dialer", e)
    }
}

/**
 * Open email client
 */
fun sendEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to open email app", Toast.LENGTH_SHORT).show()
        android.util.Log.e("ProfileHelpers", "Error opening email", e)
    }
}