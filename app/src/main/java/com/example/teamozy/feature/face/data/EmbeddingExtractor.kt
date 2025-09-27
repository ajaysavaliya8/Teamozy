package com.example.teamozy.feature.face.data

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import com.example.teamozy.feature.face.util.FaceDetector
import com.example.teamozy.feature.face.util.ImagePreprocessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.sqrt

class EmbeddingExtractor private constructor(
    private val appContext: Context
) {
    companion object {
        const val MODEL_FILE = "face_embedding.onnx"
        const val INPUT_SIZE = 112
        const val EMBEDDING_SIZE = 512

        @Volatile private var INSTANCE: EmbeddingExtractor? = null
        fun getInstance(context: Context): EmbeddingExtractor =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmbeddingExtractor(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession by lazy { createSession() }
    private val faceDetector by lazy { FaceDetector(appContext) }

    private fun createSession(): OrtSession {
        val modelBytes = appContext.assets.open(MODEL_FILE).use { it.readBytes() }
        val so: SessionOptions = SessionOptions()
        return env.createSession(modelBytes, so)
    }

    /** Detect → align/crop 112×112 → CHW [-1,1] → ONNX → L2-norm 512f */
    suspend fun extract(bitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        val face = faceDetector.detectBestFace(bitmap)
            ?: throw IllegalStateException("No face detected. Please face the camera in good lighting.")

        val aligned = ImagePreprocessor.cropAndAlign(
            source = bitmap,
            faceRect = face.boundingBox,
            leftEye = face.leftEye,
            rightEye = face.rightEye,
            targetSize = INPUT_SIZE
        )

        val chw: FloatArray = ImagePreprocessor.bitmapToCHWFloat(aligned)
        runOnnx(chw)
    }

    private fun runOnnx(chwInput: FloatArray): FloatArray {
        val inputName: String = session.inputNames.firstOrNull()
            ?: throw IllegalStateException("ONNX model has no input names.")

        val shape = longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        val fb = FloatBuffer.wrap(chwInput)
        val inputTensor: OnnxTensor = OnnxTensor.createTensor(env, fb, shape)
        val feeds: Map<String, OnnxTensor> = mapOf(inputName to inputTensor)

        var resultVec: FloatArray
        session.run(feeds).use { results ->
            val out = results[0].value
            val raw: FloatArray = when (out) {
                is FloatArray -> out
                is Array<*> -> {
                    val first = out.firstOrNull()
                        ?: throw IllegalStateException("Empty ONNX output.")
                    if (first is FloatArray) first
                    else error("Unexpected ONNX output type: ${first::class.java}")
                }
                else -> error("Unexpected ONNX output type: ${out?.let { it::class.java }}")
            }
            resultVec = l2NormalizeInPlace(raw)
        }
        return resultVec
    }

    private fun l2NormalizeInPlace(vec: FloatArray): FloatArray {
        var sum = 0.0
        for (x in vec) sum += (x * x)
        val denom = sqrt(sum.toFloat()).coerceAtLeast(1e-12f)
        for (i in vec.indices) vec[i] = vec[i] / denom
        return vec
    }
}
