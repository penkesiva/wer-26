package com.golfcues.app.domain.model

enum class EventType {
    PROBABLE_SHOT,
    POSSIBLE_SHOT,
    IGNORED_AUDIO_HIT,
    MANUAL_MISSED_SHOT
}

enum class FeedbackType {
    UNMARKED,
    CORRECT,
    NOT_A_SHOT,
    MISSED_SHOT
}

enum class MotionState {
    IDLE,
    NEAR_IDLE,
    WALKING,
    RUNNING,
    VEHICLE,
    UNKNOWN
}

enum class SensitivityLevel {
    LOW,
    NORMAL,
    HIGH;

    fun threshold(): Float = when (this) {
        LOW -> 0.90f
        NORMAL -> 0.80f
        HIGH -> 0.70f
    }

    companion object {
        fun fromName(name: String): SensitivityLevel =
            entries.find { it.name == name } ?: NORMAL
    }
}

enum class AudioStatus {
    LISTENING,
    POSSIBLE_HIT_DETECTED,
    NOISE_IGNORED,
    MUTED_OR_PERMISSION_MISSING
}

data class GolfSession(
    val id: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val durationMillis: Long?,
    val totalProbableShots: Int,
    val confirmedShots: Int,
    val rejectedShots: Int,
    val missedShots: Int
)

data class ShotEvent(
    val id: String,
    val sessionId: String,
    val timestampMillis: Long,
    val eventType: EventType,
    val audioConfidence: Float,
    val motionState: MotionState,
    val feedbackType: FeedbackType,
    val audioSnippetPath: String?,
    val ignoreReason: String? = null
)

data class DetectionSettings(
    val sensitivity: SensitivityLevel = SensitivityLevel.NORMAL,
    val motionGateEnabled: Boolean = true,
    val cooldownMillis: Long = 5_000L,
    val saveAudioSnippets: Boolean = false
)
