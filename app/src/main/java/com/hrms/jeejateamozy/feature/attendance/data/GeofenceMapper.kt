package com.hrms.jeejateamozy.feature.attendance.data

import com.hrms.jeejateamozy.core.network.GeofenceDto
import com.hrms.jeejateamozy.feature.attendance.domain.geofence.Geofence
import com.hrms.jeejateamozy.feature.attendance.domain.geofence.GeoPoint

/** Maps the wire DTO (snake_case) to the domain model (camelCase). */
fun GeofenceDto.toDomain(): Geofence = Geofence(
    enabled = enabled,
    type = type,
    toleranceMeters = tolerance_meters,
    centerLatitude = center_latitude,
    centerLongitude = center_longitude,
    radiusMeters = radius_meters,
    polygon = polygon_coordinates?.map { GeoPoint(it.latitude, it.longitude) }
)
