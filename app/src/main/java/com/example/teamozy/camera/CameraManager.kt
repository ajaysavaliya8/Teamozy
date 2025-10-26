package com.example.teamozy.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Production-level Camera Manager
 * Handles all camera operations with guaranteed cleanup and proper lifecycle management
 */
class CameraManager private constructor() {

    companion object {
        private const val TAG = "CameraManager"

        @Volatile
        private var instance: CameraManager? = null

        fun getInstance(): CameraManager {
            return instance ?: synchronized(this) {
                instance ?: CameraManager().also { instance = it }
            }
        }
    }

    // State management
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _analysisResult = MutableStateFlow<FaceAnalysisResult?>(null)
    val analysisResult: StateFlow<FaceAnalysisResult?> = _analysisResult.asStateFlow()

    // Camera resources
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null

    // State tracking
    private var isInitialized = false
    private var isBusy = false

    /**
     * Initialize camera with guaranteed cleanup of previous session
     */
    suspend fun initializeCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
        lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    ): Result<Unit> = suspendCoroutine { continuation ->

        if (isBusy) {
            Log.w(TAG, "Camera initialization already in progress")
            continuation.resume(Result.failure(Exception("Camera is busy")))
            return@suspendCoroutine
        }

        isBusy = true
        _cameraState.value = CameraState.Initializing

        try {
            Log.d(TAG, "Starting camera initialization...")

            // Step 1: Cleanup any existing camera session
            cleanupCameraResourcesSync()

            // Step 2: Create new executor
            cameraExecutor = Executors.newSingleThreadExecutor()

            // Step 3: Get camera provider
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                try {
                    // Get provider
                    cameraProvider = cameraProviderFuture.get()

                    // Unbind all use cases before rebinding
                    cameraProvider?.unbindAll()

                    // Small delay to ensure cleanup completes
                    Thread.sleep(100)

                    // Build preview
                    preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    // Build image analysis
                    imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor!!, analyzer)
                        }

                    // Select camera
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    // Bind use cases
                    camera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )

                    isInitialized = true
                    isBusy = false
                    _cameraState.value = CameraState.Ready

                    Log.d(TAG, "✅ Camera initialized successfully")
                    continuation.resume(Result.success(Unit))

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Camera initialization failed", e)
                    cleanupCameraResourcesSync()
                    isBusy = false
                    _cameraState.value = CameraState.Error(e.message ?: "Unknown error")
                    continuation.resume(Result.failure(e))
                }
            }, cameraExecutor ?: Executors.newSingleThreadExecutor())

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start camera initialization", e)
            cleanupCameraResourcesSync()
            isBusy = false
            _cameraState.value = CameraState.Error(e.message ?: "Unknown error")
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * Cleanup camera resources synchronously - GUARANTEED cleanup
     */
    fun cleanupCameraResourcesSync() {
        Log.d(TAG, "🧹 Starting camera cleanup...")

        try {
            // Clear analysis result
            _analysisResult.value = null

            // Stop image analysis
            imageAnalysis?.clearAnalyzer()
            imageAnalysis = null

            // Clear preview
            preview = null

            // Unbind all use cases
            cameraProvider?.unbindAll()

            // Release camera
            camera = null

            // Shutdown executor
            cameraExecutor?.shutdown()
            cameraExecutor = null

            // Clear provider reference
            cameraProvider = null

            isInitialized = false
            _cameraState.value = CameraState.Idle

            Log.d(TAG, "✅ Camera cleanup completed")

        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error during camera cleanup", e)
        }
    }

    /**
     * Cleanup with small delay to ensure hardware release
     */
    suspend fun cleanupWithDelay(delayMs: Long = 200) {
        cleanupCameraResourcesSync()
        kotlinx.coroutines.delay(delayMs)
    }

    /**
     * Update analysis result
     */
    fun updateAnalysisResult(result: FaceAnalysisResult) {
        _analysisResult.value = result
    }

    /**
     * Reset analysis result
     */
    fun resetAnalysisResult() {
        _analysisResult.value = null
    }

    /**
     * Check if camera is ready
     */
    fun isReady(): Boolean = isInitialized && !isBusy

    /**
     * Check if camera is busy
     */
    fun isCameraBusy(): Boolean = isBusy
}

/**
 * Camera state sealed class
 */
sealed class CameraState {
    object Idle : CameraState()
    object Initializing : CameraState()
    object Ready : CameraState()
    data class Error(val message: String) : CameraState()
}

/**
 * Face analysis result
 */
data class FaceAnalysisResult(
    val hasMultipleFaces: Boolean = false,
    val noFaceDetected: Boolean = false,
    val qualityScore: Float? = null,
    val isQualityGood: Boolean = false,
    val capturedImageBase64: String? = null,
    val errorMessage: String? = null
)