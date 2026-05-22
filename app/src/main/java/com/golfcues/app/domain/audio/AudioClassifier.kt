package com.golfcues.app.domain.audio

data class AudioClassificationResult(
    val golfHit: Float,
    val nonHit: Float,
    val speech: Float,
    val clubRattle: Float,
    val wind: Float,
    val cartNoise: Float,
    val footsteps: Float,
    val background: Float
) {
    val topConfidence: Float get() = golfHit
}

interface AudioClassifier {
    fun classify(audioBuffer: ShortArray): AudioClassificationResult
    fun close()
}
