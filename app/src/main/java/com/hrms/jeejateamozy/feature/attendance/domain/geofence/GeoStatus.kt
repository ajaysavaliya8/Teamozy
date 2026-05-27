package com.hrms.jeejateamozy.feature.attendance.domain.geofence

/**
 * UI-facing live geofence status for the attendance screen. Display-only — never gates a punch.
 */
sealed interface GeoStatus {
    /** No zone for this employee, or geofence not enforced. */
    data object Disabled : GeoStatus

    /** Zone known, but no fix yet accurate enough to judge. */
    data object Acquiring : GeoStatus

    /** Location services off / unavailable. */
    data object LocationOff : GeoStatus

    /** Current fix is inside the zone (or within tolerance). */
    data object InRange : GeoStatus

    /** Current fix is outside; [metres] is distance beyond the tolerance boundary. */
    data class OutOfRange(val metres: Int) : GeoStatus
}
