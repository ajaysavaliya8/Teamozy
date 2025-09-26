package com.example.teamozy.core.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun areAllGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { p ->
            ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** Returns true if *any* permission is permanently denied ("Don't ask again"). */
    fun anyPermanentlyDenied(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.any { p ->
            ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, p)
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Quick GPS state check (no prompt). */
    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return runCatching {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    /** Open system Location settings (non-blocking). */
    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
