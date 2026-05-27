package com.hrms.jeejateamozy.feature.attendance.domain.geofence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic tests for [GeofenceCalc]. The local calc must match the server's geofence
 * rule for the same coordinates, with tolerance taken from the payload.
 */
class GeofenceCalcTest {

    private val center = GeoPoint(21.19822, 72.87306)

    /** Metres → degrees of latitude (haversine uses ~111194.9 m per degree at R=6371000). */
    private fun latOffsetForMeters(m: Double) = m / 111194.9

    // ---- haversine ----

    @Test
    fun haversine_returns_about_111m_for_one_milli_degree_latitude() {
        val d = GeofenceCalc.haversineMeters(21.0, 72.0, 21.001, 72.0)
        assertEquals(111.19, d, 1.0)
    }

    // ---- circle ----

    @Test
    fun circle_point_at_center_is_in_range_zero_outside() {
        val e = GeofenceCalc.eval(center.latitude, center.longitude, circle(radius = 80.0, tolerance = 20))
        assertTrue(e.inRange)
        assertEquals(0.0, e.metresOutside, 0.001)
    }

    @Test
    fun circle_point_within_tolerance_band_is_in_range() {
        // 90 m out; boundary = radius 80 + tolerance 20 = 100
        val lat = center.latitude + latOffsetForMeters(90.0)
        val e = GeofenceCalc.eval(lat, center.longitude, circle(radius = 80.0, tolerance = 20))
        assertTrue(e.inRange)
        assertEquals(0.0, e.metresOutside, 0.001)
    }

    @Test
    fun circle_point_beyond_tolerance_is_out_of_range_with_distance() {
        // 130 m out; boundary 100 -> 30 m beyond
        val lat = center.latitude + latOffsetForMeters(130.0)
        val e = GeofenceCalc.eval(lat, center.longitude, circle(radius = 80.0, tolerance = 20))
        assertFalse(e.inRange)
        assertEquals(30.0, e.metresOutside, 1.0)
    }

    // ---- polygon ----

    @Test
    fun polygon_point_inside_is_in_range() {
        val e = GeofenceCalc.eval(21.1990, 72.8730, square(tolerance = 10))
        assertTrue(e.inRange)
        assertEquals(0.0, e.metresOutside, 0.001)
    }

    @Test
    fun polygon_point_just_outside_within_tolerance_is_in_range() {
        // ~5.6 m north of the top edge (lat 21.2000), tolerance 10
        val e = GeofenceCalc.eval(21.20005, 72.8730, square(tolerance = 10))
        assertTrue(e.inRange)
        assertEquals(0.0, e.metresOutside, 0.001)
    }

    @Test
    fun polygon_point_beyond_tolerance_is_out_of_range_with_distance() {
        // ~55.66 m north of top edge -> ~45.66 m beyond tolerance 10
        val e = GeofenceCalc.eval(21.20050, 72.8730, square(tolerance = 10))
        assertFalse(e.inRange)
        assertEquals(45.66, e.metresOutside, 1.0)
    }

    @Test
    fun pointInPolygon_true_inside_false_outside() {
        val poly = squarePoly()
        assertTrue(GeofenceCalc.pointInPolygon(21.1990, 72.8730, poly))
        assertFalse(GeofenceCalc.pointInPolygon(21.2050, 72.8730, poly))
    }

    // ---- no restriction ----

    @Test
    fun unknown_type_is_treated_as_no_restriction() {
        val g = Geofence(
            enabled = true, type = null, toleranceMeters = 0,
            centerLatitude = null, centerLongitude = null, radiusMeters = null, polygon = null
        )
        val e = GeofenceCalc.eval(0.0, 0.0, g)
        assertTrue(e.inRange)
        assertEquals(0.0, e.metresOutside, 0.001)
    }

    @Test
    fun circle_with_missing_geometry_is_no_restriction_not_crash() {
        val g = Geofence(
            enabled = true, type = "circle", toleranceMeters = 20,
            centerLatitude = null, centerLongitude = null, radiusMeters = null, polygon = null
        )
        val e = GeofenceCalc.eval(21.0, 72.0, g)
        assertTrue(e.inRange)
        assertEquals(0.0, e.metresOutside, 0.001)
    }

    @Test
    fun polygon_with_null_points_is_no_restriction_not_crash() {
        val g = Geofence(
            enabled = true, type = "polygon", toleranceMeters = 10,
            centerLatitude = null, centerLongitude = null, radiusMeters = null, polygon = null
        )
        val e = GeofenceCalc.eval(21.0, 72.0, g)
        assertTrue(e.inRange)
        assertEquals(0.0, e.metresOutside, 0.001)
    }

    @Test
    fun polygon_with_too_few_points_is_no_restriction_not_crash() {
        val g = Geofence(
            enabled = true, type = "polygon", toleranceMeters = 10,
            centerLatitude = null, centerLongitude = null, radiusMeters = null,
            polygon = listOf(GeoPoint(21.0, 72.0), GeoPoint(21.001, 72.0))
        )
        val e = GeofenceCalc.eval(21.0, 72.0, g)
        assertTrue(e.inRange)
        assertEquals(0.0, e.metresOutside, 0.001)
    }

    // ---- fixtures ----

    private fun circle(radius: Double, tolerance: Int) = Geofence(
        enabled = true, type = "circle", toleranceMeters = tolerance,
        centerLatitude = center.latitude, centerLongitude = center.longitude,
        radiusMeters = radius, polygon = null
    )

    private fun squarePoly() = listOf(
        GeoPoint(21.1980, 72.8720),
        GeoPoint(21.2000, 72.8720),
        GeoPoint(21.2000, 72.8740),
        GeoPoint(21.1980, 72.8740)
    )

    private fun square(tolerance: Int) = Geofence(
        enabled = true, type = "polygon", toleranceMeters = tolerance,
        centerLatitude = null, centerLongitude = null, radiusMeters = null,
        polygon = squarePoly()
    )
}
