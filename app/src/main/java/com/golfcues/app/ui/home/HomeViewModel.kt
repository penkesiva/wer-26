package com.golfcues.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.golfcues.app.GolfCuesApplication
import com.golfcues.app.domain.model.GolfSession
import com.golfcues.app.service.GolfModeForegroundService
import com.golfcues.app.service.GolfModeSessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isSessionActive: Boolean = false,
    val recentSession: GolfSession? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GolfCuesApplication

    val uiState: StateFlow<HomeUiState> = GolfModeSessionState.state
        .map { live ->
            HomeUiState(isSessionActive = live.isActive)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val recentSessions = app.sessionRepository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startGolfMode() {
        GolfModeForegroundService.start(getApplication())
    }

    fun stopGolfMode() {
        GolfModeForegroundService.stop(getApplication())
    }

    fun refreshRecent() {
        viewModelScope.launch {
            app.sessionRepository.getActiveSession()
        }
    }
}
