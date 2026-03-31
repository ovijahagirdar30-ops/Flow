package com.markel.flowstate.feature.habits.details.components.bool

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import androidx.compose.ui.platform.LocalLocale

@Composable
fun HabitMonthCalendar(
    year: Int,
    month: Int,  // 0-based
    completedEpochDays: Set<Long>,
    habitColor: Color,
    showMonthLabel: Boolean = false,
    compact: Boolean = false,
    showNumbers: Boolean = !compact,
    modifier: Modifier = Modifier
) {
    val yearMonth = YearMonth.of(year, month + 1)
    val today = LocalDate.now()
    val firstDayOfWeek = DayOfWeek.MONDAY
    val firstDay = yearMonth.atDay(1)
    val offset = ((firstDay.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    Column(modifier = modifier) {
        if (showMonthLabel) {
            Text(
                text = firstDay.month.getDisplayName(TextStyle.SHORT, LocalLocale.current.platformLocale)
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (showNumbers) {
            Row(modifier = Modifier.fillMaxWidth()) {
                (0..6).forEach { i ->
                    val dow = DayOfWeek.of(((firstDayOfWeek.value - 1 + i) % 7) + 1)
                    Text(
                        text = dow.getDisplayName(TextStyle.NARROW, LocalLocale.current.platformLocale).uppercase(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Days
        val totalCells = offset + daysInMonth
        val rows = (totalCells + 6) / 7

        (0 until rows).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                (0..6).forEach { col ->
                    val index = row * 7 + col
                    val day = index - offset + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..daysInMonth) {
                            val date = yearMonth.atDay(day)
                            val epochDay = date.toEpochDay()
                            val isDone = epochDay in completedEpochDays
                            val isFuture = date.isAfter(today)
                            val isToday = date == today

                            val bg = when {
                                isDone && !isFuture -> habitColor
                                isFuture -> Color.Transparent
                                else -> habitColor.copy(alpha = 0.12f)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (compact) 1.dp else 2.dp)
                                    .clip(RoundedCornerShape(if (compact) 3.dp else 6.dp))
                                    .background(bg)
                                    .then(
                                        if (isToday && !isDone)
                                            Modifier.border(1.5.dp, habitColor, RoundedCornerShape(6.dp))
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showNumbers) {
                                    Text(
                                        text = day.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isDone && !isFuture -> Color.White
                                            isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}