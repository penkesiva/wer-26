package com.golfcues.app.ui.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.golfcues.app.GolfCuesApplication
import com.golfcues.app.domain.model.EventType
import com.golfcues.app.domain.model.FeedbackType
import com.golfcues.app.domain.model.GolfSession
import com.golfcues.app.domain.model.ShotEvent
import com.golfcues.app.service.GolfModeForegroundService
import com.golfcues.app.service.GolfModeSessionState
import com.golfcues.app.service.LiveSessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class LiveGolfModeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GolfCuesApplication

    val liveState: StateFlow<LiveSessionState> = GolfModeSessionState.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LiveSessionState())

    fun stopGolfMode() {
        GolfModeForegroundService.stop(getApplication())
    }

    fun submitFeedback(event: ShotEvent, feedback: FeedbackType) {
        viewModelScope.launch {
            val updated = event.copy(feedbackType = feedback)
            app.eventRepository.updateEvent(updated)

            val session = app.sessionRepository.getActiveSession() ?: return@launch
            val adjusted = when (feedback) {
                FeedbackType.CORRECT -> session.copy(confirmedShots = session.confirmedShots + 1)
                FeedbackType.NOT_A_SHOT -> session.copy(rejectedShots = session.rejectedShots + 1)
                else -> session
            }
            app.sessionRepository.updateSession(adjusted)

            GolfModeSessionState.update { state ->
                state.copy(
                    session = adjusted,
                    events = state.events.map { if (it.id == event.id) updated else it }
                )
            }
        }
    }

    fun addMissedShot() {
        viewModelScope.launch {
            val session = app.sessionRepository.getActiveSession() ?: return@launch
            val event = ShotEvent(
                id = UUID.randomUUID().toString(),
                sessionId = session.id,
                timestampMillis = System.currentTimeMillis(),
                eventType = EventType.MANUAL_MISSED_SHOT,
                audioConfidence = 0f,
                motionState = liveState.value.motionState,
                feedbackType = FeedbackType.MISSED_SHOT,
                audioSnippetPath = null
            )
            app.eventRepository.insertEvent(event)
            val updated = session.copy(missedShots = session.missedShots + 1)
            app.sessionRepository.updateSession(updated)
            GolfModeSessionState.update {
                it.copy(
                    session = updated,
                    events = listOf(event) + it.events
                )
            }
        }
    }
}
