@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.face.presentation

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
import java.util.concurrent.Executors

private const val TAG = "FaceCapture"
private const val RETRY_INTERVAL_MS = 800L

@Composable
fun FaceCaptureScreen(
    generation: Int = 0,
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

    // Camera references
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val analysisRef = remember { mutableStateOf<ImageAnalysis?>(null) }

    // ============================================
    // BACK CONFIRMATION DIALOG STATE
    // ============================================
    var showCancelDialog by remember { mutableStateOf(false) }

    // ✨ Cleanup function - defined BEFORE use
    fun cleanupCameraSync() {
        Log.d(TAG, "🧹 Starting camera cleanup (generation=$generation)")
        try {
            analysisRef.value?.clearAnalyzer()
            analysisRef.value = null
            cameraProviderRef.value?.unbindAll()
            cameraProviderRef.value = null
            Log.d(TAG, "✅ Camera cleanup completed (generation=$generation)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cleanup error (generation=$generation)", e)
        }
    }

    // ✨ Handle system back button - Show confirmation dialog
    BackHandler {
        Log.d(TAG, "⬅️ System back button pressed (generation=$generation)")
        if (!isSubmitting) {
            showCancelDialog = true
        }
        // If submitting, ignore back press
    }

    // ✨ DisposableEffect tied to generation
    DisposableEffect(generation) {
        Log.d(TAG, "🟢 FaceCaptureScreen ENTERED (generation=$generation)")

        onDispose {
            Log.d(TAG, "🔴 FaceCaptureScreen DISPOSING (generation=$generation)")
            cleanupCameraSync()
        }
    }

    // 60-second timeout
    var secondsLeft by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    if (secondsLeft <= 0) {
        Log.d(TAG, "⏱️ Timeout reached (generation=$generation)")
        cleanupCameraSync()
        onDismiss()
    }

    var reason by remember { mutableStateOf("") }

    // ============================================
    // CANCEL CONFIRMATION DIALOG
    // ============================================
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Face Verification?") },
            text = {
                Text("Are you sure you want to cancel the face verification process?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        cleanupCameraSync()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text("Continue Verification")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Verification") },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "⬅️ TopBar back button pressed (generation=$generation)")
                        if (!isSubmitting) {
                            showCancelDialog = true
                        }
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
            // Timer display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (secondsLeft <= 10)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Time remaining: ${secondsLeft}s",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (secondsLeft <= 10)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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

                                // Clean slate
                                Log.d(TAG, "📷 Unbinding previous use cases (generation=$generation)")
                                cameraProvider.unbindAll()
                                Thread.sleep(100)

                                Log.d(TAG, "📷 Initializing camera (generation=$generation)")

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
                                        processFrame(
                                            imageProxy = imageProxy,
                                            isSubmitting = isSubmitting,
                                            isProcessing = isProcessing,
                                            lastAttemptTime = lastAttemptTime,
                                            onUpdateLastAttemptTime = { lastAttemptTime = it },
                                            onUpdateProcessing = { isProcessing = it },
                                            onUpdateAttemptCount = { attemptCount++ },
                                            onUpdateStatus = { statusText = it },
                                            onBitmapCaptured = onBitmapCaptured,
                                            onError = { errorText = it },
                                            generation = generation
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Analyzer error", e)
                                        imageProxy.close()
                                    }
                                }

                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    preview,
                                    analysis
                                )

                                Log.d(TAG, "✅ Camera bound successfully (generation=$generation)")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Camera setup error (generation=$generation)", e)
                                errorText = "Camera initialization failed: ${e.message}"
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // Oval guide overlay
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawOval(
                        color = JetColor.White,
                        style = Stroke(width = 4.dp.toPx()),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            size.width * 0.15f,
                            size.height * 0.1f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            size.width * 0.7f,
                            size.height * 0.8f
                        )
                    )
                }
            }

            // Status text
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge
            )

            // Error text
            errorText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Server error
            serverError?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Reason field (for late check-in, etc.)
            if (showReasonField) {
                reasonMessage?.let {
                    Text(text = it, style = MaterialTheme.typography.labelLarge)
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
                            showCancelDialog = true
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

private fun processFrame(
    imageProxy: ImageProxy,
    isSubmitting: Boolean,
    isProcessing: Boolean,
    lastAttemptTime: Long,
    onUpdateLastAttemptTime: (Long) -> Unit,
    onUpdateProcessing: (Boolean) -> Unit,
    onUpdateAttemptCount: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onBitmapCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit,
    generation: Int
) {
    val now = System.currentTimeMillis()

    if (isSubmitting) {
        imageProxy.close()
        return
    }

    if (isProcessing) {
        imageProxy.close()
        return
    }

    if (now - lastAttemptTime < RETRY_INTERVAL_MS) {
        imageProxy.close()
        return
    }

    onUpdateLastAttemptTime(now)
    onUpdateProcessing(true)
    onUpdateAttemptCount()

    val currentAttempt = ((now - lastAttemptTime) / RETRY_INTERVAL_MS).toInt() + 1
    onUpdateStatus("Verifying... (attempt $currentAttempt)")

    try {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = imageProxy.toBitmap()
            .rotateAndMirror(rotation, mirror = true)
            .copy(Bitmap.Config.ARGB_8888, false)

        imageProxy.close()

        Log.d(TAG, "📸 Attempt $currentAttempt - capturing frame (generation=$generation)")

        onBitmapCaptured(bitmap)
        onUpdateProcessing(false)

    } catch (e: Exception) {
        Log.e(TAG, "❌ Frame processing error (generation=$generation)", e)
        onError(e.message ?: "Processing error")
        onUpdateProcessing(false)
        try {
            imageProxy.close()
        } catch (e2: Exception) {
            Log.e(TAG, "❌ Error closing imageProxy", e2)
        }
    }
}

private fun Bitmap.rotateAndMirror(rotationDegrees: Int, mirror: Boolean = true): Bitmap {
    val m = Matrix()
    if (rotationDegrees != 0) m.postRotate(rotationDegrees.toFloat())
    if (mirror) m.postScale(-1f, 1f)
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}