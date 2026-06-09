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
    var cropPaddingPx: Int = 8

    @Volatile
    var cropMode: CropMode = CropMode.BETWEEN_HANDS

    @Volatile
    var squareCrop: Boolean = true

    @Volatile
    var gestureMode: GestureMode = GestureMode.TWO_HANDS

    private var lastAppliedTimestampMs = 0L
    private var consecutiveEmptyResults = 0
    private var lastSnapshot = HandDetectionSnapshot()

    fun setup() {
        handLandmarker?.close()
        handLandmarker = null
        if (closed) return

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val numHands = if (gestureMode == GestureMode.ONE_FINGER_POINT) 1 else 2

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(numHands)
            .setMinHandDetectionConfidence(0.5f)
            .setMinHandPresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener { result, _ -> dispatchResult(result) }
            .setErrorListener { error -> Log.e(TAG, "Hand landmarker error", error) }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(appContext, options)
        lastSnapshot = HandDetectionSnapshot(gestureMode = gestureMode)
    }

    fun updateGestureMode(mode: GestureMode) {
        if (gestureMode == mode) return
        gestureMode = mode
        setup()
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
            HandDetectionSnapshot(gestureMode = gestureMode)
        } else {
            consecutiveEmptyResults = 0
            val imageWidth = lastFrameWidth
            val imageHeight = lastFrameHeight
            if (imageWidth <= 0 || imageHeight <= 0) {
                return
            }

            when (gestureMode) {
                GestureMode.ONE_FINGER_POINT -> buildPointSnapshot(landmarks[0], imageWidth, imageHeight)
                GestureMode.TWO_HANDS -> buildTwoHandSnapshot(landmarks, imageWidth, imageHeight)
            }
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

    private fun buildPointSnapshot(
        hand: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        imageWidth: Int,
        imageHeight: Int,
    ): HandDetectionSnapshot {
        val frame = BetweenHandsCropper.handFrameFromLandmarks(hand, imageWidth, imageHeight)
        val pointing = PointGestureDetector.detectPointing(hand, imageWidth, imageHeight)
        val crop = pointing?.let {
            PointCropper.computePointCrop(
                aimPoint = it.aimPoint,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                paddingPx = cropPaddingPx,
                squareCrop = squareCrop,
            )
        }

        return HandDetectionSnapshot(
            handCount = 1,
            cropRect = crop,
            handBoxes = listOf(frame.bounds),
            isPointing = pointing != null,
            aimPoint = pointing?.aimPoint,
            gestureMode = GestureMode.ONE_FINGER_POINT,
        )
    }

    private fun buildTwoHandSnapshot(
        landmarks: List<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>>,
        imageWidth: Int,
        imageHeight: Int,
    ): HandDetectionSnapshot {
        val handFrames = landmarks.map { hand ->
            BetweenHandsCropper.handFrameFromLandmarks(hand, imageWidth, imageHeight)
        }
        val boxes = handFrames.map { it.bounds }
        val crop = BetweenHandsCropper.computeCropForHands(
            handFrames = handFrames,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            paddingPx = cropPaddingPx,
            mode = cropMode,
            squareCrop = squareCrop,
        )

        return HandDetectionSnapshot(
            handCount = landmarks.size,
            cropRect = crop,
            handBoxes = boxes,
            gestureMode = GestureMode.TWO_HANDS,
        )
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
