package com.golfcues.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.golfcues.app.GolfCuesApplication
import com.golfcues.app.domain.model.FeedbackType
import com.golfcues.app.domain.model.GolfSession
import com.golfcues.app.domain.model.ShotEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val session: GolfSession? = null,
    val events: List<ShotEvent> = emptyList()
)

class SessionDetailViewModel(
    application: Application,
    private val sessionId: String
) : AndroidViewModel(application) {
    private val app = application as GolfCuesApplication

    val uiState: StateFlow<SessionDetailUiState> = combine(
        app.sessionRepository.observeSession(sessionId),
        app.eventRepository.observeEvents(sessionId)
    ) { session, events ->
        SessionDetailUiState(session, events)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionDetailUiState())

    fun submitFeedback(event: ShotEvent, feedback: FeedbackType) {
        viewModelScope.launch {
            app.eventRepository.updateEvent(event.copy(feedbackType = feedback))
            val session = app.sessionRepository.observeSession(sessionId)
            // Update counts via repository
            uiState.value.session?.let { s ->
                val updated = when (feedback) {
                    FeedbackType.CORRECT -> s.copy(confirmedShots = s.confirmedShots + 1)
                    FeedbackType.NOT_A_SHOT -> s.copy(rejectedShots = s.rejectedShots + 1)
                    FeedbackType.MISSED_SHOT -> s.copy(missedShots = s.missedShots + 1)
                    FeedbackType.UNMARKED -> s
                }
                app.sessionRepository.updateSession(updated)
            }
        }
    }
}
