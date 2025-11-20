package com.hrms.jeejateamozy.feature.auth.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.hrms.jeejateamozy.core.network.ApiService
import com.hrms.jeejateamozy.core.network.BasicResponse
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.core.utils.DeviceInfoHelper
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Response

sealed class AuthOutcome {
    data class Success(val message: String) : AuthOutcome()
    data class Error(val message: String) : AuthOutcome()
    data class DeviceNotRegistered(val message: String) : AuthOutcome()
}

class AuthRepository(
    private val context: Context,
    private val api: ApiService
) {
    private val pm = PreferencesManager.getInstance(context)

    @SuppressLint("HardwareIds")
    private fun androidId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"
    }

    private fun extractMessage(res: Response<BasicResponse>): String {
        return try {
            val errBody = res.errorBody()?.string()
            if (errBody.isNullOrBlank()) res.message()
            else {
                val gson = com.google.gson.Gson()
                val obj = gson.fromJson(errBody, BasicResponse::class.java)
                obj.message ?: res.message()
            }
        } catch (e: Exception) {
            res.message()
        }
    }

    private fun extractDetailMessage(res: Response<*>): String {
        return try {
            val errBody = res.errorBody()?.string()
            if (errBody.isNullOrBlank()) res.message()
            else {
                val gson = com.google.gson.Gson()
                val map = gson.fromJson(errBody, Map::class.java)
                map["detail"]?.toString() ?: res.message()
            }
        } catch (e: Exception) {
            res.message()
        }
    }

    suspend fun sendLoginCode(phone: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()
            Log.d("AUTH", "sendLoginCode - deviceId: $deviceId, phone: $phone")

            val res = api.sendLogin(
                mobileNumber = phone.toLong(),
                deviceId = deviceId
            )
            Log.d("NET", "sendLogin -> code=${res.code()}")

            when (res.code()) {
                409 -> {
                    val msg = extractMessage(res)
                    AuthOutcome.DeviceNotRegistered(msg)
                }
                else -> toOutcome(res, requireToken = false)
            }
        } catch (e: Exception) {
            AuthOutcome.Error(e.message ?: "Failed to send login code")
        }
    }

    // Alias for backward compatibility
    suspend fun sendOtp(phone: String): AuthOutcome = sendLoginCode(phone)

    suspend fun loginWithPassword(phone: String, password: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()
            val appVersion = com.hrms.jeejateamozy.BuildConfig.VERSION_NAME

            // Get OneSignal tokens if available (waits up to 15 seconds for real Player ID)
            val onesignalPlayerId = getOneSignalPlayerId()  // Real OneSignal UUID
            val onesignalSubscriptionId = getOneSignalSubscriptionId()  // FCM token (different!)

            Log.d("AUTH", "loginWithPassword - deviceId: $deviceId, phone: $phone")
            Log.d("AUTH", "  app_version: $appVersion")
            Log.d("AUTH", "  onesignal_player_id (UUID): ${onesignalPlayerId?.take(20) ?: "null"}...")
            Log.d("AUTH", "  onesignal_subscription_id (FCM): ${onesignalSubscriptionId?.take(20) ?: "null"}...")

            val res = api.verifyLogin(
                mobileNumber = phone.toLong(),
                deviceId = deviceId,
                password = password,
                otp = null,
                appVersion = appVersion,
                onesignalPlayerId = onesignalPlayerId,  // Send real UUID or null
                onesignalSubscriptionId = onesignalSubscriptionId,  // Send FCM token
                fcmToken = null
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

    suspend fun loginWithOtp(phone: String, otp: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()
            val appVersion = com.hrms.jeejateamozy.BuildConfig.VERSION_NAME

            // Get OneSignal tokens if available (waits up to 15 seconds for real Player ID)
            val onesignalPlayerId = getOneSignalPlayerId()  // Real OneSignal UUID
            val onesignalSubscriptionId = getOneSignalSubscriptionId()  // FCM token (different!)

            Log.d("AUTH", "loginWithOtp - deviceId: $deviceId, phone: $phone")
            Log.d("AUTH", "  app_version: $appVersion")
            Log.d("AUTH", "  onesignal_player_id (UUID): ${onesignalPlayerId?.take(20) ?: "null"}...")
            Log.d("AUTH", "  onesignal_subscription_id (FCM): ${onesignalSubscriptionId?.take(20) ?: "null"}...")

            val res = api.verifyLogin(
                mobileNumber = phone.toLong(),
                deviceId = deviceId,
                password = null,
                otp = otp,
                appVersion = appVersion,
                onesignalPlayerId = onesignalPlayerId,  // Send real UUID or null
                onesignalSubscriptionId = onesignalSubscriptionId,  // Send FCM token
                fcmToken = null
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
                val errorMessage = try {
                    val errBody = res.errorBody()?.string()
                    if (errBody.isNullOrBlank()) {
                        res.message()
                    } else {
                        val gson = com.google.gson.Gson()
                        val map = gson.fromJson(errBody, Map::class.java)
                        map["message"]?.toString() ?: res.message()
                    }
                } catch (e: Exception) {
                    res.message()
                }
                AuthOutcome.Error(errorMessage)
            }
        } catch (e: Exception) {
            AuthOutcome.Error(e.message ?: "Token verification failed")
        }
    }

    suspend fun sendChangeDeviceOtp(mobileNumber: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val res = api.sendChangeDeviceOtp(mobileNumber.toLong())
            Log.d("NET", "sendChangeDeviceOtp -> code=${res.code()}")
            toOutcome(res, requireToken = false)
        } catch (e: Exception) {
            AuthOutcome.Error(e.message ?: "Failed to send OTP")
        }
    }

    suspend fun requestDeviceChange(
        mobileNumber: String,
        otp: String,
        reason: String = ""
    ): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceInfo = DeviceInfoHelper.getDeviceInfo(context)

            val res = api.requestChangeDevice(
                otp = otp,
                mobileNumber = mobileNumber.toLong(),
                newDeviceId = deviceInfo.deviceId,
                newDeviceOs = deviceInfo.os,
                newDeviceModel = deviceInfo.model,
                newDeviceCompanyName = deviceInfo.manufacturer,
                reason = reason
            )

            Log.d("NET", "requestDeviceChange -> code=${res.code()} url=${res.raw().request.url}")

            when {
                res.isSuccessful -> {
                    val body = res.body()
                    val msg = body?.detail ?: "Device change request submitted successfully"
                    AuthOutcome.Success(msg)
                }
                else -> {
                    val msg = extractDetailMessage(res)
                    AuthOutcome.Error(msg)
                }
            }
        } catch (e: Exception) {
            Log.e("NET", "requestDeviceChange error", e)
            AuthOutcome.Error(e.message ?: "Failed to submit device change request")
        }
    }

    /**
     * Get OneSignal Player ID (the actual UUID from OneSignal servers)
     * Waits up to 15 seconds for the real OneSignal ID
     * This is CRITICAL - we need the real OneSignal Player ID, not FCM token!
     */
    private suspend fun getOneSignalPlayerId(): String? {
        return try {
            // Try up to 30 times with 500ms delay = 15 seconds total
            repeat(30) { attempt ->
                // Try to get the REAL OneSignal ID (UUID format)
                val onesignalId = OneSignal.User.onesignalId

                if (!onesignalId.isNullOrBlank()) {
                    // Validate it's actually a UUID format (not FCM token)
                    if (onesignalId.contains("-") && onesignalId.length == 36) {
                        Log.d("AUTH", "✅ OneSignal Player ID (UUID) retrieved: ${onesignalId.take(20)}...")
                        return onesignalId
                    } else {
                        Log.w("AUTH", "⚠️ Got ID but not UUID format: ${onesignalId.take(20)}...")
                    }
                }

                if (attempt < 29) {
                    if (attempt % 5 == 0) {
                        Log.d("AUTH", "⏳ Waiting for OneSignal Player ID... (attempt ${attempt + 1}/30)")
                    }
                    delay(500) // Wait 500ms before retry
                }
            }

            Log.w("AUTH", "⚠️ OneSignal Player ID not available after 15 seconds")
            null
        } catch (e: Exception) {
            Log.e("AUTH", "❌ Failed to get OneSignal Player ID", e)
            null
        }
    }

    /**
     * Get OneSignal Subscription ID (FCM Token)
     * This is the Firebase token, NOT the same as Player ID!
     */
    private suspend fun getOneSignalSubscriptionId(): String? {
        return try {
            val subscriptionId = OneSignal.User.pushSubscription.token
            if (!subscriptionId.isNullOrBlank()) {
                Log.d("AUTH", "✅ OneSignal Subscription Token (FCM) retrieved: ${subscriptionId.take(20)}...")
                return subscriptionId
            }
            Log.w("AUTH", "⚠️ OneSignal Subscription Token not available")
            null
        } catch (e: Exception) {
            Log.e("AUTH", "❌ Failed to get OneSignal Subscription Token", e)
            null
        }
    }

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

                // Verify it was saved
                val savedDeviceId = pm.deviceId
                Log.d("AUTH", "✅ Verification - Device ID from prefs: $savedDeviceId")

                if (savedDeviceId.isBlank()) {
                    Log.e("AUTH", "❌ ERROR: Device ID is BLANK after saving!")
                }

                // Save profile and company info if available
                body.profile_url?.let { pm.profileUrl = it }
                body.full_name?.let { pm.fullName = it }
                body.company_name?.let { pm.companyName = it }
                body.company_logo_url?.let { pm.companyLogoUrl = it }

                // Log push notification status if available
                body.push_notifications?.let { pushStatus ->
                    Log.d("AUTH", "📱 Push Notification Status:")
                    Log.d("AUTH", "  Registered: ${pushStatus.registered}")
                    Log.d("AUTH", "  Enabled: ${pushStatus.enabled}")
                    Log.d("AUTH", "  Player ID: ${pushStatus.onesignal_player_id?.take(20)}...")
                    Log.d("AUTH", "  Has FCM Backup: ${pushStatus.has_fcm_backup}")
                    Log.d("AUTH", "  Failure Count: ${pushStatus.failure_count}")
                }
            }

            AuthOutcome.Success(msg)
        } else {
            AuthOutcome.Error(extractMessage(res))
        }
    }

    /**
     * Logout user and optionally clear push notification tokens
     * @param clearPushToken If true, clears all OneSignal and FCM tokens from backend
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

                // Log logout details
                body?.let {
                    Log.d("AUTH", "✅ Logout successful")
                    Log.d("AUTH", "  Logout time: ${it.logout_time}")
                    Log.d("AUTH", "  Device ID: ${it.device_id}")

                    it.push_notifications?.let { pushInfo ->
                        Log.d("AUTH", "  Push tokens cleared: ${pushInfo.cleared}")
                    }
                }

                // Clear local preferences
                pm.clearAll()
                Log.d("AUTH", "✅ Local preferences cleared")

                AuthOutcome.Success(msg)
            } else {
                // Extract error message from logout response
                val errorMsg = try {
                    res.body()?.message ?: res.message() ?: "Logout failed"
                } catch (e: Exception) {
                    "Logout failed"
                }

                Log.e("AUTH", "❌ Logout failed: $errorMsg")

                // Still clear local data even if API call fails
                pm.clearAll()

                AuthOutcome.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("AUTH", "❌ Logout error", e)

            // Clear local data even on exception
            pm.clearAll()

            AuthOutcome.Error(e.message ?: "Logout failed")
        }
    }
}