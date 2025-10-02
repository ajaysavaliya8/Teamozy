// ImagePreprocessor.kt
package com.example.teamozy.feature.face.util

import android.graphics.*
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.WorkerThread
import kotlin.math.hypot
import kotlin.math.max

/**
 * Produces a face crop aligned to a canonical 2-eye geometry (ideal for FaceNet-style models).
 *
 * Strategy:
 * 1) If both eye landmarks exist → similarity transform so eyes land at fixed positions
 *    inside the output (targetSize x targetSize).
 * 2) Otherwise → square, padded crop around faceRect (no global rotation).
 *
 * Notes:
 * - Output is exactly targetSize x targetSize (ARGB_8888).
 * - Outside areas after warp are painted with [background].
 * - Guards reject extreme landmark geometry to avoid crazy zooms.
 * - Intermediate copies are recycled; the caller's source is never recycled here.
 * - Trust ML Kit's anatomical labels (subject-left/right), even on mirrored front-camera frames.
 */
object ImagePreprocessor {

    // ---- Logging ----
    private const val TAG = "ImagePreprocessor"

    // ---- Defaults / Tunables ----
    private const val DEFAULT_OUT = 112                     // FaceNet common input
    private const val DEFAULT_PADDING_RATIO = 0.35f         // Fallback crop padding

    // Canonical eye placement (fractions of width/height in the output)
    private const val EYE_Y = 0.40f
    private const val EYE_X_LEFT = 0.35f
    private const val EYE_X_RIGHT = 0.65f

    // Landmark sanity guards
    private const val MIN_EYE_DIST_PX = 8f
    private const val MIN_SCALE = 0.25f
    private const val MAX_SCALE = 4.00f

    /**
     * Crop + align face into [targetSize] square bitmap.
     *
     * @param source       Source bitmap (NOT recycled here)
     * @param faceRect     Face box in source coordinates (non-empty)
     * @param leftEye      Left eye landmark in source coords (nullable) — subject-left
     * @param rightEye     Right eye landmark in source coords (nullable) — subject-right
     * @param targetSize   Output size (default 112)
     * @param paddingRatio Extra padding for fallback crop (default 0.35)
     * @param background   Background color used outside warped regions (default BLACK)
     */
    @JvmOverloads
    @WorkerThread
    fun cropAndAlign(
        source: Bitmap,
        faceRect: Rect,
        leftEye: PointF?,
        rightEye: PointF?,
        @IntRange(from = 1) targetSize: Int = DEFAULT_OUT,
        @FloatRange(from = 0.0) paddingRatio: Float = DEFAULT_PADDING_RATIO,
        @ColorInt background: Int = Color.BLACK
    ): Bitmap {
        require(!source.isRecycled) { "Source bitmap is recycled" }
        require(!faceRect.isEmpty) { "faceRect is empty" }
        require(targetSize > 0) { "targetSize must be positive, got $targetSize" }
        require(paddingRatio >= 0f) { "paddingRatio must be >= 0, got $paddingRatio" }

        // Preferred path: two-eye canonical alignment
        if (leftEye != null && rightEye != null) {
            alignByTwoEyesOrNull(
                src = source,
                left = leftEye,
                right = rightEye,
                targetSize = targetSize,
                background = background
            )?.let { return it }
            Log.w(TAG, "Two-eye alignment failed/guarded; using padded square crop fallback.")
        }

        // Fallback: padded square crop + scale
        return paddedSquareCrop(
            src = source,
            faceRect = faceRect,
            targetSize = targetSize,
            paddingRatio = paddingRatio,
            background = background
        )
    }

