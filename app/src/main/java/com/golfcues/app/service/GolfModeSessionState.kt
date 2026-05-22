package com.golfcues.app.service

import com.golfcues.app.domain.model.AudioStatus
import com.golfcues.app.domain.model.GolfSession
import com.golfcues.app.domain.model.MotionState
import com.golfcues.app.domain.model.ShotEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveSessionState(
    val isActive: Boolean = false,
    val session: GolfSession? = null,
    val elapsedMillis: Long = 0L,
    val audioStatus: AudioStatus = AudioStatus.LISTENING,
    val motionState: MotionState = MotionState.UNKNOWN,
    val latestConfidence: Float = 0f,
    val shotCount: Int = 0,
    val events: List<ShotEvent> = emptyList()
)

object GolfModeSessionState {
    private val _state = MutableStateFlow(LiveSessionState())
    val state: StateFlow<LiveSessionState> = _state.asStateFlow()

    fun update(transform: (LiveSessionState) -> LiveSessionState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = LiveSessionState()
    }
}
