package com.visualselect.app

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * Computes an axis-aligned crop for the region between two detected hands.
 * Left hand is the hand with the smaller center-X; crop spans inner edges with padding.
 */
object BetweenHandsCropper {

    data class CropRect(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top

        fun isValid(minSizePx: Int = 48): Boolean =
            width >= minSizePx && height >= minSizePx
    }

    fun boundingBoxFromLandmarks(
        normalizedLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        imageWidth: Int,
        imageHeight: Int,
    ): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (landmark in normalizedLandmarks) {
            minX = minOf(minX, landmark.x())
            minY = minOf(minY, landmark.y())
            maxX = maxOf(maxX, landmark.x())
            maxY = maxOf(maxY, landmark.y())
        }
        return RectF(
            minX * imageWidth,
            minY * imageHeight,
            maxX * imageWidth,
            maxY * imageHeight,
        )
    }

    fun computeBetweenHands(
        handBoxes: List<RectF>,
        imageWidth: Int,
        imageHeight: Int,
        paddingPx: Int = 24,
    ): CropRect? {
        if (handBoxes.size < 2 || imageWidth <= 0 || imageHeight <= 0) return null

        val sorted = handBoxes.sortedBy { it.centerX() }
        val leftHand = sorted[0]
        val rightHand = sorted[1]

        val left = (leftHand.right + paddingPx).toInt().coerceIn(0, imageWidth - 1)
        val right = (rightHand.left - paddingPx).toInt().coerceIn(left + 1, imageWidth)
        val top = (minOf(leftHand.top, rightHand.top) - paddingPx).toInt().coerceIn(0, imageHeight - 1)
        val bottom = (maxOf(leftHand.bottom, rightHand.bottom) + paddingPx).toInt().coerceIn(top + 1, imageHeight)

        val rect = CropRect(left, top, right, bottom)
        return if (rect.isValid()) rect else null
    }

    fun cropBitmap(source: Bitmap, rect: CropRect): Bitmap =
        Bitmap.createBitmap(source, rect.left, rect.top, rect.width, rect.height)
}
