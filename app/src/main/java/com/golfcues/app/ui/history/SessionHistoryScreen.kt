package com.golfcues.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.golfcues.app.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    onSessionClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SessionHistoryViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val completed = sessions.filter { it.endTimeMillis != null }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Session History") }) }
    ) { padding ->
        if (completed.isEmpty()) {
            Text(
                text = "No sessions yet. Start Golf Mode to begin.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                items(completed, key = { it.id }) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onSessionClick(session.id) },
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = FormatUtils.formatDate(session.startTimeMillis),
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "Start: ${FormatUtils.formatTime(session.startTimeMillis)}")
                            Text(text = "Duration: ${FormatUtils.formatDuration(session.durationMillis ?: 0)}")
                            Text(text = "Probable shots: ${session.totalProbableShots}")
                            Text(text = "Confirmed: ${session.confirmedShots} · False positives: ${session.rejectedShots}")
                        }
                    }
                }
            }
        }
    }
}
