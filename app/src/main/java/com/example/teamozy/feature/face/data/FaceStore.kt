//FaceStore.kt
package com.example.teamozy.feature.face.data

import android.content.Context
import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Stores ONLY the 512-d float embedding locally.
 * No photos saved. No encryption.
 */
class FaceStore private constructor(appContext: Context) {

    companion object {
        private const val TAG = "FaceStore"
        private const val PREF_NAME = "face_store_v1"
        private const val KEY_EMB = "face_embedding_512"
        private const val KEY_TS = "face_enroll_ts"
        private const val KEY_VER = "face_enroll_ver"
        private const val CURRENT_VER = 1
        private const val FLOAT_SIZE = 4

        @Volatile private var INSTANCE: FaceStore? = null

        fun getInstance(context: Context): FaceStore {
            Log.d(TAG, "getInstance() called")
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FaceStore(context.applicationContext).also {
                    INSTANCE = it
                    Log.d(TAG, "New FaceStore instance created")
                }
            }
        }

        fun toBytes(vec: FloatArray): ByteArray {
            Log.d(TAG, "toBytes() - Converting FloatArray to ByteArray, size: ${vec.size}")
            val bb = ByteBuffer.allocate(vec.size * FLOAT_SIZE).order(ByteOrder.LITTLE_ENDIAN)
            for (f in vec) bb.putFloat(f)
            val result = bb.array()
            Log.d(TAG, "toBytes() - Conversion complete, byte array size: ${result.size}")
            return result
        }

        fun toFloatArray(bytes: ByteArray): FloatArray {
            Log.d(TAG, "toFloatArray() - Converting ByteArray to FloatArray, byte size: ${bytes.size}")
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val out = FloatArray(bytes.size / FLOAT_SIZE)
            var i = 0
            while (bb.hasRemaining()) {
                out[i++] = bb.float
            }
            Log.d(TAG, "toFloatArray() - Conversion complete, float array size: ${out.size}")
            return out
        }
    }

    private val prefs by lazy {
        Log.d(TAG, "Initializing SharedPreferences: $PREF_NAME")
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).also {
            Log.d(TAG, "SharedPreferences initialized successfully")
        }
    }

    fun hasEnrollment(): Boolean {
        Log.d(TAG, "hasEnrollment() called")
        val exists = prefs.contains(KEY_EMB)
        Log.d(TAG, "hasEnrollment() - Result: $exists")
        return exists
    }

    fun saveEmbedding(vec: FloatArray) {
        Log.d(TAG, "saveEmbedding() called with vector size: ${vec.size}")

        try {
            Log.d(TAG, "saveEmbedding() - Converting vector to bytes")
            val bytes = toBytes(vec)

            Log.d(TAG, "saveEmbedding() - Encoding to Base64")
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Log.d(TAG, "saveEmbedding() - Base64 string length: ${b64.length}")

            val timestamp = System.currentTimeMillis()
            Log.d(TAG, "saveEmbedding() - Timestamp: $timestamp")

            Log.d(TAG, "saveEmbedding() - Saving to SharedPreferences")
            prefs.edit()
                .putString(KEY_EMB, b64)
                .putLong(KEY_TS, timestamp)
                .putInt(KEY_VER, CURRENT_VER)
                .apply()

            Log.i(TAG, "saveEmbedding() - Successfully saved embedding (version: $CURRENT_VER)")

            // Verify save
            val verification = prefs.contains(KEY_EMB)
            Log.d(TAG, "saveEmbedding() - Verification check: $verification")

        } catch (e: Exception) {
            Log.e(TAG, "saveEmbedding() - Error occurred", e)
            throw e
        }
    }

    fun loadEmbedding(): FloatArray? {
        Log.d(TAG, "loadEmbedding() called")

        try {
            if (!prefs.contains(KEY_EMB)) {
                Log.w(TAG, "loadEmbedding() - No embedding found in preferences")
                return null
            }

            Log.d(TAG, "loadEmbedding() - Retrieving Base64 string from preferences")
            val b64 = prefs.getString(KEY_EMB, null)

            if (b64 == null) {
                Log.w(TAG, "loadEmbedding() - Base64 string is null")
                return null
            }

            Log.d(TAG, "loadEmbedding() - Base64 string length: ${b64.length}")

            val storedTimestamp = prefs.getLong(KEY_TS, 0L)
            val storedVersion = prefs.getInt(KEY_VER, 0)
            Log.d(TAG, "loadEmbedding() - Stored timestamp: $storedTimestamp, version: $storedVersion")

            Log.d(TAG, "loadEmbedding() - Decoding Base64 string")
            val bytes = Base64.decode(b64, Base64.NO_WRAP)
            Log.d(TAG, "loadEmbedding() - Decoded byte array size: ${bytes.size}")

            Log.d(TAG, "loadEmbedding() - Converting bytes to FloatArray")
            val result = toFloatArray(bytes)

            Log.i(TAG, "loadEmbedding() - Successfully loaded embedding, size: ${result.size}")
            Log.d(TAG, "loadEmbedding() - First 5 values: ${result.take(5).joinToString()}")

            return result

        } catch (e: Exception) {
            Log.e(TAG, "loadEmbedding() - Error occurred", e)
            return null
        }
    }

    fun clear() {
        Log.d(TAG, "clear() called")

        try {
            val hadData = prefs.contains(KEY_EMB)
            Log.d(TAG, "clear() - Had enrollment data before clear: $hadData")

            prefs.edit().clear().apply()

            val verifyCleared = !prefs.contains(KEY_EMB)
            Log.d(TAG, "clear() - Verification after clear (should be true): $verifyCleared")

            Log.i(TAG, "clear() - Successfully cleared all data")

        } catch (e: Exception) {
            Log.e(TAG, "clear() - Error occurred", e)
            throw e
        }
    }
}