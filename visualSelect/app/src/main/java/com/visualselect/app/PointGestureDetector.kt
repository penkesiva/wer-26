package com.visualselect.app

import android.graphics.PointF
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.hypot

object PointGestureDetector {

    data class PointingTarget(
        val fingertip: PointF,
        val aimPoint: PointF,
    )

    private const val INDEX_TIP = 8
    private const val MIDDLE_TIP = 12
    private const val RING_TIP = 16
    private const val PINKY_TIP = 20
    private const val WRIST = 0
    private const val INDEX_PIP = 6

    fun detectPointing(
        normalizedLandmarks: List<NormalizedLandmark>,
        imageWidth: Int,
        imageHeight: Int,
    ): PointingTarget? {
        if (normalizedLandmarks.size < 21 || imageWidth <= 0 || imageHeight <= 0) return null
        if (!isIndexPointing(normalizedLandmarks)) return null

        val wrist = toPoint(normalizedLandmarks[WRIST], imageWidth, imageHeight)
        val indexTip = toPoint(normalizedLandmarks[INDEX_TIP], imageWidth, imageHeight)
        val indexPip = toPoint(normalizedLandmarks[INDEX_PIP], imageWidth, imageHeight)

        val dirX = indexTip.x - indexPip.x
        val dirY = indexTip.y - indexPip.y
        val dirLen = hypot(dirX.toDouble(), dirY.toDouble()).toFloat().coerceAtLeast(1f)

        // Aim slightly past the fingertip along the index direction.
        val aheadPx = maxOf(dirLen * 1.8f, imageWidth * 0.04f)
        val aimPoint = PointF(
            indexTip.x + dirX / dirLen * aheadPx,
            indexTip.y + dirY / dirLen * aheadPx,
        )

        return PointingTarget(fingertip = indexTip, aimPoint = aimPoint)
    }

    private fun isIndexPointing(landmarks: List<NormalizedLandmark>): Boolean {
        val wrist = landmarks[WRIST]
        val indexTip = landmarks[INDEX_TIP]
        val middleTip = landmarks[MIDDLE_TIP]
        val ringTip = landmarks[RING_TIP]
        val pinkyTip = landmarks[PINKY_TIP]

        fun ext(tip: NormalizedLandmark): Float =
            hypot(
                (tip.x() - wrist.x()).toDouble(),
                (tip.y() - wrist.y()).toDouble(),
            ).toFloat()

        val indexExt = ext(indexTip)
        val middleExt = ext(middleTip)
        val ringExt = ext(ringTip)
        val pinkyExt = ext(pinkyTip)

        if (indexExt < 0.11f) return false

        return indexExt > middleExt * 1.12f &&
            indexExt > ringExt * 1.12f &&
            indexExt > pinkyExt * 1.12f &&
            indexExt > maxOf(middleExt, ringExt, pinkyExt) + 0.02f
    }

    private fun toPoint(landmark: NormalizedLandmark, imageWidth: Int, imageHeight: Int): PointF =
        PointF(landmark.x() * imageWidth, landmark.y() * imageHeight)
}
