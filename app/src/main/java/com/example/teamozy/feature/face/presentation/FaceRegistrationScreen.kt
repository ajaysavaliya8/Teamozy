@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.face.presentation

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.teamozy.feature.face.data.FaceStore
import com.example.teamozy.feature.face.data.EmbeddingExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * FaceRegistrationScreen - Multi-frame enrollment with quality checks
 *
 * Captures multiple frames, averages embeddings, and saves to FaceStore.
 * Uses the production-grade EmbeddingExtractor and FaceDetector.
 */
@Composable
fun FaceRegistrationScreen(
    onDismiss: () -> Unit,
    onEnrolled: () -> Unit,
    targetFrames: Int = 8,
    minFacePctOfCircle: Float = 0.28f,
    captureIntervalMs: Long = 650L
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var progress by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Position your face inside the circle") }
    var isSaving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Permission
    val hasPermission = remember { mutableStateOf(false) }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission.value = granted
        if (!granted) errorText = "Camera permission is required to enroll your face."
    }
    LaunchedEffect(Unit) { requestPermission.launch(Manifest.permission.CAMERA) }

    // Camera references
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val analysisRef = remember { mutableStateOf<ImageAnalysis?>(null) }
    val previewRef = remember { mutableStateOf<Preview?>(null) }

    // Cleanup function
    fun cleanupCamera() {
        Log.d("FaceReg", "Cleaning up camera...")
        try {
            analysisRef.value?.clearAnalyzer()
            analysisRef.value = null
            cameraProviderRef.value?.unbindAll()
            cameraProviderRef.value = null
            previewRef.value?.setSurfaceProvider(null)
            previewRef.value = null
        } catch (e: Exception) {
            Log.e("FaceReg", "Cleanup error", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose { cleanupCamera() }
    }

    // Frame collection state
    val vectors = remember { mutableStateListOf<FloatArray>() }
    var isProcessing by remember { mutableStateOf(false) }
    var lastCaptureAt by remember { mutableLongStateOf(0L) }

    // 2-minute auto-close
    LaunchedEffect(Unit) {
        delay(120_000)
        cleanupCamera()
        onDismiss()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Registration") },
                navigationIcon = {
                    IconButton(onClick = {
                        cleanupCamera()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Position your face inside the circle",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Hold steady • good light • 20–30 cm away",
                color = Color(0xFFB0B0B0),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                if (hasPermission.value) {
                    CameraPreviewWithAnalysis(
                        lifecycleOwner = lifecycleOwner,
                        onCameraReady = { provider, preview, analysis ->
                            cameraProviderRef.value = provider
                            previewRef.value = preview
                            analysisRef.value = analysis
                        },
                        onFrameProcessed = { bitmap, rotation ->
                            // Rate limiting
                            val now = System.currentTimeMillis()
                            if (isProcessing || isSaving ||
                                vectors.size >= targetFrames ||
                                (now - lastCaptureAt) < captureIntervalMs) {
                                bitmap.recycle()  // Don't process, but recycle immediately
                                return@CameraPreviewWithAnalysis
                            }

                            isProcessing = true
                            scope.launch {
                                try {
                                    status = "Checking face quality..."

                                    // Use the production EmbeddingExtractor
                                    val extractor = EmbeddingExtractor.getInstance(
                                        context = context,
                                        numThreads = 4,
                                        enableGpu = false,
                                        debugLogging = true
                                    )

                                    val embedding = withContext(Dispatchers.Default) {
                                        extractor.extractNoRetry(bitmap, rotation)
                                    }

                                    vectors.add(embedding)
                                    lastCaptureAt = System.currentTimeMillis()
                                    progress = vectors.size
                                    status = "Good capture ${vectors.size} / $targetFrames"
                                    Log.d("FaceReg", "Frame ${vectors.size}/$targetFrames captured")

                                    // Check if done
                                    if (vectors.size >= targetFrames) {
                                        isSaving = true
                                        cleanupCamera()  // Stop camera ASAP
                                        status = "Finalizing…"

                                        withContext(Dispatchers.Default) {
                                            val avg = averageAndNormalize(vectors.toList())
                                            FaceStore.getInstance(context).saveEmbedding(avg)
                                            Log.d("FaceReg", "Enrollment complete: ${avg.take(5)}")
                                        }

                                        status = "Face enrolled"
                                        onEnrolled()
                                    }

                                } catch (e: IllegalStateException) {
                                    // FaceDetectOutcome rejection messages
                                    status = e.message ?: "Face quality check failed"
                                    Log.w("FaceReg", "Face rejected: ${e.message}")
                                } catch (e: Exception) {
                                    errorText = e.message ?: "Failed to process frame"
                                    Log.e("FaceReg", "Processing error", e)
                                } finally {
                                    bitmap.recycle()  // ALWAYS recycle after processing
                                    isProcessing = false
                                }
                            }
                        },
                        onError = { error ->
                            errorText = error
                            Log.e("FaceReg", "Camera error: $error")
                        }
                    )
                } else {
                    PermissionHint(
                        onOpenSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                    )
                }

                ProgressRingOverlay(
                    modifier = Modifier.matchParentSize(),
                    totalTicks = 48,
                    completed = progress,
                    target = targetFrames
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                status,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (errorText != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    errorText!!,
                    color = Color(0xFFFF6B6B),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.weight(1f))
            if (isSaving) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CameraPreviewWithAnalysis(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCameraReady: (ProcessCameraProvider, Preview, ImageAnalysis) -> Unit,
    onFrameProcessed: (Bitmap, Int) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .setTargetResolution(Size(720, 1280))
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(720, 1280))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(executor) { imageProxy ->
                        try {
                            val rotation = imageProxy.imageInfo.rotationDegrees
                            val bitmap = imageProxy.toBitmap()
                                .rotateAndMirror(rotation, mirror = true)
                                .copy(Bitmap.Config.ARGB_8888, false)  // Make a copy we can keep

                            onFrameProcessed(bitmap, 0)  // Pass rotation=0 since we pre-rotated

                            // Don't recycle here - let the processing coroutine handle it

                        } catch (e: Exception) {
                            Log.e("CameraPreview", "Frame error", e)
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
                        onCameraReady(cameraProvider, preview, analysis)
                        Log.d("CameraPreview", "Camera started successfully")
                    } catch (e: Exception) {
                        onError("Failed to start camera: ${e.message}")
                    }

                } catch (e: Exception) {
                    onError("Camera init failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@Composable
private fun ProgressRingOverlay(
    modifier: Modifier,
    totalTicks: Int,
    completed: Int,
    target: Int
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = (minOf(w, h) / 2f) - 6.dp.toPx()
        val outer = radius + 8.dp.toPx()

        val ticks = totalTicks
        val tickLen = 10.dp.toPx()
        val stroke = 3.dp.toPx()
        val activeCount = ((completed.toFloat() / target.toFloat()) * ticks)
            .toInt().coerceIn(0, ticks)

        for (i in 0 until ticks) {
            val angle = (i / ticks.toFloat()) * (2f * Math.PI).toFloat()
            val sx = cx + (outer - tickLen) * kotlin.math.cos(angle)
            val sy = cy + (outer - tickLen) * kotlin.math.sin(angle)
            val ex = cx + outer * kotlin.math.cos(angle)
            val ey = cy + outer * kotlin.math.sin(angle)
            drawLine(
                color = if (i < activeCount) Color(0xFF3EA6FF) else Color(0xFF2D2D2D),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = stroke
            )
        }
    }
}

@Composable
private fun PermissionHint(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Camera permission required", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Enable the camera to enroll your face.", color = Color(0xFFB0B0B0))
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onOpenSettings) { Text("Open Settings") }
    }
}

private fun averageAndNormalize(list: List<FloatArray>): FloatArray {
    require(list.isNotEmpty())
    val dim = list[0].size
    val out = FloatArray(dim)
    for (v in list) for (i in 0 until dim) out[i] += v[i]
    val n = list.size.toFloat()
    for (i in 0 until dim) out[i] /= n
    var sum = 0.0
    for (x in out) sum += (x * x)
    val denom = kotlin.math.sqrt(sum).toFloat().coerceAtLeast(1e-12f)
    for (i in out.indices) out[i] = out[i] / denom
    return out
}

private fun Bitmap.rotateAndMirror(rotationDegrees: Int, mirror: Boolean = true): Bitmap {
    val m = Matrix()
    if (rotationDegrees != 0) m.postRotate(rotationDegrees.toFloat())
    if (mirror) m.postScale(-1f, 1f)
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}