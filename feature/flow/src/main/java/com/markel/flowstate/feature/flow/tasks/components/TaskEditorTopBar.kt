package com.markel.flowstate.feature.flow.tasks.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.feature.tasks.R
import com.markel.flowstate.feature.flow.tasks.util.asColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorTopBar(
    priority: Priority,
    onPriorityChange: (Priority) -> Unit,
    dueDate: Long?,
    onDueDateChange: (Long?) -> Unit,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Close"
                )
            }
        },
        actions = {
            IconButton(onClick = {
                val nextPriority = when (priority) {
                    Priority.NOTHING -> Priority.LOW
                    Priority.LOW -> Priority.MEDIUM
                    Priority.MEDIUM -> Priority.HIGH
                    Priority.HIGH -> Priority.NOTHING
                }
                onPriorityChange(nextPriority)
            }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.flag_2_24px),
                    contentDescription = "Priority",
                    tint = priority.asColor(),
                    modifier = Modifier.size(24.dp)
                )
            }

            DateSelector(
                dueDate = dueDate,
                onDueDateChange = onDueDateChange,
                showLabel = true
            )

            IconButton(onClick = { /* TODO: Implement format */ }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.format_color_text_24px),
                    contentDescription = "Format",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.95f)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}