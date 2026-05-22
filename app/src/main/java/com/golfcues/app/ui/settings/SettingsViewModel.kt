package com.golfcues.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.golfcues.app.GolfCuesApplication
import com.golfcues.app.domain.model.DetectionSettings
import com.golfcues.app.domain.model.SensitivityLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GolfCuesApplication

    val settings: StateFlow<DetectionSettings> = app.settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetectionSettings())

    fun updateSensitivity(level: SensitivityLevel) {
        viewModelScope.launch {
            val current = settings.value
            app.settingsRepository.saveSettings(current.copy(sensitivity = level))
        }
    }

    fun updateMotionGate(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            app.settingsRepository.saveSettings(current.copy(motionGateEnabled = enabled))
        }
    }

    fun updateCooldown(seconds: Int) {
        viewModelScope.launch {
            val current = settings.value
            app.settingsRepository.saveSettings(current.copy(cooldownMillis = seconds * 1000L))
        }
    }

    fun updateSaveSnippets(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            app.settingsRepository.saveSettings(current.copy(saveAudioSnippets = enabled))
        }
    }

    fun clearLocalData(onComplete: () -> Unit) {
        viewModelScope.launch {
            app.settingsRepository.clearAllData(app.sessionRepository, app.eventRepository)
            onComplete()
        }
    }
}
