package com.hrms.jeejateamozy.feature.location.model

/**
 * Location data model for API sync and local storage
 */
data class LocationData(
    val recordedAt: String,
    val latitude: Double,
    val longitude: Double,
    val locationAccuracy: Float? = null,
    val altitude: Double? = null,
    val verticalAccuracy: Float? = null,
    val speed: Float? = null,
    val heading: Float? = null,
    val deviceId: String,
    val appVersion: String,
    val networkType: String? = null,
    val wifiName: String? = null,
    val wifiMacAddress: String? = null,
    val batteryLevel: Int? = null,
    val geofenceId: String? = null
)