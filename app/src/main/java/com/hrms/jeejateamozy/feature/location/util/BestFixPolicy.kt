package com.hrms.jeejateamozy.feature.location.util

/**
 * Pure decision rule for the continuous commit-location refiner: should a freshly-arrived GPS fix
 * replace the best fix held so far? Mirrors GeofenceLocationStream's "keep the most accurate fix,
 * but accept a newer one if the held fix has gone stale" rule, plus a guard that rejects fixes
 * whose accuracy is non-positive (unknown). Android-free so it is unit-testable.
 */
object BestFixPolicy {

    /** A held fix older than this is replaced even by a less-accurate newer fix (ms). */
    const val STALE_MS = 10_000L

    /**
     * @param heldAccuracyM accuracy of the best fix held so far, or null if none held yet
     * @param heldAgeMs     age of the held fix in ms (ignored when [heldAccuracyM] is null)
     * @param newAccuracyM  accuracy of the newly-arrived fix
     * @return true if the new fix should become the held best fix
     */
    fun shouldReplace(heldAccuracyM: Float?, heldAgeMs: Long, newAccuracyM: Float): Boolean {
        if (newAccuracyM <= 0f) return false        // unknown/invalid accuracy — never trust it
        if (heldAccuracyM == null) return true       // nothing held yet — take the first valid fix
        return newAccuracyM <= heldAccuracyM || heldAgeMs > STALE_MS
    }
}
