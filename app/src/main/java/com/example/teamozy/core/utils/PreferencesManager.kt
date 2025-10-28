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

    // New fields for face verification requirements
    var requireFaceCheckin: Boolean
        get() = prefs.getBoolean(KEY_REQUIRE_FACE_CHECKIN, false)
        set(value) = prefs.edit().putBoolean(KEY_REQUIRE_FACE_CHECKIN, value).apply()

    var requireFaceBreak: Boolean
        get() = prefs.getBoolean(KEY_REQUIRE_FACE_BREAK, false)
        set(value) = prefs.edit().putBoolean(KEY_REQUIRE_FACE_BREAK, value).apply()

    // ===== NEW PROFILE FIELDS =====
    var mobileNumber: Long
        get() = prefs.getLong(KEY_MOBILE_NUMBER, 0L)
        set(value) = prefs.edit().putLong(KEY_MOBILE_NUMBER, value).apply()

    var fullName: String?
        get() = prefs.getString(KEY_FULL_NAME, null)
        set(value) = prefs.edit().putString(KEY_FULL_NAME, value).apply()

    var profileUrl: String?
        get() = prefs.getString(KEY_PROFILE_URL, null)
        set(value) = prefs.edit().putString(KEY_PROFILE_URL, value).apply()

    var branchName: String?
        get() = prefs.getString(KEY_BRANCH_NAME, null)
        set(value) = prefs.edit().putString(KEY_BRANCH_NAME, value).apply()

    var departmentName: String?
        get() = prefs.getString(KEY_DEPARTMENT_NAME, null)
        set(value) = prefs.edit().putString(KEY_DEPARTMENT_NAME, value).apply()

    var shiftName: String?
        get() = prefs.getString(KEY_SHIFT_NAME, null)
        set(value) = prefs.edit().putString(KEY_SHIFT_NAME, value).apply()

    // Social Media
    var facebook: String?
        get() = prefs.getString(KEY_FACEBOOK, null)
        set(value) = prefs.edit().putString(KEY_FACEBOOK, value).apply()

    var linkedin: String?
        get() = prefs.getString(KEY_LINKEDIN, null)
        set(value) = prefs.edit().putString(KEY_LINKEDIN, value).apply()

    var x: String?
        get() = prefs.getString(KEY_X, null)
        set(value) = prefs.edit().putString(KEY_X, value).apply()

    var instagram: String?
        get() = prefs.getString(KEY_INSTAGRAM, null)
        set(value) = prefs.edit().putString(KEY_INSTAGRAM, value).apply()

    var snapchat: String?
        get() = prefs.getString(KEY_SNAPCHAT, null)
        set(value) = prefs.edit().putString(KEY_SNAPCHAT, value).apply()

    // Company Information
    var companyName: String?
        get() = prefs.getString(KEY_COMPANY_NAME, null)
        set(value) = prefs.edit().putString(KEY_COMPANY_NAME, value).apply()

    var companyAddress: String?
        get() = prefs.getString(KEY_COMPANY_ADDRESS, null)
        set(value) = prefs.edit().putString(KEY_COMPANY_ADDRESS, value).apply()

    var companyEmail: String?
        get() = prefs.getString(KEY_COMPANY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_COMPANY_EMAIL, value).apply()

    var companyContact: String?
        get() = prefs.getString(KEY_COMPANY_CONTACT, null)
        set(value) = prefs.edit().putString(KEY_COMPANY_CONTACT, value).apply()

    var companyWebsite: String?
        get() = prefs.getString(KEY_COMPANY_WEBSITE, null)
        set(value) = prefs.edit().putString(KEY_COMPANY_WEBSITE, value).apply()

    var companyLogoUrl: String?
        get() = prefs.getString(KEY_COMPANY_LOGO_URL, null)
        set(value) = prefs.edit().putString(KEY_COMPANY_LOGO_URL, value).apply()

    // Support Information
    var hrEmail: String?
        get() = prefs.getString(KEY_HR_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_HR_EMAIL, value).apply()

    var technicalSupportNumber: String?
        get() = prefs.getString(KEY_TECH_SUPPORT_NUMBER, null)
        set(value) = prefs.edit().putString(KEY_TECH_SUPPORT_NUMBER, value).apply()

    var technicalSupportEmail: String?
        get() = prefs.getString(KEY_TECH_SUPPORT_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_TECH_SUPPORT_EMAIL, value).apply()

    // ===== PERMISSION SCREEN FLAG =====
    var hasShownPermissions: Boolean
        get() = prefs.getBoolean(KEY_HAS_SHOWN_PERMISSIONS, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SHOWN_PERMISSIONS, value).apply()

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
        private const val KEY_REQUIRE_FACE_CHECKIN = "require_face_checkin"
        private const val KEY_REQUIRE_FACE_BREAK = "require_face_break"

        // New keys for profile data
        private const val KEY_MOBILE_NUMBER = "mobile_number"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_PROFILE_URL = "profile_url"
        private const val KEY_BRANCH_NAME = "branch_name"
        private const val KEY_DEPARTMENT_NAME = "department_name"
        private const val KEY_SHIFT_NAME = "shift_name"
        private const val KEY_FACEBOOK = "facebook"
        private const val KEY_LINKEDIN = "linkedin"
        private const val KEY_X = "x"
        private const val KEY_INSTAGRAM = "instagram"
        private const val KEY_SNAPCHAT = "snapchat"
        private const val KEY_COMPANY_NAME = "company_name"
        private const val KEY_COMPANY_ADDRESS = "company_address"
        private const val KEY_COMPANY_EMAIL = "company_email"
        private const val KEY_COMPANY_CONTACT = "company_contact"
        private const val KEY_COMPANY_WEBSITE = "company_website"
        private const val KEY_COMPANY_LOGO_URL = "company_logo_url"
        private const val KEY_HR_EMAIL = "hr_email"
        private const val KEY_TECH_SUPPORT_NUMBER = "tech_support_number"
        private const val KEY_TECH_SUPPORT_EMAIL = "tech_support_email"

        // Permission screen flag
        private const val KEY_HAS_SHOWN_PERMISSIONS = "has_shown_permissions"

        @Volatile private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}