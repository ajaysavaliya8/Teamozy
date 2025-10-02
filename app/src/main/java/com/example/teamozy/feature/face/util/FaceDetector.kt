//FaceDetector.kt

package com.example.teamozy.feature.face.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max

// ---------- Public data & result types ----------

data class DetectedFace(
    val boundingBox: Rect,
    val leftEye: PointF?,
    val rightEye: PointF?,
    val score: Float = 0f,
    val trackingId: Int? = null,
    // Exposed for liveness/UX
    val yawDeg: Float = 0f,          // signed ML Kit yaw (left/right)
    val pitchDeg: Float = 0f,        // signed ML Kit pitch (up/down)
    val rollDeg: Float = 0f,         // signed ML Kit roll (tilt)
    val leftEyeOpenProb: Float? = null,
    val rightEyeOpenProb: Float? = null
)

sealed class FaceDetectOutcome {
    data class Success(val face: DetectedFace) : FaceDetectOutcome()
    data class Rejected(val reason: Reason) : FaceDetectOutcome()
    data class Failure(val error: Throwable) : FaceDetectOutcome()

    enum class Reason {
        NO_FACES_DETECTED,
        NO_FACES_WITH_VISIBLE_EYES,
        MLKIT_FAILED,
        FACE_TOO_SMALL,
        FACE_TOO_LARGE,
        POSE_YAW,
        POSE_PITCH,
        POSE_ROLL,
        EYES_CLOSED,
        MISSING_EYE_LANDMARKS,
        RATE_LIMITED  // For camera frame skipping
    }
}

fun FaceDetectOutcome.Reason.userMessage(): String = when (this) {
    FaceDetectOutcome.Reason.NO_FACES_DETECTED ->
        "No face detected. Please center your face in good lighting."
    FaceDetectOutcome.Reason.NO_FACES_WITH_VISIBLE_EYES ->
        "We couldn't see your eyes clearly. Look at the camera and keep both eyes visible."
    FaceDetectOutcome.Reason.MLKIT_FAILED ->
        "Face detection failed. Please try again."
    FaceDetectOutcome.Reason.FACE_TOO_SMALL ->
        "Move closer to the camera."
    FaceDetectOutcome.Reason.FACE_TOO_LARGE ->
        "Move back from the camera."
    FaceDetectOutcome.Reason.POSE_YAW ->
        "Please turn your head less left/right."
    FaceDetectOutcome.Reason.POSE_PITCH ->
        "Please lower/raise your chin slightly."
    FaceDetectOutcome.Reason.POSE_ROLL ->
        "Please keep your head upright."
    FaceDetectOutcome.Reason.EYES_CLOSED ->
        "Please open your eyes."
    FaceDetectOutcome.Reason.MISSING_EYE_LANDMARKS ->
        "Keep your eyes visible and look at the camera."
    FaceDetectOutcome.Reason.RATE_LIMITED ->
        "Processing... please wait."
}

data class Frame(
    val bitmap: Bitmap,
    val rotationDegrees: Int = 0
)

enum class RetryStrategy {
    AGGRESSIVE,
    CONSERVATIVE
}

interface FaceDetectorTelemetry {
    fun onDetectionAttempt(durationMs: Long, outcome: FaceDetectOutcome)
    fun onRetryAttempt(attemptNumber: Int, reason: FaceDetectOutcome.Reason)
}

/**
 * Thread-safe ML Kit face detector optimized for camera streams.
 *
 * IMPORTANT: This detector performs QUALITY CHECKS only, NOT liveness detection.
 * For liveness/anti-spoofing, implement challenge-response (blink, head movement) on top.
 *
 * @param frameRateLimitMs Minimum ms between detections to avoid overloading (10fps default).
 * @param debugLogging Set from your app as BuildConfig.DEBUG if desired.
 */
