package com.visualselect.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

class ChimePlayer(context: Context) {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)

    fun playTwoHandsChime() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 180)
    }

    fun release() {
        toneGenerator.release()
    }
}
