@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.teamozy.feature.face.presentation

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.provider.Settings
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
import com.example.teamozy.feature.face.util.FaceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * FaceRegistrationScreen
 * - Captures multiple frames (default 8), spaced by captureIntervalMs (default 650ms)
 * - Averages & L2-normalizes to a stable 512-D vector
 * - Saves locally (no API call), logs vector, and closes the camera cleanly
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

    var progress by remember { mutableStateOf(0) }
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

    // CameraX references (so we can unbind/stop properly)
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val analysisRef = remember { mutableStateOf<ImageAnalysis?>(null) }
    val previewRef  = remember { mutableStateOf<Preview?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            // Full teardown on screen dispose
            analysisRef.value?.clearAnalyzer()
            cameraProviderRef.value?.unbindAll()
            previewRef.value?.setSurfaceProvider(null)
        }
    }

    val vectors = remember { mutableStateListOf<FloatArray>() }
    val busy = remember { AtomicBoolean(false) }
    var lastCaptureAt by remember { mutableLongStateOf(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Registration") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Teardown on dismiss
                        analysisRef.value?.clearAnalyzer()
                        cameraProviderRef.value?.unbindAll()
                        previewRef.value?.setSurfaceProvider(null)
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
                    AndroidView(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.BLACK)
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                cameraProviderRef.value = cameraProvider

                                val preview = Preview.Builder()
                                    .setTargetResolution(Size(720, 1280))
                                    .build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                previewRef.value = preview

                                val analysis = ImageAnalysis.Builder()
                                    .setTargetResolution(Size(720, 1280))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                analysisRef.value = analysis

                                val faceDetector = FaceDetector(ctx)
                                val extractor = EmbeddingExtractor.getInstance(ctx)

                                analysis.setAnalyzer(Dispatchers.Default.asExecutor()) { image ->
                                    val now = System.currentTimeMillis()
                                    if (isSaving || vectors.size >= targetFrames ||
                                        (now - lastCaptureAt) < captureIntervalMs ||
                                        !busy.compareAndSet(false, true)
                                    ) {
                                        image.close()
                                        return@setAnalyzer
                                    }

                                    // Convert to Bitmap and fix orientation + mirror for front camera
                                    val rotation = image.imageInfo.rotationDegrees
                                    val bmp: Bitmap = image.toBitmap().rotateAndMirror(rotationDegrees = rotation, mirror = true)
                                    image.close()

                                    scope.launch {
                                        try {
                                            status = "Checking face…"
                                            val detected = withContext(Dispatchers.Default) {
                                                faceDetector.detectBestFace(bmp)
                                            }
                                            if (detected == null) {
                                                status = "No face detected. Hold steady."
                                                return@launch
                                            }

                                            val accept = isFaceCenteredEnough(
                                                bmp.width, bmp.height, detected.boundingBox, minFacePctOfCircle
                                            )
                                            if (!accept) {
                                                status = "Move closer and center your face."
                                                return@launch
                                            }

                                            status = "Capturing… (${vectors.size + 1}/$targetFrames)"
                                            val vec = withContext(Dispatchers.Default) { extractor.extract(bmp) }
                                            vectors.add(vec)
                                            lastCaptureAt = System.currentTimeMillis()
                                            progress = vectors.size
                                            status = "Good capture ${vectors.size} / $targetFrames"

                                            if (vectors.size >= targetFrames) {
                                                isSaving = true
                                                status = "Finalizing…"
                                                withContext(Dispatchers.Default) {
                                                    val avg = averageAndNormalize(vectors.toList())
                                                    // Log the final 512-D vector
                                                    android.util.Log.d("FaceEnroll", "embedding512=" + avg.joinToString(prefix = "[", postfix = "]"))
                                                    FaceStore.getInstance(ctx).saveEmbedding(avg)
                                                }

                                                // 🔻 stop camera cleanly BEFORE leaving
                                                withContext(Dispatchers.Main) {
                                                    analysisRef.value?.clearAnalyzer()
                                                    cameraProviderRef.value?.unbindAll()
                                                    previewRef.value?.setSurfaceProvider(null)
                                                }

                                                status = "Face enrolled"
                                                onEnrolled()
                                            }
                                        } catch (t: Throwable) {
                                            errorText = t.message ?: "Failed to process frame"
                                        } finally {
                                            busy.set(false)
                                        }
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
                                } catch (t: Throwable) {
                                    errorText = t.message
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
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

/** Progress ring with dotted ticks, fills as frames are captured */
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

/** Shown when CAMERA permission is not granted */
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

/** Quick accept heuristic: face big enough & near center of the circle */
private fun isFaceCenteredEnough(w: Int, h: Int, face: Rect, minPct: Float): Boolean {
    val cx = w / 2f; val cy = h / 2f
    val radius = minOf(cx, cy) * 0.95f
    val fx = face.centerX().toFloat()
    val fy = face.centerY().toFloat()
    val dist = kotlin.math.hypot(fx - cx, fy - cy)
    val sizeOk = (minOf(face.width(), face.height()).toFloat() / (radius * 2f)) >= minPct
    val centerOk = dist <= radius * 0.35f
    return sizeOk && centerOk
}

/** Average a set of 512-D vectors and L2-normalize */
private fun averageAndNormalize(list: List<FloatArray>): FloatArray {
    require(list.isNotEmpty())
    val dim = list[0].size
    val out = FloatArray(dim)
    for (v in list) for (i in 0 until dim) out[i] += v[i]
    val n = list.size.toFloat()
    for (i in 0 until dim) out[i] /= n
    var sum = 0.0; for (x in out) sum += (x * x)
    val denom = kotlin.math.sqrt(sum).toFloat().coerceAtLeast(1e-12f)
    for (i in out.indices) out[i] = out[i] / denom
    return out
}

/** Rotate by camera rotation and mirror horizontally for front camera */
private fun Bitmap.rotateAndMirror(rotationDegrees: Int, mirror: Boolean = true): Bitmap {
    val m = Matrix()
    if (rotationDegrees != 0) m.postRotate(rotationDegrees.toFloat())
    if (mirror) m.postScale(-1f, 1f)
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}
