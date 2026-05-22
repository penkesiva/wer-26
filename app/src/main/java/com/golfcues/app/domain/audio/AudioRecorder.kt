package com.golfcues.app.domain.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class AudioRecorder(
    private val scope: CoroutineScope,
    private val onWindow: (ShortArray) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        const val SAMPLE_RATE = 16_000
        const val WINDOW_SAMPLES = SAMPLE_RATE // 1 second
        const val HOP_SAMPLES = SAMPLE_RATE / 4 // 250 ms
    }

    private var audioRecord: AudioRecord? = null
    private var job: Job? = null
    private val running = AtomicReference(false)

    fun start(): Boolean {
        if (running.get()) return true

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer == AudioRecord.ERROR || minBuffer == AudioRecord.ERROR_BAD_VALUE) {
            onError("Unable to determine audio buffer size")
            return false
        }

        val bufferSize = maxOf(minBuffer, WINDOW_SAMPLES * 2)
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            onError("AudioRecord failed to initialize")
            return false
        }

        audioRecord = record
        running.set(true)
        record.startRecording()

        job = scope.launch(Dispatchers.Default) {
            val ring = ShortArray(WINDOW_SAMPLES)
            var filled = 0

            while (isActive && running.get()) {
                val chunk = ShortArray(HOP_SAMPLES)
                val read = record.read(chunk, 0, chunk.size)
                if (read <= 0) continue

                for (i in 0 until read) {
                    ring[filled % WINDOW_SAMPLES] = chunk[i]
                    filled++
                    if (filled >= WINDOW_SAMPLES && filled % HOP_SAMPLES == 0) {
                        val window = ShortArray(WINDOW_SAMPLES)
                        val start = filled % WINDOW_SAMPLES
                        System.arraycopy(ring, start, window, 0, WINDOW_SAMPLES - start)
                        if (start > 0) {
                            System.arraycopy(ring, 0, window, WINDOW_SAMPLES - start, start)
                        }
                        onWindow(window)
                    }
                }
            }
        }
        return true
    }

    fun stop() {
        running.set(false)
        job?.cancel()
        job = null
        audioRecord?.let {
            try {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        audioRecord = null
    }

    fun isRunning(): Boolean = running.get()
}
