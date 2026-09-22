package com.markel.flowstate.feature.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.domain.checkin.CheckinMoodState

@Composable
fun MoodCheckinStep(
    moodState: CheckinMoodState,
    onEnergyChange: (Int) -> Unit,
    onSleepinessChange: (Int) -> Unit,
    onStressChange: (Int) -> Unit,
    onHeadacheChange: (Int) -> Unit,
    onMotivationChange: (Int) -> Unit,
    onEnergyCommentChange: (String) -> Unit,
    onSleepinessCommentChange: (String) -> Unit,
    onStressCommentChange: (String) -> Unit,
    onHeadacheCommentChange: (String) -> Unit,
    onMotivationCommentChange: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Hi Ovi, how are you feeling today?",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        MoodSliderSection("Energy", moodState.energy, moodState.energyComment, onEnergyChange, onEnergyCommentChange)
        MoodSliderSection("Sleepiness", moodState.sleepiness, moodState.sleepinessComment, onSleepinessChange, onSleepinessCommentChange)
        MoodSliderSection("Stress", moodState.stress, moodState.stressComment, onStressChange, onStressCommentChange)
        MoodSliderSection("Headache / discomfort", moodState.headache, moodState.headacheComment, onHeadacheChange, onHeadacheCommentChange)
        MoodSliderSection("Motivation", moodState.motivation, moodState.motivationComment, onMotivationChange, onMotivationCommentChange)

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
        ) {
            Text("Next", color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MoodSliderSection(
    label: String,
    value: Int,
    comment: String,
    onValueChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF9C27B0),
                activeTrackColor = Color(0xFF9C27B0),
                inactiveTrackColor = Color(0xFF424242),
                activeTickColor = Color(0xFF9C27B0),
                inactiveTickColor = Color(0xFF424242)
            )
        )
        OutlinedTextField(
            value = comment,
            onValueChange = onCommentChange,
            placeholder = { Text("Optional comment...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF9C27B0),
                unfocusedBorderColor = Color(0xFF616161),
                cursorColor = Color(0xFF9C27B0),
                focusedPlaceholderColor = Color.Gray,
                unfocusedPlaceholderColor = Color.Gray
            ),
            singleLine = true
        )
    }
}