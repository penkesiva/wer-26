package com.golfcues.app.domain.audio

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Placeholder classifier for development. Generates confidence from audio energy
 * spikes to simulate golf hit detection during testing.
 */
class FakeAudioClassifier : AudioClassifier {
    private var lastSpikeTime = 0L

    override fun classify(audioBuffer: ShortArray): AudioClassificationResult {
        if (audioBuffer.isEmpty()) {
            return idleResult()
        }

        val rms = sqrt(audioBuffer.map { it.toDouble() * it }.average())
        val peak = audioBuffer.maxOf { abs(it.toInt()) }
        val now = System.currentTimeMillis()

        // Simulate occasional high-confidence hits on loud transient sounds
        val isSpike = peak > 12000 && rms > 2000
        val golfHit = when {
            isSpike && now - lastSpikeTime > 800 -> {
                lastSpikeTime = now
                0.85f + Random.nextFloat() * 0.12f
            }
            rms > 1500 -> 0.45f + Random.nextFloat() * 0.2f
            else -> 0.05f + Random.nextFloat() * 0.15f
        }

        val speech = if (rms in 800.0..2500.0) 0.3f + Random.nextFloat() * 0.3f else 0.1f
        val wind = if (rms < 500) 0.2f + Random.nextFloat() * 0.2f else 0.05f
        val footsteps = if (rms in 600.0..1800.0) 0.15f + Random.nextFloat() * 0.2f else 0.05f

        return AudioClassificationResult(
            golfHit = golfHit.coerceIn(0f, 1f),
            nonHit = (1f - golfHit) * 0.4f,
            speech = speech.coerceIn(0f, 1f),
            clubRattle = if (isSpike) 0.2f else 0.05f,
            wind = wind.coerceIn(0f, 1f),
            cartNoise = 0.05f,
            footsteps = footsteps.coerceIn(0f, 1f),
            background = 0.1f
        )
    }

    override fun close() = Unit

    private fun idleResult() = AudioClassificationResult(
        golfHit = 0f, nonHit = 1f, speech = 0f, clubRattle = 0f,
        wind = 0f, cartNoise = 0f, footsteps = 0f, background = 1f
    )
}
