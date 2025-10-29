// FaceVectorUtil.kt
package com.hrms.jeejateamozy.feature.face.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Utility class for parsing and handling face vectors from the API.
 * The backend returns face_vector as a JSON string of a 512-dimensional array.
 */
object FaceVectorUtil {

    private const val TAG = "FaceVectorUtil"
    private const val EXPECTED_SIZE = 512

    /**
     * Parse face vector string from API to FloatArray.
     *
     * The API returns face_vector in one of these formats:
     * 1. JSON array string: "[0.123, -0.456, ...]"
     * 2. Comma-separated string: "0.123,-0.456,..."
     *
     * @param faceVectorString The face vector string from API
     * @return FloatArray of 512 dimensions, or null if parsing fails
     */
    fun parseFaceVector(faceVectorString: String?): FloatArray? {
        if (faceVectorString.isNullOrBlank()) {
            Log.w(TAG, "Face vector string is null or empty")
            return null
        }

        return try {
            // Try parsing as JSON array first
            val trimmed = faceVectorString.trim()
            val floatArray = if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                // Parse JSON array
                parseJsonArray(trimmed)
            } else {
                // Parse comma-separated values
                parseCommaSeparated(trimmed)
            }

            // Validate size
            if (floatArray != null && floatArray.size != EXPECTED_SIZE) {
                Log.e(TAG, "Invalid face vector size: ${floatArray.size}, expected $EXPECTED_SIZE")
                return null
            }

            floatArray
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing face vector: ${e.message}", e)
            null
        }
    }

    /**
     * Parse JSON array string to FloatArray
     */
    private fun parseJsonArray(jsonString: String): FloatArray? {
        return try {
            val gson = Gson()
            val type = object : TypeToken<List<Float>>() {}.type
            val list: List<Float> = gson.fromJson(jsonString, type)
            list.toFloatArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON array: ${e.message}")
            null
        }
    }

    /**
     * Parse comma-separated string to FloatArray
     */
    private fun parseCommaSeparated(csvString: String): FloatArray? {
        return try {
            csvString.split(",")
                .map { it.trim().toFloat() }
                .toFloatArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing comma-separated values: ${e.message}")
            null
        }
    }

    /**
     * Verify if face vector is valid
     */
    fun isValidFaceVector(faceVector: FloatArray?): Boolean {
        if (faceVector == null || faceVector.size != EXPECTED_SIZE) {
            return false
        }

        // Check if all values are finite (not NaN or Infinity)
        return faceVector.all { it.isFinite() }
    }

    /**
     * Calculate cosine similarity between two face vectors
     * Returns value between 0 and 1, where 1 means identical faces
     */
    fun calculateSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
        require(vector1.size == EXPECTED_SIZE && vector2.size == EXPECTED_SIZE) {
            "Face vectors must be $EXPECTED_SIZE dimensions"
        }

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            norm1 += vector1[i] * vector1[i]
            norm2 += vector2[i] * vector2[i]
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            Log.w(TAG, "Zero norm detected in similarity calculation")
            return 0f
        }

        val similarity = dotProduct / (kotlin.math.sqrt(norm1) * kotlin.math.sqrt(norm2))
        return similarity.toFloat().coerceIn(0f, 1f)
    }

    /**
     * Get user-friendly message based on similarity score
     */
    fun getSimilarityMessage(similarity: Float, threshold: Float): String {
        val percentage = (similarity * 100).toInt()
        return when {
            similarity >= threshold -> "Face verified! Match: $percentage%"
            similarity >= threshold - 0.1f -> "Almost matched ($percentage%). Please try again in better lighting."
            similarity >= threshold - 0.2f -> "Partial match ($percentage%). Ensure proper lighting and face positioning."
            else -> "Face does not match ($percentage%). Please try again."
        }
    }
}