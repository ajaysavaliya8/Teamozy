package com.hrms.jeejateamozy.feature.attendance.domain.geofence

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoStatusEvalTest {

    private val center = GeoPoint(21.19822, 72.87306)
    private fun latOffsetForMeters(m: Double) = m / 111194.9

    private fun circle() = Geofence(
        enabled = true, type = "circle", toleranceMeters = 20,
        centerLatitude = center.latitude, centerLongitude = center.longitude,
        radiusMeters = 80.0, polygon = null
    )

    @Test
    fun null_geofence_is_disabled() {
        assertEquals(GeoStatus.Disabled, GeoStatusEval.derive(null, 0.0, 0.0, 5f, true))
    }

    @Test
    fun disabled_geofence_is_disabled() {
        val g = circle().copy(enabled = false)
        assertEquals(GeoStatus.Disabled, GeoStatusEval.derive(g, center.latitude, center.longitude, 5f, true))
    }

    @Test
    fun no_fix_is_acquiring() {
        assertEquals(GeoStatus.Acquiring, GeoStatusEval.derive(circle(), 0.0, 0.0, null, false))
    }

    @Test
    fun null_accuracy_is_acquiring() {
        assertEquals(GeoStatus.Acquiring, GeoStatusEval.derive(circle(), center.latitude, center.longitude, null, true))
    }

    @Test
    fun poor_accuracy_is_acquiring() {
        // 75 m accuracy > 50 m display threshold
        assertEquals(GeoStatus.Acquiring, GeoStatusEval.derive(circle(), center.latitude, center.longitude, 75f, true))
    }

    @Test
    fun good_fix_inside_is_in_range() {
        assertEquals(GeoStatus.InRange, GeoStatusEval.derive(circle(), center.latitude, center.longitude, 10f, true))
    }

    @Test
    fun good_fix_outside_is_out_of_range_with_rounded_distance() {
        // 130 m out; boundary radius 80 + tol 20 = 100 -> 30 m beyond
        val lat = center.latitude + latOffsetForMeters(130.0)
        val status = GeoStatusEval.derive(circle(), lat, center.longitude, 10f, true)
        assertEquals(GeoStatus.OutOfRange(30), status)
    }
}
