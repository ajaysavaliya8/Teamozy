package com.hrms.jeejateamozy.feature.location.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hrms.jeejateamozy.feature.location.data.LocationData

/**
 * Database entity for persisting location queue
 * Survives app kills, service restarts, and device reboots
 */
@Entity(tableName = "pending_locations")
data class PendingLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Location data
    val recordedAt: String,
    val latitude: Double,
    val longitude: Double,
    val locationAccuracy: Float?,
    val altitude: Double?,
    val verticalAccuracy: Float?,
    val speed: Float?,
    val heading: Float?,

    // Device metadata
    val deviceId: String,
    val appVersion: String,
    val networkType: String?,
    val wifiName: String?,
    val wifiMacAddress: String?,
    val batteryLevel: Int?,
    val geofenceId: Int?,

    // Queue management metadata
    val createdAt: Long = System.currentTimeMillis(),
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long? = null
) {
    /**
     * Convert to LocationData for API sync
     */
    fun toLocationData(): LocationData {
        return LocationData(
            recordedAt = recordedAt,
            latitude = latitude,
            longitude = longitude,
            locationAccuracy = locationAccuracy,
            altitude = altitude,
            verticalAccuracy = verticalAccuracy,
            speed = speed,
            heading = heading,
            deviceId = deviceId,
            appVersion = appVersion,
            networkType = networkType,
            wifiName = wifiName,
            wifiMacAddress = wifiMacAddress,
            batteryLevel = batteryLevel,
            geofenceId = geofenceId
        )
    }

    companion object {
        /**
         * Create entity from LocationData
         */
        fun fromLocationData(location: LocationData): PendingLocationEntity {
            return PendingLocationEntity(
                recordedAt = location.recordedAt,
                latitude = location.latitude,
                longitude = location.longitude,
                locationAccuracy = location.locationAccuracy,
                altitude = location.altitude,
                verticalAccuracy = location.verticalAccuracy,
                speed = location.speed,
                heading = location.heading,
                deviceId = location.deviceId,
                appVersion = location.appVersion,
                networkType = location.networkType,
                wifiName = location.wifiName,
                wifiMacAddress = location.wifiMacAddress,
                batteryLevel = location.batteryLevel,
                geofenceId = location.geofenceId
            )
        }
    }
}