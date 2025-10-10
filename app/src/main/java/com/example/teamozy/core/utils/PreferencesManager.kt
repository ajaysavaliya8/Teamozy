package com.example.teamozy.core.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var faceThreshold: Float
        get() = prefs.getFloat(KEY_FACE_THRESHOLD, 0.57f)
        set(value) = prefs.edit().putFloat(KEY_FACE_THRESHOLD, value).apply()

    // Alias for backward compatibility
    var faceAccuracyThreshold: Float
        get() = faceThreshold
        set(value) { faceThreshold = value }

    var faceVector: String?
        get() = prefs.getString(KEY_FACE_VECTOR, null)
        set(value) = prefs.edit().putString(KEY_FACE_VECTOR, value).apply()

    fun isLoggedIn(): Boolean = !authToken.isNullOrBlank()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "teamozy_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_FACE_THRESHOLD = "face_threshold"
        private const val KEY_FACE_VECTOR = "face_vector"

        @Volatile private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}