    // ---- Two-eye similarity alignment ----
    private fun alignByTwoEyesOrNull(
        src: Bitmap,
        left: PointF,
        right: PointF,
        targetSize: Int,
        @ColorInt background: Int
    ): Bitmap? {
        // Guard: non-finite landmarks (trust anatomical labels; do not swap)
        if (!left.isFinite() || !right.isFinite()) {
            Log.w(TAG, "Non-finite eye landmarks; skipping alignment")
            return null
        }

        val eyeDist = hypot(
            (right.x - left.x).toDouble(),
            (right.y - left.y).toDouble()
        ).toFloat()
        if (eyeDist < MIN_EYE_DIST_PX) {
            Log.w(TAG, "Eye distance too small: $eyeDist px")
            return null
        }

        // Desired eye distance in the output (keeps forehead/chin balance)
        val desiredEyeDist = (EYE_X_RIGHT - EYE_X_LEFT) * targetSize
        val scale = desiredEyeDist / eyeDist
        if (scale !in MIN_SCALE..MAX_SCALE) {
            Log.w(TAG, "Suspicious eye scale: $scale (eyeDist=$eyeDist, desired=$desiredEyeDist)")
            return null
        }

        // Map SUBJECT-left → canonical left, SUBJECT-right → canonical right
        val dstLeft = PointF(EYE_X_LEFT * targetSize, EYE_Y * targetSize)
        val dstRight = PointF(EYE_X_RIGHT * targetSize, EYE_Y * targetSize)
        val srcPts = floatArrayOf(left.x, left.y, right.x, right.y)
        val dstPts = floatArrayOf(dstLeft.x, dstLeft.y, dstRight.x, dstRight.y)

        val matrix = Matrix()
        if (!matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 2)) {
            Log.w(TAG, "setPolyToPoly failed; cannot compute similarity transform.")
            return null
        }

        var srcSafe: Bitmap? = null
        var out: Bitmap? = null
        try {
            srcSafe = ensureArgb8888(src)
            out = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)

            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            val canvas = Canvas(out)
            canvas.drawColor(background)
            canvas.drawBitmap(srcSafe, matrix, paint)

            return out.also { out = null }
        } catch (e: Exception) {
            Log.e(TAG, "alignByTwoEyesOrNull failed", e)
            out?.recycle()
            return null
        } finally {
            if (srcSafe !== null && srcSafe !== src) srcSafe.recycle()
        }
    }

    // ---- Fallback: square, padded crop (no global rotation) ----
    private fun paddedSquareCrop(
        src: Bitmap,
        faceRect: Rect,
        targetSize: Int,
        paddingRatio: Float,
        @ColorInt background: Int
    ): Bitmap {
        val base = max(faceRect.width(), faceRect.height())
        val pad = (base * paddingRatio).toInt()
        val half = (base / 2f + pad).toInt()
        val cx = faceRect.centerX()
        val cy = faceRect.centerY()

        // Clamp initial padded rect to image bounds
        val left = (cx - half).coerceAtLeast(0)
        val top = (cy - half).coerceAtLeast(0)
        val right = (cx + half).coerceAtMost(src.width)
        val bottom = (cy + half).coerceAtMost(src.height)
        val safe = Rect(left, top, right, bottom)

        // Make square around center, clamped to bounds
        val side = max(safe.width(), safe.height())
        val sqLeft = (safe.centerX() - side / 2).coerceAtLeast(0)
        val sqTop  = (safe.centerY() - side / 2).coerceAtLeast(0)
        val sqRight = (sqLeft + side).coerceAtMost(src.width)
        val sqBottom = (sqTop + side).coerceAtMost(src.height)
        val square = Rect(sqLeft, sqTop, sqRight, sqBottom)

        // Direct draw: source rect → target size (eliminates intermediate bitmap)
        val out = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val dst = Rect(0, 0, targetSize, targetSize)

        canvas.drawColor(background)
        canvas.drawBitmap(src, square, dst, paint)
        return out
    }

    // ---- Helpers ----
    private fun ensureArgb8888(src: Bitmap): Bitmap =
        if (src.config == Bitmap.Config.ARGB_8888) src
        else src.copy(Bitmap.Config.ARGB_8888, /*isMutable=*/false)

    private fun PointF.isFinite(): Boolean = x.isFinite() && y.isFinite()

    @Suppress("unused")
    private fun mapRect(matrix: Matrix, rect: Rect): Rect {
        val rf = RectF(rect)
        matrix.mapRect(rf)
        return Rect(rf.left.toInt(), rf.top.toInt(), rf.right.toInt(), rf.bottom.toInt())
    }
}