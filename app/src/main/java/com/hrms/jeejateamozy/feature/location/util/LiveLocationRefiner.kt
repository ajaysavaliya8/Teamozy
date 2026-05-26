package com.hrms.jeejateamozy.feature.location.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Continuously samples high-accuracy GPS for the duration of a punch (check-in/out) and holds the
 * single most-accurate fix seen. At commit, [finish] turns that best fix into the committed
 * LiveLocationResult via [LiveLocationHelper.buildResult], so first_location / last_location is
 * identical in shape to the one-shot path.
 *
 * Mirrors GeofenceLocationStream's request config + keep-best rule (via [BestFixPolicy]). Display
 * and auto-reverify use that stream; this refiner exists only to sharpen the committed location
 * while the user fills the reason / work-report sheets. Never blocks the punch — if no usable fix
 * was gathered, [finish] falls back to [LiveLocationHelper.captureLiveLocation].
 */
class LiveLocationRefiner(private val context: Context) {

    private companion object { const val TAG = "LiveLocationRefiner" }

    private val fused: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    private val helper by lazy { LiveLocationHelper(context) }

    @Volatile private var best: Location? = null
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (callback != null) return
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (loc.latitude == 0.0 && loc.longitude == 0.0) return
                val held = best
                val heldAgeMs = held?.let {
                    (SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) / 1_000_000L
                } ?: 0L
                if (BestFixPolicy.shouldReplace(held?.accuracy, heldAgeMs, loc.accuracy)) {
                    best = loc
                }
            }
        }
        try {
            fused.requestLocationUpdates(req, cb, Looper.getMainLooper())
            callback = cb
            Log.d(TAG, "📍 Refiner started")
        } catch (e: SecurityException) {
            // Location permission revoked between the punch button and here — leave the refiner
            // inert; finish() will fall back to the one-shot capture (which surfaces the error).
            Log.w(TAG, "📍 Refiner not started — location permission missing", e)
        }
    }

    fun stop() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
        Log.d(TAG, "📍 Refiner stopped")
    }

    /**
     * Stop sampling and build the committed location from the best fix gathered. Falls back to a
     * one-shot capture if nothing usable was gathered. Safe to call once at commit.
     */
    suspend fun finish(): LiveLocationResult {
        stop()
        val b = best
        return if (b != null) {
            Log.d(TAG, "📍 Refiner finishing with best acc=${b.accuracy}m")
            helper.buildResult(b)
        } else {
            Log.w(TAG, "📍 Refiner had no fix — falling back to one-shot capture")
            helper.captureLiveLocation()
        }
    }
}
