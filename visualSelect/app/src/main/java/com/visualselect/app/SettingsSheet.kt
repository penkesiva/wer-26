package com.visualselect.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(text = stringResource(R.string.settings_title))

            SettingToggle(
                label = stringResource(R.string.settings_autofocus),
                subtitle = stringResource(R.string.settings_autofocus_hint),
                checked = settings.autofocusEnabled,
                onCheckedChange = settings::updateAutofocusEnabled,
            )

            SettingToggle(
                label = stringResource(R.string.settings_wide_lens),
                subtitle = stringResource(R.string.settings_wide_lens_hint),
                checked = settings.preferWideLens,
                onCheckedChange = settings::updatePreferWideLens,
            )

            Text(
                text = stringResource(R.string.settings_zoom, settings.zoomRatio),
                modifier = Modifier.padding(top = 16.dp),
            )
            Slider(
                value = settings.zoomRatio,
                onValueChange = settings::updateZoomRatio,
                valueRange = 0.5f..1f,
                steps = 4,
            )
            Text(text = stringResource(R.string.settings_zoom_hint))

            SettingToggle(
                label = stringResource(R.string.settings_chime),
                subtitle = stringResource(R.string.settings_chime_hint),
                checked = settings.chimeOnTwoHands,
                onCheckedChange = settings::updateChimeOnTwoHands,
                modifier = Modifier.padding(top = 8.dp),
            )

            SettingToggle(
                label = stringResource(R.string.settings_auto_save),
                subtitle = stringResource(R.string.settings_auto_save_hint),
                checked = settings.autoSaveEnabled,
                onCheckedChange = settings::updateAutoSaveEnabled,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (settings.autoSaveEnabled) {
                Text(
                    text = stringResource(R.string.settings_auto_save_stability, settings.autoSaveStabilitySec),
                    modifier = Modifier.padding(top = 16.dp),
                )
                Slider(
                    value = settings.autoSaveStabilitySec,
                    onValueChange = settings::updateAutoSaveStabilitySec,
                    valueRange = 1f..3f,
                    steps = 3,
                )
                Text(text = stringResource(R.string.settings_auto_save_stability_hint))

                Text(
                    text = stringResource(R.string.settings_auto_save_cooldown, settings.autoSaveCooldownSec),
                    modifier = Modifier.padding(top = 16.dp),
                )
                Slider(
                    value = settings.autoSaveCooldownSec,
                    onValueChange = settings::updateAutoSaveCooldownSec,
                    valueRange = 2f..10f,
                    steps = 7,
                )
                Text(text = stringResource(R.string.settings_auto_save_cooldown_hint))
            }

            Text(
                text = stringResource(R.string.settings_crop_padding, settings.cropPaddingPx),
                modifier = Modifier.padding(top = 16.dp),
            )
            Slider(
                value = settings.cropPaddingPx.toFloat(),
                onValueChange = { settings.updateCropPaddingPx(it.toInt()) },
                valueRange = 8f..64f,
                steps = 6,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label)
            Text(text = subtitle)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
