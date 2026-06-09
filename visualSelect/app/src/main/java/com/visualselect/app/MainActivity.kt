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
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val settings = remember { AppSettings(context) }
    val chimePlayer = remember { ChimePlayer(context) }

    var handSnapshot by remember { mutableStateOf(HandDetectionSnapshot()) }
    var previewSize by remember { mutableStateOf(Size.Zero) }
    var showSettings by remember { mutableStateOf(false) }
    var hadTwoHands by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var justSaved by remember { mutableStateOf(false) }
    var holdProgressSec by remember { mutableFloatStateOf(0f) }

    val autoSaveTracker = remember {
        AutoSaveTracker(stabilityMs = 1500L, cooldownMs = 3000L)
    }

    val scope = rememberCoroutineScope()

    val latestBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val helper = remember {
        HandLandmarkerHelper(context) { snapshot ->
            handSnapshot = snapshot
        }.also { it.setup() }
    }

    DisposableEffect(Unit) {
        onDispose {
            helper.close()
            chimePlayer.release()
            latestBitmap.value?.recycle()
        }
    }

    LaunchedEffect(settings.cropPaddingPx) {
        helper.cropPaddingPx = settings.cropPaddingPx
    }

    LaunchedEffect(handSnapshot, settings.chimeOnTwoHands) {
        val ready = handSnapshot.isReady
        if (settings.chimeOnTwoHands && ready && !hadTwoHands) {
            chimePlayer.playTwoHandsChime()
        }
        hadTwoHands = ready
    }

    LaunchedEffect(
        settings.autoSaveEnabled,
        settings.autoSaveStabilitySec,
        settings.autoSaveCooldownSec,
    ) {
        while (isActive) {
            autoSaveTracker.stabilityMs = (settings.autoSaveStabilitySec * 1000).toLong()
            autoSaveTracker.cooldownMs = (settings.autoSaveCooldownSec * 1000).toLong()

            val snapshot = snapshotFlow { handSnapshot }.first()
            val ready = snapshot.isReady
            val rect = snapshot.cropRect
            val now = System.currentTimeMillis()

            if (settings.autoSaveEnabled && ready && rect != null) {
                val progressMs = autoSaveTracker.stableProgressMs(true, rect, now)
                holdProgressSec = progressMs / 1000f

                if (!isSaving && autoSaveTracker.shouldSave(true, rect, now)) {
                    isSaving = true
                    val frame = snapshotFlow { latestBitmap.value }.first()
                    val saved = saveCropToGallery(context, frame, rect)
                    isSaving = false
                    if (saved) {
                        justSaved = true
                        Toast.makeText(context, R.string.saved_to_gallery, Toast.LENGTH_SHORT).show()
                        delay(2000)
                        justSaved = false
                    } else {
                        Toast.makeText(context, R.string.save_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                holdProgressSec = 0f
                autoSaveTracker.stableProgressMs(ready, rect, now)
            }

            delay(100)
        }
    }

    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analyzerExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        key(settings.cameraConfigVersion) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val analysis = CameraSession.buildImageAnalysis(settings.autofocusEnabled)

                        analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                            processFrame(imageProxy, helper, latestBitmap)
                        }

                        CameraSession.bind(
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            settings = settings,
                            analysis = analysis,
                            onCameraReady = {},
                        )
                    }
                },
                onRelease = {
                    runCatching {
                        ProcessCameraProvider.getInstance(context).get().unbindAll()
                    }
                },
                update = { previewView ->
                    previewSize = Size(previewView.width.toFloat(), previewView.height.toFloat())
                },
            )
        }

        if (previewSize.width > 0f && previewSize.height > 0f) {
            OverlayCanvas(
                handBoxes = handSnapshot.handBoxes,
                cropRect = handSnapshot.cropRect,
                frameWidth = helper.lastFrameWidth,
                frameHeight = helper.lastFrameHeight,
                previewWidth = previewSize.width,
                previewHeight = previewSize.height,
            )
        }

        Text(
            text = when {
                isSaving -> stringResource(R.string.status_saving)
                justSaved -> stringResource(R.string.status_saved)
                handSnapshot.isReady && settings.autoSaveEnabled && holdProgressSec > 0f ->
                    stringResource(
                        R.string.status_holding,
                        holdProgressSec,
                        settings.autoSaveStabilitySec,
                    )
                handSnapshot.isReady -> stringResource(R.string.status_ready)
                handSnapshot.handCount >= 2 -> stringResource(R.string.status_two_hands_no_crop)
                handSnapshot.handCount == 1 -> stringResource(R.string.status_one_hand)
                else -> stringResource(R.string.status_waiting)
            },
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
        )

        TextButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            Text(stringResource(R.string.settings), color = Color.White)
        }

        Button(
            onClick = {
                val rect = handSnapshot.cropRect
                if (rect == null) {
                    Toast.makeText(context, R.string.no_crop, Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (isSaving) return@Button
                scope.launch {
                    isSaving = true
                    val saved = saveCropToGallery(context, latestBitmap.value, rect)
                    isSaving = false
                    Toast.makeText(
                        context,
                        if (saved) R.string.saved_to_gallery else R.string.save_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            enabled = handSnapshot.isReady && !isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.35f),
                disabledContentColor = Color.Black.copy(alpha = 0.45f),
            ),
        ) {
            Text(stringResource(R.string.save_crop))
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            onDismiss = { showSettings = false },
        )
    }
}

private suspend fun saveCropToGallery(
    context: android.content.Context,
    frame: Bitmap?,
    rect: BetweenHandsCropper.CropRect,
): Boolean = withContext(Dispatchers.IO) {
    if (frame == null) return@withContext false
    val cropped = BetweenHandsCropper.cropBitmap(frame, rect)
    val saved = GallerySaver.savePng(context, cropped)
    if (cropped !== frame) {
        cropped.recycle()
    }
    saved
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
                size = Size(box.width() * scale, box.height() * scale),
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
