package com.markel.flowstate.feature.habits.details.components.numeric

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.feature.habits.details.MonthlyProgress
import kotlin.math.min
import com.markel.flowstate.feature.habits.R
import com.markel.flowstate.feature.habits.util.formatFloat


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MonthlyGoalCard(
    progress: MonthlyProgress?,
    habitColor: Color,
    unit: String?,
    modifier: Modifier = Modifier
) {
    progress ?: return
    
    val progressPercentage = progress.targetValue?.let {
        min(progress.currentValue / it, 1f)
    } ?: 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(durationMillis = 800),
        label = "circular_progress"
    )

    val density = LocalDensity.current
    val strokePx = with(density) { 8.dp.toPx() }
    val amplitudePx = with(density) { 8.dp.toPx() }

    Column(modifier = modifier) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular graph
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = habitColor,
                    trackColor = habitColor.copy(alpha = 0.1f),
                    stroke = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    ),
                    trackStroke = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    ),
                    gapSize = 0.dp,
                    amplitude = { amplitudePx },
                    wavelength = 20.dp,
                    waveSpeed = 8.dp
                )
                
                // Percentage
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progressPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = habitColor
                    )
                    Text(
                        text = stringResource(R.string.habit_monthly_completed),
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.habit_monthly_label, progress.month),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                // Actual value
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${formatFloat(progress.currentValue)} ${unit ?: ""}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = habitColor
                    )
                    progress.targetValue?.let { target ->
                        Text(
                            text = " / ${formatFloat(target)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // Days with data
                Text(
                    text = stringResource(
                        R.string.habit_days_completed,
                        progress.daysCompleted,
                        progress.totalDays
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (progress.daysCompleted > 0) habitColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Deficit
                progress.deficit?.let { deficit ->
                    val formattedDeficit = formatFloat(deficit)
                    Text(
                        text = stringResource(
                            R.string.habit_deficit_label,
                            formattedDeficit,
                            unit ?: ""
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}