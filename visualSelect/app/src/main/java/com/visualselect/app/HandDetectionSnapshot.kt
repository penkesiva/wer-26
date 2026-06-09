package com.visualselect.app

import android.graphics.PointF
import android.graphics.RectF

data class HandDetectionSnapshot(
    val handCount: Int = 0,
    val cropRect: BetweenHandsCropper.CropRect? = null,
    val handBoxes: List<RectF> = emptyList(),
    val isPointing: Boolean = false,
    val aimPoint: PointF? = null,
    val gestureMode: GestureMode = GestureMode.TWO_HANDS,
) {
    val isReady: Boolean
        get() = when (gestureMode) {
            GestureMode.TWO_HANDS -> handCount >= 2 && cropRect != null
            GestureMode.ONE_FINGER_POINT -> handCount >= 1 && isPointing && cropRect != null
        }
}
