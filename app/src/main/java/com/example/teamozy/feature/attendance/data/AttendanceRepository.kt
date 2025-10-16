package com.example.teamozy.feature.attendance.data

import android.content.Context
import android.util.Log
import com.example.teamozy.core.network.NetworkModule
import com.example.teamozy.core.utils.PreferencesManager
import com.example.teamozy.core.state.AppStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class AttendanceOutcome {
    data class Success(
        val currentState: String,
        val faceRecognitionEnabled: Boolean,
        val faceVector: String?,
        val minimumQualityScore: Float,
        val message: String,
        val attendanceStatus: String?,
        val isComplete: Boolean?
    ) : AttendanceOutcome()

    data class Error(val message: String) : AttendanceOutcome()
}

class AttendanceRepository(context: Context) {

    private val api = NetworkModule.apiService
    private val pm = PreferencesManager.getInstance(context)

    private fun deviceId(): String = pm.deviceId
    private fun token(): String = pm.authToken.orEmpty()

    /**
     * Get current attendance status
     * Returns the current state (CHECK_IN_NEEDED, CHECK_OUT_NEEDED, COMPLETED)
     * and face recognition requirements
     */
    suspend fun getStatus(): AttendanceOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val res = api.checkStatus(
                deviceId = deviceId(),
                token = token()
            )

            Log.d("NET", "checkStatus -> code=${res.code()} url=${res.raw().request.url}")

            when {
                res.isSuccessful && res.code() == 200 -> {
                    val body = res.body()
                    val data = body?.data

                    if (data != null) {
                        Log.d("NET", "Status: ${data.current_state}, Message: ${data.message}")
                        Log.d("NET", "Face Recognition: ${data.face_recognition_enabled}, Quality: ${data.minimum_quality_score}")

                        // Save face recognition settings to preferences if provided
                        if (data.face_recognition_enabled && data.face_vector != null) {
                            pm.faceThreshold = data.minimum_quality_score
                            Log.d("NET", "Saved face threshold: ${data.minimum_quality_score}")
                        }

                        AttendanceOutcome.Success(
                            currentState = data.current_state,
                            faceRecognitionEnabled = data.face_recognition_enabled,
                            faceVector = data.face_vector,
                            minimumQualityScore = data.minimum_quality_score,
                            message = data.message,
                            attendanceStatus = data.attendance_status,
                            isComplete = data.is_complete
                        )
                    } else {
                        AttendanceOutcome.Error("Invalid response from server")
                    }
                }

                res.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    AttendanceOutcome.Error("Unauthorized. Please login again.")
                }

                res.code() == 403 -> {
                    val errorMsg = extractError(res)
                    AttendanceOutcome.Error(errorMsg)
                }

                res.code() == 404 -> {
                    val errorMsg = extractError(res)
                    AttendanceOutcome.Error(errorMsg)
                }

                else -> {
                    AttendanceOutcome.Error(extractError(res))
                }
            }
        } catch (e: Exception) {
            Log.e("NET", "Exception in checkStatus", e)
            AttendanceOutcome.Error(friendlyNetError(e))
        }
    }

    private fun extractError(res: retrofit2.Response<*>): String {
        return try {
            val raw = res.errorBody()?.string().orEmpty()
            if (raw.isBlank()) {
                "Request failed with ${res.code()}"
            } else {
                val j = JSONObject(raw)
                j.optString("message", "").ifBlank {
                    "Request failed with ${res.code()}"
                }
            }
        } catch (_: Exception) {
            "Request failed with ${res.code()}"
        }
    }

    private fun friendlyNetError(e: Throwable): String = when (e) {
        is java.net.UnknownHostException -> "Can't reach server. Check your internet connection."
        is java.net.SocketTimeoutException -> "Server timed out. Please try again."
        else -> e.message ?: "Network error, please try again."
    }
}