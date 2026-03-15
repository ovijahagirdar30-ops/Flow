package com.markel.flowstate.feature.habits.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.format.TextStyle
import com.markel.flowstate.feature.habits.R
import java.time.Month
import java.time.temporal.ChronoUnit
import java.util.Locale

fun HabitDetailUiState.completionPct(): String {
    if (allEntries.isEmpty()) return "0%"
    val now = LocalDate.now()
    val start = when (viewMode) {
        CalendarViewMode.ONE_MONTH -> now.withDayOfMonth(1)
        CalendarViewMode.THREE_MONTHS -> now.withDayOfMonth(1).minusMonths(2)
        CalendarViewMode.ONE_YEAR -> now.withDayOfYear(1)
    }
    val days = ChronoUnit.DAYS.between(start, now.plusDays(1)).toInt()
    val done = allEntries.count { epochDay ->
        val d = LocalDate.ofEpochDay(epochDay)
        !d.isBefore(start) && !d.isAfter(now)
    }
    return "${(done * 100 / days)}%"
}

@Composable
fun HabitDetailUiState.pctLabel() = when (viewMode) {
    CalendarViewMode.ONE_MONTH -> stringResource(R.string.habit_detail_pct_month)
    CalendarViewMode.THREE_MONTHS -> stringResource(R.string.habit_detail_pct_3months)
    CalendarViewMode.ONE_YEAR -> stringResource(R.string.habit_detail_pct_year)
}

fun CalendarViewMode.label() = when (this) {
    CalendarViewMode.ONE_MONTH -> "1M"
    CalendarViewMode.THREE_MONTHS -> "3M"
    CalendarViewMode.ONE_YEAR -> "1A"
}

fun HabitDetailUiState.navigationLabel(locale: Locale): String {
    val monthNames = Month.values()
    return when (viewMode) {
        CalendarViewMode.ONE_MONTH ->
            "${monthNames[displayMonth].getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { it.uppercase() }} $displayYear"
        CalendarViewMode.THREE_MONTHS -> {
            var mStart = displayMonth - 2
            var yStart = displayYear
            if (mStart < 0) { mStart += 12; yStart-- }
            "${monthNames[mStart].getDisplayName(TextStyle.SHORT, locale)
                .replaceFirstChar { it.uppercase() }} – ${monthNames[displayMonth]
                .getDisplayName(TextStyle.SHORT, locale)
                .replaceFirstChar { it.uppercase() }} $displayYear"
        }
        CalendarViewMode.ONE_YEAR -> "$displayYear"
    }
}