package com.markel.flowstate.feature.calendar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.feature.calendar.R
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

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

    var isExpanded by rememberSaveable { mutableStateOf(true) }
    var dragOffset by remember { mutableFloatStateOf(0f) }  // State for the accumulated drag
    val threshold = 100f  // Threshold to change states (pixels)

    val monthState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    val weekState = rememberWeekCalendarState(
        startDate = startMonth.atStartOfMonth(),
        endDate = endMonth.atEndOfMonth(),
        firstVisibleWeekDate = selectedDate,
        firstDayOfWeek = firstDayOfWeek
    )

    val listState = rememberLazyListState()  // Tasks list

    // NestedScrollConnection to handle the list scroll
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {

                val delta = available.y

                // If it is expanded and scroll upwards (negative delta)
                if (isExpanded && delta < 0) {
                    dragOffset += delta
                    if (dragOffset < -threshold) {
                        isExpanded = false
                        dragOffset = 0f
                    }
                    // Consume part of the scroll while close to the threshold
                    return if (abs(dragOffset) < threshold) androidx.compose.ui.geometry.Offset(0f, delta * 0.5f) else androidx.compose.ui.geometry.Offset.Zero
                }

                // If it is collapsed, the list is at the top and we scroll downwards (positive delta)
                if (!isExpanded && delta > 0 && !listState.canScrollBackward) {
                    dragOffset += delta
                    if (dragOffset > threshold) {
                        isExpanded = true
                        dragOffset = 0f
                    }
                    // Consume downward scroll when the list is at the top
                    return androidx.compose.ui.geometry.Offset(0f, delta)
                }

                // Reset the offset if we change direction
                if ((delta > 0 && dragOffset < 0) || (delta < 0 && dragOffset > 0)) {
                    dragOffset = 0f
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Reset offset after fling
                dragOffset = 0f
                return Velocity.Zero
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {  // So that drag is counted outside the task list
                detectVerticalDragGestures(
                    onDragEnd = {
                        // Evaluate whether we should change state on release
                        if (isExpanded && dragOffset < -threshold) {
                            isExpanded = false
                        } else if (!isExpanded && dragOffset > threshold) {
                            isExpanded = true
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        // Accumulate drag anywhere on the screen
                        if (isExpanded && dragAmount < 0) {
                            // Expanded and dragging upwards
                            dragOffset += dragAmount
                        } else if (!isExpanded && dragAmount > 0) {
                            // Collapsed and dragging downwards
                            dragOffset += dragAmount
                        } else {
                            // Change of direction, reset
                            dragOffset = 0f
                        }
                    }
                )
            }
    ){
        // MONTH HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayMonth = if (isExpanded) {
                monthState.firstVisibleMonth.yearMonth
            } else {
                YearMonth.from(weekState.firstVisibleWeek.days.first().date)
            }

            Text(
                text = displayMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() } + " " + displayMonth.year,
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

        // Month view
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 250),
                expandFrom = Alignment.Top
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 250),
                shrinkTowards = Alignment.Top
            )
        ) {
            HorizontalCalendar(
                state = monthState,
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
        }
        // Week view
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 250),
                expandFrom = Alignment.Top
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 250),
                shrinkTowards = Alignment.Top
            )
        ) {
            WeekCalendar(
                state = weekState,
                dayContent = { day ->
                    val tasksForDay = tasksByDate[day.date] ?: emptyList()
                    val isSelected = selectedDate == day.date
                    WeekDayCell(
                        day = day,
                        isSelected = isSelected,
                        hasTasks = tasksForDay.isNotEmpty(),
                        isToday = day.date == java.time.LocalDate.now(),
                        onClick = { onDateSelected(day.date) }
                    )
                }
            )
        }

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
                state = listState,
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                items(tasksForSelectedDate, key = { task -> task.id }) { task ->
                    Box(
                        modifier = Modifier.animateItem()
                    ) {
                        InteractiveTaskRow(
                            task = task,
                            onToggle = { onTaskToggle(task) }
                        )
                    }
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
                        .padding(bottom = 6.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}


@Composable
fun WeekDayCell(
    day: WeekDay,
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
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = day.date.dayOfMonth.toString(),
            color = if (isSelected)
                MaterialTheme.colorScheme.onTertiary
            else if (isToday)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
        )

        // Dot if there are tasks
        if (hasTasks) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.onTertiary
                        else
                            MaterialTheme.colorScheme.tertiary
                    )
            )
        }
    }
}
