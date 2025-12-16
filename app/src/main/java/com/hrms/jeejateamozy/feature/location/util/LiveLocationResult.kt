package com.hrms.jeejateamozy.feature.location.util

import com.hrms.jeejateamozy.feature.location.model.LocationData

/**
 * Result class for live location capture
 */
sealed class LiveLocationResult {
    data class Success(val locationData: LocationData) : LiveLocationResult()
    data class Error(val message: String) : LiveLocationResult()
}