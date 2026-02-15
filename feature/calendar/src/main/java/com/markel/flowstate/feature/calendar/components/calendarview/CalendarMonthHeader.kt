package com.markel.flowstate.feature.calendar.components.calendarview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.weekcalendar.WeekCalendarState
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarMonthHeader(
    isExpanded: Boolean,
    monthState: CalendarState,
    weekState: WeekCalendarState,
    selectedDate: java.time.LocalDate
    ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val displayMonth = if (isExpanded) {
            monthState.firstVisibleMonth.yearMonth
        } else {
            // En vista de semana: si el día seleccionado está en la semana visible, mostrar su mes
            // Si no, mostrar el mes de la semana visible (cuando te desplazas sin seleccionar)
            val selectedYearMonth = YearMonth.from(selectedDate)
            val visibleWeekYearMonth = YearMonth.from(weekState.firstVisibleWeek.days.first().date)

            // Verificar si selectedDate está en la semana visible
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
    }
}