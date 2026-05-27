package com.hrms.jeejateamozy.feature.attendance.domain.geofence

import kotlin.math.roundToInt

/**
 * Derives the live [GeoStatus] for display from the cached zone + the best current fix.
 * The verdict only updates when the fix is accurate enough; otherwise we report
 * [GeoStatus.Acquiring] rather than flip-flop on noisy readings.
 *
 * Display-only: this never gates a punch. [GeoStatus.LocationOff] is set by the caller
 * when location services are unavailable — it can't be inferred from these inputs.
 */
object GeoStatusEval {

    /** Only trust the in/out verdict when the fix is at least this accurate (metres). */
    const val DISPLAY_ACCURACY_M = 50f

    fun derive(
        geofence: Geofence?,
        lat: Double,
        lng: Double,
        accuracyM: Float?,
        hasFix: Boolean
    ): GeoStatus {
        if (geofence == null || !geofence.enabled) return GeoStatus.Disabled
        if (!hasFix || accuracyM == null || accuracyM > DISPLAY_ACCURACY_M) return GeoStatus.Acquiring
        val e = GeofenceCalc.eval(lat, lng, geofence)
        return if (e.inRange) GeoStatus.InRange else GeoStatus.OutOfRange(e.metresOutside.roundToInt())
    }
}
