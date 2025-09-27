@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.face.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

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

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // 2-minute auto-close
    var secondsLeft by remember { mutableStateOf(120) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) { delay(1000); secondsLeft-- }
        onDismiss()
    }

    // Auto-capture once, 5s after camera ready
    var autoTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(imageCapture) {
        if (imageCapture != null && !autoTriggered) {
            autoTriggered = true
            delay(5000)
            val cap = imageCapture
            if (cap != null && !isCapturing) {
                isCapturing = true
                captureToBytes(context, cap) { bytes, err ->
                    isCapturing = false
                    if (err != null) errorText = err
                    else if (bytes != null) {
                        onCaptured(bytes)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let(onBitmapCaptured)
                    }
                }
            }
        }
    }

    var reason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Verify") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
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
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder()
                                .setTargetResolution(Size(720, 1280))
                                .build()
                                .apply { setSurfaceProvider(previewView.surfaceProvider) }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    preview, capture
                                )
                                imageCapture = capture
                            } catch (t: Throwable) {
                                errorText = t.message ?: "Camera error"
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
                    val label =
                        when {
                            isCapturing -> "Capturing…"
                            !autoTriggered && imageCapture == null -> "Preparing camera…"
                            !autoTriggered -> "Auto-capturing in 5s…"
                            else -> "Processing…"
                        }
                    Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }

            LinearProgressIndicator(progress = { (120 - secondsLeft) / 120f }, modifier = Modifier.fillMaxWidth())
            Text("Auto Close in %02d:%02d".format(secondsLeft / 60, secondsLeft % 60))

            if (errorText != null) Text(errorText!!, color = MaterialTheme.colorScheme.error)

            if (!serverError.isNullOrBlank()) {
                Text(serverError, color = MaterialTheme.colorScheme.error)
            }


            // Reason + Submit + Cancel (only when redirect)
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
                        onClick = onDismiss
                    ) { Text("Cancel") }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting && reason.isNotBlank(),
                        onClick = { onSubmit(reason.trim()) }
                    ) { Text(if (isSubmitting) "Submitting…" else "Submit") }
                }
            } else {
                // No reason required: nothing to show; HomePage will auto-close on success.
            }
        }
    }
}

/* helpers */
private fun captureToBytes(
    context: Context,
    capture: ImageCapture,
    onResult: (bytes: ByteArray?, error: String?) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            try {
                val bmp = image.toBitmap()
                val bytes = bmp.compressToJpeg()
                onResult(bytes, null)
            } catch (t: Throwable) {
                onResult(null, t.message ?: "Capture failed")
            } finally { image.close() }
        }
        override fun onError(exception: ImageCaptureException) {
            onResult(null, exception.message ?: "Capture error")
        }
    })
}

private fun ImageProxy.toBitmap(): Bitmap {
    val nv21 = yuv420ToNv21(this)
    val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuv.compressToJpeg(Rect(0, 0, width, height), 90, out)
    val jpegBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
}

private fun Bitmap.compressToJpeg(quality: Int = 90): ByteArray {
    val bos = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, bos)
    return bos.toByteArray()
}

private fun yuv420ToNv21(image: ImageProxy): ByteArray {
    val yBuffer: ByteBuffer = image.planes[0].buffer
    val uBuffer: ByteBuffer = image.planes[1].buffer
    val vBuffer: ByteBuffer = image.planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    val chromaRowStride = image.planes[1].rowStride
    val chromaPixelStride = image.planes[1].pixelStride
    var offset = ySize
    for (row in 0 until image.height / 2) {
        var col = 0
        while (col < image.width / 2) {
            val vuIndex = row * chromaRowStride + col * chromaPixelStride
            nv21[offset++] = vBuffer.get(vuIndex)
            nv21[offset++] = uBuffer.get(vuIndex)
            col++
        }
    }
    return nv21
}
