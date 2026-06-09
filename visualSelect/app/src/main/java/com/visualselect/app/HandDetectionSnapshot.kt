package com.visualselect.app

import android.graphics.RectF

data class HandDetectionSnapshot(
    val handCount: Int = 0,
    val cropRect: BetweenHandsCropper.CropRect? = null,
    val handBoxes: List<RectF> = emptyList(),
) {
    val isReady: Boolean get() = handCount >= 2 && cropRect != null
}
