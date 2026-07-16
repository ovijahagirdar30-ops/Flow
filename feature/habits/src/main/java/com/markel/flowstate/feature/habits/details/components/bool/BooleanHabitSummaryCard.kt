package com.markel.flowstate.feature.habits.details.components.bool

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.feature.habits.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A dashboard summary card designed specifically for boolean habits.
 * * Features a dual-card layout side-by-side with asymmetric rounded corners to provide
 * a modern, cohesive UI aesthetic.
 * * - **Left Section (Streaks & Timeline)**: Displays the habit's starting date, current active streak, and all-time best streak.
 * - **Right Section (Consistency)**: Displays an expressive circular wavy progress indicator showing completion rates over a targeted period.
 * * @param startDate The date when the habit was created/started.
 * @param currentStreak The current consecutive days streak for the habit.
 * @param bestStreak The historical record streak achieved for this habit.
 * @param consistency A percentage string (e.g. "85%") representing completion density.
 * @param consistencyLabel A descriptor label for the timeframe (e.g. "1 month", "1 year").
 * @param accentColor The dynamic color used to highlight the current streak and the progress indicator.
 * @param modifier The modifier to be applied to the layout.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BooleanHabitSummaryCard(
    startDate: LocalDate,
    currentStreak: Int,
    bestStreak: Int,
    consistency: String,
    consistencyLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val consistencyFraction = remember(consistency) {
        consistency.trim().removeSuffix("%").trim().toFloatOrNull()?.div(100f) ?: 0f
    }

    val animatedConsistency by animateFloatAsState(
        targetValue = consistencyFraction.coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "summary_consistency_anim"
    )

    val formattedDate = remember(startDate) {
        startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    val leftCardShape = remember {
        RoundedCornerShape(
            topStart = 24.dp,
            bottomStart = 24.dp,
            topEnd = 6.dp,
            bottomEnd = 6.dp
        )
    }

    val rightCardShape = remember {
        RoundedCornerShape(
            topStart = 6.dp,
            bottomStart = 6.dp,
            topEnd = 24.dp,
            bottomEnd = 24.dp
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1.15f)
                .height(116.dp),
            shape = leftCardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.habit_summary_started_on, formattedDate),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = stringResource(R.string.habit_summary_streak_header),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.habit_summary_current_label),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                        Row(
                            modifier = Modifier
                                .widthIn(max = 70.dp)
                                .basicMarquee(repeatDelayMillis = 3500),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = currentStreak.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1).sp
                                ),
                                color = accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                            Text(
                                text = stringResource(R.string.habit_summary_days_suffix),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.habit_summary_best_label),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                        Row(
                            modifier = Modifier
                                .widthIn(max = 70.dp)
                                .basicMarquee(repeatDelayMillis = 3500),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = bestStreak.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-1).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                            Text(
                                text = stringResource(R.string.habit_summary_days_suffix),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(0.85f)
                .height(116.dp),
            shape = rightCardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 10.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp)
                ) {
                    CircularWavyProgressIndicator(
                        progress = { animatedConsistency },
                        modifier = Modifier.size(90.dp),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.12f),
                        stroke = Stroke(
                            width = 12f,
                            cap = StrokeCap.Round
                        ),
                        trackStroke = Stroke(
                            width = 12f,
                            cap = StrokeCap.Round
                        ),
                        amplitude = { progress ->
                            if (progress <= 0f || progress >= 1f) 0f else 1f
                        }
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = consistency,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 25.sp,
                                lineHeight = 25.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "($consistencyLabel)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}