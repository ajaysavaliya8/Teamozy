// EmbeddingExtractor.kt
package com.example.teamozy.feature.face.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.util.Pools.SynchronizedPool
import com.example.teamozy.feature.face.util.DetectedFace
import com.example.teamozy.feature.face.util.FaceDetectOutcome
import com.example.teamozy.feature.face.util.FaceDetector
import com.example.teamozy.feature.face.util.ImagePreprocessor
import com.example.teamozy.feature.face.util.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import kotlin.math.sqrt

/**
 * Camera bitmap -> ML Kit face (eyes) -> aligned 112x112 -> NHWC [-1,1]
 * -> FaceNet (TFLite) -> L2-normalized 512-D embedding.
 *
 * Features:
 * - Safe single-Interpreter usage guarded by a Mutex (thread-safe .run)
 * - Retry-aware extract() and low-latency extractNoRetry()
 * - Optional GPU (preferred) or NNAPI delegate; CPU with XNNPACK as fallback
 * - Tiny ByteBuffer pool to reduce GC in camera loops
 * - Model I/O validation (shape + dtype)
 * - Production telemetry hooks
 * - Similarity helpers (cosine, L2) + extractOrNull()
 */

class EmbeddingExtractor private constructor(
    private val appContext: Context,
    private val numThreads: Int,
    private val enableGpu: Boolean,
    private val enableNnapi: Boolean,
    private val debugLogging: Boolean,
    private val telemetry: EmbeddingTelemetry?
) {

    companion object {
        private const val TAG = "EmbeddingExtractor"

        private const val MODEL_FILE = "facenet_512.tflite"
        private const val INPUT_SIZE = 160
        private const val INPUT_CHANNELS = 3
        private const val EMBEDDING_SIZE = 512
        private const val DEFAULT_MAX_RETRY = 3
        private const val INPUT_CAP_BYTES = 4 * INPUT_SIZE * INPUT_SIZE * INPUT_CHANNELS // float32

        @Volatile private var INSTANCE: EmbeddingExtractor? = null

        /**
         * Get singleton instance.
         *
         * @param numThreads number of CPU threads (>=1)
         * @param enableGpu enable GPU delegate if supported
         * @param enableNnapi enable NNAPI delegate (older devices; don't combine with GPU)
         * @param debugLogging verbose logs
         * @param telemetry optional hooks for metrics
         */
        fun getInstance(
            context: Context,
            numThreads: Int = 3,
            enableGpu: Boolean = false,
            enableNnapi: Boolean = false,
            debugLogging: Boolean = false,
            telemetry: EmbeddingTelemetry? = null
        ): EmbeddingExtractor {
            require(numThreads > 0) { "numThreads must be positive, got $numThreads" }
            require(!(enableGpu && enableNnapi)) {
                "Cannot enable both GPU and NNAPI delegates simultaneously"
            }
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmbeddingExtractor(
                    appContext = context.applicationContext,
                    numThreads = numThreads,
                    enableGpu = enableGpu,
                    enableNnapi = enableNnapi,
                    debugLogging = debugLogging,
                    telemetry = telemetry
                ).also { INSTANCE = it }
            }
        }

        // --- Verification helpers (shared across the app) ---

        /** Cosine similarity in [-1, 1]; 1.0 = identical. */
        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            require(a.size == EMBEDDING_SIZE && b.size == EMBEDDING_SIZE) {
                "Embedding sizes differ: ${a.size} vs ${b.size}"
            }
            var dot = 0f
            var na = 0f
            var nb = 0f
            for (i in 0 until EMBEDDING_SIZE) {
                val x = a[i]; val y = b[i]
                dot += x * y
                na += x * x
                nb += y * y
            }
            val denom = (kotlin.math.sqrt(na.toDouble()) * kotlin.math.sqrt(nb.toDouble()))
                .toFloat()
                .coerceAtLeast(1e-12f)
            return dot / denom
        }

        /** L2 distance in [0, 2] for unit vectors; 0.0 = identical. */
        fun l2Distance(a: FloatArray, b: FloatArray): Float {
            require(a.size == EMBEDDING_SIZE && b.size == EMBEDDING_SIZE) {
                "Embedding sizes differ: ${a.size} vs ${b.size}"
            }
            var s = 0f
            for (i in 0 until EMBEDDING_SIZE) {
                val d = a[i] - b[i]
                s += d * d
            }
            return kotlin.math.sqrt(s)
        }
    }

    // ---- Interpreter & delegates (re-creatable; not lazy to allow recovery) ----

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var nnapiDelegate: NnApiDelegate? = null

    // Serialize .run() calls (TFLite interpreter isn't thread-safe for concurrent runs).
    private val inferLock = Mutex()

    // Tiny pool to avoid frequent ByteBuffer allocations in camera loops.
    private val bufferPool = object : SynchronizedPool<ByteBuffer>(3) {
        override fun acquire(): ByteBuffer {
            return super.acquire() ?: ByteBuffer
                .allocateDirect(INPUT_CAP_BYTES)
                .order(ByteOrder.nativeOrder())
        }
    }

    private fun releaseBuffer(buf: ByteBuffer) {
        buf.clear()
        bufferPool.release(buf)
    }

    // Face detector with quality checks
    private val faceDetector by lazy {
        FaceDetector(
            context = appContext,
            debugLogging = debugLogging
        )
    }

    // ===== Public API =====

    /**
     * Extract a 512-D embedding with internal retry to smooth transient issues.
     */
    suspend fun extract(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        maxAttempts: Int = DEFAULT_MAX_RETRY
    ): FloatArray = withContext(Dispatchers.Default) {
        require(!bitmap.isRecycled) { "Input bitmap has been recycled" }

        val t0 = System.currentTimeMillis()
        try {
            when (val outcome = faceDetector.detectBestFaceRetry(
                bitmap = bitmap,
                rotationDegrees = rotationDegrees,
                maxAttempts = maxAttempts
            )) {
                is FaceDetectOutcome.Success -> {
                    val emb = processDetectedFace(bitmap, outcome.face)
                    val dt = System.currentTimeMillis() - t0
                    telemetry?.onExtractionComplete(dt, true)
                    logD("extract() success in ${dt} ms")
                    emb
                }
                is FaceDetectOutcome.Rejected -> {
                    val dt = System.currentTimeMillis() - t0
                    telemetry?.onExtractionComplete(dt, false)
                    logW("Face rejected: ${outcome.reason}")
                    throw IllegalStateException(outcome.reason.userMessage())
                }
                is FaceDetectOutcome.Failure -> {
                    val dt = System.currentTimeMillis() - t0
                    telemetry?.onExtractionComplete(dt, false)
                    logE("Face detection failure", outcome.error)
                    throw outcome.error
                }
            }
        } catch (e: Exception) {
            val dt = System.currentTimeMillis() - t0
            telemetry?.onExtractionComplete(dt, false)
            logE("extract() failed in ${dt} ms", e)
            throw e
        }
    }

    /**
     * Convenience wrapper: returns null instead of throwing on rejection/failure.
     * Handy for camera loops where you just want to skip bad frames quietly.
     */
    suspend fun extractOrNull(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        maxAttempts: Int = DEFAULT_MAX_RETRY
    ): FloatArray? = try {
        extract(bitmap, rotationDegrees, maxAttempts)
    } catch (_: Throwable) {
        null
    }

    /**
     * Variant without retry; use when upstream already handles rate limiting
     * and you want immediate, low-latency results.
     */
    suspend fun extractNoRetry(
        bitmap: Bitmap,
        rotationDegrees: Int = 0
    ): FloatArray = withContext(Dispatchers.Default) {
        require(!bitmap.isRecycled) { "Input bitmap has been recycled" }
        when (val outcome = faceDetector.detectBestFace(bitmap, rotationDegrees)) {
            is FaceDetectOutcome.Success -> processDetectedFace(bitmap, outcome.face)
            is FaceDetectOutcome.Rejected -> throw IllegalStateException(outcome.reason.userMessage())
            is FaceDetectOutcome.Failure -> throw outcome.error
        }
    }

    /**
     * Optional: run one dummy inference to avoid first-call latency.
     */
    suspend fun warmUp(): Boolean = withContext(Dispatchers.Default) {
        logD("warmUp() starting…")
        return@withContext runCatching {
            val dummy = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
            val buf = bufferPool.acquire()
            val out = Array(1) { FloatArray(EMBEDDING_SIZE) }
            try {
                fillBufferMinus1to1(dummy, buf)
                runInferenceSafely(buf, out)
            } finally {
                releaseBuffer(buf)
                dummy.recycle()
            }
        }.onSuccess { logI("warmUp() done") }
            .onFailure { logW("warmUp() failed: ${it.message}") }
            .isSuccess
    }

    /**
     * Release all native resources.
     */
    fun close() {
        logD("Closing EmbeddingExtractor…")
        faceDetector.close()
        releaseInterpreter()
        logD("EmbeddingExtractor closed")
    }

    // ===== Internal helpers =====

    private suspend fun processDetectedFace(
        source: Bitmap,
        face: DetectedFace
    ): FloatArray {
        require(!source.isRecycled) { "Source bitmap has been recycled" }

        // Align & crop to model input size
        val aligned = ImagePreprocessor.cropAndAlign(
            source = source,
            faceRect = face.boundingBox,
            leftEye = face.leftEye,
            rightEye = face.rightEye,
            targetSize = INPUT_SIZE
        )

        // Ensure ARGB_8888 for stable pixel reads; copy if needed
        val argb = if (aligned.config != Bitmap.Config.ARGB_8888) {
            aligned.copy(Bitmap.Config.ARGB_8888, false).also { aligned.recycle() }
        } else aligned

        // Prepare pooled input buffer ([-1, 1], NHWC)
        val inputBuffer = bufferPool.acquire()
        fillBufferMinus1to1(argb, inputBuffer)

        // Free temp bitmap ASAP
        argb.recycle()

        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
        try {
            runInferenceSafely(inputBuffer, output)
        } finally {
            releaseBuffer(inputBuffer)
        }

        return l2NormalizeInPlace(output[0])
    }

    /**
     * Fill a direct Float32 buffer with pixels in NHWC order, range [-1, 1].
     */
    private fun fillBufferMinus1to1(bm: Bitmap, buf: ByteBuffer) {
        val w = bm.width
        val h = bm.height
        require(w == INPUT_SIZE && h == INPUT_SIZE) {
            "Expected ${INPUT_SIZE}x${INPUT_SIZE}, got ${w}x${h}"
        }
        require(buf.capacity() >= INPUT_CAP_BYTES) {
            "Input buffer too small: ${buf.capacity()} < $INPUT_CAP_BYTES"
        }

        buf.clear()

        val pixels = IntArray(w * h)
        bm.getPixels(pixels, 0, w, 0, 0, w, h)

        var idx = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = pixels[idx++]
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                buf.putFloat((r / 127.5f) - 1f)
                buf.putFloat((g / 127.5f) - 1f)
                buf.putFloat((b / 127.5f) - 1f)
            }
        }
        buf.rewind()
    }

    /**
     * L2 normalize embedding to unit length.
     */
    private fun l2NormalizeInPlace(vec: FloatArray): FloatArray {
        var s = 0.0
        for (x in vec) s += x * x
        val d = sqrt(s).toFloat().coerceAtLeast(1e-12f)
        for (i in vec.indices) vec[i] /= d
        return vec
    }

    // --- Inference path with one-shot recovery if the interpreter got invalidated ---

    private suspend fun runInferenceSafely(
        input: ByteBuffer,
        output: Array<FloatArray>
    ) {
        try {
            inferLock.withLock { getInterpreter().run(input, output) }
        } catch (e: IllegalStateException) {
            // Rare: if native interpreter becomes invalid (e.g., after delegate issue), rebuild once.
            logW("Interpreter invalid state: ${e.message}. Recreating and retrying once…")
            rebuildInterpreter()
            inferLock.withLock { getInterpreter().run(input, output) }
        }
    }

    // --- Interpreter lifecycle ---

    @Synchronized
    private fun getInterpreter(): Interpreter {
        if (interpreter == null) {
            interpreter = buildInterpreter()
        }
        return interpreter!!
    }

    @Synchronized
    private fun rebuildInterpreter() {
        releaseInterpreter()
        interpreter = buildInterpreter()
    }

    @Synchronized
    private fun releaseInterpreter() {
        try { interpreter?.close() } catch (_: Exception) {}
        interpreter = null
        try { gpuDelegate?.close() } catch (_: Exception) {}
        gpuDelegate = null
        try { nnapiDelegate?.close() } catch (_: Exception) {}
        nnapiDelegate = null
    }

    private fun buildInterpreter(): Interpreter {
        logD("Initializing TFLite (threads=$numThreads, gpu=$enableGpu, nnapi=$enableNnapi)")
        requireModelPresent()

        val mm: MappedByteBuffer = FileUtil.loadMappedFile(appContext, MODEL_FILE)
        val opts = Interpreter.Options().apply {
            setNumThreads(numThreads)

            // Faster CPU backend; safe no-op if unsupported
            @Suppress("DEPRECATION")
            setUseXNNPACK(true)

            if (enableGpu && isGpuCompatible()) {
                try {
                    gpuDelegate = GpuDelegate()
                    addDelegate(gpuDelegate)
                    logI("GPU delegate enabled")
                } catch (e: Exception) {
                    logW("GPU init failed, falling back to CPU: ${e.message}")
                }
            } else if (enableNnapi) {
                try {
                    nnapiDelegate = NnApiDelegate()
                    addDelegate(nnapiDelegate)
                    logI("NNAPI delegate enabled")
                } catch (e: Exception) {
                    logW("NNAPI init failed: ${e.message}")
                }
            }
        }

        val interp = Interpreter(mm, opts)
        validateModelIO(interp)

        telemetry?.onModelInitialized(
            gpu = (gpuDelegate != null),
            xnnpack = true
        )

        logI("TFLite interpreter initialized successfully")
        return interp
    }

    private fun requireModelPresent() {
        if (!modelFileExists()) {
            throw IOException("Model file '$MODEL_FILE' not found in assets/")
        }
    }

    private fun modelFileExists(): Boolean = try {
        appContext.assets.open(MODEL_FILE).use { true }
    } catch (_: IOException) {
        false
    }

    private fun isGpuCompatible(): Boolean = try {
        CompatibilityList().isDelegateSupportedOnThisDevice
    } catch (e: Exception) {
        logW("GPU compatibility check failed: ${e.message}")
        false
    }

    /**
     * Validate model input/output shapes and data types at runtime.
     * Fails fast if wrong model is bundled.
     */
    private fun validateModelIO(interp: Interpreter) {
        // Input: [1,112,112,3] float32
        val inTensor = interp.getInputTensor(0)
        val inShape = inTensor.shape()
        val inType = inTensor.dataType()
        require(inShape.size == 4 && inShape[0] == 1 &&
                inShape[1] == INPUT_SIZE && inShape[2] == INPUT_SIZE &&
                inShape[3] == INPUT_CHANNELS) {
            "Invalid input shape: ${inShape.contentToString()}, expected [1,$INPUT_SIZE,$INPUT_SIZE,$INPUT_CHANNELS]"
        }
        require(inType == DataType.FLOAT32) {
            "Invalid input type: $inType, expected FLOAT32"
        }

        // Output: [1,512] float32
        val outTensor = interp.getOutputTensor(0)
        val outShape = outTensor.shape()
        val outType = outTensor.dataType()
        require(outShape.size == 2 && outShape[0] == 1 &&
                outShape[1] == EMBEDDING_SIZE) {
            "Invalid output shape: ${outShape.contentToString()}, expected [1,$EMBEDDING_SIZE]"
        }
        require(outType == DataType.FLOAT32) {
            "Invalid output type: $outType, expected FLOAT32"
        }

        logD("Model validation passed: input=${inShape.contentToString()}/$inType, output=${outShape.contentToString()}/$outType")
    }

    // ---- Logging ----

    private inline fun logD(msg: String) { if (debugLogging || Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, msg) }
    private inline fun logI(msg: String) { if (debugLogging || Log.isLoggable(TAG, Log.INFO)) Log.i(TAG, msg) }
    private inline fun logW(msg: String) { if (debugLogging || Log.isLoggable(TAG, Log.WARN)) Log.w(TAG, msg) }
    private inline fun logE(msg: String, tr: Throwable? = null) { if (debugLogging || Log.isLoggable(TAG, Log.ERROR)) Log.e(TAG, msg, tr) }
}

/** Optional telemetry interface for production metrics and analytics. */
interface EmbeddingTelemetry {
    /**
     * Called after each extraction attempt (success or failure).
     * @param durationMs total duration including face detection and inference
     * @param success true if embedding was successfully extracted
     */
    fun onExtractionComplete(durationMs: Long, success: Boolean)

    /**
     * Called once when TFLite model is initialized.
     * @param gpu true if GPU delegate was successfully enabled
     * @param xnnpack true if XNNPACK CPU optimization was requested
     */
    fun onModelInitialized(gpu: Boolean, xnnpack: Boolean)
}
