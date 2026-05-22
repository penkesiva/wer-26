package com.golfcues.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "golf_sessions")
data class GolfSessionEntity(
    @PrimaryKey val id: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val durationMillis: Long?,
    val totalProbableShots: Int,
    val confirmedShots: Int,
    val rejectedShots: Int,
    val missedShots: Int
)

@Entity(tableName = "shot_events")
data class ShotEventEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val timestampMillis: Long,
    val eventType: String,
    val audioConfidence: Float,
    val motionState: String,
    val feedbackType: String,
    val audioSnippetPath: String?,
    val ignoreReason: String?
)

@Entity(tableName = "detection_settings")
data class DetectionSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val sensitivity: String,
    val motionGateEnabled: Boolean,
    val cooldownMillis: Long,
    val saveAudioSnippets: Boolean
)
