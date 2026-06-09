package com.visualselect.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var autofocusEnabled by mutableStateOf(prefs.getBoolean(KEY_AUTOFOCUS, false))
        private set

    var preferWideLens by mutableStateOf(prefs.getBoolean(KEY_PREFER_WIDE_LENS, true))
        private set

    var zoomRatio by mutableFloatStateOf(prefs.getFloat(KEY_ZOOM_RATIO, 0.5f))
        private set

    var chimeOnTwoHands by mutableStateOf(prefs.getBoolean(KEY_CHIME, true))
        private set

    var cropPaddingPx by mutableIntStateOf(prefs.getInt(KEY_CROP_PADDING, 24))
        private set

    var autoSaveEnabled by mutableStateOf(prefs.getBoolean(KEY_AUTO_SAVE, true))
        private set

    var autoSaveStabilitySec by mutableFloatStateOf(prefs.getFloat(KEY_AUTO_SAVE_STABILITY, 1.5f))
        private set

    var autoSaveCooldownSec by mutableFloatStateOf(prefs.getFloat(KEY_AUTO_SAVE_COOLDOWN, 2f))
        private set

    /** Bumps when any camera-related setting changes so the preview rebinds. */
    var cameraConfigVersion by mutableIntStateOf(0)
        private set

    fun updateAutofocusEnabled(enabled: Boolean) {
        autofocusEnabled = enabled
        prefs.edit().putBoolean(KEY_AUTOFOCUS, enabled).apply()
        bumpCameraConfig()
    }

    fun updatePreferWideLens(enabled: Boolean) {
        preferWideLens = enabled
        prefs.edit().putBoolean(KEY_PREFER_WIDE_LENS, enabled).apply()
        bumpCameraConfig()
    }

    fun updateZoomRatio(ratio: Float) {
        zoomRatio = ratio.coerceIn(0.5f, 1f)
        prefs.edit().putFloat(KEY_ZOOM_RATIO, zoomRatio).apply()
        bumpCameraConfig()
    }

    fun updateChimeOnTwoHands(enabled: Boolean) {
        chimeOnTwoHands = enabled
        prefs.edit().putBoolean(KEY_CHIME, enabled).apply()
    }

    fun updateCropPaddingPx(padding: Int) {
        cropPaddingPx = padding.coerceIn(8, 64)
        prefs.edit().putInt(KEY_CROP_PADDING, cropPaddingPx).apply()
    }

    fun updateAutoSaveEnabled(enabled: Boolean) {
        autoSaveEnabled = enabled
        prefs.edit().putBoolean(KEY_AUTO_SAVE, enabled).apply()
    }

    fun updateAutoSaveStabilitySec(seconds: Float) {
        autoSaveStabilitySec = seconds.coerceIn(1f, 3f)
        prefs.edit().putFloat(KEY_AUTO_SAVE_STABILITY, autoSaveStabilitySec).apply()
    }

    fun updateAutoSaveCooldownSec(seconds: Float) {
        autoSaveCooldownSec = seconds.coerceIn(2f, 10f)
        prefs.edit().putFloat(KEY_AUTO_SAVE_COOLDOWN, autoSaveCooldownSec).apply()
    }

    private fun bumpCameraConfig() {
        cameraConfigVersion += 1
    }

    companion object {
        private const val PREFS_NAME = "visual_select_settings"
        private const val KEY_AUTOFOCUS = "autofocus_enabled"
        private const val KEY_PREFER_WIDE_LENS = "prefer_wide_lens"
        private const val KEY_ZOOM_RATIO = "zoom_ratio"
        private const val KEY_CHIME = "chime_on_two_hands"
        private const val KEY_CROP_PADDING = "crop_padding_px"
        private const val KEY_AUTO_SAVE = "auto_save_enabled"
        private const val KEY_AUTO_SAVE_STABILITY = "auto_save_stability_sec"
        private const val KEY_AUTO_SAVE_COOLDOWN = "auto_save_cooldown_sec"
    }
}
