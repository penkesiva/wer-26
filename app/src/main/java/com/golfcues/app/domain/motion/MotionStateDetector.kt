package com.golfcues.app.domain.motion

import com.golfcues.app.domain.model.MotionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MotionStateDetector(
    private val scope: CoroutineScope,
    private val sensorManager: MotionSensorManager,
    private val onUpdate: (MotionStateResult) -> Unit
) {
    private var job: Job? = null

    fun start() {
        sensorManager.start()
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                onUpdate(sensorManager.evaluate())
                delay(1_000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        sensorManager.stop()
    }

    fun currentState(): MotionState = sensorManager.latestResult.value.state
}
