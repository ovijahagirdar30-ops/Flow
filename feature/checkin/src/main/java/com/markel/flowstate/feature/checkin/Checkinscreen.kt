package com.markel.flowstate.feature.checkin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.markel.flowstate.core.domain.CheckinItem
import com.markel.flowstate.core.domain.CheckinItemType

/**
 * First real version — a simple list of today's not-yet-done tasks and
 * habits, just to prove the alarm/overlay trigger (ported from the
 * standalone Phase 0 test project) can reliably launch real FlowState
 * data. The actual mood/energy check-in and AI plan come in later stages.
 */
@Composable
fun CheckinScreen(
    onDismiss: () -> Unit,
    viewModel: CheckinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Check-in time",
            style = MaterialTheme.typography.headlineMedium
        )

        when (val state = uiState) {
            is CheckinUiState.Loading -> Unit

            is CheckinUiState.Success -> {
                if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nothing left for today — nice.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.items) { item ->
                            CheckinItemRow(item)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Close")
        }
    }
}

@Composable
private fun CheckinItemRow(item: CheckinItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(text = item.title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (item.type == CheckinItemType.TASK) "Task" else "Habit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}