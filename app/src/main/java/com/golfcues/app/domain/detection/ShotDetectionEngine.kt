package com.golfcues.app.domain.detection

import com.golfcues.app.domain.model.DetectionSettings
import com.golfcues.app.domain.model.EventType
import com.golfcues.app.domain.model.MotionState
import com.golfcues.app.domain.model.SensitivityLevel

data class ShotDetectionConfig(
    val sensitivity: SensitivityLevel = SensitivityLevel.NORMAL,
    val motionGateEnabled: Boolean = true,
    val cooldownMillis: Long = 5_000L
) {
    companion object {
        fun fromSettings(settings: DetectionSettings) = ShotDetectionConfig(
            sensitivity = settings.sensitivity,
            motionGateEnabled = settings.motionGateEnabled,
            cooldownMillis = settings.cooldownMillis
        )
    }
}

sealed class ShotDetectionResult {
    data class Logged(
        val eventType: EventType,
        val confidence: Float,
        val motionState: MotionState
    ) : ShotDetectionResult()

    data class Ignored(
        val reason: String,
        val confidence: Float,
        val motionState: MotionState
    ) : ShotDetectionResult()

    data object NoAction : ShotDetectionResult()
}

class ShotDetectionEngine {
    private var lastLoggedShotTimestamp = 0L

    fun reset() {
        lastLoggedShotTimestamp = 0L
    }

    fun evaluate(
        audioHitConfidence: Float,
        motionState: MotionState,
        config: ShotDetectionConfig,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): ShotDetectionResult {
        val threshold = config.sensitivity.threshold()

        if (audioHitConfidence < threshold) {
            return if (audioHitConfidence >= threshold * 0.85f) {
                ShotDetectionResult.Ignored(
                    reason = "Ignored: low confidence",
                    confidence = audioHitConfidence,
                    motionState = motionState
                )
            } else {
                ShotDetectionResult.NoAction
            }
        }

        if (config.motionGateEnabled &&
            motionState !in listOf(MotionState.IDLE, MotionState.NEAR_IDLE)
        ) {
            return ShotDetectionResult.Ignored(
                reason = "Ignored: ${motionState.name.lowercase().replace('_', ' ')} detected",
                confidence = audioHitConfidence,
                motionState = motionState
            )
        }

        if (currentTimeMillis - lastLoggedShotTimestamp < config.cooldownMillis) {
            return ShotDetectionResult.Ignored(
                reason = "Ignored: cooldown active",
                confidence = audioHitConfidence,
                motionState = motionState
            )
        }

        lastLoggedShotTimestamp = currentTimeMillis
        return ShotDetectionResult.Logged(
            eventType = EventType.PROBABLE_SHOT,
            confidence = audioHitConfidence,
            motionState = motionState
        )
    }
}
