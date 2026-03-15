package com.markel.flowstate.feature.habits.details.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.format.TextStyle as JTextStyle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.platform.LocalLocale

@Composable
fun RadarChart(
    dayOfWeekCompletions: Map<Int, Int>,  // 1=Mon..7=Sun
    habitColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColorStrong = habitColor

    val maxVal = dayOfWeekCompletions.values.maxOrNull()?.takeIf { it > 0 } ?: 1
    val values = (1..7).map { dow ->
        (dayOfWeekCompletions[dow] ?: 0).toFloat() / maxVal
    }
    val labels = (1..7).map { dow ->
        DayOfWeek.of(dow)
            .getDisplayName(JTextStyle.NARROW, LocalLocale.current.platformLocale)
            .uppercase()
    }

    Canvas(modifier = modifier.size(240.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.width * 0.35f
        val n = 7

        fun point(i: Int, r: Float): Offset {
            val angle = (2 * PI * i / n - PI / 2).toFloat()
            return Offset(cx + r * cos(angle), cy + r * sin(angle))
        }

        // Grid rings
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { scale ->
            val path = Path()
            (0 until n).forEach { i ->
                val p = point(i, radius * scale)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, color = gridColor, style = Stroke(width = 1.dp.toPx()))
        }

        // Axes
        (0 until n).forEach { i ->
            val p = point(i, radius)
            drawLine(gridColor, Offset(cx, cy), p, strokeWidth = 1.dp.toPx())
        }

        // Filled area
        val dataPath = Path()
        values.forEachIndexed { i, v ->
            val p = point(i, radius * v.coerceAtLeast(0.05f))
            if (i == 0) dataPath.moveTo(p.x, p.y) else dataPath.lineTo(p.x, p.y)
        }
        dataPath.close()
        drawPath(dataPath, color = habitColor.copy(alpha = 0.2f))
        drawPath(dataPath, color = habitColor, style = Stroke(width = 2.dp.toPx()))

        // Dots
        values.forEachIndexed { i, v ->
            val p = point(i, radius * v.coerceAtLeast(0.05f))
            drawCircle(habitColor, radius = 4.dp.toPx(), center = p)
        }

        // Labels
        val labelRadius = radius + 22.dp.toPx()
        values.forEachIndexed { i, v ->
            val p = point(i, labelRadius)
            val isStrong = v > 0.7f
            val dayLabel = labels[i]
            val pctLabel = "${(v * 100).toInt()}%"

            val dayMeasured = textMeasurer.measure(
                dayLabel,
                TextStyle(
                    fontSize = 11.sp,
                    fontWeight = if (isStrong) FontWeight.Medium else FontWeight.Normal,
                    color = if (isStrong) labelColorStrong else labelColor
                )
            )
            val pctMeasured = textMeasurer.measure(
                pctLabel,
                TextStyle(
                    fontSize = 9.sp,
                    color = if (isStrong) labelColorStrong.copy(alpha = 0.7f)
                    else labelColor.copy(alpha = 0.6f)
                )
            )

            drawText(
                dayMeasured,
                topLeft = Offset(
                    p.x - dayMeasured.size.width / 2f,
                    p.y - dayMeasured.size.height - 1.dp.toPx()
                )
            )
            drawText(
                pctMeasured,
                topLeft = Offset(
                    p.x - pctMeasured.size.width / 2f,
                    p.y + 1.dp.toPx()
                )
            )
        }
    }
}