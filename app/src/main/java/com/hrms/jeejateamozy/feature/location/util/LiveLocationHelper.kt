package com.hrms.jeejateamozy.feature.location.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.hrms.jeejateamozy.BuildConfig
import com.hrms.jeejateamozy.feature.location.model.LocationData
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

/**
 * Helper class for capturing live location with all device metadata
 * Used for check-in first location and check-out last location
 */
class LiveLocationHelper(private val context: Context) {

    companion object {
        private const val TAG = "LiveLocationHelper"
        private const val LOCATION_TIMEOUT_MS = 10_000L
    }

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Capture current live location with all device metadata
     * This always gets a FRESH location, not cached
     */
    @SuppressLint("MissingPermission")
    suspend fun captureLiveLocation(): LiveLocationResult {
        Log.d(TAG, "📍 Capturing live location...")

        return try {
            // Get fresh location with timeout
            val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                getFreshLocation()
            }

            if (location == null) {
                Log.e(TAG, "❌ Location timeout")
                return LiveLocationResult.Error("Unable to get location (timeout)")
            }

            if (location.latitude == 0.0 && location.longitude == 0.0) {
                Log.e(TAG, "❌ Invalid location coordinates")
                return LiveLocationResult.Error("Unable to get valid location")
            }

            // Collect all device metadata (with safe handling for missing permissions)
            val deviceId = getDeviceId()
            val appVersion = BuildConfig.VERSION_NAME
            val batteryLevel = getBatteryLevel()
            val networkType = getNetworkType()
            val wifiInfo = getWifiInfoSafe()  // Safe version
            val recordedAt = getCurrentISOTimestamp()

            val locationData = LocationData(
                recordedAt = recordedAt,
                latitude = location.latitude,
                longitude = location.longitude,
                locationAccuracy = location.accuracy,
                altitude = if (location.hasAltitude()) location.altitude else null,
                verticalAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy()) {
                    location.verticalAccuracyMeters
                } else null,
                speed = if (location.hasSpeed()) location.speed else null,
                heading = if (location.hasBearing()) location.bearing else null,
                deviceId = deviceId,
                appVersion = appVersion,
                networkType = networkType,
                wifiName = wifiInfo.first,
                wifiMacAddress = wifiInfo.second,
                batteryLevel = batteryLevel,
                geofenceId = null // Will be determined by server
            )

            Log.d(TAG, "✅ Live location captured: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")
            LiveLocationResult.Success(locationData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error capturing location", e)
            LiveLocationResult.Error(e.message ?: "Failed to capture location")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(): Location? = suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cts.token
        ).addOnSuccessListener { location ->
            if (!cont.isCompleted) {
                cont.resume(location)
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "❌ Location request failed", e)
            if (!cont.isCompleted) {
                cont.resume(null)
            }
        }

        cont.invokeOnCancellation {
            cts.cancel()
        }
    }

    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""
    }

    private fun getBatteryLevel(): Int? {
        return try {
            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            batteryIntent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    (level * 100 / scale)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level", e)
            null
        }
    }

    private fun getNetworkType(): String? {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return "OFFLINE"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "OFFLINE"

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
                else -> "OTHER"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network type", e)
            null
        }
    }

    /**
     * Safe version of getWifiInfo that handles missing ACCESS_WIFI_STATE permission
     */
    private fun getWifiInfoSafe(): Pair<String?, String?> {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return Pair(null, null)

            val wifiInfo = wifiManager.connectionInfo ?: return Pair(null, null)

            val ssid = wifiInfo.ssid?.replace("\"", "")?.takeIf { it != "<unknown ssid>" }
            val bssid = wifiInfo.bssid?.takeIf { it != "02:00:00:00:00:00" }

            Pair(ssid, bssid)
        } catch (e: SecurityException) {
            // ACCESS_WIFI_STATE permission not granted - this is OK, just return nulls
            Log.w(TAG, "ACCESS_WIFI_STATE permission not granted - WiFi info unavailable")
            Pair(null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi info", e)
            Pair(null, null)
        }
    }

    private fun getCurrentISOTimestamp(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT)
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .format(java.util.Date())
        }
    }
}