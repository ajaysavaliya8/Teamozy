package com.example.teamozy.feature.face.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Stores ONLY the 512-d float embedding locally & securely.
 * No photos saved.
 */
class FaceStore private constructor(appContext: Context) {

    companion object {
        private const val PREF_NAME = "face_store_v1"
        private const val KEY_EMB = "face_embedding_512"
        private const val KEY_TS = "face_enroll_ts"
        private const val KEY_VER = "face_enroll_ver"
        private const val CURRENT_VER = 1
        private const val FLOAT_SIZE = 4

        @Volatile private var INSTANCE: FaceStore? = null
        fun getInstance(context: Context): FaceStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FaceStore(context.applicationContext).also { INSTANCE = it }
            }

        fun toBytes(vec: FloatArray): ByteArray {
            val bb = ByteBuffer.allocate(vec.size * FLOAT_SIZE).order(ByteOrder.LITTLE_ENDIAN)
            for (f in vec) bb.putFloat(f)
            return bb.array()
        }

        fun toFloatArray(bytes: ByteArray): FloatArray {
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val out = FloatArray(bytes.size / FLOAT_SIZE)
            var i = 0
            while (bb.hasRemaining()) {
                out[i++] = bb.float
            }
            return out
        }
    }

    private val prefs by lazy {
        val key = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            PREF_NAME,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasEnrollment(): Boolean = prefs.contains(KEY_EMB)

    fun saveEmbedding(vec: FloatArray) {
        val b64 = Base64.encodeToString(toBytes(vec), Base64.NO_WRAP)
        prefs.edit()
            .putString(KEY_EMB, b64)
            .putLong(KEY_TS, System.currentTimeMillis())
            .putInt(KEY_VER, CURRENT_VER)
            .apply()
    }

    fun loadEmbedding(): FloatArray? {
        val b64 = prefs.getString(KEY_EMB, null) ?: return null
        val bytes = Base64.decode(b64, Base64.NO_WRAP)
        return toFloatArray(bytes)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
