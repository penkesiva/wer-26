package com.golfcues.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golfcues.app.domain.model.SensitivityLevel
import com.golfcues.app.ui.components.SecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear local data?") },
            text = { Text("This deletes all sessions, events, and resets settings.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearLocalData { showClearDialog = false }
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(text = "Detection sensitivity", fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth()) {
                SensitivityLevel.entries.forEach { level ->
                    FilterChip(
                        selected = settings.sensitivity == level,
                        onClick = { viewModel.updateSensitivity(level) },
                        label = { Text(level.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingToggle(
                label = "Motion gating",
                checked = settings.motionGateEnabled,
                onCheckedChange = viewModel::updateMotionGate
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Cooldown duration", fontWeight = FontWeight.Bold)
            Row {
                listOf(3, 5, 10).forEach { seconds ->
                    FilterChip(
                        selected = settings.cooldownMillis == seconds * 1000L,
                        onClick = { viewModel.updateCooldown(seconds) },
                        label = { Text("${seconds}s") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingToggle(
                label = "Save audio snippets",
                checked = settings.saveAudioSnippets,
                onCheckedChange = viewModel::updateSaveSnippets
            )
            Text(
                text = "When enabled, 1 second before and after each detection is saved locally. " +
                    "Raw audio is not saved by default.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Privacy", fontWeight = FontWeight.Bold)
            Text(
                text = "GolfCues listens only while Golf Mode is active. Audio is processed on your " +
                    "device to detect golf impact sounds. Raw audio is not saved unless you enable audio snippets.",
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            SecondaryButton(text = "Clear local data", onClick = { showClearDialog = true })
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
