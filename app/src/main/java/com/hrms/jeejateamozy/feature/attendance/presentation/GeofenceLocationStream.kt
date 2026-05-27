package com.hrms.jeejateamozy.feature.attendance.presentation

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One continuous high-accuracy location stream that runs **while the attendance screen is open**.
 * It converges in the background (while the user reads the screen) so a punch can use the best
 * fresh fix it already holds — instant *and* accurate. The same stream feeds the live in/out
 * display and the auto-reverify watcher.
 *
 * Foreground-only: start on screen-visible, stop on screen-hidden / after a commit. This is
 * unrelated to the shift-long [com.hrms.jeejateamozy.feature.location.service.LocationTrackingService]
 * (the tracking trail) — leave that untouched.
 */
class GeofenceLocationStream(private val fused: FusedLocationProviderClient) {

    private companion object {
        const val TAG = "GeofenceStream"
    }

    private val _best = MutableStateFlow<Location?>(null)
    val best: StateFlow<Location?> = _best.asStateFlow()

    private var cb: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (cb != null) return
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()
        val callback = object : LocationCallback() {
            private var firstFixLogged = false
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val cur = _best.value
                // Keep the most accurate fix; always accept a newer one if the held fix is stale.
                if (cur == null || loc.accuracy <= cur.accuracy || isStale(cur)) {
                    _best.value = loc
                    if (!firstFixLogged) {
                        firstFixLogged = true
                        Log.d(TAG, "📍 first fix acc=${loc.accuracy}m")
                    }
                }
            }
        }
        cb = callback
        fused.requestLocationUpdates(req, callback, Looper.getMainLooper())
        Log.d(TAG, "📍 Geofence stream started")
    }

    fun stop() {
        cb?.let { fused.removeLocationUpdates(it) }
        cb = null
        _best.value = null
        Log.d(TAG, "📍 Geofence stream stopped")
    }

    /** Best fix currently held, but only if fresher than [maxAgeMs]; otherwise null. */
    fun freshBest(maxAgeMs: Long = 5000L): Location? =
        _best.value?.takeIf { ageMs(it) <= maxAgeMs }

    private fun isStale(loc: Location, maxAgeMs: Long = 10_000L) = ageMs(loc) > maxAgeMs

    private fun ageMs(loc: Location): Long =
        (SystemClock.elapsedRealtimeNanos() - loc.elapsedRealtimeNanos) / 1_000_000L
}
