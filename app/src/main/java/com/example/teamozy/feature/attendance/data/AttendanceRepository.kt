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

import com.example.teamozy.core.state.AppEvent
import com.example.teamozy.core.state.AppStateManager

sealed class AttendanceOutcome {
    data class Success(val canCheckIn: Boolean) : AttendanceOutcome()
    data class Violation(val token: String, val message: String) : AttendanceOutcome()
    data class Error(val message: String) : AttendanceOutcome()
}

private enum class LastAction { CHECK_IN, CHECK_OUT }

class AttendanceRepository(context: Context) {

    private val api = NetworkModule.apiService
    private val pm = PreferencesManager.getInstance(context)

    private fun deviceId(): String = pm.deviceId
    private fun token(): String = pm.authToken.orEmpty()

    private var lastAction: LastAction? = null

    // ---------------- Status ----------------
    suspend fun getStatus(): AttendanceOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val res = api.checkStatus(
                deviceId = deviceId(),
                token = token()
            )
            Log.d("NET", "checkStatus -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}")

            when {
                res.isSuccessful -> {
                    val body: CheckStatusEnvelope? = res.body()
                    val state = body?.data?.let { data ->
                        try { data.javaClass.getDeclaredField("currentState").let { f ->
                            f.isAccessible = true
                            (f.get(data) as? String) ?: "CHECK_IN_NEEDED"
                        }} catch (_: Exception) { "CHECK_IN_NEEDED" }
                    } ?: "CHECK_IN_NEEDED"

                    val can = when (state) {
                        "CHECK_IN_NEEDED" -> true
                        "CHECK_OUT_NEEDED" -> false
                        else -> true
                    }
                    AttendanceOutcome.Success(canCheckIn = can)
                }

                res.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    AttendanceOutcome.Error("Unauthorized. Please login again.")
                }

                else -> AttendanceOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            AttendanceOutcome.Error(friendlyNetError(e))
        }
    }

    // ---------------- Check In ----------------
    suspend fun checkIn(
        lat: Double,
        lng: Double,
        @Suppress("UNUSED_PARAMETER") accuracy: Float,
        faceRecognitionQualityScore: Float,
        faceRecognition: Boolean = false,
        faceVerify: Boolean = false
    ): AttendanceOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            lastAction = LastAction.CHECK_IN
            val res = api.checkIn(
                deviceId = deviceId(),
                longitude = lng,
                latitude = lat,
                faceRecognitionQualityScore = faceRecognitionQualityScore,
                faceRecognition = faceRecognition,
                faceVerify = faceVerify,
                token = token()
            )

            Log.d("NET", "checkIn -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}")
            Log.d("NET", "checkIn -> face_recognition_quality_score=$faceRecognitionQualityScore, face_recognition=$faceRecognition, face_verify=$faceVerify")

            when {
                res.code() == 307 -> {
                    val parsed = parseActionFromNon2xx(res)
                    AttendanceOutcome.Violation(
                        token = parsed?.tToken ?: "",
                        message = parsed?.message ?: ""
                    )
                }

                res.isSuccessful -> AttendanceOutcome.Success(canCheckIn = false)

                res.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    AttendanceOutcome.Error("Unauthorized. Please login again.")
                }

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
        @Suppress("UNUSED_PARAMETER") accuracy: Float,
        faceRecognitionQualityScore: Float,
        faceRecognition: Boolean = false,
        faceVerify: Boolean = false
    ): AttendanceOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            lastAction = LastAction.CHECK_OUT
            val res = api.checkOut(
                deviceId = deviceId(),
                longitude = lng,
                latitude = lat,
                faceRecognitionQualityScore = faceRecognitionQualityScore,
                faceRecognition = faceRecognition,
                faceVerify = faceVerify,
                token = token()
            )

            Log.d("NET", "checkOut -> code=${res.code()} url=${res.raw().request.url} msg=${res.message()}")
            Log.d("NET", "checkOut -> face_recognition_quality_score=$faceRecognitionQualityScore, face_recognition=$faceRecognition, face_verify=$faceVerify")

            when {
                res.code() == 307 -> {
                    val parsed = parseActionFromNon2xx(res)
                    AttendanceOutcome.Violation(
                        token = parsed?.tToken ?: "",
                        message = parsed?.message ?: ""
                    )
                }

                res.isSuccessful -> AttendanceOutcome.Success(canCheckIn = true)

                res.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    AttendanceOutcome.Error("Unauthorized. Please login again.")
                }

                else -> AttendanceOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            AttendanceOutcome.Error(friendlyNetError(e))
        }
    }

    // ---------------- Violation submit (explicit) ----------------
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
                res.isSuccessful -> AttendanceOutcome.Success(canCheckIn = false)
                res.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    AttendanceOutcome.Error("Unauthorized. Please login again.")
                }
                else -> AttendanceOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            AttendanceOutcome.Error(friendlyNetError(e))
        }
    }

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
                res.isSuccessful -> AttendanceOutcome.Success(canCheckIn = true)
                res.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    AttendanceOutcome.Error("Unauthorized. Please login again.")
                }
                else -> AttendanceOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            AttendanceOutcome.Error(friendlyNetError(e))
        }
    }

    // ---------------- Violation submit (auto-route) ----------------
    suspend fun submitViolation(
        tToken: String,
        lateReason: String? = null,
        earlyReason: String? = null,
        geoReason: String? = null
    ): AttendanceOutcome = withContext(Dispatchers.IO) {
        return@withContext when (lastAction ?: LastAction.CHECK_IN) {
            LastAction.CHECK_IN  -> submitCheckInViolation(tToken, lateReason, geoReason)
            LastAction.CHECK_OUT -> submitCheckOutViolation(tToken, earlyReason, geoReason)
        }
    }

    // ---------------- Helpers ----------------
    private fun parseActionFromNon2xx(res: Response<ActionResponse>): ActionResponse? {
        return try {
            val raw = res.errorBody()?.string().orEmpty()
            if (raw.isBlank()) return null
            val o = JSONObject(raw)
            ActionResponse(
                status = o.optString("status", ""),
                message = o.optString("message", ""),
                isLate = if (o.has("is_late")) o.optBoolean("is_late") else null,
                isEarly = if (o.has("is_early")) o.optBoolean("is_early") else null,
                locationVerified = if (o.has("location_verified")) o.optBoolean("location_verified") else null,
                tToken = o.optString("t_token", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractError(res: Response<*>): String {
        return try {
            val raw = res.errorBody()?.string().orEmpty()
            if (raw.isBlank()) {
                "Request failed with ${res.code()}"
            } else {
                val j = JSONObject(raw)
                (j.optString("message", "")
                    .ifBlank {
                        j.optJSONObject("data")?.optString("message", "").orEmpty()
                    })
                    .ifBlank { "Request failed with ${res.code()}" }
            }
        } catch (_: Exception) {
            "Request failed with ${res.code()}"
        }
    }

    private fun friendlyNetError(e: Throwable): String = when (e) {
        is java.net.UnknownHostException -> "Can't reach server. Check your internet or server URL."
        is java.net.SocketTimeoutException -> "Server timed out. Please try again."
        else -> e.message ?: "Network error, please try again."
    }
}