package com.golfcues.app.ui.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golfcues.app.domain.model.AudioStatus
import com.golfcues.app.domain.model.EventType
import com.golfcues.app.ui.components.PrimaryButton
import com.golfcues.app.ui.components.SecondaryButton
import com.golfcues.app.ui.components.ShotEventCard
import com.golfcues.app.ui.theme.GolfGreen
import com.golfcues.app.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveGolfModeScreen(
    onBack: () -> Unit,
    viewModel: LiveGolfModeViewModel = viewModel()
) {
    val state by viewModel.liveState.collectAsState()
    val timelineEvents = state.events.filter {
        it.eventType == EventType.PROBABLE_SHOT ||
            it.eventType == EventType.MANUAL_MISSED_SHOT
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Live Golf Mode") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = if (state.isActive) "Golf Mode Active" else "Golf Mode Inactive",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GolfGreen
            )
            Text(
                text = "Elapsed: ${FormatUtils.formatDuration(state.elapsedMillis)}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${state.shotCount}",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = "Shots Detected",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusChip(label = "Audio", value = formatAudioStatus(state.audioStatus))
                StatusChip(
                    label = "Motion",
                    value = state.motionState.name.lowercase().replace('_', ' ')
                        .replaceFirstChar { it.uppercase() }
                )
            }

            Text(
                text = "Latest confidence: ${FormatUtils.formatConfidence(state.latestConfidence)}",
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = if (state.isActive) "Listening for shots" else "Not listening",
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            SecondaryButton(text = "Log Missed Shot", onClick = viewModel::addMissedShot)

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Timeline", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(timelineEvents, key = { it.id }) { event ->
                    ShotEventCard(
                        event = event,
                        onFeedback = { feedback -> viewModel.submitFeedback(event, feedback) }
                    )
                }
            }

            PrimaryButton(
                text = "Stop Golf Mode",
                onClick = {
                    viewModel.stopGolfMode()
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, value: String) {
    Column {
        Text(text = label, fontWeight = FontWeight.Bold)
        Text(text = value)
    }
}

private fun formatAudioStatus(status: AudioStatus): String = when (status) {
    AudioStatus.LISTENING -> "Listening"
    AudioStatus.POSSIBLE_HIT_DETECTED -> "Possible hit"
    AudioStatus.NOISE_IGNORED -> "Noise ignored"
    AudioStatus.MUTED_OR_PERMISSION_MISSING -> "Muted / no permission"
}
