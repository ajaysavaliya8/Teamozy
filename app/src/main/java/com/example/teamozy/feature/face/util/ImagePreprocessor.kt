package com.example.teamozy.feature.face.util

import android.graphics.*
import kotlin.math.atan2

object ImagePreprocessor {

    fun cropAndAlign(
        source: Bitmap,
        faceRect: Rect,
        leftEye: PointF?,
        rightEye: PointF?,
        targetSize: Int
    ): Bitmap {
        val aligned = if (leftEye != null && rightEye != null) {
            val angle = atan2(
                (rightEye.y - leftEye.y),
                (rightEye.x - leftEye.x)
            )
            rotateAroundCenter(source, Math.toDegrees(angle.toDouble()).toFloat())
        } else source

        val pad = (faceRect.width() * 0.35f).toInt()
        val cx = faceRect.centerX()
        val cy = faceRect.centerY()
        val half = ((faceRect.width().coerceAtLeast(faceRect.height())) / 2f + pad).toInt()

        val left = (cx - half).coerceAtLeast(0)
        val top = (cy - half).coerceAtLeast(0)
        val right = (cx + half).coerceAtMost(aligned.width)
        val bottom = (cy + half).coerceAtMost(aligned.height)

        val safe = Rect(left, top, right, bottom)
        val cropped = Bitmap.createBitmap(
            aligned, safe.left, safe.top,
            safe.width().coerceAtLeast(1),
            safe.height().coerceAtLeast(1)
        )
        return Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
    }

    private fun rotateAroundCenter(src: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return src
        val m = Matrix().apply { setRotate(degrees, src.width / 2f, src.height / 2f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    /** CHW, normalized to [-1, 1] */
    fun bitmapToCHWFloat(bm: Bitmap): FloatArray {
        val w = bm.width
        val h = bm.height
        val out = FloatArray(3 * w * h)
        val pixels = IntArray(w * h)
        bm.getPixels(pixels, 0, w, 0, 0, w, h)

        val plane = w * h
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val p = pixels[i]
                val r = (p shr 16 and 0xFF) / 255f
                val g = (p shr 8 and 0xFF) / 255f
                val b = (p and 0xFF) / 255f
                out[i] = (r - 0.5f) / 0.5f
                out[plane + i] = (g - 0.5f) / 0.5f
                out[2 * plane + i] = (b - 0.5f) / 0.5f
            }
        }
        return out
    }
}
