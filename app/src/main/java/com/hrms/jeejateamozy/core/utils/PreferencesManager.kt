package com.hrms.jeejateamozy.core.utils

import android.content.Context
import android.content.SharedPreferences


class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ============================================
    // AUTHENTICATION
    // ============================================

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

    // ============================================
    // USER PROFILE INFORMATION
    // ============================================

    var mobileNumber: Long
        get() = prefs.getLong(KEY_MOBILE_NUMBER, 0L)
        set(value) = prefs.edit().putLong(KEY_MOBILE_NUMBER, value).apply()

    var fullName: String?
        get() = prefs.getString(KEY_FULL_NAME, null)
        set(value) = prefs.edit().putString(KEY_FULL_NAME, value).apply()

    var profileUrl: String?
        get() = prefs.getString(KEY_PROFILE_URL, null)
        set(value) = prefs.edit().putString(KEY_PROFILE_URL, value).apply()

    var gender: String?
        get() = prefs.getString(KEY_GENDER, null)
        set(value) = prefs.edit().putString(KEY_GENDER, value).apply()

    var branchName: String?
        get() = prefs.getString(KEY_BRANCH_NAME, null)
        set(value) = prefs.edit().putString(KEY_BRANCH_NAME, value).apply()

    var departmentName: String?
        get() = prefs.getString(KEY_DEPARTMENT_NAME, null)
        set(value) = prefs.edit().putString(KEY_DEPARTMENT_NAME, value).apply()

    var shiftName: String?
        get() = prefs.getString(KEY_SHIFT_NAME, null)
        set(value) = prefs.edit().putString(KEY_SHIFT_NAME, value).apply()

    // ============================================
    // SOCIAL MEDIA LINKS
    // ============================================

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

    // ============================================
    // COMPANY CODE (from find-company)
    // ============================================

    var companyCode: String
        get() = prefs.getString(KEY_COMPANY_CODE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_COMPANY_CODE, value).apply()

    // ============================================
    // COMPANY INFORMATION
    // ============================================

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

    // ============================================
    // SUPPORT INFORMATION
    // ============================================

    var hrEmail: String?
        get() = prefs.getString(KEY_HR_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_HR_EMAIL, value).apply()

    var technicalSupportNumber: String?
        get() = prefs.getString(KEY_TECH_SUPPORT_NUMBER, null)
        set(value) = prefs.edit().putString(KEY_TECH_SUPPORT_NUMBER, value).apply()

    var technicalSupportEmail: String?
        get() = prefs.getString(KEY_TECH_SUPPORT_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_TECH_SUPPORT_EMAIL, value).apply()

    // ============================================
    // FIREBASE CLOUD MESSAGING (NEW)
    // ============================================

    /**
     * Firebase Cloud Messaging (FCM) Token
     * This token is used to send push notifications to this specific device.
     *
     * The token is:
     * - Generated automatically by Firebase when the app launches
     * - Saved in PreferencesManager by TeamozyFirebaseMessagingService
     * - Sent to backend during login (verify-login endpoint)
     * - Refreshed automatically by Firebase when needed
     * - Cleared on logout
     */
    var fcmToken: String?
        get() = prefs.getString(KEY_FCM_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_FCM_TOKEN, value).apply()

    // ============================================
    // UI STATE
    // ============================================

    var hasShownPermissions: Boolean
        get() = prefs.getBoolean(KEY_HAS_SHOWN_PERMISSIONS, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SHOWN_PERMISSIONS, value).apply()

    /**
     * Track if background permission dialog has been shown
     * For aggressive OEMs (Xiaomi, Huawei, Oppo, Vivo) that need
     * extra autostart/background permissions for reliable location tracking
     */
    var hasShownBackgroundPermissionDialog: Boolean
        get() = prefs.getBoolean(KEY_HAS_SHOWN_BG_PERMISSION, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SHOWN_BG_PERMISSION, value).apply()

    // ============================================
    // POST-SHIFT CHECK
    // ============================================

    var postShiftDataJson: String?
        get() = prefs.getString(KEY_POST_SHIFT_DATA_JSON, null)
        set(value) = prefs.edit().putString(KEY_POST_SHIFT_DATA_JSON, value).apply()

    fun clearPostShiftData() {
        prefs.edit().remove(KEY_POST_SHIFT_DATA_JSON).apply()
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Check if user is currently logged in
     * @return true if auth token exists and is not blank
     */
    fun isLoggedIn(): Boolean = !authToken.isNullOrBlank()

    /**
     * Clear all stored preferences
     * Use this on logout to remove all user data
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ============================================
    // COMPANION OBJECT (Singleton Pattern)
    // ============================================

    companion object {
        private const val PREF_NAME = "teamozy_prefs"

        // Authentication Keys
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"

        // User Profile Keys
        private const val KEY_MOBILE_NUMBER = "mobile_number"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_PROFILE_URL = "profile_url"
        private const val KEY_GENDER = "gender"
        private const val KEY_BRANCH_NAME = "branch_name"
        private const val KEY_DEPARTMENT_NAME = "department_name"
        private const val KEY_SHIFT_NAME = "shift_name"

        // Social Media Keys
        private const val KEY_FACEBOOK = "facebook"
        private const val KEY_LINKEDIN = "linkedin"
        private const val KEY_X = "x"
        private const val KEY_INSTAGRAM = "instagram"
        private const val KEY_SNAPCHAT = "snapchat"

        // Company Code Key
        private const val KEY_COMPANY_CODE = "company_code"

        // Company Information Keys
        private const val KEY_COMPANY_NAME = "company_name"
        private const val KEY_COMPANY_ADDRESS = "company_address"
        private const val KEY_COMPANY_EMAIL = "company_email"
        private const val KEY_COMPANY_CONTACT = "company_contact"
        private const val KEY_COMPANY_WEBSITE = "company_website"
        private const val KEY_COMPANY_LOGO_URL = "company_logo_url"

        // Support Information Keys
        private const val KEY_HR_EMAIL = "hr_email"
        private const val KEY_TECH_SUPPORT_NUMBER = "tech_support_number"
        private const val KEY_TECH_SUPPORT_EMAIL = "tech_support_email"

        // Firebase Cloud Messaging Key
        private const val KEY_FCM_TOKEN = "fcm_token"

        // UI State Keys
        private const val KEY_HAS_SHOWN_PERMISSIONS = "has_shown_permissions"
        private const val KEY_HAS_SHOWN_BG_PERMISSION = "has_shown_bg_permission"

        // Post-Shift Check Key
        private const val KEY_POST_SHIFT_DATA_JSON = "post_shift_data_json"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        /**
         * Get singleton instance of PreferencesManager
         * Thread-safe using double-checked locking
         *
         * @param context Application context
         * @return PreferencesManager singleton instance
         */
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}