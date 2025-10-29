package com.hrms.jeejateamozy.core.utils

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceInfoHelper {

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""
    }

    fun getDeviceModel(): String {
        return Build.MODEL ?: "Unknown"
    }

    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER ?: "Unknown"
    }

    fun getDeviceOS(): String {
        return "Android ${Build.VERSION.RELEASE}"
    }

    fun getDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            deviceId = getDeviceId(context),
            model = getDeviceModel(),
            manufacturer = getDeviceManufacturer(),
            os = getDeviceOS()
        )
    }
}

data class DeviceInfo(
    val deviceId: String,
    val model: String,
    val manufacturer: String,
    val os: String
)