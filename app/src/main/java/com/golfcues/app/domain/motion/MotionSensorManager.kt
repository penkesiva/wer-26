package com.golfcues.app.domain.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.golfcues.app.domain.model.MotionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class MotionSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val accelSamples = ArrayDeque<Float>(64)
    private val gyroSamples = ArrayDeque<Float>(64)
    private var lastStepTime = 0L
    private var stepCountLastMinute = 0

    private val _latestResult = MutableStateFlow(
        MotionStateResult(MotionState.UNKNOWN, 0f, 0f, 0)
    )
    val latestResult: StateFlow<MotionStateResult> = _latestResult.asStateFlow()

    private var registered = false

    fun start() {
        if (registered) return
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        registered = true
    }

    fun stop() {
        if (!registered) return
        sensorManager.unregisterListener(this)
        registered = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val magnitude = sqrt(
                    event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]
                )
                addSample(accelSamples, magnitude)
            }
            Sensor.TYPE_GYROSCOPE -> {
                val magnitude = sqrt(
                    event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]
                )
                addSample(gyroSamples, magnitude)
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                lastStepTime = System.currentTimeMillis()
                stepCountLastMinute++
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    fun evaluate(): MotionStateResult {
        val now = System.currentTimeMillis()
        if (now - lastStepTime > 60_000) {
            stepCountLastMinute = 0
        }

        val accelVar = variance(accelSamples)
        val gyroVar = variance(gyroSamples)
        val recentSteps = if (now - lastStepTime < 3_000) stepCountLastMinute else 0

        val state = classify(accelVar, gyroVar, recentSteps, now - lastStepTime)
        val result = MotionStateResult(state, accelVar, gyroVar, recentSteps)
        _latestResult.value = result
        return result
    }

    private fun classify(
        accelVar: Float,
        gyroVar: Float,
        recentSteps: Int,
        msSinceLastStep: Long
    ): MotionState {
        return when {
            accelVar > 4f && gyroVar > 1.5f && recentSteps > 2 -> MotionState.RUNNING
            accelVar > 1.5f || recentSteps > 0 || msSinceLastStep < 2_000 -> MotionState.WALKING
            accelVar > 0.8f && gyroVar > 0.5f -> MotionState.VEHICLE
            accelVar < 0.05f && gyroVar < 0.02f && recentSteps == 0 -> MotionState.IDLE
            accelVar < 0.15f && gyroVar < 0.08f && recentSteps == 0 -> MotionState.NEAR_IDLE
            accelVar < 0.3f && gyroVar < 0.15f -> MotionState.NEAR_IDLE
            else -> MotionState.UNKNOWN
        }
    }

    private fun addSample(buffer: ArrayDeque<Float>, value: Float) {
        if (buffer.size >= 64) buffer.removeFirst()
        buffer.addLast(value)
    }

    private fun variance(samples: Collection<Float>): Float {
        if (samples.isEmpty()) return 0f
        val mean = samples.average().toFloat()
        return samples.map { (it - mean) * (it - mean) }.average().toFloat()
    }
}
