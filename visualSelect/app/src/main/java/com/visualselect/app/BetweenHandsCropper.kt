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

    /**
     * Prefers the strip between both hands; falls back to the union of both boxes when
     * hands overlap or the gap is too narrow (common when the second hand enters frame).
     */
    fun computeCropForHands(
        handBoxes: List<RectF>,
        imageWidth: Int,
        imageHeight: Int,
        paddingPx: Int = 24,
    ): CropRect? {
        if (handBoxes.size < 2 || imageWidth <= 0 || imageHeight <= 0) return null
        return computeBetweenHands(handBoxes, imageWidth, imageHeight, paddingPx)
            ?: computeUnionOfHands(handBoxes, imageWidth, imageHeight, paddingPx)
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

        val innerLeft = leftHand.right + paddingPx
        val innerRight = rightHand.left - paddingPx
        if (innerLeft >= innerRight) return null

        val left = innerLeft.toInt().coerceIn(0, imageWidth - 1)
        val right = innerRight.toInt().coerceIn(left + 1, imageWidth)
        val top = (minOf(leftHand.top, rightHand.top) - paddingPx).toInt().coerceIn(0, imageHeight - 1)
        val bottom = (maxOf(leftHand.bottom, rightHand.bottom) + paddingPx).toInt().coerceIn(top + 1, imageHeight)

        val rect = CropRect(left, top, right, bottom)
        return if (rect.isValid(minSizePx = 32)) rect else null
    }

    fun computeUnionOfHands(
        handBoxes: List<RectF>,
        imageWidth: Int,
        imageHeight: Int,
        paddingPx: Int = 24,
    ): CropRect? {
        if (handBoxes.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return null

        var minLeft = Float.MAX_VALUE
        var minTop = Float.MAX_VALUE
        var maxRight = Float.MIN_VALUE
        var maxBottom = Float.MIN_VALUE
        for (box in handBoxes) {
            minLeft = minOf(minLeft, box.left)
            minTop = minOf(minTop, box.top)
            maxRight = maxOf(maxRight, box.right)
            maxBottom = maxOf(maxBottom, box.bottom)
        }

        val left = (minLeft - paddingPx).toInt().coerceIn(0, imageWidth - 1)
        val right = (maxRight + paddingPx).toInt().coerceIn(left + 1, imageWidth)
        val top = (minTop - paddingPx).toInt().coerceIn(0, imageHeight - 1)
        val bottom = (maxBottom + paddingPx).toInt().coerceIn(top + 1, imageHeight)

        val rect = CropRect(left, top, right, bottom)
        return if (rect.isValid(minSizePx = 32)) rect else null
    }

    fun cropBitmap(source: Bitmap, rect: CropRect): Bitmap =
        Bitmap.createBitmap(source, rect.left, rect.top, rect.width, rect.height)
}
