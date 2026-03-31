package com.markel.flowstate.feature.habits.details.components.numeric

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markel.flowstate.feature.habits.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun NumericHeatmapCard(
    heatmapData: Map<LocalDate, Float>,
    targetValue: Float?,
    habitColor: Color,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val weeksToDisplay = 18
    val startDate = today.with(DayOfWeek.MONDAY).minusWeeks((weeksToDisplay - 1).toLong())

    val maxValue = heatmapData.values.maxOrNull() ?: targetValue ?: 1f

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val intensitySteps = listOf(0f, 0.15f, 0.35f, 0.60f, 1f)

    fun getSteppedIntensity(rawIntensity: Float): Float {
        if (rawIntensity <= 0f) return 0f
        return intensitySteps.lastOrNull { it <= rawIntensity } ?: intensitySteps.first()
    }

    @Composable
    fun getHeatmapColor(steppedIntensity: Float, isFuture: Boolean): Color {
        return when {
            isFuture -> Color.Transparent
            steppedIntensity <= 0f -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
            else -> habitColor.copy(alpha = 0.2f + (steppedIntensity * 0.8f))
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.habit_heatmap_weeks, weeksToDisplay),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (selectedDate != null) {
                val value = heatmapData[selectedDate] ?: 0f

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${selectedDate.toString()}: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = habitColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Spacer(modifier = Modifier.width(26.dp))
            Row(modifier = Modifier.weight(1f)) {
                (0 until weeksToDisplay).forEach { weekIndex ->
                    val firstDayOfWeek = startDate.plusWeeks(weekIndex.toLong())
                    // Only show the name if is the first day of the month
                    val showMonth = weekIndex == 0 || firstDayOfWeek.dayOfMonth <= 7

                    Box(modifier = Modifier.weight(1f)) {
                        if (showMonth) {
                            Text(
                                text = firstDayOfWeek.month.getDisplayName(TextStyle.SHORT, LocalLocale.current.platformLocale),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.width(22.dp),
                verticalArrangement = Arrangement.spacedBy(3.2.dp)
            ) {
                listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).forEachIndexed { index, dow ->
                    Box(modifier = Modifier.height(14.dp), contentAlignment = Alignment.CenterStart) {
                        // Only show some to avoid saturating the user
                        if (index % 2 == 0) {
                            Text(
                                text = dow.getDisplayName(TextStyle.NARROW, LocalLocale.current.platformLocale),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Week grid
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                (0 until weeksToDisplay).forEach { weekIndex ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        (0..6).forEach { dayIndex ->
                            val date = startDate.plusWeeks(weekIndex.toLong()).plusDays(dayIndex.toLong())
                            val value = heatmapData[date] ?: 0f
                            val isFuture = date.isAfter(today)
                            val isSelected = date == selectedDate

                            val rawIntensity = when {
                                isFuture || value <= 0f -> 0f
                                targetValue != null && targetValue > 0f -> (value / targetValue).coerceIn(0f, 1f)
                                maxValue > 0f -> (value / maxValue).coerceIn(0f, 1f)
                                else -> 0f
                            }

                            val steppedIntensity = getSteppedIntensity(rawIntensity)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(getHeatmapColor(steppedIntensity, isFuture))
                                    .then(
                                        if (isSelected) Modifier.border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                        else Modifier
                                    )
                                    .clickable(enabled = !isFuture) {
                                        selectedDate = if (selectedDate == date) null else date
                                    }
                            )
                        }
                    }
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Menos",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                intensitySteps.forEach { step ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getHeatmapColor(step, isFuture = false)
                        )
                    )
                }
            }
            Text(
                text = "Más",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}