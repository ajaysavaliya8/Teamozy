package com.hrms.jeejateamozy.feature.location.data

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Represents a single GPS location point with all metadata
 */
data class LocationData(
    val recordedAt: String,              // ISO 8601 timestamp
    val latitude: Double,
    val longitude: Double,
    val locationAccuracy: Float? = null,
    val altitude: Double? = 0.0,         // Default to 0 as per API docs
    val verticalAccuracy: Float? = null,
    val speed: Float? = null,            // in km/h
    val heading: Float? = null,          // 0-360 degrees
    val deviceId: String,
    val appVersion: String,
    val networkType: String? = null,     // WIFI, CELLULAR, etc.
    val wifiName: String? = null,
    val wifiMacAddress: String? = null,
    val batteryLevel: Int? = null,       // 0-100
    val geofenceId: Int? = null
) {
    companion object {
        /**
         * Create LocationData from current device state
         */
        fun create(
            latitude: Double,
            longitude: Double,
            accuracy: Float?,
            altitude: Double?,
            speed: Float?,        // in m/s from GPS
            bearing: Float?,
            deviceId: String,
            appVersion: String,
            networkType: String?,
            wifiName: String?,
            wifiMacAddress: String?,
            batteryLevel: Int?,
            geofenceId: Int? = null
        ): LocationData {
            val now = ZonedDateTime.now()
            val timestamp = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            // Convert speed from m/s to km/h (GPS gives m/s, backend expects km/h)
            val speedKmh = speed?.let { it * 3.6f }

            return LocationData(
                recordedAt = timestamp,
                latitude = latitude,
                longitude = longitude,
                locationAccuracy = accuracy,
                altitude = altitude ?: 0.0,
                verticalAccuracy = null,
                speed = speedKmh,
                heading = bearing,
                deviceId = deviceId,
                appVersion = appVersion,
                networkType = networkType,
                wifiName = wifiName,
                wifiMacAddress = wifiMacAddress,
                batteryLevel = batteryLevel,
                geofenceId = geofenceId
            )
        }
    }
}

/**
 * API request body for location sync
 */
data class LocationSyncRequest(
    val locations: List<LocationData>
)

/**
 * API response for location sync
 */
data class LocationSyncResponse(
    val status: String  // "success" or "error"
)