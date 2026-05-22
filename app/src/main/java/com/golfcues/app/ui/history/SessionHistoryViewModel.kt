package com.golfcues.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.golfcues.app.GolfCuesApplication
import com.golfcues.app.domain.model.GolfSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SessionHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GolfCuesApplication

    val sessions: StateFlow<List<GolfSession>> = app.sessionRepository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
