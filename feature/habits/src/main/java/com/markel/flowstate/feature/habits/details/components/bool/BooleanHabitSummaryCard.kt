package com.markel.flowstate.feature.habits.details.components.bool

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
 * A summary dashboard card for a habit, following the Material 3 Expressive design specifications.
 * Fully localized using Android resource string resolution.
 *
 * @param startDate The registration or creation date of the habit.
 * @param currentStreak The current active consecutive days the habit has been completed.
 * @param bestStreak The historical record of consecutive days completed for this habit.
 * @param consistency A pre-formatted string representing the completion rate (e.g., "56%").
 * @param consistencyLabel A label defining the context/timeframe of the consistency rate (e.g., "3M").
 * @param accentColor The dynamic brand or category color associated with the habit.
 * @param modifier The modifier to be applied to the card layout.
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // --- Left Section: Dates & Grouped Streaks Column ---
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Creation timeline header (Dynamic format injection)
                Text(
                    text = stringResource(R.string.habit_summary_started_on, formattedDate),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                // Streak subsection container
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Unified section header
                    Text(
                        text = stringResource(R.string.habit_summary_streak_header),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Current Streak Column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.habit_summary_current_label),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 70.dp)
                                    .basicMarquee(
                                        repeatDelayMillis = 3500
                                    ),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Subtle vertical separator
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        )

                        // Best Streak Column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.habit_summary_best_label),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 70.dp)
                                    .basicMarquee(repeatDelayMillis = 3500),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // --- Right Section: Expressive Consistency Ring ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(88.dp)
                ) {
                    CircularWavyProgressIndicator(
                        progress = { animatedConsistency },
                        modifier = Modifier.size(88.dp),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.12f),
                        stroke = Stroke(
                            width = 15f,
                            cap = StrokeCap.Round
                        ),
                        trackStroke = Stroke(
                            width = 15f,
                            cap = StrokeCap.Round
                        ),
                    )
                    Text(
                        text = consistency,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 25.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.habit_summary_consistency_label),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "($consistencyLabel)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}