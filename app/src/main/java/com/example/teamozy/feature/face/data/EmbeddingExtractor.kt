package com.example.teamozy.feature.face.data

import android.content.Context
import android.graphics.Bitmap
import com.example.teamozy.feature.face.util.FaceDetector
import com.example.teamozy.feature.face.util.ImagePreprocessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.MappedByteBuffer
import kotlin.math.sqrt

class EmbeddingExtractor private constructor(
    private val appContext: Context
) {

    companion object {
        private const val MODEL_FILE = "facenet_512.tflite"   // <<— TFLite model
        private const val INPUT_SIZE = 112
        private const val EMBEDDING_SIZE = 512

        @Volatile private var INSTANCE: EmbeddingExtractor? = null
        fun getInstance(context: Context): EmbeddingExtractor =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmbeddingExtractor(context.applicationContext).also { INSTANCE = it }
            }
    }

    // Lazy TFLite interpreter
    private val tflite: Interpreter by lazy {
        val mm: MappedByteBuffer = FileUtil.loadMappedFile(appContext, MODEL_FILE)
        val opts = Interpreter.Options().apply { numThreads = 3 }
        Interpreter(mm, opts)
    }

    private val faceDetector by lazy { FaceDetector(appContext) }

    /**
     * Detect best face -> align/crop 112 -> NHWC [-1,1] -> TFLite -> L2-normalized 512f
     */
    suspend fun extract(bitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        val face = faceDetector.detectBestFace(bitmap)
            ?: throw IllegalStateException("No face detected. Please face the camera in good lighting.")

        // Align/crop to 112x112
        val aligned = ImagePreprocessor.cropAndAlign(
            source = bitmap,
            faceRect = face.boundingBox,
            leftEye = face.leftEye,
            rightEye = face.rightEye,
            targetSize = INPUT_SIZE
        )

        // Prepare NHWC float [-1,1]
        val input = toNHWCMinus1to1(aligned)                // shape [1,112,112,3]
        val output = Array(1) { FloatArray(EMBEDDING_SIZE) } // shape [1,512]

        tflite.run(input, output)
        l2NormalizeInPlace(output[0])
    }

    /**
     * Convert Bitmap (112x112) to float[1][112][112][3] in [-1,1] (NHWC).
     * NOTE: TFLite expects NHWC (not NCHW).
     */
    private fun toNHWCMinus1to1(bm: Bitmap): Array<Array<Array<FloatArray>>> {
        val w = bm.width
        val h = bm.height
        require(w == INPUT_SIZE && h == INPUT_SIZE) { "Expected 112x112, got ${w}x${h}" }
        val arr = Array(1) { Array(h) { Array(w) { FloatArray(3) } } }
        val pixels = IntArray(w * h)
        bm.getPixels(pixels, 0, w, 0, 0, w, h)
        var idx = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = pixels[idx++]
                val r = (p shr 16 and 0xFF)
                val g = (p shr 8 and 0xFF)
                val b = (p and 0xFF)
                arr[0][y][x][0] = (r / 127.5f) - 1f
                arr[0][y][x][1] = (g / 127.5f) - 1f
                arr[0][y][x][2] = (b / 127.5f) - 1f
            }
        }
        return arr
    }

    private fun l2NormalizeInPlace(vec: FloatArray): FloatArray {
        var s = 0.0
        for (x in vec) s += x * x
        val d = sqrt(s).toFloat().coerceAtLeast(1e-12f)
        for (i in vec.indices) vec[i] = vec[i] / d
        return vec
    }
}
