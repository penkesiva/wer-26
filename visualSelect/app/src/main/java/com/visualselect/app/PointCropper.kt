package com.visualselect.app

import android.graphics.PointF

object PointCropper {

    fun computePointCrop(
        aimPoint: PointF,
        imageWidth: Int,
        imageHeight: Int,
        paddingPx: Int = 8,
        squareCrop: Boolean = true,
    ): BetweenHandsCropper.CropRect? {
        if (imageWidth <= 0 || imageHeight <= 0) return null

        val paddingBoost = paddingPx / 24f * 0.06f
        val sizeFraction = (0.24f + paddingBoost).coerceIn(0.18f, 0.34f)
        val baseSize = (minOf(imageWidth, imageHeight) * sizeFraction).toInt().coerceIn(96, 560)

        val half = baseSize / 2f
        var left = (aimPoint.x - half).toInt()
        var top = (aimPoint.y - half).toInt()
        var right = left + baseSize
        var bottom = top + baseSize

        if (left < 0) {
            right -= left
            left = 0
        }
        if (top < 0) {
            bottom -= top
            top = 0
        }
        if (right > imageWidth) {
            left -= right - imageWidth
            right = imageWidth
        }
        if (bottom > imageHeight) {
            top -= bottom - imageHeight
            bottom = imageHeight
        }

        left = left.coerceIn(0, imageWidth - 1)
        top = top.coerceIn(0, imageHeight - 1)
        right = right.coerceIn(left + 1, imageWidth)
        bottom = bottom.coerceIn(top + 1, imageHeight)

        val rect = BetweenHandsCropper.CropRect(left, top, right, bottom)
        return if (squareCrop) {
            BetweenHandsCropper.toSquareCrop(rect, imageWidth, imageHeight)
        } else {
            if (rect.isValid()) rect else null
        }
    }
}
