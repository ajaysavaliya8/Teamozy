package com.hrms.jeejateamozy.feature.location.data.remote

import com.google.gson.annotations.SerializedName
import com.hrms.jeejateamozy.feature.location.data.local.PendingLocationEntity

/**
 * DTO for location sync API request
 * Uses snake_case to match backend API expectations
 */
data class LocationSyncRequest(
    @SerializedName("recorded_at")
    val recordedAt: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("location_accuracy")
    val locationAccuracy: Float? = null,

    @SerializedName("altitude")
    val altitude: Double? = null,

    @SerializedName("vertical_accuracy")
    val verticalAccuracy: Float? = null,

    @SerializedName("speed")
    val speed: Float? = null,

    @SerializedName("heading")
    val heading: Float? = null,

    @SerializedName("device_id")
    val deviceId: String? = null,

    @SerializedName("app_version")
    val appVersion: String? = null,

    @SerializedName("network_type")
    val networkType: String? = null,

    @SerializedName("wifi_name")
    val wifiName: String? = null,

    @SerializedName("wifi_mac_address")
    val wifiMacAddress: String? = null,

    @SerializedName("battery_level")
    val batteryLevel: Int? = null,

    @SerializedName("geofence_id")
    val geofenceId: Int? = null
) {
    companion object {
        /**
         * Convert from Room entity to API DTO
         */
        fun fromEntity(entity: PendingLocationEntity): LocationSyncRequest {
            return LocationSyncRequest(
                recordedAt = entity.recordedAt,
                latitude = entity.latitude,
                longitude = entity.longitude,
                locationAccuracy = entity.locationAccuracy,
                altitude = entity.altitude ?: 0.0,  // Default to 0 as per API spec
                verticalAccuracy = entity.verticalAccuracy,
                speed = entity.speed,
                heading = entity.heading,
                deviceId = entity.deviceId,
                appVersion = entity.appVersion,
                networkType = entity.networkType,
                wifiName = entity.wifiName,
                wifiMacAddress = entity.wifiMacAddress,
                batteryLevel = entity.batteryLevel,
                geofenceId = entity.geofenceId
            )
        }
    }
}