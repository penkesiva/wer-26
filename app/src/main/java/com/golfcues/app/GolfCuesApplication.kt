package com.golfcues.app

import android.app.Application
import com.golfcues.app.data.db.GolfCuesDatabase
import com.golfcues.app.data.repository.GolfSessionRepository
import com.golfcues.app.data.repository.SettingsRepository
import com.golfcues.app.data.repository.ShotEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GolfCuesApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { GolfCuesDatabase.getInstance(this) }
    val sessionRepository by lazy { GolfSessionRepository(database.golfSessionDao()) }
    val eventRepository by lazy { ShotEventRepository(database.shotEventDao()) }
    val settingsRepository by lazy { SettingsRepository(database.settingsDao()) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            settingsRepository.ensureDefaults()
        }
    }
}
