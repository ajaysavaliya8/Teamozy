package com.hrms.jeejateamozy.feature.auth.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.hrms.jeejateamozy.core.network.ApiService
import com.hrms.jeejateamozy.core.network.BasicResponse
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

// ============================================
// AUTH OUTCOME SEALED CLASS
// ============================================

/**
 * Sealed class representing authentication operation outcomes
 */
sealed class AuthOutcome {
    data class Success(val message: String) : AuthOutcome()
    data class Error(val message: String) : AuthOutcome()
    data class DeviceNotRegistered(val message: String) : AuthOutcome()
}

// ============================================
// AUTH REPOSITORY
// ============================================

class AuthRepository(
    private val api: ApiService,
    private val pm: PreferencesManager,
    private val ctx: Context
) {

    private fun androidId(): String {
        return Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    /**
     * Get device manufacturer (e.g., Samsung, Google, Xiaomi)
     */
    private fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER.replaceFirstChar { it.uppercaseChar() }
    }

    /**
     * Get device model (e.g., Galaxy S21, Pixel 6)
     */
    private fun getDeviceModel(): String {
        return Build.MODEL
    }

    /**
     * Get Android OS version (e.g., Android 13, Android 11)
     */
    private fun getDeviceOSVersion(): String {
        return "Android ${Build.VERSION.RELEASE}"
    }

    /**
     * Extract error message from response
     */
    private fun extractMessage(res: Response<*>): String {
        return try {
            val errBody = res.errorBody()?.string()
            if (errBody.isNullOrBlank()) {
                res.message()
            } else {
                val gson = Gson()
                val map = gson.fromJson(errBody, Map::class.java)
                map["message"]?.toString() ?: res.message()
            }
        } catch (e: Exception) {
            res.message()
        }
    }

    /**
     * Send login code (OTP) to user's mobile number
     */
    suspend fun sendLoginCode(phone: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()
            Log.d("AUTH", "sendLoginCode - deviceId: $deviceId, phone: $phone")

            val res = api.sendLogin(mobileNumber = phone.toLong(), deviceId = deviceId)
            Log.d("NET", "sendLogin -> code=${res.code()}")

            toOutcome(res, requireToken = false)
        } catch (e: Exception) {
            Log.e("AUTH", "sendLoginCode error", e)
            AuthOutcome.Error(e.message ?: "Failed to send login code")
        }
    }

    // Alias for backward compatibility
    suspend fun sendOtp(phone: String): AuthOutcome = sendLoginCode(phone)

    /**
     * Login with password - includes FCM token
     */
    suspend fun loginWithPassword(phone: String, password: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()
            val appVersion = com.hrms.jeejateamozy.BuildConfig.VERSION_NAME

            // Get FCM token from preferences
            val fcmToken = pm.fcmToken

            // Get device information
            val deviceManufacturer = getDeviceManufacturer()
            val deviceModel = getDeviceModel()
            val deviceOsVersion = getDeviceOSVersion()

            Log.d("AUTH", "loginWithPassword - deviceId: $deviceId, phone: $phone")
            Log.d("AUTH", "  app_version: $appVersion")
            Log.d("AUTH", "  fcm_token: ${fcmToken?.take(30) ?: "null"}...")
            Log.d("AUTH", "  device: $deviceManufacturer $deviceModel")
            Log.d("AUTH", "  os_version: $deviceOsVersion")

            val res = api.verifyLogin(
                mobileNumber = phone.toLong(),
                deviceId = deviceId,
                password = password,
                otp = null,
                appVersion = appVersion,
                fcmToken = fcmToken,
                deviceManufacturer = deviceManufacturer,
                deviceModel = deviceModel,
                deviceOsVersion = deviceOsVersion
            )
            Log.d("NET", "verifyLogin(password) -> code=${res.code()}")

            when (res.code()) {
                409 -> {
                    val msg = extractMessage(res)
                    AuthOutcome.DeviceNotRegistered(msg)
                }
                else -> toOutcome(res, requireToken = true)
            }
        } catch (e: Exception) {
            Log.e("AUTH", "loginWithPassword error", e)
            AuthOutcome.Error(e.message ?: "Password login failed")
        }
    }

    /**
     * Login with OTP - includes FCM token
     */
    suspend fun loginWithOtp(phone: String, otp: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()
            val appVersion = com.hrms.jeejateamozy.BuildConfig.VERSION_NAME

            // Get FCM token from preferences
            val fcmToken = pm.fcmToken

            // Get device information
            val deviceManufacturer = getDeviceManufacturer()
            val deviceModel = getDeviceModel()
            val deviceOsVersion = getDeviceOSVersion()

            Log.d("AUTH", "loginWithOtp - deviceId: $deviceId, phone: $phone")
            Log.d("AUTH", "  app_version: $appVersion")
            Log.d("AUTH", "  fcm_token: ${fcmToken?.take(30) ?: "null"}...")
            Log.d("AUTH", "  device: $deviceManufacturer $deviceModel")
            Log.d("AUTH", "  os_version: $deviceOsVersion")

            val res = api.verifyLogin(
                mobileNumber = phone.toLong(),
                deviceId = deviceId,
                password = null,
                otp = otp,
                appVersion = appVersion,
                fcmToken = fcmToken,
                deviceManufacturer = deviceManufacturer,
                deviceModel = deviceModel,
                deviceOsVersion = deviceOsVersion
            )
            Log.d("NET", "verifyLogin(otp) -> code=${res.code()}")

            when (res.code()) {
                409 -> {
                    val msg = extractMessage(res)
                    AuthOutcome.DeviceNotRegistered(msg)
                }
                else -> toOutcome(res, requireToken = true)
            }
        } catch (e: Exception) {
            Log.e("AUTH", "loginWithOtp error", e)
            AuthOutcome.Error(e.message ?: "OTP verification failed")
        }
    }

    /**
     * Verify JWT token validity
     */
    suspend fun verifyToken(): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = pm.authToken ?: return@withContext AuthOutcome.Error("No token found")

            // Use the app version from BuildConfig
            val appVersion = com.hrms.jeejateamozy.BuildConfig.VERSION_NAME

            val res = api.verifyToken(appVersion)
            Log.d("NET", "verifyToken -> app_version=$appVersion, code=${res.code()}")

            // Handle VerifyTokenResponse directly (not BasicResponse)
            if (res.isSuccessful && res.code() == 200) {
                val body = res.body()
                if (body?.status == "success") {
                    AuthOutcome.Success(body.message ?: "Token verified")
                } else {
                    AuthOutcome.Error(body?.message ?: "Token verification failed")
                }
            } else {
                // Extract error message from response
                val errorMessage = extractMessage(res)
                AuthOutcome.Error(errorMessage)
            }
        } catch (e: Exception) {
            AuthOutcome.Error(e.message ?: "Token verification failed")
        }
    }

    /**
     * Send OTP for device change
     */
    suspend fun sendChangeDeviceOtp(phone: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("AUTH", "sendChangeDeviceOtp - phone: $phone")

            val res = api.sendChangeDeviceOtp(mobileNumber = phone.toLong())
            Log.d("NET", "sendChangeDeviceOtp -> code=${res.code()}")

            toOutcome(res, requireToken = false)
        } catch (e: Exception) {
            Log.e("AUTH", "sendChangeDeviceOtp error", e)
            AuthOutcome.Error(e.message ?: "Failed to send change device OTP")
        }
    }

    /**
     * Request device change
     * NOTE: This endpoint returns DeviceChangeResponse (with "detail" field), not BasicResponse
     */
    suspend fun requestChangeDevice(
        phone: String,
        otp: String
    ): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val newDeviceId = androidId()
            val newDeviceOs = getDeviceOSVersion()
            val newDeviceModel = getDeviceModel()
            val newDeviceCompanyName = getDeviceManufacturer()

            Log.d("AUTH", "requestChangeDevice")
            Log.d("AUTH", "  phone: $phone")
            Log.d("AUTH", "  newDeviceId: $newDeviceId")
            Log.d("AUTH", "  newDeviceOs: $newDeviceOs")
            Log.d("AUTH", "  newDeviceModel: $newDeviceModel")
            Log.d("AUTH", "  newDeviceCompanyName: $newDeviceCompanyName")

            val res = api.requestChangeDevice(
                otp = otp,
                mobileNumber = phone.toLong(),
                newDeviceId = newDeviceId,
                newDeviceOs = newDeviceOs,
                newDeviceModel = newDeviceModel,
                newDeviceCompanyName = newDeviceCompanyName,
                reason = null
            )
            Log.d("NET", "requestChangeDevice -> code=${res.code()}")

            // Handle DeviceChangeResponse (has "detail" field, not "message")
            if (res.isSuccessful && res.code() == 200) {
                val body = res.body()
                val msg = body?.detail ?: "Device change request submitted successfully"
                Log.d("AUTH", "✅ Device change request successful: $msg")
                AuthOutcome.Success(msg)
            } else {
                val errorMessage = try {
                    val errBody = res.errorBody()?.string()
                    if (errBody.isNullOrBlank()) {
                        res.message()
                    } else {
                        val gson = Gson()
                        val map = gson.fromJson(errBody, Map::class.java)
                        // Try "detail" field first (DeviceChangeResponse), then "message"
                        map["detail"]?.toString() ?: map["message"]?.toString() ?: res.message()
                    }
                } catch (e: Exception) {
                    res.message()
                }
                Log.e("AUTH", "❌ Device change request failed: $errorMessage")
                AuthOutcome.Error(errorMessage)
            }
        } catch (e: Exception) {
            Log.e("AUTH", "requestChangeDevice error", e)
            AuthOutcome.Error(e.message ?: "Failed to submit device change request")
        }
    }

    /**
     * Logout user and optionally clear push notification tokens
     * @param clearPushToken If true, clears FCM tokens from backend
     */
    suspend fun logout(clearPushToken: Boolean = true): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()

            Log.d("AUTH", "🚪 Logging out - deviceId: $deviceId, clearPushToken: $clearPushToken")

            val res = api.logout(
                deviceId = deviceId,
                clearPushToken = clearPushToken
            )

            Log.d("NET", "logout -> code=${res.code()}")

            if (res.isSuccessful) {
                val body = res.body()
                val msg = body?.message ?: "Logged out successfully"

                // Clear local preferences
                pm.clearAll()
                Log.d("AUTH", "✅ Local preferences cleared")

                AuthOutcome.Success(msg)
            } else {
                AuthOutcome.Error(extractMessage(res))
            }
        } catch (e: Exception) {
            Log.e("AUTH", "logout error", e)
            // Even if API call fails, clear local preferences
            pm.clearAll()
            AuthOutcome.Error(e.message ?: "Logout failed")
        }
    }

    /**
     * Convert API response to AuthOutcome
     */
    private fun toOutcome(res: Response<BasicResponse>, requireToken: Boolean): AuthOutcome {
        return if (res.isSuccessful && res.code() == 200) {
            val body = res.body()
            val msg = body?.message ?: "Success"

            if (requireToken && body?.token.isNullOrBlank()) {
                return AuthOutcome.Error("Token missing in response")
            }

            // Save authentication data during login
            if (requireToken && !body?.token.isNullOrBlank()) {
                val deviceId = androidId()

                // Save token
                pm.authToken = body?.token
                Log.d("AUTH", "✅ Token saved")

                // Save device ID
                pm.deviceId = deviceId
                Log.d("AUTH", "✅ Device ID saved: $deviceId")

                // Save profile and company info if available
                body.profile_url?.let { pm.profileUrl = it }
                body.full_name?.let { pm.fullName = it }
                body.company_name?.let { pm.companyName = it }
                body.company_logo_url?.let { pm.companyLogoUrl = it }

                Log.d("AUTH", "✅ Login successful - all data saved")
            }

            AuthOutcome.Success(msg)
        } else {
            AuthOutcome.Error(extractMessage(res))
        }
    }
}