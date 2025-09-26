package com.example.teamozy.feature.auth.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.teamozy.core.network.BasicResponse
import com.example.teamozy.core.network.NetworkModule
import com.example.teamozy.core.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

sealed class AuthOutcome {
    data class Success(val message: String, val token: String? = null) : AuthOutcome()
    data class Error(val message: String) : AuthOutcome()
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
            toOutcome(res, requireToken = false)
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
            toOutcome(res, requireToken = true)
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
                otp = otp.toIntOrNull()
            )
            Log.d("NET", "verifyLogin(otp) -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}")
            toOutcome(res, requireToken = true)
        } catch (e: Exception) {
            AuthOutcome.Error(e.message ?: "Login failed")
        }
    }

    private fun toOutcome(res: Response<BasicResponse>, requireToken: Boolean): AuthOutcome {
        if (res.isSuccessful) {
            val body = res.body()
            if (body?.status == "success") {
                if (requireToken) {
                    val token = body.token.orEmpty()
                    if (token.isBlank()) return AuthOutcome.Error("Missing token")
                    // persist
                    pm.authToken = token
                    pm.deviceId = androidId()
                    return AuthOutcome.Success(body.message ?: "Login successful.", token)
                }
                return AuthOutcome.Success(body?.message ?: "OK")
            }
            return AuthOutcome.Error(body?.message ?: "Unknown error")
        }
        val msg = try {
            val raw = res.errorBody()?.string().orEmpty()
            if (raw.startsWith("{")) {
                val o = JSONObject(raw)
                o.optString("message").ifBlank { o.optString("error") }
            } else raw
        } catch (_: Exception) { "" }
        return AuthOutcome.Error(msg.ifBlank { "Request failed with ${res.code()}" })
    }
}
