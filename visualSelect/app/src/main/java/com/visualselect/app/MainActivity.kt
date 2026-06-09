package com.visualselect.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VisualSelectScreen()
                }
            }
        }
    }
}

@Composable
private fun VisualSelectScreen() {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text(stringResource(R.string.grant_camera))
            }
        }
        return
    }

    CameraCaptureScreen()
}

@Composable
private fun CameraCaptureScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var handCount by remember { mutableIntStateOf(0) }
    var cropRect by remember { mutableStateOf<BetweenHandsCropper.CropRect?>(null) }
    var handBoxes by remember { mutableStateOf<List<RectF>>(emptyList()) }
    var previewSize by remember { mutableStateOf(Size.Zero) }

    val latestBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val helper = remember {
        HandLandmarkerHelper(context) { count, rect, boxes ->
            handCount = count
            cropRect = rect
            handBoxes = boxes
        }.also { it.setup() }
    }

    DisposableEffect(Unit) {
        onDispose { helper.close() }
    }

    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analyzerExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()

                        analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                            processFrame(imageProxy, helper, latestBitmap)
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            update = { previewView ->
                previewSize = Size(previewView.width.toFloat(), previewView.height.toFloat())
            },
        )

        if (previewSize.width > 0f && previewSize.height > 0f) {
            OverlayCanvas(
                handBoxes = handBoxes,
                cropRect = cropRect,
                frameWidth = helper.lastFrameWidth,
                frameHeight = helper.lastFrameHeight,
                previewWidth = previewSize.width,
                previewHeight = previewSize.height,
            )
        }

        Text(
            text = when {
                handCount >= 2 && cropRect != null -> stringResource(R.string.status_ready)
                handCount == 1 -> stringResource(R.string.status_one_hand)
                else -> stringResource(R.string.status_waiting)
            },
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
        )

        Button(
            onClick = {
                val frame = latestBitmap.value
                val rect = cropRect
                if (frame == null || rect == null) {
                    Toast.makeText(context, R.string.no_crop, Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val cropped = BetweenHandsCropper.cropBitmap(frame, rect)
                val saved = GallerySaver.savePng(context, cropped)
                Toast.makeText(
                    context,
                    if (saved) R.string.saved_to_gallery else R.string.save_failed,
                    Toast.LENGTH_SHORT,
                ).show()
                if (cropped !== frame) {
                    cropped.recycle()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            enabled = handCount >= 2 && cropRect != null,
        ) {
            Text(stringResource(R.string.save_crop))
        }
    }
}

private fun processFrame(
    imageProxy: ImageProxy,
    helper: HandLandmarkerHelper,
    latestBitmap: androidx.compose.runtime.MutableState<Bitmap?>,
) {
    try {
        val bitmap = HandLandmarkerHelper.imageProxyToBitmap(imageProxy)
        helper.noteFrameSize(bitmap.width, bitmap.height)

        val previous = latestBitmap.value
        latestBitmap.value = bitmap
        previous?.recycle()

        helper.detectAsync(bitmap, System.currentTimeMillis())
    } finally {
        imageProxy.close()
    }
}

@Composable
private fun OverlayCanvas(
    handBoxes: List<RectF>,
    cropRect: BetweenHandsCropper.CropRect?,
    frameWidth: Int,
    frameHeight: Int,
    previewWidth: Float,
    previewHeight: Float,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (frameWidth <= 0 || frameHeight <= 0) return@Canvas

        val scale = maxOf(previewWidth / frameWidth, previewHeight / frameHeight)
        val drawnWidth = frameWidth * scale
        val drawnHeight = frameHeight * scale
        val offsetX = (previewWidth - drawnWidth) / 2f
        val offsetY = (previewHeight - drawnHeight) / 2f

        fun mapX(x: Float) = offsetX + x * scale
        fun mapY(y: Float) = offsetY + y * scale

        handBoxes.forEach { box ->
            drawRect(
                color = Color(0xFF4FC3F7),
                topLeft = Offset(mapX(box.left), mapY(box.top)),
                size = Size((box.width()) * scale, (box.height()) * scale),
                style = Stroke(width = 3f),
            )
        }

        cropRect?.let { rect ->
            drawRect(
                color = Color(0xFFFFEB3B),
                topLeft = Offset(mapX(rect.left.toFloat()), mapY(rect.top.toFloat())),
                size = Size(rect.width * scale, rect.height * scale),
                style = Stroke(width = 4f),
            )
        }
    }
}
