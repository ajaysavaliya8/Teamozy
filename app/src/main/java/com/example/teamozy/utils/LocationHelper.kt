package com.example.teamozy.utils

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    suspend fun getCurrentLocation(): LocationResult {
        return if (hasLocationPermission()) {
            if (!isLocationEnabled()) {
                return LocationResult.Error("GPS is disabled. Please enable location services.")
            }

            try {
                // Try to get high accuracy location with timeout
                val location = withTimeoutOrNull(30000L) { // 30 second timeout
                    getCurrentLocationInternal()
                }

                if (location != null && isLocationAccurate(location)) {
                    LocationResult.Success(location.latitude, location.longitude, location.accuracy)
                } else if (location != null) {
                    // Location obtained but not very accurate
                    LocationResult.Success(location.latitude, location.longitude, location.accuracy,
                        warning = "Location accuracy is ${location.accuracy.toInt()}m. Consider moving to open area for better accuracy.")
                } else {
                    LocationResult.Error("Unable to get location within 30 seconds. Please try again in an open area.")
                }
            } catch (e: Exception) {
                LocationResult.Error("Location error: ${e.message}")
            }
        } else {
            LocationResult.Error("Location permission not granted")
        }
    }

    @Suppress("MissingPermission")
    private suspend fun getCurrentLocationInternal(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // High accuracy location request
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L // Update every 1 second for faster response
        ).apply {
            setMinUpdateIntervalMillis(500L) // Minimum 500ms between updates
            setMaxUpdateDelayMillis(5000L) // Maximum 5 seconds delay
            setMaxUpdates(1) // Only need one accurate location
            setGranularity(Granularity.GRANULARITY_FINE) // Fine granularity
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                fusedLocationClient.removeLocationUpdates(this)

                println("DEBUG: Received location - lat: ${location?.latitude}, lng: ${location?.longitude}, accuracy: ${location?.accuracy}m")
                continuation.resume(location)
            }
        }

        // Try to get last known location first
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation: Location? ->
            if (lastLocation != null && isLocationRecent(lastLocation) && isLocationAccurate(lastLocation)) {
                println("DEBUG: Using recent cached location - accuracy: ${lastLocation.accuracy}m")
                continuation.resume(lastLocation)
            } else {
                println("DEBUG: Requesting fresh high-accuracy location...")
                // Request fresh high-accuracy location
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }.addOnFailureListener {
            println("DEBUG: Failed to get last location, requesting fresh location...")
            // Request fresh location on failure
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        continuation.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun isLocationRecent(location: Location): Boolean {
        val timeDifference = System.currentTimeMillis() - location.time
        return timeDifference < 30000 // 30 seconds for attendance accuracy
    }

    private fun isLocationAccurate(location: Location): Boolean {
        // Consider location accurate if within 20 meters
        return location.hasAccuracy() && location.accuracy <= 20f
    }

    fun hasLocationPermission(): Boolean {
        return com.example.teamozy.PermissionHelper.checkAllPermissions(
            context,
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun waitForAccurateLocation(): LocationResult {
        return if (hasLocationPermission()) {
            if (!isLocationEnabled()) {
                return LocationResult.Error("GPS is disabled. Please enable location services.")
            }

            try {
                // Wait longer for very accurate location
                val location = withTimeoutOrNull(45000L) { // 45 second timeout
                    waitForHighAccuracyLocation()
                }

                if (location != null) {
                    LocationResult.Success(location.latitude, location.longitude, location.accuracy)
                } else {
                    LocationResult.Error("Unable to get accurate location. Please ensure you're in an open area with clear sky view.")
                }
            } catch (e: Exception) {
                LocationResult.Error("Location error: ${e.message}")
            }
        } else {
            LocationResult.Error("Location permission not granted")
        }
    }

    @Suppress("MissingPermission")
    private suspend fun waitForHighAccuracyLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            500L // Very frequent updates
        ).apply {
            setMinUpdateIntervalMillis(250L)
            setMaxUpdateDelayMillis(2000L)
            setGranularity(Granularity.GRANULARITY_FINE)
        }.build()

        var bestLocation: Location? = null
        var updateCount = 0
        val maxUpdates = 10 // Try up to 10 location updates

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation ?: return
                updateCount++

                println("DEBUG: Location update $updateCount - accuracy: ${location.accuracy}m")

                // Keep the most accurate location
                if (bestLocation == null || location.accuracy < bestLocation!!.accuracy) {
                    bestLocation = location
                }

                // Stop if we get very accurate location (under 10m) or reached max updates
                if (location.accuracy <= 10f || updateCount >= maxUpdates) {
                    fusedLocationClient.removeLocationUpdates(this)
                    continuation.resume(bestLocation)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        continuation.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}

sealed class LocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val warning: String? = null
    ) : LocationResult()
    data class Error(val message: String) : LocationResult()
}