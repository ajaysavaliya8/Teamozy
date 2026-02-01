@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.face.presentation

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

private const val TAG = "FaceCapture"
private const val RETRY_INTERVAL_MS = 800L
private const val TOTAL_TIME_SECONDS = 60

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
    val activity = context as? Activity

    var errorText by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("Position your face in the frame") }
    var isProcessing by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(0) }

    // Camera references
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val analysisRef = remember { mutableStateOf<ImageAnalysis?>(null) }

    // Cancel dialog state
    var showCancelDialog by remember { mutableStateOf(false) }

    // Brightness control - Set to 100% when screen opens
    DisposableEffect(Unit) {
        val window = activity?.window
        val layoutParams = window?.attributes
        val originalBrightness = layoutParams?.screenBrightness ?: -1f

        // Set brightness to maximum (100%)
        layoutParams?.screenBrightness = 1.0f
        window?.attributes = layoutParams
        Log.d(TAG, "🔆 Brightness set to 100%")

        onDispose {
            // Restore original brightness
            layoutParams?.screenBrightness = originalBrightness
            window?.attributes = layoutParams
            Log.d(TAG, "🔅 Brightness restored to original: $originalBrightness")
        }
    }

    // Cleanup function
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

    // Handle system back button
    // BackHandler moved inside Dialog below

    // DisposableEffect tied to generation
    DisposableEffect(generation) {
        Log.d(TAG, "🟢 FaceCaptureScreen ENTERED (generation=$generation)")
        onDispose {
            Log.d(TAG, "🔴 FaceCaptureScreen DISPOSING (generation=$generation)")
            cleanupCameraSync()
        }
    }

    // Timer
    var secondsLeft by remember { mutableIntStateOf(TOTAL_TIME_SECONDS) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    if (secondsLeft <= 0) {
        cleanupCameraSync()
        onDismiss()
    }

    var reason by remember { mutableStateOf("") }

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    "Cancel Verification?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to cancel the face verification?")
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
                    Text("Continue")
                }
            }
        )
    }

    // Full screen dialog
    Dialog(
        onDismissRequest = { if (!isSubmitting) showCancelDialog = true },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        BackHandler {
            if (!isSubmitting) {
                showCancelDialog = true
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close button
                    IconButton(
                        onClick = { if (!isSubmitting) showCancelDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Title
                    Text(
                        text = "Face Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Timer
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (secondsLeft <= 10)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${secondsLeft}s",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = if (secondsLeft <= 10)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Camera Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Camera Preview with rounded corners
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Camera Preview
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp)),
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
                                            cameraProvider.unbindAll()
                                            Thread.sleep(100)

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
                                        } catch (e: Exception) {
                                            Log.e(TAG, "❌ Camera setup error", e)
                                            errorText = "Camera initialization failed"
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))

                                    previewView
                                }
                            )

                            // Face oval guide overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .fillMaxHeight(0.6f)
                                    .border(
                                        width = 3.dp,
                                        color = if (isProcessing) Color(0xFF4CAF50) else Color.White,
                                        shape = RoundedCornerShape(50)
                                    )
                            )

                            // Processing indicator overlay
                            if (isProcessing) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 16.dp)
                                        .background(
                                            Color(0xFF4CAF50).copy(alpha = 0.9f),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Verifying...",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Section - fixed height to prevent camera resizing when error text appears
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Face icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Face,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Status text
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Error messages
                    serverError?.let { error ->
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    errorText?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Reason field (for late check-in, etc.)
                    if (showReasonField) {
                        Spacer(Modifier.height(16.dp))

                        reasonMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(8.dp))
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

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = !isSubmitting,
                                onClick = { showCancelDialog = true }
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = !isSubmitting && reason.isNotBlank(),
                                onClick = { onSubmit(reason.trim()) }
                            ) {
                                Text(if (isSubmitting) "Submitting…" else "Submit")
                            }
                        }
                    }

                    // Processing indicator
                    if (isSubmitting) {
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
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
    onUpdateStatus("Verifying face...")

    try {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = imageProxy.toBitmap()
            .rotateAndMirror(rotation, mirror = true)
            .copy(Bitmap.Config.ARGB_8888, false)

        imageProxy.close()

        Log.d(TAG, "📸 Capturing frame (generation=$generation)")

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
