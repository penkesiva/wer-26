package com.golfcues.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfcues.app.domain.model.FeedbackType
import com.golfcues.app.domain.model.MotionState
import com.golfcues.app.domain.model.ShotEvent
import com.golfcues.app.ui.theme.GolfGreen
import com.golfcues.app.utils.FormatUtils

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GolfGreen)
    ) {
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text, fontSize = 16.sp, modifier = Modifier.padding(4.dp))
    }
}

@Composable
fun ShotEventCard(
    event: ShotEvent,
    showFeedback: Boolean = true,
    onFeedback: ((FeedbackType) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = eventTitle(event),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = FormatUtils.formatTime(event.timestampMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (event.eventType.name != "IGNORED_AUDIO_HIT") {
                Text(text = "Confidence: ${FormatUtils.formatConfidence(event.audioConfidence)}")
            }
            Text(text = "Motion: ${formatMotion(event.motionState)}")
            event.ignoreReason?.let {
                Text(text = it, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            if (event.feedbackType != FeedbackType.UNMARKED) {
                Text(text = "Feedback: ${formatFeedback(event.feedbackType)}")
            }
            if (showFeedback && onFeedback != null && event.eventType.name == "PROBABLE_SHOT") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { onFeedback(FeedbackType.CORRECT) }) {
                        Text("Correct")
                    }
                    OutlinedButton(onClick = { onFeedback(FeedbackType.NOT_A_SHOT) }) {
                        Text("Not a shot")
                    }
                }
            }
        }
    }
}

private fun eventTitle(event: ShotEvent): String = when (event.eventType.name) {
    "PROBABLE_SHOT" -> "Probable Shot"
    "POSSIBLE_SHOT" -> "Possible Shot"
    "IGNORED_AUDIO_HIT" -> "Ignored Event"
    "MANUAL_MISSED_SHOT" -> "Missed Shot (Manual)"
    else -> event.eventType.name
}

private fun formatMotion(state: MotionState): String =
    state.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun formatFeedback(type: FeedbackType): String = when (type) {
    FeedbackType.CORRECT -> "Correct"
    FeedbackType.NOT_A_SHOT -> "Not a shot"
    FeedbackType.MISSED_SHOT -> "Missed shot"
    FeedbackType.UNMARKED -> "Unmarked"
}
