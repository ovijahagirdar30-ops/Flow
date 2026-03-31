package com.markel.flowstate.feature.habits.details.components.bool

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markel.flowstate.feature.habits.details.WeeklyBarsMode
import java.time.LocalDate
import com.markel.flowstate.feature.habits.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeeklyBarsCard(
    weeklyCompletions: List<Pair<LocalDate, Int>>,
    selectedIndex: Int?,
    barsMode: WeeklyBarsMode,
    habitColor: Color,
    onBarSelected: (Int) -> Unit,
    onModeChanged: (WeeklyBarsMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = when (barsMode) {
        WeeklyBarsMode.EIGHT -> weeklyCompletions.takeLast(8)
        WeeklyBarsMode.SIXTEEN -> weeklyCompletions
    }

    val effectiveSelected = selectedIndex ?: (data.size - 1)
    val max = data
        .map { it.second.toDouble() }
        .takeIf { it.any { v -> v != 0.0 } }
        ?.maxOrNull()

    val badgeBgColor = habitColor.copy(
        red = habitColor.red * 0.55f,
        green = habitColor.green * 0.55f,
        blue = habitColor.blue * 0.55f
    )

    val badgeSize = if (barsMode == WeeklyBarsMode.SIXTEEN) 16.dp else 24.dp
    val badgeSizeSecondary = if (barsMode == WeeklyBarsMode.SIXTEEN) 13.dp else 20.dp
    val barSpacing = if (barsMode == WeeklyBarsMode.SIXTEEN) 1.dp else 2.dp

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                ButtonGroupDefaults.ConnectedSpaceBetween
            )
        ) {
            listOf(
                WeeklyBarsMode.EIGHT to stringResource(R.string.weekly_bars_8),
                WeeklyBarsMode.SIXTEEN to stringResource(R.string.weekly_bars_16)
            ).forEachIndexed { i, (mode, label) ->
                ToggleButton(
                    checked = barsMode == mode,
                    onCheckedChange = { onModeChanged(mode) },
                    shapes = when (i) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        checkedContainerColor = habitColor,
                        checkedContentColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Text(text = label)
                }
            }
        }

        if (max != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(barSpacing)
            ) {
                data.forEachIndexed { index, (_, count) ->
                    val isSelected = index == effectiveSelected
                    val isMax = count.toDouble() == max
                    val isAboveHalf = count > (max / 2) && barsMode != WeeklyBarsMode.SIXTEEN
                    val barColor = if (isSelected) habitColor else habitColor.copy(alpha = 0.3f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (count > 0) Modifier.clickable { onBarSelected(index) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // If count = 0  we don't draw the bar
                        if (count > 0) {
                            val barHeight = ((count.toFloat() / max.toFloat()) * 140).dp

                            Box(
                                modifier = Modifier
                                    .height(barHeight.coerceAtLeast(8.dp))
                                    .fillMaxWidth()
                                    .background(
                                        color = barColor,
                                        shape = CircleShape
                                    )
                            ) {
                                when {
                                    isMax -> if (barsMode != WeeklyBarsMode.SIXTEEN) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = 4.dp)
                                                .size(badgeSize)
                                                .background(
                                                    color = badgeBgColor,
                                                    shape = MaterialShapes.SoftBurst.toShape()
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    isAboveHalf -> Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 4.dp)
                                            .size(badgeSizeSecondary)
                                            .background(
                                                color = badgeBgColor.copy(alpha = 0.7f),
                                                shape = MaterialShapes.Circle.toShape()
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.habit_detail_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}