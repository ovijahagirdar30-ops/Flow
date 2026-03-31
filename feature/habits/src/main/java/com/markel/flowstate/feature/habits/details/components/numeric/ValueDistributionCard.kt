package com.markel.flowstate.feature.habits.details.components.numeric

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.feature.habits.details.ValueRange
import com.markel.flowstate.feature.habits.R

@Composable
fun ValueDistributionCard(
    distribution: List<ValueRange>,
    habitColor: Color,
    unit: String?,
    modifier: Modifier = Modifier
) {
    val maxCount = distribution.maxOfOrNull { it.count } ?: 1f
    
    var animationTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationTrigger = true
    }

    Column(modifier = modifier) {
        if (distribution.isEmpty()) {
            Text(
                text = stringResource(R.string.habit_dist_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                distribution.forEach { range ->
                    val heightProgress by animateFloatAsState(
                        targetValue = if (animationTrigger) range.count / maxCount else 0f,
                        animationSpec = tween(durationMillis = 600),
                        label = "bar_height_${range.label}"
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Range label
                        Text(
                            text = range.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(80.dp)
                        )
                        
                        // Bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(heightProgress)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(habitColor)
                            )
                        }
                        
                        // Count
                        Text(
                            text = if (range.count % 1 == 0f) {
                                range.count.toInt().toString()
                            } else {
                                String.format("%.2f", range.count)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = habitColor,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            }
            
            // Note
            Spacer(modifier = Modifier.height(8.dp))
            val unitSuffix = unit?.let {
                stringResource(R.string.habit_dist_unit_prefix, it)
            } ?: ""
            Text(
                text = stringResource(R.string.habit_dist_recorded_amounts, unitSuffix),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
