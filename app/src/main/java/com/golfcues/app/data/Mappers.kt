package com.golfcues.app.data

import com.golfcues.app.data.entity.DetectionSettingsEntity
import com.golfcues.app.data.entity.GolfSessionEntity
import com.golfcues.app.data.entity.ShotEventEntity
import com.golfcues.app.domain.model.DetectionSettings
import com.golfcues.app.domain.model.EventType
import com.golfcues.app.domain.model.FeedbackType
import com.golfcues.app.domain.model.GolfSession
import com.golfcues.app.domain.model.MotionState
import com.golfcues.app.domain.model.SensitivityLevel
import com.golfcues.app.domain.model.ShotEvent

fun GolfSessionEntity.toDomain(): GolfSession = GolfSession(
    id = id,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    durationMillis = durationMillis,
    totalProbableShots = totalProbableShots,
    confirmedShots = confirmedShots,
    rejectedShots = rejectedShots,
    missedShots = missedShots
)

fun GolfSession.toEntity(): GolfSessionEntity = GolfSessionEntity(
    id = id,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    durationMillis = durationMillis,
    totalProbableShots = totalProbableShots,
    confirmedShots = confirmedShots,
    rejectedShots = rejectedShots,
    missedShots = missedShots
)

fun ShotEventEntity.toDomain(): ShotEvent = ShotEvent(
    id = id,
    sessionId = sessionId,
    timestampMillis = timestampMillis,
    eventType = EventType.valueOf(eventType),
    audioConfidence = audioConfidence,
    motionState = MotionState.valueOf(motionState),
    feedbackType = FeedbackType.valueOf(feedbackType),
    audioSnippetPath = audioSnippetPath,
    ignoreReason = ignoreReason
)

fun ShotEvent.toEntity(): ShotEventEntity = ShotEventEntity(
    id = id,
    sessionId = sessionId,
    timestampMillis = timestampMillis,
    eventType = eventType.name,
    audioConfidence = audioConfidence,
    motionState = motionState.name,
    feedbackType = feedbackType.name,
    audioSnippetPath = audioSnippetPath,
    ignoreReason = ignoreReason
)

fun DetectionSettingsEntity.toDomain(): DetectionSettings = DetectionSettings(
    sensitivity = SensitivityLevel.fromName(sensitivity),
    motionGateEnabled = motionGateEnabled,
    cooldownMillis = cooldownMillis,
    saveAudioSnippets = saveAudioSnippets
)

fun DetectionSettings.toEntity(): DetectionSettingsEntity = DetectionSettingsEntity(
    sensitivity = sensitivity.name,
    motionGateEnabled = motionGateEnabled,
    cooldownMillis = cooldownMillis,
    saveAudioSnippets = saveAudioSnippets
)
