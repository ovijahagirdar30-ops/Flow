package com.markel.flowstate.feature.habits.details.components.numeric

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.feature.habits.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A summary dashboard card designed specifically for numeric (measurable) habits.
 *
 * This component features a unified, side-by-side card layout with asymmetric rounded corners
 * to visually connect the habit's historic progress metrics (left) with its key performance indicators (right).
 *
 * Left Section:
 * - Creation timeline metadata.
 * - Dynamic, current and best streak counters.
 *
 * Right Section:
 * - A prominent display of the total average value.
 * - Dynamic unit rendering with marquee animation support to handle long unit names safely.
 *
 * @param startDate The registration or creation date of the numeric habit.
 * @param currentStreak The current active consecutive days the habit has been completed.
 * @param bestStreak The historical maximum consecutive days achieved.
 * @param averageValue The global average value computed for the habit.
 * @param unit The physical unit of measurement (e.g., "Books", "km").
 * @param accentColor The dynamic brand or category color associated with this specific habit.
 * @param modifier The modifier to be applied to the outer layout container.
 */
@Composable
fun NumericHabitSummaryCard(
    startDate: LocalDate,
    currentStreak: Int,
    bestStreak: Int,
    averageValue: Float,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(startDate) {
        startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    val formattedAverage = remember(averageValue) {
        String.format("%.2f", averageValue)
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
                                color = accentColor
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
                                color = MaterialTheme.colorScheme.onSurface
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
                    .padding(top = 16.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedAverage,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 44.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-3.5).sp,
                        ),
                        color = accentColor
                    )
                    Text(
                        text = " $unit",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accentColor,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(bottom = 6.dp)
                            .basicMarquee(
                                repeatDelayMillis = 3500
                            )
                    )
                }
                Text(
                    text = stringResource(R.string.habit_detail_average),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}