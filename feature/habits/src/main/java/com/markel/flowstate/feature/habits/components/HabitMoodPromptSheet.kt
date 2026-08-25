package com.markel.flowstate.feature.habits.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val moodOptions = listOf(
    1 to "😞",
    2 to "😕",
    3 to "😐",
    4 to "🙂",
    5 to "😄"
)

/**
 * Shown right after marking a habit complete — a quick, fully skippable
 * check on how it felt. Never appears when un-completing something, and
 * tapping outside or "Skip" both dismiss it with no consequence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitMoodPromptSheet(
    habitName: String,
    onMoodSelected: (Int) -> Unit,
    onSkip: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onSkip,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "How did \"$habitName\" go?",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                moodOptions.forEach { (value, emoji) ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onMoodSelected(value) }
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }
    }
}

