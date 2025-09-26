package com.example.teamozy.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface LocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float
    ) : LocationResult
    data class Error(val message: String) : LocationResult
}

class LocationHelper(private val context: Context) {

    fun isLocationEnabled(): Boolean = PermissionHelper.isLocationEnabled(context)

    /**
     * Obtain one reasonably fresh location fix.
     * NB: This is a skeleton; wire to FusedLocationProviderClient in your project if needed.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!isLocationEnabled()) {
            return LocationResult.Error("GPS is disabled. Please enable Location.")
        }

        // Try last known first (fast path)
        val quick = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (quick != null) {
            return LocationResult.Success(
                latitude = quick.latitude,
                longitude = quick.longitude,
                accuracy = quick.accuracy
            )
        }

        // (Optional) Collect a single update. Here we just return an informative error to keep it simple.
        return suspendCancellableCoroutine { cont ->
            cont.resume(LocationResult.Error("Unable to get location. Try moving outdoors."))
        }
    }
}
