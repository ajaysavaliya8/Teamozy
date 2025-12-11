package com.hrms.jeejateamozy.feature.location.model

import com.google.gson.annotations.SerializedName

/**
 * Location data model for API
 */
data class LocationData(
    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("accuracy")
    val accuracy: Float? = null,

    @SerializedName("provider")
    val provider: String? = null,

    @SerializedName("device_id")
    val deviceId: String? = null
)