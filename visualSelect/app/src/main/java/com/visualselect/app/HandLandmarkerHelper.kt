package com.visualselect.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    context: Context,
    private val onHandsDetected: (HandDetectionSnapshot) -> Unit,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var handLandmarker: HandLandmarker? = null
    @Volatile
    private var closed = false

    @Volatile
    var cropPaddingPx: Int = 24

    @Volatile
    var cropMode: CropMode = CropMode.BETWEEN_HANDS

    private var lastAppliedTimestampMs = 0L
    private var consecutiveEmptyResults = 0
    private var lastSnapshot = HandDetectionSnapshot()

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
        val timestampMs = result.timestampMs()
        if (timestampMs < lastAppliedTimestampMs) {
            return
        }
        lastAppliedTimestampMs = timestampMs

        val landmarks = result.landmarks()
        val snapshot = if (landmarks.isEmpty()) {
            consecutiveEmptyResults++
            if (consecutiveEmptyResults < EMPTY_FRAMES_BEFORE_CLEAR) {
                return
            }
            HandDetectionSnapshot()
        } else {
            consecutiveEmptyResults = 0
            val imageWidth = lastFrameWidth
            val imageHeight = lastFrameHeight
            if (imageWidth <= 0 || imageHeight <= 0) {
                return
            }

            val boxes = landmarks.map { hand ->
                BetweenHandsCropper.boundingBoxFromLandmarks(hand, imageWidth, imageHeight)
            }
            val crop = BetweenHandsCropper.computeCropForHands(
                handBoxes = boxes,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                paddingPx = cropPaddingPx,
                mode = cropMode,
            )
            HandDetectionSnapshot(
                handCount = landmarks.size,
                cropRect = crop,
                handBoxes = boxes,
            )
        }

        if (snapshot == lastSnapshot) {
            return
        }
        lastSnapshot = snapshot

        mainHandler.post {
            if (!closed) {
                onHandsDetected(snapshot)
            }
        }
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
        private const val EMPTY_FRAMES_BEFORE_CLEAR = 3

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
