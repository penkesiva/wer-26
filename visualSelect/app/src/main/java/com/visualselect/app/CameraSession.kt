package com.visualselect.app

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

@OptIn(ExperimentalCamera2Interop::class)
object CameraSession {

    private const val TAG = "CameraSession"

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        settings: AppSettings,
        analysis: ImageAnalysis,
        onCameraReady: (Camera?) -> Unit,
    ) {
        val context = previewView.context
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = buildPreview(previewView, settings.autofocusEnabled)
                val selector = buildCameraSelector(settings.preferWideLens)

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    analysis,
                )
                applyZoom(camera, settings.zoomRatio)
                onCameraReady(camera)
            } catch (error: Exception) {
                Log.e(TAG, "Camera bind failed", error)
                onCameraReady(null)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun buildImageAnalysis(autofocusEnabled: Boolean): ImageAnalysis {
        val builder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        if (!autofocusEnabled) {
            Camera2Interop.Extender(builder).setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF,
            )
        }
        return builder.build()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun buildPreview(previewView: PreviewView, autofocusEnabled: Boolean): Preview {
        val builder = Preview.Builder()
        if (!autofocusEnabled) {
            Camera2Interop.Extender(builder).setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF,
            )
        }
        return builder.build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun buildCameraSelector(preferWideLens: Boolean): CameraSelector {
        if (!preferWideLens) {
            return CameraSelector.DEFAULT_BACK_CAMERA
        }
        return CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .addCameraFilter(WidestBackCameraFilter())
            .build()
    }

    private fun applyZoom(camera: Camera, requestedRatio: Float) {
        val zoomState = camera.cameraInfo.zoomState.value ?: return
        val min = zoomState.minZoomRatio
        val max = minOf(1f, zoomState.maxZoomRatio)
        // Slider 0.5 = widest (device min zoom), 1.0 = ~1x
        val t = ((requestedRatio - 0.5f) / 0.5f).coerceIn(0f, 1f)
        val target = max - t * (max - min)
        camera.cameraControl.setZoomRatio(target.coerceIn(min, max))
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private class WidestBackCameraFilter : CameraFilter {
        override fun filter(cameraInfos: List<CameraInfo>): List<CameraInfo> {
            if (cameraInfos.isEmpty()) return cameraInfos

            val backCameras = cameraInfos.filter {
                it.lensFacing == CameraSelector.LENS_FACING_BACK
            }
            val candidates = if (backCameras.isEmpty()) cameraInfos else backCameras

            val ranked = candidates.mapNotNull { info ->
                val focalLengths = Camera2CameraInfo.from(info)
                    .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val minFocal = focalLengths?.minOrNull() ?: return@mapNotNull null
                info to minFocal
            }

            if (ranked.isEmpty()) {
                return listOf(candidates.first())
            }
            return listOf(ranked.minByOrNull { it.second }!!.first)
        }
    }
}
