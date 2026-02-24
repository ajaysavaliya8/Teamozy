package com.hrms.jeejateamozy.feature.auth.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.hrms.jeejateamozy.core.network.ApiService
import com.hrms.jeejateamozy.core.network.AuthResponse
import com.hrms.jeejateamozy.core.network.LoginResponse
import com.hrms.jeejateamozy.core.network.ResetOtpResponse
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.core.utils.NetworkErrorHandler
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Response

// ============================================
// AUTH OUTCOME SEALED CLASS
// ============================================

sealed class AuthOutcome {
    data class Success(val message: String) : AuthOutcome()
    data class Error(val message: String) : AuthOutcome()
    data class DeviceNotRegistered(val message: String) : AuthOutcome()
    data class UpdateRequired(val message: String) : AuthOutcome()
}

sealed class ResetOutcome {
    data class Success(val message: String) : ResetOutcome()
    data class Error(val message: String) : ResetOutcome()
    data class TokenReceived(val resetToken: String, val message: String) : ResetOutcome()
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

    private fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER.replaceFirstChar { it.uppercaseChar() }
    }

    private fun getDeviceModel(): String {
        return Build.MODEL
    }

    private fun getDeviceOSVersion(): String {
        return "Android ${Build.VERSION.RELEASE}"
    }

    private suspend fun getFcmToken(): String? {
        var token = pm.fcmToken

        if (token.isNullOrBlank()) {
            Log.d("AUTH", "FCM token not in preferences, fetching from Firebase...")
            try {
                token = FirebaseMessaging.getInstance().token.await()
                if (!token.isNullOrBlank()) {
                    pm.fcmToken = token
                    Log.d("AUTH", "FCM token fetched and saved: ${token.take(30)}...")
                }
            } catch (e: Exception) {
                Log.e("AUTH", "Failed to get FCM token from Firebase", e)
                token = null
            }
        }

        return token
    }

    private fun extractMessage(res: Response<*>): String {
        return NetworkErrorHandler.extractErrorMessage(res, res.message())
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

            when (res.code()) {
                409 -> {
                    val msg = extractMessage(res)
                    Log.d("AUTH", "Device not registered: $msg")
                    AuthOutcome.DeviceNotRegistered(msg)
                }
                426 -> {
                    val msg = extractMessage(res)
                    Log.d("AUTH", "Update required: $msg")
                    AuthOutcome.UpdateRequired(msg)
                }
                else -> toAuthOutcome(res)
            }
        } catch (e: Exception) {
            Log.e("AUTH", "sendLoginCode error", e)
            AuthOutcome.Error(e.message ?: "Failed to send login code")
        }
    }

    suspend fun sendOtp(phone: String): AuthOutcome = sendLoginCode(phone)

    /**
     * Login with password
     */
    suspend fun loginWithPassword(phone: String, password: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()
            val appVersion = com.hrms.jeejateamozy.BuildConfig.VERSION_NAME
            val fcmToken = getFcmToken()
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
                426 -> {
                    val msg = extractMessage(res)
                    AuthOutcome.UpdateRequired(msg)
                }
                else -> toLoginOutcome(res)
            }
        } catch (e: Exception) {
            Log.e("AUTH", "loginWithPassword error", e)
            AuthOutcome.Error(e.message ?: "Password login failed")
        }
    }

    /**
     * Login with OTP
     */
    suspend fun loginWithOtp(phone: String, otp: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()
            val appVersion = com.hrms.jeejateamozy.BuildConfig.VERSION_NAME
            val fcmToken = getFcmToken()
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
                426 -> {
                    val msg = extractMessage(res)
                    AuthOutcome.UpdateRequired(msg)
                }
                else -> toLoginOutcome(res)
            }
        } catch (e: Exception) {
            Log.e("AUTH", "loginWithOtp error", e)
            AuthOutcome.Error(e.message ?: "OTP verification failed")
        }
    }

    /**
     * Verify JWT token validity.
     * Backend returns fresh token + updated user data — save it all.
     */
    suspend fun verifyToken(): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = pm.authToken ?: return@withContext AuthOutcome.Error("No token found")

            val appVersion = com.hrms.jeejateamozy.BuildConfig.VERSION_NAME
            val fcmToken = pm.fcmToken

            val res = api.verifyToken(appVersion, fcmToken)
            Log.d("NET", "verifyToken -> app_version=$appVersion, fcm_token=${fcmToken?.take(20)}..., code=${res.code()}")

            when (res.code()) {
                426 -> {
                    val msg = extractMessage(res)
                    Log.d("AUTH", "Update required during token verification: $msg")
                    AuthOutcome.UpdateRequired(msg)
                }
                else -> {
                    if (res.isSuccessful && res.code() == 200) {
                        val body = res.body()
                        if (body?.success == true) {
                            // Save fresh token + updated user data
                            body.data?.let { data ->
                                saveUserData(data)
                                Log.d("AUTH", "Token verified - user data refreshed")
                            }
                            AuthOutcome.Success(body.message ?: "Token verified")
                        } else {
                            AuthOutcome.Error(body?.message ?: "Token verification failed")
                        }
                    } else {
                        val errorMessage = extractMessage(res)
                        AuthOutcome.Error(errorMessage)
                    }
                }
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

            toAuthOutcome(res)
        } catch (e: Exception) {
            Log.e("AUTH", "sendChangeDeviceOtp error", e)
            AuthOutcome.Error(e.message ?: "Failed to send change device OTP")
        }
    }

    /**
     * Request device change
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

            if (res.isSuccessful && res.code() == 200) {
                val body = res.body()
                val msg = body?.detail ?: "Device change request submitted successfully"
                Log.d("AUTH", "Device change request successful: $msg")
                AuthOutcome.Success(msg)
            } else {
                val errorMessage = try {
                    val errBody = res.errorBody()?.string()
                    if (errBody.isNullOrBlank()) {
                        res.message()
                    } else {
                        val gson = Gson()
                        val map = gson.fromJson(errBody, Map::class.java)
                        map["detail"]?.toString() ?: map["message"]?.toString() ?: res.message()
                    }
                } catch (e: Exception) {
                    res.message()
                }
                Log.e("AUTH", "Device change request failed: $errorMessage")
                AuthOutcome.Error(errorMessage)
            }
        } catch (e: Exception) {
            Log.e("AUTH", "requestChangeDevice error", e)
            AuthOutcome.Error(e.message ?: "Failed to submit device change request")
        }
    }

    /**
     * Logout user and optionally clear push notification tokens
     */
    suspend fun logout(clearPushToken: Boolean = true): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()

            Log.d("AUTH", "Logging out - deviceId: $deviceId, clearPushToken: $clearPushToken")

            val res = api.logout(
                deviceId = deviceId,
                clearPushToken = clearPushToken
            )

            Log.d("NET", "logout -> code=${res.code()}")

            if (res.isSuccessful) {
                val body = res.body()
                val msg = body?.message ?: "Logged out successfully"

                pm.clearAll()
                Log.d("AUTH", "Local preferences cleared")

                AuthOutcome.Success(msg)
            } else {
                AuthOutcome.Error(extractMessage(res))
            }
        } catch (e: Exception) {
            Log.e("AUTH", "logout error", e)
            pm.clearAll()
            AuthOutcome.Error(e.message ?: "Logout failed")
        }
    }

    /**
     * Send forgot password OTP
     */
    suspend fun forgotPassword(phone: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("AUTH", "forgotPassword - phone: $phone")
            val res = api.forgotPassword(number = phone.toLong())
            Log.d("NET", "forgotPassword -> code=${res.code()}")
            toAuthOutcome(res)
        } catch (e: Exception) {
            Log.e("AUTH", "forgotPassword error", e)
            AuthOutcome.Error(e.message ?: "Failed to send reset OTP")
        }
    }

    /**
     * Verify reset OTP and receive reset token
     */
    suspend fun verifyResetOtp(phone: String, otp: String): ResetOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("AUTH", "verifyResetOtp - phone: $phone")
            val res = api.verifyResetOtp(number = phone.toLong(), otp = otp)
            Log.d("NET", "verifyResetOtp -> code=${res.code()}")
            toResetOtpOutcome(res)
        } catch (e: Exception) {
            Log.e("AUTH", "verifyResetOtp error", e)
            ResetOutcome.Error(e.message ?: "OTP verification failed")
        }
    }

    /**
     * Reset password using reset token
     */
    suspend fun resetPassword(
        resetToken: String,
        newPassword: String,
        confirmPassword: String
    ): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("AUTH", "resetPassword")
            val res = api.resetPassword(
                resetToken = resetToken,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )
            Log.d("NET", "resetPassword -> code=${res.code()}")
            toAuthOutcome(res)
        } catch (e: Exception) {
            Log.e("AUTH", "resetPassword error", e)
            AuthOutcome.Error(e.message ?: "Password reset failed")
        }
    }

    /**
     * Convert AuthResponse (send-login, send-change-device-otp) to AuthOutcome
     */
    private fun toAuthOutcome(res: Response<AuthResponse>): AuthOutcome {
        return if (res.isSuccessful && res.code() == 200) {
            val body = res.body()
            if (body?.success == true) {
                AuthOutcome.Success(body.message ?: "Success")
            } else {
                AuthOutcome.Error(body?.message ?: "Request failed")
            }
        } else {
            AuthOutcome.Error(extractMessage(res))
        }
    }

    /**
     * Convert ResetOtpResponse to ResetOutcome
     */
    private fun toResetOtpOutcome(res: Response<ResetOtpResponse>): ResetOutcome {
        return if (res.isSuccessful && res.code() == 200) {
            val body = res.body()
            if (body?.success == true && body.data != null) {
                ResetOutcome.TokenReceived(
                    resetToken = body.data.reset_token,
                    message = body.message ?: "OTP verified"
                )
            } else {
                ResetOutcome.Error(body?.message ?: "OTP verification failed")
            }
        } else {
            ResetOutcome.Error(extractMessage(res))
        }
    }

    /**
     * Save user data from LoginData to PreferencesManager.
     * Shared between verify-login and verify-token responses.
     */
    private fun saveUserData(data: com.hrms.jeejateamozy.core.network.LoginData) {
        // Save token if present
        data.token?.let { pm.authToken = it }

        // Save employee info
        data.mobile_number?.let { pm.mobileNumber = it }
        data.name?.let { pm.userName = it }
        data.full_name?.let { pm.fullName = it }
        data.profile_url?.let { pm.profileUrl = it }
        data.branch_name?.let { pm.branchName = it }
        data.department_name?.let { pm.departmentName = it }
        data.shift_name?.let { pm.shiftName = it }

        // Save social media
        data.facebook?.let { pm.facebook = it }
        data.linkedin?.let { pm.linkedin = it }
        data.x?.let { pm.x = it }
        data.instagram?.let { pm.instagram = it }
        data.snapchat?.let { pm.snapchat = it }

        // Save company info
        data.company_name?.let { pm.companyName = it }
        data.company_address?.let { pm.companyAddress = it }
        data.company_email?.let { pm.companyEmail = it }
        data.company_contact?.let { pm.companyContact = it }
        data.company_website?.let { pm.companyWebsite = it }
        data.company_logo_url?.let { pm.companyLogoUrl = it }

        // Save support info
        data.hr_email?.let { pm.hrEmail = it }
        data.technical_support_number?.let { pm.technicalSupportNumber = it }
        data.technical_support_email?.let { pm.technicalSupportEmail = it }
    }

    /**
     * Convert LoginResponse (verify-login) to AuthOutcome and save user data
     */
    private fun toLoginOutcome(res: Response<LoginResponse>): AuthOutcome {
        return if (res.isSuccessful && res.code() == 200) {
            val body = res.body()
            val msg = body?.message ?: "Success"

            if (body?.success != true) {
                return AuthOutcome.Error(body?.message ?: "Login failed")
            }

            val data = body.data
            if (data?.token.isNullOrBlank()) {
                return AuthOutcome.Error("Token missing in response")
            }

            // Save device ID
            pm.deviceId = androidId()

            // Save all user data
            saveUserData(data!!)

            Log.d("AUTH", "Login successful - all data saved")

            AuthOutcome.Success(msg)
        } else {
            AuthOutcome.Error(extractMessage(res))
        }
    }
}
