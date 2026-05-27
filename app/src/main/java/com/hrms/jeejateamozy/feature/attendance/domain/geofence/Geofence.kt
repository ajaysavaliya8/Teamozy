package com.hrms.jeejateamozy.feature.attendance.domain.geofence

/**
 * A latitude/longitude pair. Field names match the `polygon_coordinates` JSON
 * (`{"latitude":…, "longitude":…}`) so Gson parses it directly.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

/**
 * Work-zone geometry from `GET /check-status` → `data.geofence`, in domain (camelCase) form.
 *
 * Display-only. The server remains the sole authority on in/out for an actual punch
 * (`POST /check-in`, `/check-out`, `-location-reverify`). This local copy only drives
 * the live UI label and the auto-reverify trigger.
 */
data class Geofence(
    val enabled: Boolean,
    val type: String?,                 // "circle" | "polygon"
    val toleranceMeters: Int,
    val centerLatitude: Double?,
    val centerLongitude: Double?,
    val radiusMeters: Double?,
    val polygon: List<GeoPoint>?
)

/**
 * Result of a local geofence evaluation.
 * [metresOutside] is the distance **beyond the tolerance boundary** — 0 when in range.
 */
data class GeoEval(
    val inRange: Boolean,
    val metresOutside: Double
)
