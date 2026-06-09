package com.visualselect.app

/**
 * Fires once when two hands and a crop stay stable long enough, then waits for hands
 * to leave before arming again.
 */
class AutoSaveTracker(
    stabilityMs: Long = 1500L,
    cooldownMs: Long = 3000L,
    private val maxCropMovementPx: Int = 24,
) {
    var stabilityMs: Long = stabilityMs
    var cooldownMs: Long = cooldownMs

    private var stableSinceMs: Long? = null
    private var lastCrop: BetweenHandsCropper.CropRect? = null
    private var lastSaveMs: Long = 0
    private var armedForGesture: Boolean = true

    fun stableProgressMs(ready: Boolean, cropRect: BetweenHandsCropper.CropRect?, nowMs: Long): Long {
        if (!ready || cropRect == null) return 0L
        updateStability(ready, cropRect, nowMs)
        val since = stableSinceMs ?: return 0L
        return (nowMs - since).coerceAtMost(stabilityMs)
    }

    fun shouldSave(ready: Boolean, cropRect: BetweenHandsCropper.CropRect?, nowMs: Long): Boolean {
        if (!ready || cropRect == null) {
            if (!ready) {
                armedForGesture = true
            }
            resetStability()
            return false
        }

        updateStability(ready, cropRect, nowMs)

        if (!armedForGesture) return false

        val since = stableSinceMs ?: return false
        if (nowMs - since < stabilityMs) return false
        if (nowMs - lastSaveMs < cooldownMs) return false

        armedForGesture = false
        lastSaveMs = nowMs
        resetStability()
        return true
    }

    private fun updateStability(
        ready: Boolean,
        cropRect: BetweenHandsCropper.CropRect,
        nowMs: Long,
    ) {
        if (!ready) {
            resetStability()
            return
        }

        val previous = lastCrop
        if (previous != null && !cropStable(previous, cropRect)) {
            stableSinceMs = nowMs
        } else if (stableSinceMs == null) {
            stableSinceMs = nowMs
        }
        lastCrop = cropRect
    }

    private fun resetStability() {
        stableSinceMs = null
        lastCrop = null
    }

    private fun cropStable(a: BetweenHandsCropper.CropRect, b: BetweenHandsCropper.CropRect): Boolean =
        kotlin.math.abs(a.left - b.left) <= maxCropMovementPx &&
            kotlin.math.abs(a.top - b.top) <= maxCropMovementPx &&
            kotlin.math.abs(a.right - b.right) <= maxCropMovementPx &&
            kotlin.math.abs(a.bottom - b.bottom) <= maxCropMovementPx
}
