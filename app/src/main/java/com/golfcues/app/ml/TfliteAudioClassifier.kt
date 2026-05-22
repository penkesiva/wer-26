package com.golfcues.app.ml

import android.content.Context
import com.golfcues.app.domain.audio.AudioClassificationResult
import com.golfcues.app.domain.audio.AudioClassifier
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

class TfliteAudioClassifier(
    context: Context,
    private val modelAssetName: String = "golf_audio_classifier.tflite",
    private val featureExtractor: AudioFeatureExtractor = AudioFeatureExtractor()
) : AudioClassifier {

    private var interpreter: Interpreter? = null
    private var labels = listOf(
        "golf_hit", "non_hit", "speech", "club_rattle",
        "wind", "cart_noise", "footsteps", "background"
    )

    init {
        try {
            val buffer = loadModelFile(context, modelAssetName)
            interpreter = Interpreter(buffer)
        } catch (_: Exception) {
            interpreter = null
        }
    }

    override fun classify(audioBuffer: ShortArray): AudioClassificationResult {
        val model = interpreter ?: return fallbackFromEnergy(audioBuffer)

        return try {
            val spectrogram = featureExtractor.extract(audioBuffer)
            val input = Array(1) { featureExtractor.flattenFeatures(spectrogram) }
            val output = Array(1) { FloatArray(labels.size) }
            model.run(input, output)
            val scores = softmax(output[0])
            AudioClassificationResult(
                golfHit = scores.getOrElse(0) { 0f },
                nonHit = scores.getOrElse(1) { 0f },
                speech = scores.getOrElse(2) { 0f },
                clubRattle = scores.getOrElse(3) { 0f },
                wind = scores.getOrElse(4) { 0f },
                cartNoise = scores.getOrElse(5) { 0f },
                footsteps = scores.getOrElse(6) { 0f },
                background = scores.getOrElse(7) { 0f }
            )
        } catch (_: Exception) {
            fallbackFromEnergy(audioBuffer)
        }
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.max()
        val exps = FloatArray(logits.size) { exp((logits[it] - max).toDouble()).toFloat() }
        val sum = exps.sum()
        return FloatArray(logits.size) { exps[it] / sum }
    }

    private fun fallbackFromEnergy(audioBuffer: ShortArray): AudioClassificationResult {
        val energy = audioBuffer.map { it * it.toLong() }.average()
        val golfHit = (energy / 1_000_000.0).coerceIn(0.0, 1.0).toFloat()
        return AudioClassificationResult(
            golfHit = golfHit,
            nonHit = 1f - golfHit,
            speech = 0.1f,
            clubRattle = 0.05f,
            wind = 0.05f,
            cartNoise = 0.05f,
            footsteps = 0.05f,
            background = 0.1f
        )
    }

    private fun loadModelFile(context: Context, assetName: String): MappedByteBuffer {
        context.assets.openFd(assetName).use { fd ->
            FileInputStream(fd.fileDescriptor).use { stream ->
                return stream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fd.startOffset,
                    fd.declaredLength
                )
            }
        }
    }
}
