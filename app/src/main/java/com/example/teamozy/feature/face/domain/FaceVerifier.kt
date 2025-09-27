package com.example.teamozy.feature.face.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object FaceVerifier {
    // Start here; we can tune after field tests.
    const val DEFAULT_THRESHOLD = 0.55f

    fun isMatch(a: FloatArray, b: FloatArray, threshold: Float = DEFAULT_THRESHOLD): Boolean {
        if (a.size != b.size) return false
        return cosineSim(a, b) >= threshold
    }

    fun cosineSim(a: FloatArray, b: FloatArray): Float {
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in a.indices) {
            val x = a[i]
            val y = b[i]
            dot += x * y
            na += x * x
            nb += y * y
        }
        val denom = sqrt(na) * sqrt(nb)
        if (denom <= 1e-12) return 0f
        // Clamp to handle tiny numeric drift
        val v = (dot / denom).toFloat()
        return max(-1f, min(1f, v))
    }
}
