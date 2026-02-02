package com.markel.flowstate.feature.tasks.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.SubTask
import com.markel.flowstate.core.domain.Task

@Composable
fun TaskEditorOverlay(
    task: Task?,
    onDismiss: () -> Unit,
    priority: Priority,
    onPriorityChange: (Priority) -> Unit,
    dueDate: Long?,
    onDueDateChange: (Long?) -> Unit,
    onUpdate: (Task, String, String, Priority, Long?, List<SubTask>) -> Unit
) {
    AnimatedVisibility(
        visible = task != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier.zIndex(10f)
    ) {
        // A BackHandler to close the editor with the back button instead of the app
        BackHandler(enabled = task != null) { onDismiss() }

        task?.let { currentTask ->  // Local capture to avoid null safety issues
            Scaffold(
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    TaskEditorTopBar(
                        priority = priority,
                        onPriorityChange = onPriorityChange,
                        dueDate = dueDate,
                        onDueDateChange = onDueDateChange,
                        onBack = onDismiss
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    TaskEditorSheetContent(
                        task = currentTask,
                        priority = priority,
                        dueDate = dueDate,
                        onAutoUpdate = { title, desc, prio, date, subTasks ->
                            onUpdate(currentTask, title, desc, prio, date, subTasks)
                        }
                    )
                }
            }
        }
    }
}