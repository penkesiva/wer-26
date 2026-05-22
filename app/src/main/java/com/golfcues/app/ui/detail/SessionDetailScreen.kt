package com.golfcues.app.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golfcues.app.domain.model.EventType
import com.golfcues.app.ui.components.ShotEventCard
import com.golfcues.app.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit
) {
    val viewModel: SessionDetailViewModel = viewModel(
        factory = SessionDetailViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
            sessionId
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.session
    val events = uiState.events.filter {
        it.eventType != EventType.IGNORED_AUDIO_HIT
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Session Detail") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            session?.let {
                Text(
                    text = FormatUtils.formatDateTime(it.startTimeMillis),
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Duration: ${FormatUtils.formatDuration(it.durationMillis ?: 0)}")
                Text(text = "Total events: ${events.size}")
                Text(text = "Confirmed: ${it.confirmedShots} · Rejected: ${it.rejectedShots} · Missed: ${it.missedShots}")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Timeline", fontWeight = FontWeight.Bold)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(events, key = { it.id }) { event ->
                    ShotEventCard(
                        event = event,
                        showFeedback = false
                    )
                }
            }
        }
    }
}
