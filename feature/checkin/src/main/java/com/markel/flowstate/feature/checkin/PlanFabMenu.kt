package com.markel.flowstate.feature.checkin

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.core.designsystem.R as DesignR

/**
 * Expandable FAB menu for the Plan tab — same [FloatingActionButtonMenu] +
 * [ToggleFloatingActionButton] recipe as the Tasks/Habits plus buttons.
 *
 * Two entry points:
 *  - "Start check-in" — manual trigger for the arrival check-in, the
 *    fallback for when the geofence never fires (home-only weekends) or the
 *    8AM weekend alarm doesn't show up;
 *  - "Edit plan" — jumps straight into the check-in's final plan step with
 *    tonight's agreed plan loaded, so the plan can be edited and re-agreed
 *    without re-running the mood/tasks steps.
 *
 * The toggle icon is a single `add` glyph rotated 45° into a "close" (×), so
 * no extra close drawable is needed — same trick as HabitFabMenu.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlanFabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onStartCheckinClick: () -> Unit,
    onEditPlanClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { onToggle() },
                modifier = Modifier.animateFloatingActionButton(
                    visible = visible || expanded,
                    alignment = Alignment.BottomEnd,
                ),
                containerSize = ToggleFloatingActionButtonDefaults.containerSizeMedium()
            ) {
                val progress = checkedProgress

                Icon(
                    imageVector = ImageVector.vectorResource(DesignR.drawable.add_24px),
                    contentDescription = if (expanded) "Close menu" else "Open menu",
                    tint = lerp(
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        MaterialTheme.colorScheme.onPrimary,
                        progress
                    ),
                    modifier = Modifier
                        .size(FloatingActionButtonDefaults.MediumIconSize)
                        .graphicsLayer { rotationZ = 45f * progress } // add → ×
                )
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = onEditPlanClick,
            icon = {
                Icon(
                    ImageVector.vectorResource(DesignR.drawable.edit_24px),
                    modifier = Modifier.size(24.dp),
                    contentDescription = null
                )
            },
            text = { Text("Edit plan", fontSize = 16.sp) }
        )
        FloatingActionButtonMenuItem(
            onClick = onStartCheckinClick,
            icon = {
                Icon(
                    ImageVector.vectorResource(DesignR.drawable.check_24px),
                    modifier = Modifier.size(24.dp),
                    contentDescription = null
                )
            },
            text = { Text("Start check-in", fontSize = 16.sp) }
        )
    }
}
