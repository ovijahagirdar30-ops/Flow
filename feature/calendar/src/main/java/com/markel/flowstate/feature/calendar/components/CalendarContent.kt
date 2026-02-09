package com.markel.flowstate.feature.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.feature.calendar.R
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarContent(
    tasksByDate: Map<java.time.LocalDate, List<Task>>,
    selectedDate: java.time.LocalDate,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onTaskToggle: (Task) -> Unit,
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {

        // MONTH HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.firstVisibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() } + " " + state.firstVisibleMonth.yearMonth.year,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        // WEEKDAYS
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = remember { firstDayOfWeekFromLocale().let { first -> (0..6).map { first.plus(it.toLong()) } } }
            for (day in daysOfWeek) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // CALENDAR
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                val tasksForDay = tasksByDate[day.date] ?: emptyList()
                val isSelected = selectedDate == day.date

                DayCell(
                    day = day,
                    isSelected = isSelected,
                    hasTasks = tasksForDay.isNotEmpty(),
                    isToday = day.date == java.time.LocalDate.now(),
                    onClick = { onDateSelected(day.date) }
                )
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // TASKS LIST
        val tasksForSelectedDate = tasksByDate[selectedDate] ?: emptyList()

        Text(
            text = if (selectedDate == java.time.LocalDate.now()) stringResource(R.string.today) else selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(16.dp)
        )

        if (tasksForSelectedDate.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.noth_new), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(tasksForSelectedDate) { task ->
                    InteractiveTaskRow(
                        task = task,
                        onToggle = { onTaskToggle(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    hasTasks: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.tertiary
                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = day.position == DayPosition.MonthDate, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (day.position == DayPosition.MonthDate) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = day.date.dayOfMonth.toString(),
                color = if (isSelected) MaterialTheme.colorScheme.onTertiary else if (isToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            )

            // Dot if there are tasks
            if (hasTasks) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom =6.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}

@Composable
fun SimpleTaskRow(task: Task) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Only visualization, no interaction yet
            Icon(
                imageVector = if(task.isDone) androidx.compose.material.icons.Icons.Default.Check else ImageVector.vectorResource(
                    R.drawable.radio_button_unchecked_24px),
                contentDescription = null,
                tint = if(task.isDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = if(task.isDone) TextDecoration.LineThrough else null)
                )
            }
        }
    }
}