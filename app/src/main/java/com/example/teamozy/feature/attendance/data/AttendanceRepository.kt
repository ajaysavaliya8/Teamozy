package com.example.teamozy.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.teamozy.network.NetworkModule
import org.json.JSONObject
import okhttp3.ResponseBody

class AttendanceRepository(context: Context) {

    private val apiService = NetworkModule.apiService
    private val prefs: SharedPreferences = context.getSharedPreferences("teamozy", Context.MODE_PRIVATE)

    private fun getAuthToken(): String {
        return prefs.getString("token", "") ?: ""
    }

    private fun getDeviceId(): String = prefs.getString("device_id", "default_device") ?: "default_device"

    suspend fun checkStatus(): AttendanceStatusResult {
        return try {
            val response = apiService.checkStatus(
                deviceId = getDeviceId(),
                token = getAuthToken()
            )

            println("DEBUG: CheckStatus response code: ${response.code()}")

            when (response.code()) {
                200 -> {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.status == "success") {
                        val currentState = responseBody.data?.currentState ?: ""
                        println("DEBUG: Current state from API: '$currentState'")

                        when (currentState) {
                            "CHECK_IN_NEEDED" -> {
                                println("DEBUG: User needs to check in")
                                AttendanceStatusResult.Success(canCheckIn = true, buttonText = "Check In")
                            }
                            "CHECK_OUT_NEEDED" -> {
                                println("DEBUG: User needs to check out")
                                AttendanceStatusResult.Success(canCheckIn = false, buttonText = "Check Out")
                            }
                            else -> {
                                println("DEBUG: Unknown state: '$currentState', defaulting to Check In")
                                AttendanceStatusResult.Success(canCheckIn = true, buttonText = "Check In")
                            }
                        }
                    } else {
                        println("DEBUG: Invalid response format for check status")
                        AttendanceStatusResult.Error("Invalid response format")
                    }
                }
                404, 403 -> {
                    // Handle specific error cases from your API
                    val errorMessage = parseErrorMessage(response.errorBody())
                    println("DEBUG: CheckStatus client error (${response.code()}): $errorMessage")
                    AttendanceStatusResult.Error(errorMessage)
                }
                else -> {
                    val errorMessage = parseErrorMessage(response.errorBody())
                    println("DEBUG: CheckStatus error (${response.code()}): $errorMessage")
                    AttendanceStatusResult.Error(errorMessage)
                }
            }
        } catch (e: Exception) {
            println("DEBUG: CheckStatus exception: ${e.message}")
            e.printStackTrace()
            AttendanceStatusResult.Error("Failed to check status: ${e.message}")
        }
    }

    suspend fun checkIn(longitude: Double, latitude: Double): AttendanceResult {
        return try {
            val token = getAuthToken()
            if (token.isEmpty()) {
                return AttendanceResult.Error("No auth token found. Please login first.")
            }

            val response = apiService.checkIn(
                deviceId = getDeviceId(),
                longitude = longitude,
                latitude = latitude,
                faceVerify = true,
                token = token
            )

            println("DEBUG: Response code: ${response.code()}")
            println("DEBUG: Response message: ${response.message()}")

            when (response.code()) {
                200 -> {
                    AttendanceResult.Success("Check-in successful")
                }
                307 -> {
                    // Handle 307 response by reading the error body
                    handleViolationResponse(response.errorBody())
                }
                else -> {
                    // Parse error message from response body
                    val errorMessage = parseErrorMessage(response.errorBody())
                    println("DEBUG: Final error message: $errorMessage")
                    AttendanceResult.Error(errorMessage)
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Exception: ${e.message}")
            e.printStackTrace()
            AttendanceResult.Error("Network error: ${e.message}")
        }
    }

    private fun handleViolationResponse(errorBody: ResponseBody?): AttendanceResult {
        return try {
            val responseBodyString = errorBody?.string() ?: ""
            println("DEBUG: 307 Response body: $responseBodyString")

            if (responseBodyString.isNotEmpty()) {
                val jsonObject = JSONObject(responseBodyString)
                val token = jsonObject.optString("t_token", "")
                val isLate = jsonObject.optBoolean("is_late", false)
                val locationVerified = jsonObject.optBoolean("location_verified", true)
                val message = jsonObject.optString("message", "Violation detected")

                println("DEBUG: Parsed violation - t_token: '$token', isLate: $isLate, locationVerified: $locationVerified, message: '$message'")

                if (token.isNotEmpty()) {
                    AttendanceResult.ViolationRequired(
                        tToken = token,
                        isLate = isLate,
                        isLocationViolation = !locationVerified,
                        message = message
                    )
                } else {
                    println("DEBUG: Empty t_token received")
                    AttendanceResult.Error("Invalid violation response - missing token")
                }
            } else {
                println("DEBUG: Empty response body for 307")
                AttendanceResult.Error("Failed to parse violation response")
            }
        } catch (e: Exception) {
            println("DEBUG: Error parsing 307 response: ${e.message}")
            e.printStackTrace()
            AttendanceResult.Error("Failed to process violation response: ${e.message}")
        }
    }

    private fun parseErrorMessage(errorBody: ResponseBody?): String {
        return try {
            val errorBodyString = errorBody?.string()
            println("DEBUG: Error body: $errorBodyString")
            if (errorBodyString != null) {
                val jsonObject = JSONObject(errorBodyString)
                val status = jsonObject.optString("status")
                val message = jsonObject.optString("message")
                if (status == "reject" && message.isNotEmpty()) {
                    message
                } else {
                    "Request failed"
                }
            } else {
                "Request failed"
            }
        } catch (e: Exception) {
            println("DEBUG: Error parsing error response: ${e.message}")
            "Request failed"
        }
    }

    suspend fun submitCheckInViolation(tToken: String, reason: String): AttendanceResult {
        return try {
            val token = getAuthToken()

            // Print payload details in logcat
            println("=== CHECK-IN VIOLATION PAYLOAD ===")
            println("URL: https://teamozy.com/m/check-in-violation")
            println("Query Parameters:")
            println("  token = $token")
            println("Form Data:")
            println("  t_token = $tToken")
            println("  late_reason = $reason")
            println("  geo_reason = $reason")
            println("================================")

            val response = apiService.submitCheckInViolation(
                token = token,
                tToken = tToken,
                lateReason = reason,
                geoReason = reason
            )

            println("=== CHECK-IN VIOLATION RESPONSE ===")
            println("Response Code: ${response.code()}")
            println("Response Body: ${response.body()}")
            println("Response Error Body: ${response.errorBody()?.string()}")
            println("===================================")

            if (response.isSuccessful) {
                AttendanceResult.Success("Check-in completed")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody())
                AttendanceResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            println("=== CHECK-IN VIOLATION ERROR ===")
            println("Exception: ${e.message}")
            println("Stack trace:")
            e.printStackTrace()
            println("================================")
            AttendanceResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun checkOut(longitude: Double, latitude: Double): AttendanceResult {
        return try {
            val response = apiService.checkOut(
                deviceId = getDeviceId(),
                longitude = longitude,
                latitude = latitude,
                faceVerify = true,
                token = getAuthToken()
            )

            println("DEBUG: CheckOut response code: ${response.code()}")

            when (response.code()) {
                200 -> AttendanceResult.Success("Check-out successful")
                307 -> {
                    // Handle 307 response by reading the error body
                    handleViolationResponse(response.errorBody())
                }
                else -> {
                    val errorMessage = parseErrorMessage(response.errorBody())
                    AttendanceResult.Error(errorMessage)
                }
            }
        } catch (e: Exception) {
            println("DEBUG: CheckOut exception: ${e.message}")
            e.printStackTrace()
            AttendanceResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun submitCheckOutViolation(tToken: String, reason: String): AttendanceResult {
        return try {
            val token = getAuthToken()

            // Print payload details in logcat
            println("=== CHECK-OUT VIOLATION PAYLOAD ===")
            println("URL: https://teamozy.com/m/check-out-violation")
            println("Query Parameters:")
            println("  token = $token")
            println("Form Data:")
            println("  t_token = $tToken")
            println("  early_reason = $reason")
            println("  geo_reason = $reason")
            println("==================================")

            val response = apiService.submitCheckOutViolation(
                token = token,
                tToken = tToken,
                earlyReason = reason,
                geoReason = reason
            )

            println("=== CHECK-OUT VIOLATION RESPONSE ===")
            println("Response Code: ${response.code()}")
            println("Response Body: ${response.body()}")
            println("Response Error Body: ${response.errorBody()?.string()}")
            println("====================================")

            if (response.isSuccessful) {
                AttendanceResult.Success("Check-out completed")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody())
                AttendanceResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            println("=== CHECK-OUT VIOLATION ERROR ===")
            println("Exception: ${e.message}")
            println("Stack trace:")
            e.printStackTrace()
            println("=================================")
            AttendanceResult.Error("Network error: ${e.message}")
        }
    }
}

sealed class AttendanceResult {
    data class Success(val message: String) : AttendanceResult()
    data class ViolationRequired(
        val tToken: String,
        val isLate: Boolean,
        val isLocationViolation: Boolean,
        val message: String
    ) : AttendanceResult()
    data class Error(val message: String) : AttendanceResult()
}

sealed class AttendanceStatusResult {
    data class Success(val canCheckIn: Boolean, val buttonText: String) : AttendanceStatusResult()
    data class Error(val message: String) : AttendanceStatusResult()
}