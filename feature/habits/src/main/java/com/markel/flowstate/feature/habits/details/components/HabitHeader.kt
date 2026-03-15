package com.markel.flowstate.feature.habits.details.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

@Composable
fun HabitHeader(
    completedToday: Int,
    totalHabits: Int,
    motivationalMessage: MotivationalMessage,
    modifier: Modifier = Modifier
) {
    val now = LocalTime.now()
    val dayProgress = (now.hour * 3600 + now.minute * 60 + now.second) / 86400f

    val habitProgress = if (totalHabits > 0) completedToday.toFloat() / totalHabits else 0f
    val allDone = completedToday == totalHabits && totalHabits > 0

    val outerColor = MaterialTheme.colorScheme.secondary // day time
    val innerColor = if (allDone)  // completed habits
        MaterialTheme.colorScheme.tertiaryContainer
    else
        MaterialTheme.colorScheme.error

    val outerTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val innerTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    val animatedDayProgress by animateFloatAsState(
        targetValue = dayProgress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "day_progress"
    )
    val animatedHabitProgress by animateFloatAsState(
        targetValue = habitProgress,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "habit_progress"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = motivationalMessage.keyword,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
                lineHeight = 18.sp
            )
            Text(
                text = motivationalMessage.rest,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        // Double ring
        Box(
            modifier = Modifier
                .size(56.dp)
                .drawWithContent {
                    val strokeOuter = 4.dp.toPx()
                    val strokeInner = 4.dp.toPx()
                    val outerRadius = size.width / 2f - strokeOuter / 2f
                    val innerRadius = size.width / 2f - strokeOuter - 4.dp.toPx() - strokeInner / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Exterior track
                    drawCircle(
                        color = outerTrackColor,
                        radius = outerRadius,
                        style = Stroke(width = strokeOuter)
                    )
                    // Exterior ring
                    drawArc(
                        color = outerColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedDayProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                        size = Size(outerRadius * 2, outerRadius * 2),
                        style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                    )

                    // Interior track
                    drawCircle(
                        color = innerTrackColor,
                        radius = innerRadius,
                        style = Stroke(width = strokeInner)
                    )
                    // Interior ring
                    if (totalHabits > 0) {
                        drawArc(
                            color = innerColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedHabitProgress,
                            useCenter = false,
                            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                            size = Size(innerRadius * 2, innerRadius * 2),
                            style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                        )
                    }

                    drawContent()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$completedToday/${totalHabits}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 11.sp
                )
            }
        }
    }
}