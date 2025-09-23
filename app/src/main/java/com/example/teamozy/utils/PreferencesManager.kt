package com.example.teamozy.utils

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

class PreferencesManager private constructor(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "teamozy_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_ORGANIZATION_NAME = "organization_name"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var authToken: String?
        get() = sharedPreferences.getString(KEY_AUTH_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var deviceId: String
        get() = sharedPreferences.getString(KEY_DEVICE_ID, null)
            ?: generateDeviceId().also { deviceId = it }
        set(value) = sharedPreferences.edit().putString(KEY_DEVICE_ID, value).apply()

    var userId: String?
        get() = sharedPreferences.getString(KEY_USER_ID, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_ID, value).apply()

    var userName: String?
        get() = sharedPreferences.getString(KEY_USER_NAME, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_NAME, value).apply()

    var organizationName: String?
        get() = sharedPreferences.getString(KEY_ORGANIZATION_NAME, null)
        set(value) = sharedPreferences.edit().putString(KEY_ORGANIZATION_NAME, value).apply()

    private fun generateDeviceId(): String {
        // Generate a unique device ID based on Android ID
        return try {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown_device_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            "unknown_device_${System.currentTimeMillis()}"
        }
    }

    fun isLoggedIn(): Boolean {
        return !authToken.isNullOrEmpty()
    }

    fun logout() {
        sharedPreferences.edit().clear().apply()
    }

    fun saveLoginData(token: String, userId: String?, userName: String?) {
        sharedPreferences.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            userId?.let { putString(KEY_USER_ID, it) }
            userName?.let { putString(KEY_USER_NAME, it) }
            apply()
        }
    }
}