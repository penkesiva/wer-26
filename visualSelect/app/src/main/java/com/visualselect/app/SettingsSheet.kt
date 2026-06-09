package com.visualselect.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
    val scrollState = rememberScrollState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleMedium,
            )

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

            SettingLabel(text = stringResource(R.string.settings_zoom, settings.zoomRatio))
            Slider(
                value = settings.zoomRatio,
                onValueChange = settings::updateZoomRatio,
                valueRange = 0.5f..1f,
                steps = 4,
            )
            SettingHint(text = stringResource(R.string.settings_zoom_hint))

            SettingToggle(
                label = stringResource(R.string.settings_chime),
                subtitle = stringResource(R.string.settings_chime_hint),
                checked = settings.chimeOnTwoHands,
                onCheckedChange = settings::updateChimeOnTwoHands,
            )

            SettingToggle(
                label = stringResource(R.string.settings_auto_save),
                subtitle = stringResource(R.string.settings_auto_save_hint),
                checked = settings.autoSaveEnabled,
                onCheckedChange = settings::updateAutoSaveEnabled,
            )

            if (settings.autoSaveEnabled) {
                SettingLabel(
                    text = stringResource(R.string.settings_auto_save_stability, settings.autoSaveStabilitySec),
                )
                Slider(
                    value = settings.autoSaveStabilitySec,
                    onValueChange = settings::updateAutoSaveStabilitySec,
                    valueRange = 1f..3f,
                    steps = 3,
                )
                SettingHint(text = stringResource(R.string.settings_auto_save_stability_hint))

                SettingLabel(
                    text = stringResource(R.string.settings_auto_save_cooldown, settings.autoSaveCooldownSec),
                )
                Slider(
                    value = settings.autoSaveCooldownSec,
                    onValueChange = settings::updateAutoSaveCooldownSec,
                    valueRange = 2f..10f,
                    steps = 7,
                )
                SettingHint(text = stringResource(R.string.settings_auto_save_cooldown_hint))
            }

            SettingToggle(
                label = stringResource(R.string.settings_include_hands),
                subtitle = stringResource(R.string.settings_include_hands_hint),
                checked = settings.includeHandsInCrop,
                onCheckedChange = settings::updateIncludeHandsInCrop,
            )

            SettingToggle(
                label = stringResource(R.string.settings_square_crop),
                subtitle = stringResource(R.string.settings_square_crop_hint),
                checked = settings.squareCrop,
                onCheckedChange = settings::updateSquareCrop,
            )

            SettingLabel(text = stringResource(R.string.settings_crop_padding, settings.cropPaddingPx))
            Slider(
                value = settings.cropPaddingPx.toFloat(),
                onValueChange = { settings.updateCropPaddingPx(it.toInt()) },
                valueRange = 0f..24f,
                steps = 8,
            )
            SettingHint(text = stringResource(R.string.settings_crop_padding_hint))

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun SettingHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
    )
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
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
