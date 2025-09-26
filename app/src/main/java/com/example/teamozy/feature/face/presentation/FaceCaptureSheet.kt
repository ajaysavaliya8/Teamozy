@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.face.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun FaceCaptureSheet(
    onDismiss: () -> Unit,
    onCaptured: (jpeg: ByteArray) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Face verification", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            AndroidView(
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
                                androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                                capture
                            )
                            imageCapture = capture
                        } catch (t: Throwable) {
                            errorText = t.message ?: "Camera error"
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
            )

            if (errorText != null) {
                Spacer(Modifier.height(8.dp))
                Text(errorText!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    enabled = !isCapturing && errorText == null,
                    onClick = {
                        val cap = imageCapture ?: return@Button
                        isCapturing = true
                        captureToBytes(context, cap) { bytes, err ->
                            isCapturing = false
                            if (err != null) errorText = err
                            else if (bytes != null) onCaptured(bytes)
                        }
                    }
                ) { Text(if (isCapturing) "Capturing…" else "Capture") }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

/* ---------------- helpers ---------------- */

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
            } finally {
                image.close()
            }
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
