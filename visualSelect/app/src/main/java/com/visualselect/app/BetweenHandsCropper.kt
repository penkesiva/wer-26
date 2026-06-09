package com.visualselect.app

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

/**
 * Computes crop regions from two hands. Thumb landmarks are excluded so extended
 * thumbs do not stretch the crop into tall narrow strips.
 */
object BetweenHandsCropper {

    /** MediaPipe thumb chain — excluded from crop bounds. */
    private val THUMB_INDICES = setOf(1, 2, 3, 4)

    /** Palm + finger bases used to anchor each hand regardless of facing direction. */
    private val PALM_INDICES = setOf(0, 5, 9, 13, 17)

    data class CropRect(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top

        fun isValid(minSizePx: Int = 24): Boolean =
            width >= minSizePx && height >= minSizePx
    }

    data class HandFrame(
        val points: List<PointF>,
        val palmCenter: PointF,
        val bounds: RectF,
    )

    fun handFrameFromLandmarks(
        normalizedLandmarks: List<NormalizedLandmark>,
        imageWidth: Int,
        imageHeight: Int,
    ): HandFrame {
        val points = normalizedLandmarks.mapIndexedNotNull { index, landmark ->
            if (index in THUMB_INDICES) return@mapIndexedNotNull null
            PointF(landmark.x() * imageWidth, landmark.y() * imageHeight)
        }

        val palmPoints = normalizedLandmarks.mapIndexedNotNull { index, landmark ->
            if (index !in PALM_INDICES) return@mapIndexedNotNull null
            PointF(landmark.x() * imageWidth, landmark.y() * imageHeight)
        }

        val palmCenter = if (palmPoints.isEmpty()) {
            val cx = points.map { it.x }.average().toFloat()
            val cy = points.map { it.y }.average().toFloat()
            PointF(cx, cy)
        } else {
            PointF(
                palmPoints.map { it.x }.average().toFloat(),
                palmPoints.map { it.y }.average().toFloat(),
            )
        }

        return HandFrame(
            points = points,
            palmCenter = palmCenter,
            bounds = boundsFromPoints(points),
        )
    }

    fun boundingBoxFromLandmarks(
        normalizedLandmarks: List<NormalizedLandmark>,
        imageWidth: Int,
        imageHeight: Int,
    ): RectF = handFrameFromLandmarks(normalizedLandmarks, imageWidth, imageHeight).bounds

    fun computeCropForHands(
        handFrames: List<HandFrame>,
        imageWidth: Int,
        imageHeight: Int,
        paddingPx: Int = 8,
        mode: CropMode = CropMode.BETWEEN_HANDS,
        squareCrop: Boolean = true,
    ): CropRect? {
        if (handFrames.size < 2 || imageWidth <= 0 || imageHeight <= 0) return null
        val rect = when (mode) {
            CropMode.BETWEEN_HANDS -> computeBetweenHands(handFrames, imageWidth, imageHeight, paddingPx)
            CropMode.INCLUDE_HANDS -> computeUnionOfHands(handFrames, imageWidth, imageHeight, paddingPx)
        } ?: return null

        return if (squareCrop) {
            toSquareCrop(rect, imageWidth, imageHeight)
        } else {
            rect
        }
    }

    fun computeBetweenHands(
        handFrames: List<HandFrame>,
        imageWidth: Int,
        imageHeight: Int,
        paddingPx: Int = 8,
    ): CropRect? {
        if (handFrames.size < 2 || imageWidth <= 0 || imageHeight <= 0) return null

        val sorted = handFrames.sortedBy { it.palmCenter.x }
        val leftHand = sorted[0]
        val rightHand = sorted[1]

        // Inner edges from non-thumb points — works when palms face same or opposite directions.
        val innerLeft = innerEdgeX(leftHand, towardOtherHand = true) + 2f
        val innerRight = innerEdgeX(rightHand, towardOtherHand = false) - 2f
        if (innerLeft >= innerRight) return null

        val verticalPad = (paddingPx * 0.35f).coerceIn(0f, 12f)
        val (topF, bottomF) = verticalSpan(leftHand, rightHand, verticalPad)

        val left = innerLeft.toInt().coerceIn(0, imageWidth - 1)
        val right = innerRight.toInt().coerceIn(left + 1, imageWidth)
        val top = topF.toInt().coerceIn(0, imageHeight - 1)
        val bottom = bottomF.toInt().coerceIn(top + 1, imageHeight)

        val rect = CropRect(left, top, right, bottom)
        return if (rect.isValid()) rect else null
    }

