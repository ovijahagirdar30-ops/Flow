package com.markel.flowstate.feature.habits.details.components.bool

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.feature.habits.R
import com.markel.flowstate.feature.habits.details.MoodLogEntry
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

/**
 * The habit detail page's Mood section chart — the same SectionHeader + Card +
 * chart layout the Weekly and Radar sections use. It plots the 30-day trend of
 * the 1–5 ratings; the average and check-in count live in the StatCards above
 * it. An empty state (habit opted in but nothing logged yet) is just a hint
 * line.
 *
 * The trend plots calendar days on x (gaps stay gaps — a stretch without
 * moods reads as a long segment, not as new data) and the rating on y.
 */
@Composable
fun MoodHistoryCard(
    history: List<MoodLogEntry>, // newest first
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) {
        Text(
            text = stringResource(R.string.habit_mood_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    // Captured in composition — draw lambdas can't read CompositionLocals.
    val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val locale = LocalLocale.current.platformLocale

    // Window: the last 30 days — but anchored to the newest entry
    // when moods stopped flowing earlier, so stale history still
    // gets a chart instead of a blank strip.
    val today = LocalDate.now()
    val windowEnd = history.maxOf { it.date }.let { if (it.isAfter(today)) today else it }
    val windowStart = windowEnd.minusDays(29)
    val points = history
        .filter { !it.date.isBefore(windowStart) && !it.date.isAfter(windowEnd) }
        .sortedBy { it.date }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.habit_mood_trend),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            if (points.isEmpty()) return@Canvas

            val inset = 10.dp.toPx()
            val chartWidth = size.width - inset * 2
            val chartHeight = size.height - inset * 2

            fun xFor(date: LocalDate): Float {
                val day = ChronoUnit.DAYS.between(windowStart, date).toFloat().coerceIn(0f, 29f)
                return inset + chartWidth * (day / 29f)
            }
            fun yFor(mood: Int): Float = inset + chartHeight * ((5 - mood) / 4f)

            // Hairlines at the scale's ends: mood 5 on top, mood 1 below.
            drawLine(guideColor, Offset(inset, yFor(5)), Offset(size.width - inset, yFor(5)), strokeWidth = 1.dp.toPx())
            drawLine(guideColor, Offset(inset, yFor(1)), Offset(size.width - inset, yFor(1)), strokeWidth = 1.dp.toPx())

            if (points.size > 1) {
                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = xFor(point.date)
                    val y = yFor(point.mood)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = accentColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            points.forEach { point ->
                drawCircle(
                    color = accentColor,
                    radius = 4.dp.toPx(),
                    center = Offset(xFor(point.date), yFor(point.mood))
                )
            }
        }

        // Time labels — the same footer the numeric evolution chart uses.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${windowStart.dayOfMonth} ${windowStart.month.getDisplayName(TextStyle.SHORT, locale)}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${windowEnd.dayOfMonth} ${windowEnd.month.getDisplayName(TextStyle.SHORT, locale)}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