class FaceDetector(
    context: Context,
    private val maxYawDeg: Float = 20f,
    private val maxPitchDeg: Float = 20f,
    private val maxRollDeg: Float = 20f,
    private val minDetectorFaceSizeRatio: Float = 0.15f,
    private val minFaceAreaRatio: Float = 0.10f,
    private val maxFaceAreaRatio: Float = 0.80f,
    private val minEyeOpenProb: Float = 0.30f,
    private val minBoxFracOfWidth: Float = 0.20f,
    private val frameRateLimitMs: Long = 100,      // Max ~10 fps
    private val telemetry: FaceDetectorTelemetry? = null,
    private val debugLogging: Boolean = false
) : AutoCloseable {

    companion object {
        private const val TAG = "FaceDetector"
        private const val DEFAULT_BACKOFF_CAP_MS = 1000L
        private const val DEFAULT_TIMEOUT_MS = 10_000L
        private const val FRONTAL_WEIGHT = 0.6f
        private const val EYE_OPEN_WEIGHT = 0.2f
    }

    // Frame rate limiting for camera streams
    @Volatile private var lastProcessedTime = 0L

    init {
        require(maxYawDeg >= 0f) { "maxYawDeg must be >= 0, got $maxYawDeg" }
        require(maxPitchDeg >= 0f) { "maxPitchDeg must be >= 0, got $maxPitchDeg" }
        require(maxRollDeg >= 0f) { "maxRollDeg must be >= 0, got $maxRollDeg" }
        require(minDetectorFaceSizeRatio in 0f..1f)
        require(minFaceAreaRatio in 0f..1f)
        require(maxFaceAreaRatio in 0f..1f)
        require(maxFaceAreaRatio >= minFaceAreaRatio) {
            "maxFaceAreaRatio ($maxFaceAreaRatio) must be >= minFaceAreaRatio ($minFaceAreaRatio)"
        }
        require(minEyeOpenProb in 0f..1f)
        require(minBoxFracOfWidth in 0f..1f)
        require(frameRateLimitMs >= 0) { "frameRateLimitMs must be >= 0" }
    }

    private val detector: com.google.mlkit.vision.face.FaceDetector =
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setMinFaceSize(minDetectorFaceSizeRatio)
                .enableTracking() // <-- correct ML Kit call
                .build()
        ).also { logD("ML Kit FaceDetector initialized (optimized for camera streams)") }

    // Landmarks are REQUIRED for alignment
    private fun hasEyes(f: Face): Boolean =
        f.getLandmark(FaceLandmark.LEFT_EYE) != null &&
                f.getLandmark(FaceLandmark.RIGHT_EYE) != null

    /**
     * Detect best quality face with frame rate limiting for camera streams.
     */
    suspend fun detectBestFace(
        bitmap: Bitmap,
        rotationDegrees: Int = 0
    ): FaceDetectOutcome {
        // Frame rate limiting
        val now = SystemClock.elapsedRealtime()
        if (frameRateLimitMs > 0 && (now - lastProcessedTime) < frameRateLimitMs) {
            return FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.RATE_LIMITED)
        }
        lastProcessedTime = now

        val t0 = SystemClock.elapsedRealtime()
        logD("detectBestFace() - Image: ${bitmap.width}x${bitmap.height}, rotation=$rotationDegrees°")

        val result = process(InputImage.fromBitmap(bitmap, rotationDegrees))
        if (result.isFailure) {
            val err = result.exceptionOrNull() ?: RuntimeException("Unknown ML Kit failure")
            logE("ML Kit detection failed", err)
            val outcome = FaceDetectOutcome.Failure(err)
            telemetry?.onDetectionAttempt(SystemClock.elapsedRealtime() - t0, outcome)
            return outcome
        }

        val faces = result.getOrNull().orEmpty()
        if (faces.isEmpty()) {
            logW("No faces detected")
            val outcome = FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.NO_FACES_DETECTED)
            telemetry?.onDetectionAttempt(SystemClock.elapsedRealtime() - t0, outcome)
            return outcome
        }

        val imgW = bitmap.width.toFloat()
        val imgH = bitmap.height.toFloat()
        val imgArea = imgW * imgH
        val minBoxPx = max(1f, imgW * minBoxFracOfWidth)

        val haveAnyWithEyes = faces.any { f -> hasEyes(f) }
        if (!haveAnyWithEyes) {
            logW("Faces present but none with BOTH eye LANDMARKS")
            val outcome = FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.NO_FACES_WITH_VISIBLE_EYES)
            telemetry?.onDetectionAttempt(SystemClock.elapsedRealtime() - t0, outcome)
            return outcome
        }

        val valid = faces.filter { f ->
            hasEyes(f) && f.boundingBox.width() >= minBoxPx
        }
        if (valid.isEmpty()) {
            logW("No valid faces after filtering (size/eyes)")
            val outcome = FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.FACE_TOO_SMALL)
            telemetry?.onDetectionAttempt(SystemClock.elapsedRealtime() - t0, outcome)
            return outcome
        }

        // Score faces: size × (1 + frontalness + eye-open bonus)
        val scored = valid.map { f ->
            val area = f.boundingBox.width().toFloat() * f.boundingBox.height().toFloat()
            val yaw = abs(f.headEulerAngleY)
            val pitch = abs(f.headEulerAngleX)
            val roll = abs(f.headEulerAngleZ)
            val frontal = (1f - (yaw / 180f + pitch / 180f + roll / 180f) / 3f).coerceIn(0f, 1f)
            val eyeOpen = ((f.leftEyeOpenProbability ?: 0.5f) + (f.rightEyeOpenProbability ?: 0.5f)) / 2f
            val score = area * (1f + FRONTAL_WEIGHT * frontal + EYE_OPEN_WEIGHT * eyeOpen)
            f to score
        }

        val best = scored.maxByOrNull { it.second }?.first ?: run {
            val outcome = FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.NO_FACES_DETECTED)
            telemetry?.onDetectionAttempt(SystemClock.elapsedRealtime() - t0, outcome)
            return outcome
        }

        // Quality gates
        val rejection = validateFaceQuality(best, imgArea)
        if (rejection != null) {
            telemetry?.onDetectionAttempt(SystemClock.elapsedRealtime() - t0, rejection)
            return rejection
        }

        // Eye centers: prefer contours (more stable), fallback to landmarks (required by validation)
        val leftEye = robustEyeCenter(best, FaceLandmark.LEFT_EYE, FaceContour.LEFT_EYE)
        val rightEye = robustEyeCenter(best, FaceLandmark.RIGHT_EYE, FaceContour.RIGHT_EYE)
        if (leftEye == null || rightEye == null) {
            val outcome = FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.MISSING_EYE_LANDMARKS)
            telemetry?.onDetectionAttempt(SystemClock.elapsedRealtime() - t0, outcome)
            return outcome
        }

        // Final score (same frontal metric)
        val yaw = abs(best.headEulerAngleY)
        val pitch = abs(best.headEulerAngleX)
        val roll = abs(best.headEulerAngleZ)
        val faceArea = best.boundingBox.width().toFloat() * best.boundingBox.height().toFloat()
        val sizeScore = (faceArea / imgArea).coerceIn(0f, 1f)
        val frontalScore = (1f - (yaw / 180f + pitch / 180f + roll / 180f) / 3f).coerceIn(0f, 1f)
        val leftProb = best.leftEyeOpenProbability ?: 0.5f
        val rightProb = best.rightEyeOpenProbability ?: 0.5f
        val eyeScore = (leftProb + rightProb) / 2f
        val score = (sizeScore * 0.4f) + (frontalScore * 0.4f) + (eyeScore * 0.2f)

        val detected = DetectedFace(
            boundingBox = best.boundingBox,
            leftEye = leftEye,
            rightEye = rightEye,
            score = score,
            trackingId = best.trackingId,
            yawDeg = best.headEulerAngleY,
            pitchDeg = best.headEulerAngleX,
            rollDeg = best.headEulerAngleZ,
            leftEyeOpenProb = best.leftEyeOpenProbability,
            rightEyeOpenProb = best.rightEyeOpenProbability
        )

        logI("SUCCESS: Best face selected with score=${score.format2()}, trackingId=${best.trackingId}")
        val outcome = FaceDetectOutcome.Success(detected)
        telemetry?.onDetectionAttempt(SystemClock.elapsedRealtime() - t0, outcome)
        return outcome
    }

    private fun validateFaceQuality(face: Face, imageArea: Float): FaceDetectOutcome.Rejected? {
        val yaw = abs(face.headEulerAngleY)
        val pitch = abs(face.headEulerAngleX)
        val roll = abs(face.headEulerAngleZ)

        logD("Face angles: yaw=${yaw.format1()}°, pitch=${pitch.format1()}°, roll=${roll.format1()}°")

        if (yaw > maxYawDeg) return FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.POSE_YAW)
        if (pitch > maxPitchDeg) return FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.POSE_PITCH)
        if (roll > maxRollDeg) return FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.POSE_ROLL)

        val faceArea = face.boundingBox.width().toFloat() * face.boundingBox.height().toFloat()
        val ratio = (faceArea / imageArea).coerceAtLeast(1e-6f)
        logD("Face area ratio: ${(ratio * 100f).format1()}%")

        if (ratio < minFaceAreaRatio) return FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.FACE_TOO_SMALL)
        if (ratio > maxFaceAreaRatio) return FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.FACE_TOO_LARGE)

        val leftProb = face.leftEyeOpenProbability ?: 0.5f
        val rightProb = face.rightEyeOpenProbability ?: 0.5f
        if (leftProb < minEyeOpenProb || rightProb < minEyeOpenProb) {
            return FaceDetectOutcome.Rejected(FaceDetectOutcome.Reason.EYES_CLOSED)
        }

        return null
    }

    /**
     * Robust eye center: prefer contours (more stable), fallback to landmarks (required by validation).
     */
    private fun robustEyeCenter(face: Face, lmType: Int, contourType: Int): PointF? {
        val pts = face.getContour(contourType)?.points
        if (!pts.isNullOrEmpty()) {
            var sx = 0f; var sy = 0f
            for (p in pts) { sx += p.x; sy += p.y }
            val inv = 1f / pts.size
            return PointF(sx * inv, sy * inv)
        }
        val lm = face.getLandmark(lmType)?.position ?: return null
        return PointF(lm.x, lm.y)
    }

    private suspend fun process(image: InputImage): Result<List<Face>> =
        suspendCancellableCoroutine { cont ->
            logD("process() - Starting ML Kit face detection")
            detector.process(image)
                .addOnSuccessListener { faces ->
                    logD("process() - Success: ${faces.size} face(s)")
                    if (cont.isActive) cont.resume(Result.success(faces))
                }
                .addOnFailureListener { e ->
                    logE("process() - Failure", e)
                    if (cont.isActive) cont.resume(Result.failure(e))
                }
                .addOnCanceledListener {
                    logD("process() - Canceled by ML Kit")
                    if (cont.isActive) cont.resume(Result.failure(CancellationException("ML Kit cancelled")))
                }
            cont.invokeOnCancellation {
                logD("process() - Coroutine cancelled")
            }
        }

    // ---------- Retry helpers ----------

    suspend fun detectBestFaceRetry(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        maxAttempts: Int = 3,
        initialBackoffMs: Long = 120,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): FaceDetectOutcome = try {
        withTimeout(timeoutMs) {
            retryLogic(
                maxAttempts = maxAttempts,
                initialBackoffMs = initialBackoffMs,
                attempt = {
                    when (val o = detectBestFace(bitmap, rotationDegrees)) {
                        is FaceDetectOutcome.Success -> RetryResult.Success(o)
                        is FaceDetectOutcome.Rejected -> RetryResult.Stop(o)
                        is FaceDetectOutcome.Failure -> RetryResult.Retry(FaceDetectOutcome.Reason.MLKIT_FAILED)
                    }
                }
            )
        }
    } catch (e: TimeoutCancellationException) {
        logE("detectBestFaceRetry timed out")
        FaceDetectOutcome.Failure(e)
    }

    suspend fun detectBestFaceRetryStream(
        getFrame: suspend () -> Frame,
        maxAttempts: Int = 3,
        initialBackoffMs: Long = 120,
        strategy: RetryStrategy = RetryStrategy.AGGRESSIVE,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): FaceDetectOutcome = try {
        withTimeout(timeoutMs) {
            retryLogic(
                maxAttempts = maxAttempts,
                initialBackoffMs = initialBackoffMs,
                attempt = {
                    val (bm, rot) = getFrame()
                    when (val o = detectBestFace(bm, rot)) {
                        is FaceDetectOutcome.Success -> RetryResult.Success(o)
                        is FaceDetectOutcome.Failure -> RetryResult.Retry(FaceDetectOutcome.Reason.MLKIT_FAILED)
                        is FaceDetectOutcome.Rejected -> {
                            if (o.reason == FaceDetectOutcome.Reason.RATE_LIMITED) {
                                RetryResult.Stop(o)
                            } else {
                                val shouldRetry = when (strategy) {
                                    RetryStrategy.AGGRESSIVE -> when (o.reason) {
                                        FaceDetectOutcome.Reason.NO_FACES_DETECTED,
                                        FaceDetectOutcome.Reason.NO_FACES_WITH_VISIBLE_EYES,
                                        FaceDetectOutcome.Reason.POSE_YAW,
                                        FaceDetectOutcome.Reason.POSE_PITCH,
                                        FaceDetectOutcome.Reason.POSE_ROLL,
                                        FaceDetectOutcome.Reason.EYES_CLOSED,
                                        FaceDetectOutcome.Reason.MISSING_EYE_LANDMARKS,
                                        FaceDetectOutcome.Reason.MLKIT_FAILED -> true
                                        FaceDetectOutcome.Reason.FACE_TOO_SMALL,
                                        FaceDetectOutcome.Reason.FACE_TOO_LARGE,
                                        FaceDetectOutcome.Reason.RATE_LIMITED -> false
                                    }
                                    RetryStrategy.CONSERVATIVE -> when (o.reason) {
                                        FaceDetectOutcome.Reason.NO_FACES_DETECTED,
                                        FaceDetectOutcome.Reason.MLKIT_FAILED -> true
                                        else -> false
                                    }
                                }
                                if (shouldRetry) RetryResult.Retry(o.reason) else RetryResult.Stop(o)
                            }
                        }
                    }
                }
            )
        }
    } catch (e: TimeoutCancellationException) {
        logE("detectBestFaceRetryStream timed out")
        FaceDetectOutcome.Failure(e)
    }

    private sealed class RetryResult {
        data class Success(val outcome: FaceDetectOutcome) : RetryResult()
        data class Stop(val outcome: FaceDetectOutcome) : RetryResult()
        data class Retry(val reason: FaceDetectOutcome.Reason) : RetryResult()
    }

    private suspend fun retryLogic(
        maxAttempts: Int,
        initialBackoffMs: Long,
        attempt: suspend () -> RetryResult
    ): FaceDetectOutcome {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }

        var attemptNum = 0
        var backoff = initialBackoffMs.coerceIn(1L, DEFAULT_BACKOFF_CAP_MS)

        while (attemptNum < maxAttempts) {
            attemptNum++

            when (val result = attempt()) {
                is RetryResult.Success -> return result.outcome
                is RetryResult.Stop -> return result.outcome
                is RetryResult.Retry -> {
                    if (attemptNum < maxAttempts) {
                        telemetry?.onRetryAttempt(attemptNum, result.reason)
                        logD("Retry attempt $attemptNum/$maxAttempts after ${backoff}ms")
                        delay(backoff)
                        backoff = nextBackoff(backoff, DEFAULT_BACKOFF_CAP_MS)
                    }
                }
            }
        }

        return FaceDetectOutcome.Failure(RuntimeException("Max retry attempts exceeded"))
    }

    override fun close() {
        logD("close() - Releasing ML Kit detector resources")
        detector.close()
    }

    private inline fun logD(msg: String) { if (debugLogging || Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, msg) }
    private inline fun logI(msg: String) { if (debugLogging || Log.isLoggable(TAG, Log.INFO)) Log.i(TAG, msg) }
    private inline fun logW(msg: String) { if (debugLogging || Log.isLoggable(TAG, Log.WARN)) Log.w(TAG, msg) }
    private inline fun logE(msg: String, tr: Throwable? = null) { if (debugLogging || Log.isLoggable(TAG, Log.ERROR)) Log.e(TAG, msg, tr) }
}

private fun Float.format1(): String = String.format("%.1f", this)
private fun Float.format2(): String = String.format("%.2f", this)

private fun nextBackoff(currentMs: Long, capMs: Long): Long {
    if (currentMs >= capMs) return capMs
    val halfCap = capMs / 2
    return if (currentMs >= halfCap) capMs else currentMs * 2
}
