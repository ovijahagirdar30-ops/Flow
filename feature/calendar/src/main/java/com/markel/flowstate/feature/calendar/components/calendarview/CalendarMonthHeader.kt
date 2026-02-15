package com.markel.flowstate.feature.calendar.components.calendarview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.weekcalendar.WeekCalendarState
import com.markel.flowstate.feature.calendar.R
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarMonthHeader(
    isExpanded: Boolean,
    monthState: CalendarState,
    weekState: WeekCalendarState,
    selectedDate: java.time.LocalDate,
    onTodayClick: () -> Unit
    ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val displayMonth = if (isExpanded) {
            monthState.firstVisibleMonth.yearMonth
        } else {
            // In the week view: if the selected day is visible, show that month
            // Else, show the one of the visible week (when scrolling)
            val selectedYearMonth = YearMonth.from(selectedDate)
            val visibleWeekYearMonth = YearMonth.from(weekState.firstVisibleWeek.days.first().date)

            val isSelectedInVisibleWeek = weekState.firstVisibleWeek.days.any { it.date == selectedDate }

            if (isSelectedInVisibleWeek) {
                selectedYearMonth
            } else {
                visibleWeekYearMonth
            }

        }

        Text(
            text = displayMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() } + " " + displayMonth.year,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        // Today button
        IconButton(
            onClick = onTodayClick ,
        ) {
            TodayIcon(
                outlineColor = MaterialTheme.colorScheme.onSurface,
                dotColor = MaterialTheme.colorScheme.tertiary
            )
        }

    }
}