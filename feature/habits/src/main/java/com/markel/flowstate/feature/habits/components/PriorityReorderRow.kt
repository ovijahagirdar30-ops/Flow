package com.markel.flowstate.feature.habits.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.domain.HabitWithStatus

/**
 * A deliberately simplified row for priority-reorder mode — just the drag
 * handle, the habit's name, and a tappable rank number. No complete/edit/
 * delete actions here; those stay in the normal habit list. Reordering
 * priority is its own focused task, not another place to manage habits.
 */
@Composable
fun PriorityReorderRow(
    habitWithStatus: HabitWithStatus,
    displayRank: Int, // 1-indexed for the person looking at it
    onRankTyped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRankDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "⠿",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = habitWithStatus.habit.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { showRankDialog = true }
        ) {
            Text(
                text = displayRank.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    if (showRankDialog) {
        var text by remember { mutableStateOf(displayRank.toString()) }
        AlertDialog(
            onDismissRequest = { showRankDialog = false },
            title = { Text("Set priority") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    label = { Text("Position (1 = highest)") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    text.toIntOrNull()?.let { onRankTyped(it) }
                    showRankDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRankDialog = false }) { Text("Cancel") }
            }
        )
    }
}
