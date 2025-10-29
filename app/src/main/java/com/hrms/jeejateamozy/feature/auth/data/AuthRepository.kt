package com.hrms.jeejateamozy.feature.auth.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.hrms.jeejateamozy.core.network.BasicResponse
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.DeviceInfoHelper
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

sealed class AuthOutcome {
    data class Success(val message: String, val token: String? = null) : AuthOutcome()
    data class Error(val message: String) : AuthOutcome()
    data class DeviceNotRegistered(val message: String) : AuthOutcome()
}

class AuthRepository(private val context: Context) {

    private val api = NetworkModule.apiService
    private val pm = PreferencesManager.getInstance(context)

    private fun androidId(): String {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        Log.d("AUTH", "androidId() = $id")
        return id
    }

    suspend fun sendOtp(phone: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()
            Log.d("AUTH", "sendOtp - deviceId: $deviceId, phone: $phone")

            val res = api.sendLogin(phone.toLong(), deviceId)
            Log.d("NET", "sendLogin -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}")

            when (res.code()) {
                409 -> {
                    val msg = extractMessage(res)
                    AuthOutcome.DeviceNotRegistered(msg)
                }
                else -> toOutcome(res, requireToken = false)
            }
        } catch (e: Exception) {
            AuthOutcome.Error(e.message ?: "Failed to send OTP")
        }
    }

    suspend fun loginWithPassword(phone: String, password: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()
            Log.d("AUTH", "loginWithPassword - deviceId: $deviceId, phone: $phone")

            val res = api.verifyLogin(
                mobileNumber = phone.toLong(),
                deviceId = deviceId,
                password = password,
                otp = null
            )
            Log.d("NET", "verifyLogin(pwd) -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}")

            when (res.code()) {
                409 -> {
                    val msg = extractMessage(res)
                    AuthOutcome.DeviceNotRegistered(msg)
                }
                else -> toOutcome(res, requireToken = true)
            }
        } catch (e: Exception) {
            AuthOutcome.Error(e.message ?: "Password login failed")
        }
    }

    suspend fun loginWithOtp(phone: String, otp: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val deviceId = androidId()
            Log.d("AUTH", "loginWithOtp - deviceId: $deviceId, phone: $phone")

            val res = api.verifyLogin(
                mobileNumber = phone.toLong(),
                deviceId = deviceId,
                password = null,
                otp = otp
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
            AuthOutcome.Error(e.message ?: "OTP verification failed")
        }
    }

    suspend fun verifyToken(): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = pm.authToken ?: return@withContext AuthOutcome.Error("No token found")
            val res = api.verifyToken(token)
            Log.d("NET", "verifyToken -> code=${res.code()}")

            // Handle VerifyTokenResponse separately
            if (res.isSuccessful && res.code() == 200) {
                val body = res.body()
                if (body?.status == "success") {
                    AuthOutcome.Success(body.message ?: "Token verified")
                } else {
                    AuthOutcome.Error(body?.message ?: "Token verification failed")
                }
            } else {
                AuthOutcome.Error(extractMessage(res))
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

    // ===== UPDATED: Properly save device_id during login =====
    private fun toOutcome(res: Response<BasicResponse>, requireToken: Boolean): AuthOutcome {
        return if (res.isSuccessful && res.code() == 200) {
            val body = res.body()
            val msg = body?.message ?: "Success"

            if (requireToken && body?.token.isNullOrBlank()) {
                return AuthOutcome.Error("Token missing in response")
            }

            // ===== CRITICAL FIX: Save device_id IMMEDIATELY during login =====
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
                } else {
                    Log.d("AUTH", "✅ Device ID successfully saved and verified")
                }
            }

            // ===== SAVE ALL NEW PROFILE DATA =====
            body?.let { data ->
                // Basic info
                data.mobile_number?.let { pm.mobileNumber = it }
                data.full_name?.let {
                    pm.fullName = it
                    pm.userName = it  // Keep backward compatibility
                }
                data.profile_url?.let { pm.profileUrl = it }

                // Work info
                data.branch_name?.let { pm.branchName = it }
                data.department_name?.let { pm.departmentName = it }
                data.shift_name?.let { pm.shiftName = it }

                // ===== FIXED: Social media - explicitly handle null values =====
                // This ensures that when API returns null, it properly clears the stored values
                pm.facebook = data.facebook
                pm.linkedin = data.linkedin
                pm.x = data.x
                pm.instagram = data.instagram
                pm.snapchat = data.snapchat

                // Company info
                data.company_name?.let { pm.companyName = it }
                data.company_address?.let { pm.companyAddress = it }
                data.company_email?.let { pm.companyEmail = it }
                data.company_contact?.let { pm.companyContact = it }
                data.company_website?.let { pm.companyWebsite = it }
                data.company_logo_url?.let { pm.companyLogoUrl = it }

                // Support info
                data.hr_email?.let { pm.hrEmail = it }
                data.technical_support_number?.let { pm.technicalSupportNumber = it }
                data.technical_support_email?.let { pm.technicalSupportEmail = it }

                Log.d("AUTH", "✅ All profile data saved")
                Log.d("AUTH", "   Full Name: ${pm.fullName}")
                Log.d("AUTH", "   Branch: ${pm.branchName}")
                Log.d("AUTH", "   Department: ${pm.departmentName}")
                Log.d("AUTH", "   Shift: ${pm.shiftName}")
                Log.d("AUTH", "   Company: ${pm.companyName}")
                Log.d("AUTH", "   Social Media - Facebook: ${pm.facebook ?: "null"}")
                Log.d("AUTH", "   Social Media - LinkedIn: ${pm.linkedin ?: "null"}")
                Log.d("AUTH", "   Social Media - X: ${pm.x ?: "null"}")
                Log.d("AUTH", "   Social Media - Instagram: ${pm.instagram ?: "null"}")
                Log.d("AUTH", "   Social Media - Snapchat: ${pm.snapchat ?: "null"}")
            }

            AuthOutcome.Success(msg, body?.token)
        } else {
            AuthOutcome.Error(extractMessage(res))
        }
    }

    private fun extractMessage(res: Response<*>): String {
        return try {
            val errBody = res.errorBody()?.string()
            if (!errBody.isNullOrBlank()) {
                val json = JSONObject(errBody)
                json.optString("message", "Request failed")
            } else {
                "Request failed: ${res.message()}"
            }
        } catch (e: Exception) {
            "Request failed: ${res.message()}"
        }
    }

    private fun extractDetailMessage(res: Response<*>): String {
        return try {
            val errBody = res.errorBody()?.string()
            if (!errBody.isNullOrBlank()) {
                val json = JSONObject(errBody)
                json.optString("detail", "Request failed")
            } else {
                "Request failed: ${res.message()}"
            }
        } catch (e: Exception) {
            "Request failed: ${res.message()}"
        }
    }
}
