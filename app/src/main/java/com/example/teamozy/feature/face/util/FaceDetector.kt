package com.example.teamozy.feature.face.util

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class DetectedFace(
    val boundingBox: Rect,
    val leftEye: PointF?,
    val rightEye: PointF?
)

class FaceDetector(context: Context) {

    private val detector: com.google.mlkit.vision.face.FaceDetector =
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .build()
        )

    suspend fun detectBestFace(bitmap: Bitmap): DetectedFace? {
        val faces = process(InputImage.fromBitmap(bitmap, 0)) ?: return null
        val best = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return null
        val l = best.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val r = best.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        return DetectedFace(
            boundingBox = best.boundingBox,
            leftEye = l?.let { PointF(it.x, it.y) },
            rightEye = r?.let { PointF(it.x, it.y) }
        )
    }

    private suspend fun process(image: InputImage): List<Face>? =
        suspendCancellableCoroutine { cont ->
            detector.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
}
