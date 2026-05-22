package com.golfcues.app.ml

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Converts PCM audio windows to log-mel spectrogram features for TFLite inference.
 */
class AudioFeatureExtractor(
    private val sampleRate: Int = 16_000,
    private val fftSize: Int = 1024,
    private val melBins: Int = 64,
    private val windowMs: Int = 25,
    private val hopMs: Int = 10
) {
    private val windowSamples = sampleRate * windowMs / 1000
    private val hopSamples = sampleRate * hopMs / 1000
    private val melFilters = buildMelFilterbank()

    fun extract(audioBuffer: ShortArray): Array<FloatArray> {
        val frames = mutableListOf<FloatArray>()
        var offset = 0
        while (offset + windowSamples <= audioBuffer.size) {
            val frame = FloatArray(windowSamples) { i ->
                audioBuffer[offset + i] / 32768f
            }
            val windowed = applyHannWindow(frame)
            val spectrum = fftMagnitude(windowed)
            val mel = applyMelFilterbank(spectrum)
            val logMel = FloatArray(melBins) { i ->
                ln(max(mel[i], 1e-10f))
            }
            frames.add(logMel)
            offset += hopSamples
        }
        return if (frames.isEmpty()) {
            arrayOf(FloatArray(melBins))
        } else {
            frames.toTypedArray()
        }
    }

    fun flattenFeatures(spectrogram: Array<FloatArray>): FloatArray {
        val flat = FloatArray(spectrogram.size * melBins)
        var idx = 0
        for (frame in spectrogram) {
            for (value in frame) {
                flat[idx++] = value
            }
        }
        return flat
    }

    private fun applyHannWindow(frame: FloatArray): FloatArray {
        val n = frame.size
        return FloatArray(n) { i ->
            frame[i] * (0.5f * (1f - cos(2.0 * Math.PI * i / (n - 1)).toFloat()))
        }
    }

    private fun fftMagnitude(frame: FloatArray): FloatArray {
        val n = fftSize
        val re = FloatArray(n)
        val im = FloatArray(n)
        for (i in frame.indices) {
            if (i < n) re[i] = frame[i]
        }
        fftInPlace(re, im)
        val bins = n / 2 + 1
        return FloatArray(bins) { k ->
            sqrt(re[k] * re[k] + im[k] * im[k])
        }
    }

    private fun applyMelFilterbank(spectrum: FloatArray): FloatArray {
        val mel = FloatArray(melBins)
        for (m in 0 until melBins) {
            var sum = 0f
            for (k in spectrum.indices) {
                sum += spectrum[k] * melFilters[m][k]
            }
            mel[m] = sum
        }
        return mel
    }

    private fun buildMelFilterbank(): Array<FloatArray> {
        val numFftBins = fftSize / 2 + 1
        val filters = Array(melBins) { FloatArray(numFftBins) }
        val melMin = hzToMel(0f)
        val melMax = hzToMel(sampleRate / 2f)
        val melPoints = FloatArray(melBins + 2) { i ->
            melMin + i * (melMax - melMin) / (melBins + 1)
        }
        val hzPoints = FloatArray(melPoints.size) { melToHz(melPoints[it]) }
        val binPoints = IntArray(hzPoints.size) { i ->
            ((fftSize + 1) * hzPoints[i] / sampleRate).toInt().coerceIn(0, numFftBins - 1)
        }
        for (m in 0 until melBins) {
            val left = binPoints[m]
            val center = binPoints[m + 1]
            val right = binPoints[m + 2]
            for (k in left until center) {
                if (center > left) {
                    filters[m][k] = (k - left).toFloat() / (center - left)
                }
            }
            for (k in center until right) {
                if (right > center) {
                    filters[m][k] = (right - k).toFloat() / (right - center)
                }
            }
        }
        return filters
    }

    private fun hzToMel(hz: Float): Float =
        (2595f * ln(1f + hz / 700f) / ln(10f))

    private fun melToHz(mel: Float): Float =
        700f * (Math.pow(10.0, (mel / 2595f).toDouble()).toFloat() - 1f)

    private fun fftInPlace(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = (-2.0 * Math.PI / len).toFloat()
            val wlenRe = cos(ang.toDouble()).toFloat()
            val wlenIm = sinApprox(ang)
            var i = 0
            while (i < n) {
                var wRe = 1f
                var wIm = 0f
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + len / 2] * wRe - im[i + k + len / 2] * wIm
                    val vIm = re[i + k + len / 2] * wIm + im[i + k + len / 2] * wRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + len / 2] = uRe - vRe
                    im[i + k + len / 2] = uIm - vIm
                    val nextWRe = wRe * wlenRe - wIm * wlenIm
                    wIm = wRe * wlenIm + wIm * wlenRe
                    wRe = nextWRe
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun sinApprox(x: Float): Float {
        val x2 = x * x
        return x * (1f - x2 / 6f * (1f - x2 / 20f))
    }
}
