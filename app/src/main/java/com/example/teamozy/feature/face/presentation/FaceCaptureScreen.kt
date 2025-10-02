@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.face.presentation

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as JetColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private const val TAG = "FaceCapture"
private const val RETRY_INTERVAL_MS = 800L  // Try every 800ms

@Composable
fun FaceCaptureScreen(
    onDismiss: () -> Unit,
    onCaptured: (jpeg: ByteArray) -> Unit,
    onBitmapCaptured: (Bitmap) -> Unit,
    showReasonField: Boolean,
    reasonMessage: String?,
    isSubmitting: Boolean,
    onSubmit: (reason: String?) -> Unit,
    serverError: String?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var errorText by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("Position your face in the frame") }
    var isProcessing by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(0) }

    // Camera references for cleanup
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val analysisRef = remember { mutableStateOf<ImageAnalysis?>(null) }

    fun cleanupCamera() {
        Log.d(TAG, "Cleaning up camera")
        try {
            analysisRef.value?.clearAnalyzer()
            analysisRef.value = null
            cameraProviderRef.value?.unbindAll()
            cameraProviderRef.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose { cleanupCamera() }
    }

    // 60-second timeout with auto-close
    var secondsLeft by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        Log.d(TAG, "Timeout reached - closing")
        cleanupCamera()
        onDismiss()
    }

    var reason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Verification") },
                navigationIcon = {
                    IconButton(onClick = {
                        cleanupCamera()
                        onDismiss()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            ) {
                AndroidView(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(18.dp)),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val executor = Executors.newSingleThreadExecutor()
                        var lastAttemptTime = 0L

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                cameraProviderRef.value = cameraProvider

                                val preview = Preview.Builder()
                                    .setTargetResolution(Size(720, 1280))
                                    .build()
                                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                                val analysis = ImageAnalysis.Builder()
                                    .setTargetResolution(Size(720, 1280))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                analysisRef.value = analysis

                                analysis.setAnalyzer(executor) { imageProxy ->
                                    try {
                                        val now = System.currentTimeMillis()

                                        // Skip if already processing
                                        if (isProcessing) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        // Rate limit: only attempt every RETRY_INTERVAL_MS
                                        if (now - lastAttemptTime < RETRY_INTERVAL_MS) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        lastAttemptTime = now
                                        isProcessing = true
                                        attemptCount++
                                        statusText = "Verifying... (attempt $attemptCount)"

                                        val rotation = imageProxy.imageInfo.rotationDegrees
                                        val bitmap = imageProxy.toBitmap()
                                            .rotateAndMirror(rotation, mirror = true)
                                            .copy(Bitmap.Config.ARGB_8888, false)

                                        imageProxy.close()

                                        Log.d(TAG, "Attempt $attemptCount - capturing frame")

                                        // Pass bitmap to HomePage for verification
                                        onBitmapCaptured(bitmap)

                                        // Reset processing flag after a short delay
                                        // HomePage will handle success/failure and close screen if matched
                                        kotlinx.coroutines.GlobalScope.launch {
                                            kotlinx.coroutines.delay(500)
                                            isProcessing = false
                                        }

                                    } catch (e: Exception) {
                                        Log.e(TAG, "Frame processing error", e)
                                        errorText = e.message
                                        isProcessing = false
                                    } finally {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_FRONT_CAMERA,
                                        preview,
                                        analysis
                                    )
                                    Log.d(TAG, "Camera started - continuous verification mode")
                                } catch (e: Exception) {
                                    errorText = e.message ?: "Failed to start camera"
                                }

                            } catch (e: Exception) {
                                errorText = e.message ?: "Camera initialization failed"
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // Overlay frame
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(8.dp)
                ) {
                    drawRoundRect(
                        color = JetColor(0xFFFFC107),
                        size = size,
                        style = Stroke(width = 10f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(36f, 36f)
                    )
                }

                // Status chip
                ElevatedCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(10.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(statusText)
                        if (attemptCount > 0) {
                            Text(
                                "Attempts: $attemptCount",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { (60 - secondsLeft) / 60f },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Time remaining: %02d:%02d".format(secondsLeft / 60, secondsLeft % 60))
                if (attemptCount > 0) {
                    Text("Attempts: $attemptCount", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (errorText != null) {
                Text(errorText!!, color = MaterialTheme.colorScheme.error)
            }

            if (!serverError.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        serverError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Keep your face visible",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• Look at the camera\n• Good lighting\n• Hold steady\n• Will retry automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Reason field (for violation flow - not used in normal verification)
            if (showReasonField) {
                if (!reasonMessage.isNullOrBlank()) {
                    Text(reasonMessage!!, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Reason") },
                    singleLine = false,
                    minLines = 2,
                    enabled = !isSubmitting
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting,
                        onClick = {
                            cleanupCamera()
                            onDismiss()
                        }
                    ) { Text("Cancel") }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting && reason.isNotBlank(),
                        onClick = { onSubmit(reason.trim()) }
                    ) { Text(if (isSubmitting) "Submitting…" else "Submit") }
                }
            }
        }
    }
}

private fun Bitmap.rotateAndMirror(rotationDegrees: Int, mirror: Boolean = true): Bitmap {
    val m = Matrix()
    if (rotationDegrees != 0) m.postRotate(rotationDegrees.toFloat())
    if (mirror) m.postScale(-1f, 1f)
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}