    fun computeUnionOfHands(
        handFrames: List<HandFrame>,
        imageWidth: Int,
        imageHeight: Int,
        paddingPx: Int = 8,
    ): CropRect? {
        if (handFrames.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return null

        var minLeft = Float.MAX_VALUE
        var minTop = Float.MAX_VALUE
        var maxRight = Float.MIN_VALUE
        var maxBottom = Float.MIN_VALUE
        for (frame in handFrames) {
            minLeft = minOf(minLeft, frame.bounds.left)
            minTop = minOf(minTop, frame.bounds.top)
            maxRight = maxOf(maxRight, frame.bounds.right)
            maxBottom = maxOf(maxBottom, frame.bounds.bottom)
        }

        val left = (minLeft - paddingPx).toInt().coerceIn(0, imageWidth - 1)
        val right = (maxRight + paddingPx).toInt().coerceIn(left + 1, imageWidth)
        val top = (minTop - paddingPx).toInt().coerceIn(0, imageHeight - 1)
        val bottom = (maxBottom + paddingPx).toInt().coerceIn(top + 1, imageHeight)

        val rect = CropRect(left, top, right, bottom)
        return if (rect.isValid(minSizePx = 32)) rect else null
    }

    fun toSquareCrop(rect: CropRect, imageWidth: Int, imageHeight: Int): CropRect? {
        val desiredSize = maxOf(rect.width, rect.height)
        if (desiredSize < 24) return null

        val centerX = (rect.left + rect.right) / 2f
        val centerY = (rect.top + rect.bottom) / 2f

        val maxHalf = minOf(
            centerX,
            imageWidth - centerX,
            centerY,
            imageHeight - centerY,
            desiredSize / 2f,
        )
        val size = (maxHalf * 2f).toInt()
        if (size < 24) return null

        val left = (centerX - size / 2f).toInt().coerceIn(0, imageWidth - size)
        val top = (centerY - size / 2f).toInt().coerceIn(0, imageHeight - size)
        val square = CropRect(left, top, left + size, top + size)
        return if (square.isValid()) square else null
    }

    fun cropBitmap(source: Bitmap, rect: CropRect): Bitmap =
        Bitmap.createBitmap(source, rect.left, rect.top, rect.width, rect.height)

    private fun innerEdgeX(hand: HandFrame, towardOtherHand: Boolean): Float {
        if (hand.points.isEmpty()) {
            return if (towardOtherHand) hand.bounds.right else hand.bounds.left
        }
        return if (towardOtherHand) {
            hand.points.maxOf { it.x }
        } else {
            hand.points.minOf { it.x }
        }
    }

    private fun verticalSpan(leftHand: HandFrame, rightHand: HandFrame, verticalPad: Float): Pair<Float, Float> {
        val overlapTop = maxOf(leftHand.bounds.top, rightHand.bounds.top)
        val overlapBottom = minOf(leftHand.bounds.bottom, rightHand.bounds.bottom)
        if (overlapBottom > overlapTop) {
            return overlapTop - verticalPad to overlapBottom + verticalPad
        }

        val centerY = (leftHand.palmCenter.y + rightHand.palmCenter.y) / 2f
        val halfHeight = minOf(leftHand.bounds.height(), rightHand.bounds.height()) / 2f
        return (centerY - halfHeight - verticalPad) to (centerY + halfHeight + verticalPad)
    }

    private fun boundsFromPoints(points: List<PointF>): RectF {
        if (points.isEmpty()) return RectF()
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (point in points) {
            minX = minOf(minX, point.x)
            minY = minOf(minY, point.y)
            maxX = maxOf(maxX, point.x)
            maxY = maxOf(maxY, point.y)
        }
        return RectF(minX, minY, maxX, maxY)
    }
}
