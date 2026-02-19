package com.markel.flowstate.feature.flow.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.feature.tasks.R

// ── Task ─────────────────────────────────────────────────────────────────────

@Composable
fun TaskGridCard(
    task: Task,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
            )
            if (task.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onComplete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.radio_button_unchecked_24px),
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Idea ──────────────────────────────────────────────────────────────────────

@Composable
fun IdeaGridCard(
    idea: Idea,
    onDelete: () -> Unit
) {
    val cardColor = Color(idea.color).copy(alpha = 0.25f)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
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
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Eliminar idea",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── CheckList ─────────────────────────────────────────────────────────────────

@Composable
fun CheckListGridCard(checkList: CheckList) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = checkList.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            checkList.items.take(3).forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (item.isDone)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(50)
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isDone)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (checkList.items.size > 3) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "+${checkList.items.size - 3} más",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}