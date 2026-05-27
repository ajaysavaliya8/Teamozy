package com.hrms.jeejateamozy.feature.attendance.domain.geofence

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Local, display-only geofence math. Mirrors the server's `check_geofence` rule so the UI
 * verdict matches the server for the same coordinates. Tolerance always comes from the
 * payload — never hardcoded. Circle and polygon are handled identically: a point is in range
 * when it lies inside the zone or within [Geofence.toleranceMeters] of its boundary, and
 * [GeoEval.metresOutside] reports how far past that tolerance boundary the point is.
 *
 * The server stays the sole authority for the actual punch; this only feeds the live label
 * and the auto-reverify trigger.
 */
object GeofenceCalc {

    private const val EARTH_RADIUS_M = 6371000.0

    /** Metres per degree of latitude on the same sphere haversine uses — keeps the circle and
     *  polygon paths internally consistent. */
    private const val METERS_PER_DEG = 2.0 * Math.PI * EARTH_RADIUS_M / 360.0

    /** Great-circle distance in metres between two lat/lng points. */
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * Evaluate a point against the zone. Unknown/missing geometry (or an unsupported type)
     * is treated as "no restriction" → in range. `enabled` is handled by the caller (a
     * disabled zone shouldn't reach here for evaluation).
     */
    fun eval(lat: Double, lng: Double, g: Geofence): GeoEval = when (g.type) {
        "circle" -> {
            val cLat = g.centerLatitude
            val cLng = g.centerLongitude
            val r = g.radiusMeters
            // Malformed payload (declared circle, missing geometry) → no restriction, never crash.
            if (cLat == null || cLng == null || r == null) {
                GeoEval(inRange = true, metresOutside = 0.0)
            } else {
                val d = haversineMeters(lat, lng, cLat, cLng)
                val edge = d - (r + g.toleranceMeters)
                GeoEval(inRange = edge <= 0.0, metresOutside = maxOf(0.0, edge))
            }
        }
        "polygon" -> {
            val poly = g.polygon
            // Need at least a triangle; otherwise treat as no restriction, never crash.
            if (poly == null || poly.size < 3) {
                GeoEval(inRange = true, metresOutside = 0.0)
            } else if (pointInPolygon(lat, lng, poly)) {
                GeoEval(inRange = true, metresOutside = 0.0)
            } else {
                val edge = edgeDistanceMeters(lat, lng, poly)
                GeoEval(inRange = edge <= g.toleranceMeters, metresOutside = maxOf(0.0, edge - g.toleranceMeters))
            }
        }
        else -> GeoEval(inRange = true, metresOutside = 0.0)
    }

    /** Ray-casting point-in-polygon test. */
    fun pointInPolygon(lat: Double, lng: Double, poly: List<GeoPoint>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val yi = poly[i].latitude; val xi = poly[i].longitude
            val yj = poly[j].latitude; val xj = poly[j].longitude
            if (((yi > lat) != (yj > lat)) && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi)) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * Shortest distance (metres) from a point to the polygon's boundary, using a local
     * equirectangular projection centred on the polygon's mean latitude.
     */
    fun edgeDistanceMeters(lat: Double, lng: Double, poly: List<GeoPoint>): Double {
        if (poly.size < 2) return 0.0
        val lat0 = poly.map { it.latitude }.average()
        val mLat = METERS_PER_DEG
        val mLng = METERS_PER_DEG * cos(Math.toRadians(lat0))
        fun px(p: GeoPoint) = p.longitude * mLng
        fun py(p: GeoPoint) = p.latitude * mLat
        val x = lng * mLng
        val y = lat * mLat
        var best = Double.MAX_VALUE
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            best = minOf(best, pointToSegment(x, y, px(a), py(a), px(b), py(b)))
        }
        return best
    }

    private fun pointToSegment(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        if (dx == 0.0 && dy == 0.0) return hypot(px - ax, py - ay)
        val t = (((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
        return hypot(px - (ax + t * dx), py - (ay + t * dy))
    }
}
