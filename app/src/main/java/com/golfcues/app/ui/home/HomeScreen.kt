package com.golfcues.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golfcues.app.ui.components.PrimaryButton
import com.golfcues.app.ui.components.SecondaryButton
import com.golfcues.app.ui.theme.GolfGreen
import com.golfcues.app.utils.FormatUtils
import com.golfcues.app.utils.rememberGolfModePermissions

@Composable
fun HomeScreen(
    onNavigateLive: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessions by viewModel.recentSessions.collectAsState()
    val permissions = rememberGolfModePermissions()

    val latestCompleted = sessions.firstOrNull { it.endTimeMillis != null }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GolfCues",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = GolfGreen
            )
            Text(
                text = "Detect golf shots using sound and motion.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = if (uiState.isSessionActive) "Mode: Active" else "Mode: Inactive",
                fontWeight = FontWeight.SemiBold,
                color = if (uiState.isSessionActive) GolfGreen else MaterialTheme.colorScheme.onSurface.copy(0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.isSessionActive) {
                PrimaryButton(text = "View Live Session", onClick = onNavigateLive)
                Spacer(modifier = Modifier.height(12.dp))
                SecondaryButton(text = "Stop Golf Mode", onClick = viewModel::stopGolfMode)
            } else {
                PrimaryButton(
                    text = "Start Golf Mode",
                    onClick = {
                        if (permissions.allGranted) {
                            viewModel.startGolfMode()
                            onNavigateLive()
                        } else {
                            permissions.requestPermissions()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            latestCompleted?.let { session ->
                Text(
                    text = "Recent Session",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "${FormatUtils.formatDate(session.startTimeMillis)} · " +
                        "${session.totalProbableShots} shots · " +
                        "${FormatUtils.formatDuration(session.durationMillis ?: 0)}",
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SecondaryButton(text = "Session History", onClick = onNavigateHistory)
            Spacer(modifier = Modifier.height(8.dp))
            SecondaryButton(text = "Settings", onClick = onNavigateSettings)
        }
    }
}
