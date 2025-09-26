package com.example.teamozy.feature.attendance.data

import android.content.Context
import android.util.Log
import com.example.teamozy.core.network.ActionResponse
import com.example.teamozy.core.network.BasicResponse
import com.example.teamozy.core.network.CheckStatusEnvelope
import com.example.teamozy.core.network.NetworkModule
import com.example.teamozy.core.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

sealed class AttendanceOutcome {
    /** canCheckIn = true  -> show "Check In"  button
     *  canCheckIn = false -> show "Check Out" button */
    data class Success(val canCheckIn: Boolean) : AttendanceOutcome()
    /** 307 case returning t_token + message for UI to collect reasons */
    data class Violation(val token: String, val message: String) : AttendanceOutcome()
    data class Error(val message: String) : AttendanceOutcome()
}

private enum class LastAction { CHECK_IN, CHECK_OUT }

class AttendanceRepository(context: Context) {

    private val api = NetworkModule.apiService
    private val pm = PreferencesManager.getInstance(context)

    private fun deviceId(): String = pm.deviceId
    private fun token(): String = pm.authToken.orEmpty()

    // Track which action produced the last 307 so submitViolation can auto-route
    private var lastAction: LastAction? = null

    // ---------------- Status ----------------
    suspend fun getStatus(): AttendanceOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val res = api.checkStatus(
                deviceId = deviceId(),
                token = token()
            )
            Log.d("NET", "checkStatus -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}")
            if (res.isSuccessful) {
                val body: CheckStatusEnvelope? = res.body()
                val state = body?.data?.currentState
                val can = when (state) {
                    "CHECK_IN_NEEDED" -> true
                    "CHECK_OUT_NEEDED" -> false
                    else -> true
                }
                AttendanceOutcome.Success(canCheckIn = can)
            } else if (res.code() == 401) {
                AttendanceOutcome.Error("Unauthorized. Please login again.")
            } else {
                AttendanceOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            AttendanceOutcome.Error(friendlyNetError(e))
        }
    }

    // ---------------- Check In ----------------
    suspend fun checkIn(
        lat: Double,
        lng: Double,
        accuracy: Float,
        faceVerify: Boolean = false      // default keeps older callers safe
    ): AttendanceOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                lastAction = LastAction.CHECK_IN
                val res = api.checkIn(
                    deviceId = deviceId(),
                    longitude = lng,
                    latitude = lat,
                    faceVerify = faceVerify,   // <— CHANGED
                    token = token()
                )
                Log.d(
                    "NET",
                    "checkIn -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}"
                )
                when {
                    res.code() == 307 -> {
                        val parsed = parseActionFromNon2xx(res)
                        AttendanceOutcome.Violation(
                            token = parsed?.tToken.orEmpty(),
                            message = parsed?.message.orEmpty()
                        )
                    }

                    res.isSuccessful -> AttendanceOutcome.Success(canCheckIn = false)
                    res.code() == 401 -> AttendanceOutcome.Error("Unauthorized. Please login again.")
                    else -> AttendanceOutcome.Error(extractError(res))
                }
            } catch (e: Exception) {
                AttendanceOutcome.Error(friendlyNetError(e))
            }
        }
    // ---------------- Check Out ----------------
    suspend fun checkOut(
        lat: Double,
        lng: Double,
        accuracy: Float,
        faceVerify: Boolean = false      // default keeps older callers safe
    ): AttendanceOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                lastAction = LastAction.CHECK_OUT
                val res = api.checkOut(
                    deviceId = deviceId(),
                    longitude = lng,
                    latitude = lat,
                    faceVerify = faceVerify,   // <— CHANGED
                    token = token()
                )
                Log.d("NET", "checkOut -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}")
                when {
                    res.code() == 307 -> {
                        val parsed = parseActionFromNon2xx(res)
                        AttendanceOutcome.Violation(
                            token = parsed?.tToken.orEmpty(),
                            message = parsed?.message.orEmpty()
                        )
                    }
                    res.isSuccessful -> AttendanceOutcome.Success(canCheckIn = true)
                    res.code() == 401 -> AttendanceOutcome.Error("Unauthorized. Please login again.")
                    else -> AttendanceOutcome.Error(extractError(res))
                }
            } catch (e: Exception) {
                AttendanceOutcome.Error(friendlyNetError(e))
            }
        }


    // ---------------- Violation submit (explicit) ----------------
    /** For CHECK-IN violation: send both reasons as your backend accepts (late_reason, geo_reason) */
    suspend fun submitCheckInViolation(
        tToken: String,
        lateReason: String? = null,
        geoReason: String? = null
    ): AttendanceOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val res = api.submitCheckInViolation(
                tToken = tToken,
                lateReason = lateReason,
                geoReason = geoReason,
                token = token()
            )
            Log.d("NET", "submitCheckInViolation -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}")
            when {
                res.isSuccessful -> AttendanceOutcome.Success(canCheckIn = false) // now user is checked in
                res.code() == 401 -> AttendanceOutcome.Error("Unauthorized. Please login again.")
                else -> AttendanceOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            AttendanceOutcome.Error(friendlyNetError(e))
        }
    }

    /** For CHECK-OUT violation: backend accepts early_reason + geo_reason (same pattern) */
    suspend fun submitCheckOutViolation(
        tToken: String,
        earlyReason: String? = null,
        geoReason: String? = null
    ): AttendanceOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val res = api.submitCheckOutViolation(
                tToken = tToken,
                earlyReason = earlyReason,
                geoReason = geoReason,
                token = token()
            )
            Log.d("NET", "submitCheckOutViolation -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}")
            when {
                res.isSuccessful -> AttendanceOutcome.Success(canCheckIn = true) // day complete
                res.code() == 401 -> AttendanceOutcome.Error("Unauthorized. Please login again.")
                else -> AttendanceOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            AttendanceOutcome.Error(friendlyNetError(e))
        }
    }

    // ---------------- Violation submit (auto-route) ----------------
    /**
     * Convenience:
     *  - If last action was CHECK-IN → calls /check-in-violation with lateReason & geoReason
     *  - If last action was CHECK-OUT → calls /check-out-violation with earlyReason & geoReason
     */
    suspend fun submitViolation(
        tToken: String,
        lateReason: String? = null,  // for check-in flow
        earlyReason: String? = null, // for check-out flow
        geoReason: String? = null
    ): AttendanceOutcome = withContext(Dispatchers.IO) {
        return@withContext when (lastAction ?: LastAction.CHECK_IN) {
            LastAction.CHECK_IN  -> submitCheckInViolation(tToken, lateReason, geoReason)
            LastAction.CHECK_OUT -> submitCheckOutViolation(tToken, earlyReason, geoReason)
        }
    }

    // ---------------- Helpers ----------------

    /** Parse JSON from non-2xx (e.g., 307) response to extract message/t_token/etc. */
    private fun parseActionFromNon2xx(res: Response<ActionResponse>): ActionResponse? {
        return try {
            val raw = res.errorBody()?.string().orEmpty()
            if (raw.isBlank()) return null
            val o = JSONObject(raw)
            ActionResponse(
                status = o.optString("status"),
                message = o.optString("message"),
                isLate = if (o.has("is_late")) o.optBoolean("is_late") else null,
                isEarly = if (o.has("is_early")) o.optBoolean("is_early") else null,
                locationVerified = if (o.has("location_verified")) o.optBoolean("location_verified") else null,
                tToken = o.optString("t_token")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractError(res: Response<*>): String {
        return try {
            val raw = res.errorBody()?.string().orEmpty()
            if (raw.isBlank()) "Request failed with ${res.code()}" else {
                if (raw.trim().startsWith("{")) {
                    val obj = JSONObject(raw)
                    obj.optString("message")
                        .ifBlank { obj.optString("error") }
                        .ifBlank { obj.optString("detail") }
                        .ifBlank { "Request failed with ${res.code()}" }
                } else raw
            }
        } catch (_: Exception) {
            "Request failed with ${res.code()}"
        }
    }

    private fun friendlyNetError(e: Throwable): String = when (e) {
        is java.net.UnknownHostException -> "Can’t reach server. Check your internet or server URL."
        is java.net.SocketTimeoutException -> "Server timed out. Please try again."
        else -> e.message ?: "Network error, please try again."
    }
}
