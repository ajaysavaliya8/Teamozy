package com.hrms.jeejateamozy.feature.location.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hrms.jeejateamozy.feature.location.model.LocationData

@Entity(tableName = "pending_locations")
data class PendingLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float?,
    val provider: String?,
    val deviceId: String?,

    val createdAt: Long = System.currentTimeMillis(),
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long = 0
) {
    companion object {
        fun fromLocationData(data: LocationData): PendingLocationEntity {
            return PendingLocationEntity(
                latitude = data.latitude,
                longitude = data.longitude,
                timestamp = data.timestamp,
                accuracy = data.accuracy,
                provider = data.provider,
                deviceId = data.deviceId
            )
        }
    }

    fun toLocationData(): LocationData {
        return LocationData(
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            accuracy = accuracy,
            provider = provider,
            deviceId = deviceId
        )
    }
}