package com.hrms.jeejateamozy.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed interface LocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val isMocked: Boolean = false
    ) : LocationResult
    data class Error(val message: String) : LocationResult
}

class LocationHelper(private val context: Context) {

    private val fused: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun isLocationEnabled(): Boolean = PermissionHelper.isLocationEnabled(context)

    /**
     * Pre-warm GPS so the next getCurrentLocation() call hits the fast path.
     * Fire-and-forget — just triggers a single fix to populate the lastLocation cache.
     */
    @SuppressLint("MissingPermission")
    fun warmUpGps() {
        if (!isLocationEnabled()) return
        val cts = CancellationTokenSource()
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
    }

    /**
     * Gets a location with:
     * 1) Fast path: last known (if accuracy looks reasonable)
     * 2) Fresh single fix with a timeout
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(
        desiredAccuracyMeters: Float = 15f,
        hardTimeoutMs: Long = 15_000,
        maxCacheAgeMs: Long = 10_000
    ): LocationResult {

        if (!isLocationEnabled()) {
            return LocationResult.Error("GPS is disabled. Please enable Location.")
        }

        // ---- Fast path: last known (only if fresh, accurate & not mocked) ----
        val last = runCatching { fused.lastLocation.awaitSafe() }.getOrNull()
        val goodLast = last?.takeIf {
            it.isReasonable(desiredAccuracyMeters) && it.isFresh(maxCacheAgeMs) && !it.isMocked()
        }
        if (goodLast != null) {
            return LocationResult.Success(
                latitude = goodLast.latitude,
                longitude = goodLast.longitude,
                accuracy = goodLast.accuracy
            )
        }

        // ---- Fresh single fix with timeout (high accuracy GPS) ----
        val fresh = withTimeoutOrNull(hardTimeoutMs) {
            fused.awaitSingleFix(priority = Priority.PRIORITY_HIGH_ACCURACY)
        } ?: return LocationResult.Error("Unable to get location (timeout). Try moving outdoors.")

        // Reject (0,0) fallback locations
        if (fresh.latitude == 0.0 && fresh.longitude == 0.0) {
            return LocationResult.Error("Unable to get valid location. Try moving outdoors.")
        }

        val picked = when {
            fresh.isReasonable(desiredAccuracyMeters) -> fresh
            last != null && fresh.accuracy <= 0f && last.isFresh(maxCacheAgeMs) -> last
            else -> fresh
        }

        return LocationResult.Success(
            latitude = picked.latitude,
            longitude = picked.longitude,
            accuracy = picked.accuracy,
            isMocked = picked.isMocked()
        )
    }
}

/* -------------------- Helpers -------------------- */

private fun Location.isReasonable(maxMeters: Float): Boolean {
    return accuracy in 1f..maxMeters
}

/** Returns true if this location fix is no older than [maxAgeMs] milliseconds. */
private fun Location.isFresh(maxAgeMs: Long): Boolean {
    val ageMs = SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos
    return ageMs <= maxAgeMs * 1_000_000L
}

/** Returns true if this location was provided by a mock/fake GPS app. */
private fun Location.isMocked(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isMock
    } else {
        @Suppress("DEPRECATION")
        isFromMockProvider
    }
}

/** Await a single fresh fix using getCurrentLocation(priority, token). Returns null on failure. */
@SuppressLint("MissingPermission")
private suspend fun FusedLocationProviderClient.awaitSingleFix(
    priority: Int = Priority.PRIORITY_HIGH_ACCURACY
): Location = suspendCancellableCoroutine { cont ->
    val cts = CancellationTokenSource()

    getCurrentLocation(priority, cts.token)
        .addOnSuccessListener { loc ->
            if (!cont.isCompleted) {
                cont.resume(loc ?: Location("fused").apply {
                    latitude = 0.0; longitude = 0.0; accuracy = 0f
                })
            }
        }
        .addOnFailureListener {
            if (!cont.isCompleted) {
                cont.resume(Location("fused").apply {
                    latitude = 0.0; longitude = 0.0; accuracy = 0f
                })
            }
        }

    cont.invokeOnCancellation { cts.cancel() }
}

/** Await helper for Task<Location> without extra libs. */
private suspend fun Task<Location>.awaitSafe(): Location? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resume(null) }
    addOnCanceledListener { cont.resume(null) }
}
