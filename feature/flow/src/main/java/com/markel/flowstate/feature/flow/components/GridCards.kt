package com.markel.flowstate.feature.flow.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.designsystem.ui.CheckListSharedKeys
import com.markel.flowstate.core.designsystem.ui.IdeaSharedKeys
import com.markel.flowstate.core.designsystem.ui.TaskSharedKeys
import com.markel.flowstate.core.designsystem.ui.sharedCardBounds
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.feature.flow.tasks.components.formatDate
import com.markel.flowstate.feature.flow.tasks.components.isDateOverdue
import com.markel.flowstate.feature.flow.tasks.util.asColor
import com.markel.flowstate.feature.tasks.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Task ─────────────────────────────────────────────────────────────────────

@Composable
fun TaskGridCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = task.priority.asColor()
    val shape = RoundedCornerShape(12.dp)

    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier
            .fillMaxWidth()
            .sharedCardBounds(
                key = TaskSharedKeys.container(task.id),
                shape = shape
            )
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
        ) {
            if (task.priority != Priority.NOTHING) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(
                            color = priorityColor,
                            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                        )
                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Task title
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = if (task.description.isBlank()) 12 else 4,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Description
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Footer: dueDate and pending subtasks
                val total = task.subTasks.size
                val completed = task.subTasks.count { it.isDone }
                val hasSubTasks = total > 0 && completed < total
                val hasDueDate = task.dueDate != null

                if (hasDueDate || hasSubTasks) {
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (hasDueDate) {
                            val isOverdue = isDateOverdue(task.dueDate!!)
                            val dateColor = if (isOverdue)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.tertiary

                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.event_24px),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = dateColor
                            )
                            Text(
                                text = formatDate(task.dueDate),
                                style = MaterialTheme.typography.labelMedium,
                                color = dateColor
                            )
                        }

                        if (hasDueDate && hasSubTasks) {
                            Text(
                                text = " ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        if (hasSubTasks) {
                            val subtaskColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.subtask_24px),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = subtaskColor
                            )
                            Text(
                                text = "$completed/$total",
                                style = MaterialTheme.typography.labelMedium,
                                color = subtaskColor
                            )
                        }

                    }
                }

            }
        }
    }
}

// ── Idea ──────────────────────────────────────────────────────────────────────

@Composable
fun IdeaGridCard(
    idea: Idea,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedColor = idea.color.resolveIdeaColor()
    val isTransparent = resolvedColor == COLOR_TRANSPARENT
    val cardColor = if (isTransparent) Color.Transparent else Color(resolvedColor)
    val shape = RoundedCornerShape(12.dp)

    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = modifier
            .fillMaxWidth()
            .sharedCardBounds(key = IdeaSharedKeys.container(idea.id), shape = shape)
            .then(
                if (isTransparent) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = shape
                ) else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = idea.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (idea.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = idea.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── CheckList ─────────────────────────────────────────────────────────────────

@Composable
fun CheckListGridCard(
    checkList: CheckList,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedColor = checkList.color.resolveIdeaColor()
    val isTransparent = resolvedColor == COLOR_TRANSPARENT
    val cardColor = if (isTransparent) Color.Transparent else Color(resolvedColor)
    val shape = RoundedCornerShape(12.dp)

    Card(
        shape = shape,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = modifier
            .fillMaxWidth()
            .sharedCardBounds(
                key = CheckListSharedKeys.container(checkList.id),
                shape = shape
            )
            .then(
                if (isTransparent) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = shape
                ) else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = checkList.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            val pendingItems = checkList.items.filter { !it.isDone }
            val visibleItems = pendingItems.take(5)
            visibleItems.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    MiniCheckbox(checked = false)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (pendingItems.size > 5) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "+${pendingItems.size - 5} " + stringResource(com.markel.flowstate.feature.tasks.R.string.more),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}


/**
 * A small read-only checkbox for use inside grid cards.
 * Uses Box + border + optional fill instead of the full Checkbox composable
 * so we can control the exact size (16dp) without the built-in padding.
 */
@Composable
private fun MiniCheckbox(checked: Boolean) {

    Box(
        modifier = Modifier
            .size(13.dp)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(3.dp)
            )
            .background(color = Color.Transparent, shape = RoundedCornerShape(3.dp))
    ) {
        if (checked) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.check_24px),
                contentDescription = null,
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
        }
    }
}
