package com.markel.flowstate.feature.mood

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.domain.MoodEntry
import java.time.format.DateTimeFormatter
import java.util.Locale

private val moodEmojis = mapOf(
    1 to "😞",
    2 to "😕",
    3 to "😐",
    4 to "🙂",
    5 to "😄"
)

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/**
 * First real version — a simple chronological list of every mood-tagged
 * habit completion. Deliberately just this for now: prove real data flows
 * correctly end to end before adding trends/patterns/heatmaps on top.
 */
@Composable
fun MoodScreen(
    viewModel: MoodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is MoodUiState.Loading -> Unit

        is MoodUiState.Success -> {
            if (state.entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No moods logged yet — complete a habit and pick how it felt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                ) {
                    items(state.entries) { entry ->
                        MoodEntryRow(entry)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodEntryRow(entry: MoodEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = moodEmojis[entry.mood] ?: "🙂",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.sourceLabel, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = entry.date.format(dateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}