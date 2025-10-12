package com.example.teamozy.feature.auth.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.teamozy.core.network.BasicResponse
import com.example.teamozy.core.network.NetworkModule
import com.example.teamozy.core.utils.DeviceInfoHelper
import com.example.teamozy.core.utils.PreferencesManager
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

    private fun androidId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

    suspend fun sendOtp(phone: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val res = api.sendLogin(phone.toLong(), androidId())
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
            val res = api.verifyLogin(
                mobileNumber = phone.toLong(),
                deviceId = androidId(),
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
            AuthOutcome.Error(e.message ?: "Login failed")
        }
    }

    suspend fun loginWithOtp(phone: String, otp: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val res = api.verifyLogin(
                mobileNumber = phone.toLong(),
                deviceId = androidId(),
                password = null,
                otp = otp
            )
            Log.d("NET", "verifyLogin(otp) -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}")

            when (res.code()) {
                409 -> {
                    val msg = extractMessage(res)
                    AuthOutcome.DeviceNotRegistered(msg)
                }
                else -> toOutcome(res, requireToken = true)
            }
        } catch (e: Exception) {
            AuthOutcome.Error(e.message ?: "Login failed")
        }
    }

    suspend fun sendChangeDeviceOtp(mobileNumber: String): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val res = api.sendChangeDeviceOtp(mobileNumber.toLong())
            Log.d("NET", "sendChangeDeviceOtp -> code=${res.code()} url=${res.raw().request.url}")

            when {
                res.isSuccessful -> {
                    val body = res.body()
                    val msg = body?.message ?: "OTP sent successfully"
                    AuthOutcome.Success(msg)
                }
                else -> {
                    val msg = extractMessage(res)
                    AuthOutcome.Error(msg.ifBlank { "Failed to send OTP" })
                }
            }
        } catch (e: Exception) {
            Log.e("NET", "sendChangeDeviceOtp error", e)
            AuthOutcome.Error(e.message ?: "Failed to send OTP")
        }
    }

    suspend fun verifyToken(): AuthOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = pm.authToken.orEmpty()
            if (token.isBlank()) {
                return@withContext AuthOutcome.Error("No token found")
            }

            val res = api.verifyToken(token)
            Log.d("NET", "verifyToken -> code=${res.code()}")

            when {
                res.isSuccessful -> {
                    val body = res.body()

                    // Update face threshold and vector using new field names
                    body?.minimum_face_recognition_quality_score?.let {
                        pm.faceThreshold = it
                        Log.d("AUTH", "Updated face threshold: $it")
                    }

                    body?.face_vector?.let {
                        pm.faceVector = it
                        Log.d("AUTH", "Updated face vector: ${it.take(50)}...")
                    }

                    // Update face verification requirements
                    body?.require_face_checkin?.let {
                        pm.requireFaceCheckin = it
                        Log.d("AUTH", "Updated require_face_checkin: $it")
                    }

                    body?.require_face_break?.let {
                        pm.requireFaceBreak = it
                        Log.d("AUTH", "Updated require_face_break: $it")
                    }

                    Log.d("AUTH", "Token verified - threshold: ${body?.minimum_face_recognition_quality_score}, has_vector: ${body?.face_vector != null}, require_face_checkin: ${body?.require_face_checkin}, require_face_break: ${body?.require_face_break}")

                    AuthOutcome.Success(body?.message ?: "Token is valid")
                }
                res.code() == 401 -> {
                    AuthOutcome.Error("Token expired or invalid")
                }
                else -> {
                    val msg = extractMessage(res)
                    AuthOutcome.Error(msg.ifBlank { "Token verification failed" })
                }
            }
        } catch (e: Exception) {
            Log.e("NET", "verifyToken error", e)
            AuthOutcome.Error(e.message ?: "Failed to verify token")
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

    private fun toOutcome(res: Response<BasicResponse>, requireToken: Boolean): AuthOutcome {
        if (res.isSuccessful) {
            val body = res.body()
            if (body?.status == "success") {
                if (requireToken) {
                    val token = body.token.orEmpty()
                    if (token.isBlank()) return AuthOutcome.Error("Missing token")

                    // Persist token and device ID
                    pm.authToken = token
                    pm.deviceId = androidId()

                    // Save face threshold and vector using new field names
                    body.minimum_face_recognition_quality_score?.let {
                        pm.faceThreshold = it
                        Log.d("AUTH", "Saved face threshold: $it")
                    } ?: run {
                        // Set default threshold if not provided
                        pm.faceThreshold = 0.57f
                        Log.d("AUTH", "Using default face threshold: 0.57")
                    }

                    body.face_vector?.let {
                        pm.faceVector = it
                        Log.d("AUTH", "Saved face vector: ${it.take(50)}...")
                    } ?: run {
                        Log.d("AUTH", "No face vector provided")
                    }

                    // Save face verification requirements
                    body.require_face_checkin?.let {
                        pm.requireFaceCheckin = it
                        Log.d("AUTH", "Saved require_face_checkin: $it")
                    } ?: run {
                        pm.requireFaceCheckin = false
                        Log.d("AUTH", "Using default require_face_checkin: false")
                    }

                    body.require_face_break?.let {
                        pm.requireFaceBreak = it
                        Log.d("AUTH", "Saved require_face_break: $it")
                    } ?: run {
                        pm.requireFaceBreak = false
                        Log.d("AUTH", "Using default require_face_break: false")
                    }

                    Log.d("AUTH", "Login successful - threshold: ${pm.faceThreshold}, has_vector: ${body.face_vector != null}, require_face_checkin: ${pm.requireFaceCheckin}, require_face_break: ${pm.requireFaceBreak}")

                    return AuthOutcome.Success(body.message ?: "Login successful.", token)
                }
                return AuthOutcome.Success(body.message ?: "OK")
            }
            return AuthOutcome.Error(body?.message ?: "Unknown error")
        }
        val msg = extractMessage(res)
        return AuthOutcome.Error(msg.ifBlank { "Request failed with ${res.code()}" })
    }

    private fun extractMessage(res: Response<*>): String {
        return try {
            val raw = res.errorBody()?.string().orEmpty()
            if (raw.startsWith("{")) {
                val o = JSONObject(raw)
                o.optString("message").ifBlank {
                    o.optString("error").ifBlank {
                        o.optString("detail")
                    }
                }
            } else raw
        } catch (_: Exception) { "" }
    }

    private fun extractDetailMessage(res: Response<*>): String {
        return try {
            val raw = res.errorBody()?.string().orEmpty()
            if (raw.startsWith("{")) {
                val o = JSONObject(raw)
                o.optString("detail").ifBlank {
                    o.optString("message").ifBlank {
                        o.optString("error")
                    }
                }
            } else raw
        } catch (_: Exception) { "" }
    }
}