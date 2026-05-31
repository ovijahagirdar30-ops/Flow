package com.markel.flowstate.feature.flow.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.feature.tasks.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableFabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onTaskClick: () -> Unit,
    onIdeaClick: () -> Unit,
    onCheckListClick: () -> Unit
) {
    val addIcon = ImageVector.vectorResource(R.drawable.add_24px)
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        label = "fab_icon_rotation"
    )

    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { onToggle() },
                containerSize = { _ -> 54.dp },
                containerCornerRadius = { _ -> 16.dp },
                containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                    initialColor = MaterialTheme.colorScheme.primary,
                    finalColor = MaterialTheme.colorScheme.primary
                ),
            ) {
                Icon(
                    imageVector = addIcon,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    contentDescription = if (expanded) "Close menu" else "Open menu",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = onCheckListClick,
            icon = { Icon(ImageVector.vectorResource(R.drawable.check_box_24px), modifier = Modifier.size(23.dp), contentDescription = null) },
            text = { Text(stringResource(R.string.checklist), fontSize = 16.sp) },
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                red = MaterialTheme.colorScheme.primaryContainer.red + 0.1f,
                green = MaterialTheme.colorScheme.primaryContainer.green + 0.1f,
                blue = MaterialTheme.colorScheme.primaryContainer.blue + 0.1f
            ),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(0.dp)
        )
        FloatingActionButtonMenuItem(
            onClick = onIdeaClick,
            icon = { Icon(ImageVector.vectorResource(R.drawable.lightbulb_24px), modifier = Modifier.size(23.dp), contentDescription = null) },
            text = { Text(stringResource(R.string.idea), fontSize = 16.sp) },
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                red = MaterialTheme.colorScheme.primaryContainer.red + 0.1f,
                green = MaterialTheme.colorScheme.primaryContainer.green + 0.1f,
                blue = MaterialTheme.colorScheme.primaryContainer.blue + 0.1f
            ),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        FloatingActionButtonMenuItem(
            onClick = onTaskClick,
            icon = { Icon(ImageVector.vectorResource(R.drawable.check_24px), modifier = Modifier.size(23.dp), contentDescription = null) },
            text = { Text(stringResource(R.string.task), fontSize = 16.sp) },
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                red = MaterialTheme.colorScheme.primaryContainer.red + 0.1f,
                green = MaterialTheme.colorScheme.primaryContainer.green + 0.1f,
                blue = MaterialTheme.colorScheme.primaryContainer.blue + 0.1f
            ),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}