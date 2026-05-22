package com.golfcues.app.data.repository

import com.golfcues.app.data.dao.SettingsDao
import com.golfcues.app.data.toDomain
import com.golfcues.app.data.toEntity
import com.golfcues.app.domain.model.DetectionSettings
import com.golfcues.app.domain.model.SensitivityLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dao: SettingsDao
) {
    fun observeSettings(): Flow<DetectionSettings> =
        dao.observe().map { it?.toDomain() ?: DetectionSettings() }

    suspend fun getSettings(): DetectionSettings =
        dao.get()?.toDomain() ?: DetectionSettings().also { saveSettings(it) }

    suspend fun saveSettings(settings: DetectionSettings) {
        dao.upsert(settings.toEntity())
    }

    suspend fun ensureDefaults() {
        if (dao.get() == null) {
            dao.upsert(DetectionSettings().toEntity())
        }
    }

    suspend fun clearAllData(sessionRepo: GolfSessionRepository, eventRepo: ShotEventRepository) {
        eventRepo.clearAll()
        sessionRepo.clearAll()
        dao.upsert(
            DetectionSettings(
                sensitivity = SensitivityLevel.NORMAL,
                motionGateEnabled = true,
                cooldownMillis = 5_000L,
                saveAudioSnippets = false
            ).toEntity()
        )
    }
}
