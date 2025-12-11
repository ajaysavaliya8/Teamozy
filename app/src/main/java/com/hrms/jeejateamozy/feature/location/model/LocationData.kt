package com.hrms.jeejateamozy.feature.location.model

/**
 * Location data model for API sync
 */
data class LocationData(
    val recordedAt: String,
    val latitude: Double,
    val longitude: Double,
    val locationAccuracy: Float?,
    val altitude: Double?,
    val verticalAccuracy: Float?,
    val speed: Float?,
    val heading: Float?,
    val deviceId: String,
    val appVersion: String,
    val networkType: String?,
    val wifiName: String?,
    val wifiMacAddress: String?,
    val batteryLevel: Int?,
    val geofenceId: String?
)