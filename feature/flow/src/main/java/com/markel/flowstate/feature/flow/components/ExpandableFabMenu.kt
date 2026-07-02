package com.markel.flowstate.feature.flow.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.feature.tasks.R

/**
 * Expandable FAB menu built on top of the native Material 3 Expressive
 * [FloatingActionButtonMenu] + [ToggleFloatingActionButton].
 *
 * Native behavior:
 *  - Container morphs squircle -> circle as the toggle is checked.
 *  - Container color morphs primaryContainer -> primary.
 *  - The bouncy spring is provided by the M3 Expressive
 *    MotionScheme, which is only active when the app is wrapped in
 *    MaterialExpressiveTheme (see Theme.kt).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableFabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onTaskClick: () -> Unit,
    onIdeaClick: () -> Unit,
    onCheckListClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addIcon = ImageVector.vectorResource(R.drawable.add_24px)
    val closeIcon = ImageVector.vectorResource(R.drawable.close_24px)

    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { onToggle() },
            ) {
                val progress = checkedProgress

                val iconTint = lerp(
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    MaterialTheme.colorScheme.onPrimary,
                    progress
                )

                Icon(
                    imageVector = if (progress < 0.5f) addIcon else closeIcon,
                    contentDescription = if (expanded) "Close menu" else "Open menu",
                    tint = iconTint
                )
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = onCheckListClick,
            icon = {
                Icon(
                    ImageVector.vectorResource(R.drawable.check_box_24px),
                    modifier = Modifier.size(24.dp),
                    contentDescription = null
                )
            },
            text = { Text(stringResource(R.string.checklist), fontSize = 16.sp) }
        )
        FloatingActionButtonMenuItem(
            onClick = onIdeaClick,
            icon = {
                Icon(
                    ImageVector.vectorResource(R.drawable.lightbulb_24px),
                    modifier = Modifier.size(24.dp),
                    contentDescription = null
                )
            },
            text = { Text(stringResource(R.string.idea), fontSize = 16.sp) }
        )
        FloatingActionButtonMenuItem(
            onClick = onTaskClick,
            icon = {
                Icon(
                    ImageVector.vectorResource(R.drawable.check_24px),
                    modifier = Modifier.size(24.dp),
                    contentDescription = null
                )
            },
            text = { Text(stringResource(R.string.task), fontSize = 16.sp) }
        )
    }
}