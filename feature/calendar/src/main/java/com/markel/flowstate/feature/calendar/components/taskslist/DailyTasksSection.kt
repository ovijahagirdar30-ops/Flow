package com.markel.flowstate.feature.calendar.components.taskslist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.feature.calendar.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DailyTasksSection(
    selectedDate: LocalDate,
    tasks: List<Task>,
    listState: LazyListState,
    onTaskToggle: (Task) -> Unit
) {
    Text(
        text = if (selectedDate == LocalDate.now()) stringResource(R.string.today) else selectedDate.format(
            DateTimeFormatter.ofPattern("d MMM yyyy")),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(16.dp)
    )

    if (tasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.noth_new), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            items(tasks, key = { task -> task.id }) { task ->
                Box(modifier = Modifier.animateItem()) {
                    InteractiveTaskRow(
                        task = task,
                        onToggle = { onTaskToggle(task) }
                    )
                }
            }
        }
    }
}