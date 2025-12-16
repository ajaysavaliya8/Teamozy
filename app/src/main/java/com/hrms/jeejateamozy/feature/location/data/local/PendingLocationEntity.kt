package com.hrms.jeejateamozy.feature.location.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing pending location data
 * Locations are stored here immediately after capture and removed after successful sync
 */
@Entity(tableName = "pending_locations")
data class PendingLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Required fields
    val recordedAt: String,          // ISO 8601 timestamp (UTC)
    val latitude: Double,
    val longitude: Double,

    // Optional location fields
    val locationAccuracy: Float? = null,
    val altitude: Double? = null,     // Always 0 as per API spec
    val verticalAccuracy: Float? = null,
    val speed: Float? = null,
    val heading: Float? = null,

    // Device info
    val deviceId: String,
    val appVersion: String,

    // Network info
    val networkType: String? = null,  // WIFI, MOBILE, OFFLINE
    val wifiName: String? = null,
    val wifiMacAddress: String? = null,

    // Other
    val batteryLevel: Int? = null,
    val geofenceId: Int? = null,

    // Local tracking
    val createdAt: Long = System.currentTimeMillis()
)