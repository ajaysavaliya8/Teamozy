package com.hrms.jeejateamozy.core.utils

import android.util.Log
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response

/**
 * Centralized Network Error Handler
 *
 * Extracts error messages from API responses in a consistent way.
 * Supports multiple error formats:
 * - Standard JSON: { "message": "error text" }
 * - FastAPI style: { "detail": "error text" }
 * - Validation errors: { "detail": [{"msg": "...", "loc": [...]}] }
 * - Gson map style: Map with "message" or "detail" keys
 *
 * Usage:
 * ```
 * val errorMsg = NetworkErrorHandler.extractErrorMessage(response)
 * MyOutcome.Error(errorMsg ?: "Failed to perform operation")
 * ```
 */
object NetworkErrorHandler {

    private const val TAG = "NetworkErrorHandler"

    /**
     * Extract error message from API response
     *
     * @param response The Retrofit response object
     * @param defaultMessage Optional default message if extraction fails
     * @return Extracted error message, or defaultMessage if provided, or generic message
     */
    fun <T> extractErrorMessage(
        response: Response<T>,
        defaultMessage: String? = null
    ): String {
        return extractErrorMessageOrNull(response)
            ?: defaultMessage
            ?: "Server error: ${response.code()}"
    }

    /**
     * Extract error message from API response, returning null if extraction fails
     *
     * @param response The Retrofit response object
     * @return Extracted error message or null
     */
    fun <T> extractErrorMessageOrNull(response: Response<T>): String? {
        return try {
            val errorBody = response.errorBody()?.string()

            if (errorBody.isNullOrBlank()) {
                return null
            }

            // Try JSONObject first (most common)
            val message = tryJsonObjectExtraction(errorBody)
            if (message != null) return message

            // Fallback to Gson Map extraction
            val gsonMessage = tryGsonMapExtraction(errorBody)
            if (gsonMessage != null) return gsonMessage

            // If we got here, couldn't parse the error
            Log.w(TAG, "Could not parse error body: $errorBody")
            null

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting error message from response", e)
            null
        }
    }

    /**
     * Try to extract error using JSONObject parsing
     * Handles: message, detail (string), detail (array with validation errors)
     */
    private fun tryJsonObjectExtraction(errorBody: String): String? {
        return try {
            val json = JSONObject(errorBody)

            // 1. Check for standard "message" field
            val message = json.optString("message")
            if (message.isNotBlank()) {
                return message
            }

            // 2. Check for "detail" field (FastAPI style)
            if (json.has("detail")) {
                val detail = json.get("detail")

                // Detail as string
                if (detail is String && detail.isNotBlank()) {
                    return detail
                }

                // Detail as array (validation errors)
                if (detail is JSONArray && detail.length() > 0) {
                    return extractValidationError(detail)
                }
            }

            // 3. No recognized error format
            null

        } catch (e: Exception) {
            Log.d(TAG, "JSONObject extraction failed: ${e.message}")
            null
        }
    }

    /**
     * Extract human-readable message from FastAPI validation error array
     * Format: [{"msg": "...", "loc": ["body", "field_name"], "type": "..."}]
     */
    private fun extractValidationError(detailArray: JSONArray): String? {
        return try {
            val firstError = detailArray.getJSONObject(0)
            val msg = firstError.optString("msg", "")

            // Try to extract field name from "loc" array
            val locArray = firstError.optJSONArray("loc")
            val fieldName = if (locArray != null && locArray.length() > 1) {
                locArray.getString(1)
            } else {
                "Field"
            }

            // Format message based on validation type
            when {
                msg.contains("at least", ignoreCase = true) -> msg
                msg.contains("required", ignoreCase = true) -> "$fieldName is required"
                msg.isNotBlank() -> msg
                else -> "Invalid input"
            }

        } catch (e: Exception) {
            Log.d(TAG, "Validation error extraction failed: ${e.message}")
            null
        }
    }

    /**
     * Try to extract error using Gson Map parsing
     * Some APIs return errors as simple maps
     */
    private fun tryGsonMapExtraction(errorBody: String): String? {
        return try {
            val gson = Gson()
            val errorMap = gson.fromJson(errorBody, Map::class.java)

            // Check for "message" key
            val message = errorMap["message"] as? String
            if (!message.isNullOrBlank()) {
                return message
            }

            // Check for "detail" key
            val detail = errorMap["detail"] as? String
            if (!detail.isNullOrBlank()) {
                return detail
            }

            null

        } catch (e: Exception) {
            Log.d(TAG, "Gson map extraction failed: ${e.message}")
            null
        }
    }

    /**
     * Get friendly error message for network exceptions
     *
     * Usage:
     * ```
     * catch (e: Exception) {
     *     val message = NetworkErrorHandler.friendlyNetworkError(e)
     *     return MyOutcome.Error(message)
     * }
     * ```
     */
    fun friendlyNetworkError(exception: Throwable): String {
        val message = exception.message ?: ""

        return when {
            message.contains("Unable to resolve host", ignoreCase = true) ||
                    message.contains("Failed to connect", ignoreCase = true) ->
                "No internet connection. Please check your network."

            message.contains("timeout", ignoreCase = true) ->
                "Connection timeout. Please try again."

            message.contains("SocketTimeoutException", ignoreCase = true) ->
                "Server timed out. Please try again."

            message.contains("UnknownHostException", ignoreCase = true) ->
                "Cannot reach server. Check your internet or server URL."

            else ->
                exception.message ?: "Network error. Please try again."
        }
    }

    /**
     * Quick helper to extract error with fallback
     *
     * Usage:
     * ```
     * else -> {
     *     val errorMsg = NetworkErrorHandler.extract(response, "Operation failed")
     *     return MyOutcome.Error(errorMsg)
     * }
     * ```
     */
    fun <T> extract(response: Response<T>, fallback: String): String {
        return extractErrorMessage(response, fallback)
    }
}