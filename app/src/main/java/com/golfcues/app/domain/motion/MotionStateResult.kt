package com.golfcues.app.domain.motion

import com.golfcues.app.domain.model.MotionState

data class MotionStateResult(
    val state: MotionState,
    val accelerometerVariance: Float,
    val gyroscopeVariance: Float,
    val recentSteps: Int
)
