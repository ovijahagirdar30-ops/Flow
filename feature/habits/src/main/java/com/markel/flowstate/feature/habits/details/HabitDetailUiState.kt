package com.markel.flowstate.feature.habits.details

import com.markel.flowstate.core.domain.Habit
import java.time.LocalDate

enum class CalendarViewMode { ONE_MONTH, THREE_MONTHS, ONE_YEAR }
enum class WeeklyBarsMode { EIGHT, SIXTEEN }

data class HabitDetailUiState(
    val habit: Habit? = null,
    val allEntries: Set<Long> = emptySet(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val viewMode: CalendarViewMode = CalendarViewMode.ONE_MONTH,
    val displayYear: Int = LocalDate.now().year,
    val displayMonth: Int = LocalDate.now().monthValue - 1,  // 0-based
    val weeklyBarsMode: WeeklyBarsMode = WeeklyBarsMode.EIGHT,
    val selectedBarIndex: Int? = null,  // null = last week by default
    val weeklyCompletions: List<Pair<LocalDate, Int>> = emptyList(),  // last 8 weeks
    val dayOfWeekCompletions: Map<Int, Int> = emptyMap()  // 1=Mon..7=Sun -> count
)