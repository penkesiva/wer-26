package com.visualselect.app

/**
 * Fires when two hands and a crop stay stable long enough, then enforces a cooldown
 * before the next save (hands can stay in frame).
 */
class AutoSaveTracker(
    stabilityMs: Long = 1500L,
    cooldownMs: Long = 3000L,
    private val maxCropMovementPx: Int = 48,
) {
    var stabilityMs: Long = stabilityMs
    var cooldownMs: Long = cooldownMs

    private var stableSinceMs: Long? = null
    private var lastCrop: BetweenHandsCropper.CropRect? = null
    private var lastSaveMs: Long = 0L

    data class Evaluation(
        val progressMs: Long,
        val shouldSave: Boolean,
        val inCooldown: Boolean,
        val cooldownRemainingMs: Long,
    )

    fun evaluate(
        ready: Boolean,
        cropRect: BetweenHandsCropper.CropRect?,
        nowMs: Long,
    ): Evaluation {
        if (!ready || cropRect == null) {
            resetStability()
            return Evaluation(
                progressMs = 0L,
                shouldSave = false,
                inCooldown = false,
                cooldownRemainingMs = 0L,
            )
        }

        val inCooldown = nowMs - lastSaveMs < cooldownMs
        val cooldownRemainingMs = if (inCooldown) cooldownMs - (nowMs - lastSaveMs) else 0L

        updateStability(cropRect, nowMs)
        val since = stableSinceMs ?: return Evaluation(0L, false, inCooldown, cooldownRemainingMs)
        val elapsed = nowMs - since
        val progressMs = if (inCooldown) 0L else elapsed.coerceAtMost(stabilityMs)
        val shouldSave = !inCooldown && elapsed >= stabilityMs

        return Evaluation(
            progressMs = progressMs,
            shouldSave = shouldSave,
            inCooldown = inCooldown,
            cooldownRemainingMs = cooldownRemainingMs,
        )
    }

    fun markSaved(nowMs: Long) {
        lastSaveMs = nowMs
        resetStability()
    }

    private fun updateStability(cropRect: BetweenHandsCropper.CropRect, nowMs: Long) {
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
