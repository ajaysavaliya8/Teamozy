package com.hrms.jeejateamozy.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.camera.CameraManager
import com.hrms.jeejateamozy.camera.CameraState
import com.hrms.jeejateamozy.camera.FaceAnalysisResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Production-level ViewModel for Home/Check-in functionality
 * Manages state, camera lifecycle, and business logic
 */
class HomeViewModel : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val CLEANUP_DELAY_MS = 300L
        private const val DEBOUNCE_DELAY_MS = 500L
    }

    // Camera Manager instance
    private val cameraManager = CameraManager.getInstance()

    // UI State
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Face verification state
    private val _faceVerificationState = MutableStateFlow(FaceVerificationState())
    val faceVerificationState: StateFlow<FaceVerificationState> = _faceVerificationState.asStateFlow()

    // Generation counter for forced recomposition
    private var verificationGeneration = 0

    // Debouncing flag
    private var isProcessing = false

    init {
        // Observe camera state
        viewModelScope.launch {
            cameraManager.cameraState.collect { cameraState ->
                Log.d(TAG, "Camera state changed: $cameraState")
                updateCameraState(cameraState)
            }
        }

        // Observe analysis results
        viewModelScope.launch {
            cameraManager.analysisResult.collect { result ->
                result?.let { handleAnalysisResult(it) }
            }
        }
    }

    /**
     * Start check-in process
     */
    fun startCheckIn(context: Context) {
        viewModelScope.launch {
            if (isProcessing) {
                Log.w(TAG, "Check-in already in progress, ignoring")
                return@launch
            }

            isProcessing = true

            try {
                Log.d(TAG, "╔═══════════════════════════════════════╗")
                Log.d(TAG, "║   CHECK IN BUTTON CLICKED             ║")
                Log.d(TAG, "╚═══════════════════════════════════════╝")

                // Set loading state
                _uiState.update { it.copy(isLoading = true) }

                // Simulate API call to check-in endpoint
                // Replace with your actual API call
                delay(500)

                // Mock response - replace with actual API response
                val response = CheckInResponse(
                    faceVerificationRequired = true,
                    minimumQualityScore = 0.55f,
                    tToken = "mock_token_12345",
                    isLate = true,
                    isOutOfRange = true,
                    lateReasonRequired = true,
                    outOfRangeReasonRequired = true,
                    message = "Check-in initiated"
                )

                handleCheckInResponse(response)

            } catch (e: Exception) {
                Log.e(TAG, "Error during check-in", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Check-in failed"
                    )
                }
            } finally {
                delay(DEBOUNCE_DELAY_MS)
                isProcessing = false
            }
        }
    }

    /**
     * Handle check-in API response
     */
    private fun handleCheckInResponse(response: CheckInResponse) {
        Log.d(TAG, "Check-in response: faceVerificationRequired=${response.faceVerificationRequired}")

        if (response.faceVerificationRequired) {
            // Increment generation counter for forced recomposition
            verificationGeneration++

            _faceVerificationState.update {
                FaceVerificationState(
                    isActive = true,
                    minimumQualityScore = response.minimumQualityScore,
                    tToken = response.tToken,
                    generation = verificationGeneration,
                    isLate = response.isLate,
                    isOutOfRange = response.isOutOfRange,
                    lateReasonRequired = response.lateReasonRequired,
                    outOfRangeReasonRequired = response.outOfRangeReasonRequired
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    showFaceVerification = true
                )
            }

            Log.d(TAG, "✅ Face verification screen activated (generation=$verificationGeneration)")
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Cancel face verification and cleanup
     */
    fun cancelFaceVerification() {
        viewModelScope.launch {
            Log.d(TAG, "🔙 Cancelling face verification...")

            if (isProcessing) {
                Log.w(TAG, "Still processing, adding delay before cleanup")
                delay(DEBOUNCE_DELAY_MS)
            }

            // Cleanup camera resources
            cameraManager.cleanupWithDelay(CLEANUP_DELAY_MS)

            // Reset all states
            _faceVerificationState.update { FaceVerificationState() }
            _uiState.update {
                it.copy(
                    showFaceVerification = false,
                    error = null
                )
            }

            Log.d(TAG, "✅ Face verification cancelled and cleaned up")
        }
    }

    /**
     * Handle successful face capture
     */
    fun onFaceCaptured(imageBase64: String, qualityScore: Float) {
        viewModelScope.launch {
            Log.d(TAG, "✅ Face captured successfully with quality: $qualityScore")

            _faceVerificationState.update {
                it.copy(
                    capturedImage = imageBase64,
                    captureSuccess = true
                )
            }

            // Proceed to next step (signature, reasons, etc.)
            proceedToNextStep()
        }
    }

    /**
     * Handle face capture error
     */
    fun onFaceCaptureError(error: String) {
        Log.e(TAG, "❌ Face capture error: $error")

        _faceVerificationState.update {
            it.copy(error = error)
        }
    }

    /**
     * Retry face verification
     */
    fun retryFaceVerification() {
        viewModelScope.launch {
            Log.d(TAG, "🔄 Retrying face verification...")

            // Cleanup and reset
            cameraManager.cleanupWithDelay(CLEANUP_DELAY_MS)

            // Increment generation for forced recomposition
            verificationGeneration++

            _faceVerificationState.update {
                it.copy(
                    error = null,
                    captureSuccess = false,
                    generation = verificationGeneration
                )
            }

            Log.d(TAG, "✅ Face verification reset (generation=$verificationGeneration)")
        }
    }

    /**
     * Update camera state
     */
    private fun updateCameraState(cameraState: CameraState) {
        _faceVerificationState.update {
            it.copy(
                cameraReady = cameraState is CameraState.Ready,
                cameraError = if (cameraState is CameraState.Error) cameraState.message else null
            )
        }
    }

    /**
     * Handle analysis result from camera
     */
    private fun handleAnalysisResult(result: FaceAnalysisResult) {
        when {
            result.hasMultipleFaces -> {
                _faceVerificationState.update {
                    it.copy(analysisMessage = "Multiple faces detected. Show only your face.")
                }
            }
            result.noFaceDetected -> {
                _faceVerificationState.update {
                    it.copy(analysisMessage = "No face detected. Position your face in the frame.")
                }
            }
            result.isQualityGood && result.capturedImageBase64 != null -> {
                // Quality good and image captured
                onFaceCaptured(result.capturedImageBase64, result.qualityScore ?: 0f)
            }
            result.qualityScore != null -> {
                _faceVerificationState.update {
                    it.copy(
                        currentQualityScore = result.qualityScore,
                        analysisMessage = "Adjusting... Quality: ${(result.qualityScore * 100).toInt()}%"
                    )
                }
            }
        }
    }

    /**
     * Proceed to next step after face capture
     */
    private fun proceedToNextStep() {
        // Implement your next step logic here
        // For example: show signature screen, show reason dialogs, etc.
        Log.d(TAG, "Proceeding to next step...")

        _uiState.update {
            it.copy(showFaceVerification = false)
        }

        // Show signature or other screens as needed
    }

    /**
     * Reset error state
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
        _faceVerificationState.update { it.copy(error = null, cameraError = null) }
    }

    /**
     * Cleanup on ViewModel cleared
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, cleaning up camera...")
        cameraManager.cleanupCameraResourcesSync()
    }
}

/**
 * UI State for Home Screen
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val showFaceVerification: Boolean = false,
    val error: String? = null
)

/**
 * Face Verification State
 */
data class FaceVerificationState(
    val isActive: Boolean = false,
    val minimumQualityScore: Float = 0.55f,
    val tToken: String? = null,
    val generation: Int = 0,
    val isLate: Boolean = false,
    val isOutOfRange: Boolean = false,
    val lateReasonRequired: Boolean = false,
    val outOfRangeReasonRequired: Boolean = false,
    val cameraReady: Boolean = false,
    val cameraError: String? = null,
    val capturedImage: String? = null,
    val captureSuccess: Boolean = false,
    val error: String? = null,
    val currentQualityScore: Float = 0f,
    val analysisMessage: String? = null
)

/**
 * Check-in API Response model
 */
data class CheckInResponse(
    val faceVerificationRequired: Boolean,
    val minimumQualityScore: Float,
    val tToken: String,
    val isLate: Boolean,
    val isOutOfRange: Boolean,
    val lateReasonRequired: Boolean,
    val outOfRangeReasonRequired: Boolean,
    val message: String
)