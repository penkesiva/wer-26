package com.visualselect.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    context: Context,
    private val onHandsDetected: (handCount: Int, cropRect: BetweenHandsCropper.CropRect?, handBoxes: List<RectF>) -> Unit,
) {
    private val appContext = context.applicationContext
    private var handLandmarker: HandLandmarker? = null
    @Volatile
    private var closed = false

    fun setup() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(2)
            .setMinHandDetectionConfidence(0.5f)
            .setMinHandPresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener { result, _ -> dispatchResult(result) }
            .setErrorListener { error -> Log.e(TAG, "Hand landmarker error", error) }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(appContext, options)
    }

    fun detectAsync(bitmap: Bitmap, timestampMs: Long) {
        if (closed) return
        val mpImage = BitmapImageBuilder(bitmap).build()
        handLandmarker?.detectAsync(mpImage, timestampMs)
    }

    fun close() {
        closed = true
        handLandmarker?.close()
        handLandmarker = null
    }

    private fun dispatchResult(result: HandLandmarkerResult) {
        val landmarks = result.landmarks()
        if (landmarks.isEmpty()) {
            onHandsDetected(0, null, emptyList())
            return
        }

        // Normalized coords are relative to the last submitted bitmap size.
        val imageWidth = lastFrameWidth
        val imageHeight = lastFrameHeight
        if (imageWidth <= 0 || imageHeight <= 0) return

        val boxes = landmarks.map { hand ->
            BetweenHandsCropper.boundingBoxFromLandmarks(hand, imageWidth, imageHeight)
        }
        val crop = BetweenHandsCropper.computeBetweenHands(boxes, imageWidth, imageHeight)
        onHandsDetected(landmarks.size, crop, boxes)
    }

    @Volatile
    var lastFrameWidth: Int = 0
        private set

    @Volatile
    var lastFrameHeight: Int = 0
        private set

    fun noteFrameSize(width: Int, height: Int) {
        lastFrameWidth = width
        lastFrameHeight = height
    }

    companion object {
        private const val TAG = "HandLandmarkerHelper"

        fun imageProxyToBitmap(image: ImageProxy): Bitmap {
            val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(image.planes[0].buffer)

            val matrix = Matrix().apply {
                postRotate(image.imageInfo.rotationDegrees.toFloat())
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            return rotated
        }
    }
}
