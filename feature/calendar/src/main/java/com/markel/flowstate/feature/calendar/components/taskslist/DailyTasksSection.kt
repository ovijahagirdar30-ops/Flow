package com.markel.flowstate.feature.calendar.components.taskslist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.domain.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale
import java.time.temporal.WeekFields

private const val DAYS_AHEAD = 90
private val DAY_COLUMN_WIDTH = 56.dp
private val firstDayOfWeek: DayOfWeek
    get() = WeekFields.of(Locale.getDefault()).firstDayOfWeek

private data class WeekBlock(
    val weekKey: String,
    val label: String,
    val days: List<DayBlock>
)

private data class DayBlock(
    val date: LocalDate,
    val tasks: List<Task>
)

@Composable
fun DailyTasksSection(
    startDate: LocalDate,
    tasksByDate: Map<LocalDate, List<Task>>,
    listState: LazyListState,
    onTaskToggle: (Task) -> Unit
) {
    val weeks = remember(startDate, tasksByDate) {
        buildWeekBlocks(startDate, tasksByDate)
    }

    LaunchedEffect(startDate) {
        val targetKey = weekKeyFor(startDate)
        val targetIndex = weeks.indexOfFirst { it.weekKey == targetKey }.takeIf { it >= 0 } ?: 0
        listState.animateScrollToItem(targetIndex)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        weeks.forEach { week ->
            item(key = "week_${week.weekKey}") {
                WeekSection(week = week, onTaskToggle = onTaskToggle)
            }
        }
    }
}

// ── Week section ──────────────────────────────────────────────────────────────

@Composable
private fun WeekSection(
    week: WeekBlock,
    onTaskToggle: (Task) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left column spacer — keeps week label aligned with task cards
        Spacer(modifier = Modifier.width(DAY_COLUMN_WIDTH))

        // Week label sits directly above its day rows, in the same column as the cards
        Text(
            text = week.label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (week.days.isNotEmpty()) {
        week.days.forEach { day ->
            DayRow(day = day, onTaskToggle = onTaskToggle)
        }
    }
}

// ── Day row ───────────────────────────────────────────────────────────────────

@Composable
private fun DayRow(
    day: DayBlock,
    onTaskToggle: (Task) -> Unit
) {
    val isToday = day.date == LocalDate.now()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left column: abbrev + number
        Column(
            modifier = Modifier.width(DAY_COLUMN_WIDTH),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = day.date.dayOfWeek
                    .getDisplayName(TextStyle.SHORT, LocalLocale.current.platformLocale)
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                color = if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }

        // Right column: task cards
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            day.tasks.forEach { task ->
                InteractiveTaskRow(
                    task = task,
                    onToggle = { onTaskToggle(task) }
                )
            }
        }
    }
}

// ── Data building ─────────────────────────────────────────────────────────────

private fun buildWeekBlocks(
    startDate: LocalDate,
    tasksByDate: Map<LocalDate, List<Task>>
): List<WeekBlock> {
    val endDate = startDate.plusDays(DAYS_AHEAD.toLong())
    val blocks = mutableListOf<WeekBlock>()
    var weekStart = startDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

    while (weekStart <= endDate) {
        val weekEnd = weekStart.plusDays(6)
        val days = (0L..6L)
            .map { weekStart.plusDays(it) }
            .filter { it in startDate..endDate }
            .mapNotNull { day ->
                tasksByDate[day]?.takeIf { it.isNotEmpty() }?.let { DayBlock(day, it) }
            }

        blocks.add(
            WeekBlock(
                weekKey = weekKeyFor(weekStart),
                label = buildWeekLabel(weekStart, weekEnd),
                days = days
            )
        )
        weekStart = weekStart.plusWeeks(1)
    }

    return blocks
}

private fun buildWeekLabel(weekStart: LocalDate, weekEnd: LocalDate): String {
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val end = if (weekStart.month == weekEnd.month) weekEnd.dayOfMonth.toString()
    else weekEnd.format(fmt)
    return "${weekStart.format(fmt)} – $end"
}

private fun weekKeyFor(date: LocalDate): String =
    date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).toString()