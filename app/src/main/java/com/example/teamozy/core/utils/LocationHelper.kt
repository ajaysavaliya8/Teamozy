package com.example.teamozy.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
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
        val accuracy: Float
    ) : LocationResult
    data class Error(val message: String) : LocationResult
}

class LocationHelper(private val context: Context) {

    private val fused: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun isLocationEnabled(): Boolean = PermissionHelper.isLocationEnabled(context)

    /**
     * Gets a location with:
     * 1) Fast path: last known (if accuracy looks reasonable)
     * 2) Fresh single fix with a timeout
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(
        desiredAccuracyMeters: Float = 75f,
        hardTimeoutMs: Long = 10_000
    ): LocationResult {

        if (!isLocationEnabled()) {
            return LocationResult.Error("GPS is disabled. Please enable Location.")
        }

        // ---- Fast path: last known ----
        val last = runCatching { fused.lastLocation.awaitSafe() }.getOrNull()
        val goodLast = last?.takeIf { it.isReasonable(desiredAccuracyMeters * 2) }
        if (goodLast != null) {
            return LocationResult.Success(
                latitude = goodLast.latitude,
                longitude = goodLast.longitude,
                accuracy = goodLast.accuracy
            )
        }

        // ---- Fresh single fix with timeout ----
        val fresh = withTimeoutOrNull(hardTimeoutMs) {
            fused.awaitSingleFix(priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY)
        } ?: return LocationResult.Error("Unable to get location (timeout). Try moving outdoors.")

        val picked = when {
            fresh.isReasonable(desiredAccuracyMeters) -> fresh
            last != null && fresh.accuracy <= 0f -> last // some OEMs report 0f accuracy
            else -> fresh
        }

        return LocationResult.Success(
            latitude = picked.latitude,
            longitude = picked.longitude,
            accuracy = picked.accuracy
        )
    }
}

/* -------------------- Helpers -------------------- */

private fun Location.isReasonable(maxMeters: Float): Boolean {
    return accuracy in 1f..maxMeters
}

/** Await a single fresh fix using getCurrentLocation(priority, token). */
@SuppressLint("MissingPermission")
private suspend fun FusedLocationProviderClient.awaitSingleFix(
    priority: Int = Priority.PRIORITY_HIGH_ACCURACY
): Location = suspendCancellableCoroutine { cont ->
    val cts = CancellationTokenSource()

    getCurrentLocation(priority, cts.token)
        .addOnSuccessListener { loc ->
            val safe = loc ?: Location("fused").apply {
                latitude = 0.0; longitude = 0.0; accuracy = 0f
            }
            if (!cont.isCompleted) cont.resume(safe)
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
