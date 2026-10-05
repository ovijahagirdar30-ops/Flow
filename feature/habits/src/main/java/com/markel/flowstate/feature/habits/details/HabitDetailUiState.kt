package com.markel.flowstate.feature.habits.details

import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitType
import java.time.LocalDate

enum class CalendarViewMode { ONE_MONTH, THREE_MONTHS, ONE_YEAR }
enum class WeeklyBarsMode { EIGHT, SIXTEEN }

data class HabitDetailUiState(
    val habit: Habit? = null,

    // For the boolean habits
    val allEntries: Set<Long> = emptySet(),  // completed days for both types (goal-met for numeric)
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val weeklyCompletions: List<Pair<LocalDate, Int>> = emptyList(),  // last 8 weeks
    val dayOfWeekCompletions: Map<Int, Float> = emptyMap(), // 1=Mon..7=Sun -> completion rate (0..1)
    val moodHistory: List<MoodLogEntry> = emptyList(), // mood-tagged completions, newest first

    // For the numeric habits
    val numericEntries: Map<LocalDate, Float> = emptyMap(),
    val dailyValues: List<Pair<LocalDate, Float>> = emptyList(), // last 10 days
    val monthlyProgress: MonthlyProgress? = null,
    val dayOfWeekAverages: List<ValueRange> = emptyList(),
    val heatmapData: Map<LocalDate, Float> = emptyMap(), // last 18 weeks

    // For both
    val viewMode: CalendarViewMode = CalendarViewMode.ONE_MONTH,
    val displayYear: Int = LocalDate.now().year,
    val displayMonth: Int = LocalDate.now().monthValue - 1,  // 0-based
    val weeklyBarsMode: WeeklyBarsMode = WeeklyBarsMode.EIGHT,
    val selectedBarIndex: Int? = null  // null = last week by default
){
    val isNumeric: Boolean get() = habit?.habitType == HabitType.NUMERIC

    /** Average of the logged moods; null while nothing is logged yet. */
    val moodAverage: Double? get() = moodHistory.map { it.mood }.average().takeIf { moodHistory.isNotEmpty() }
}

/** One mood-tagged completion, shown on the habit's detail page. */
data class MoodLogEntry(val date: LocalDate, val mood: Int)

data class MonthlyProgress(
    val month: String,
    val currentValue: Float,
    val targetValue: Float?,
    val daysCompleted: Int,
    val totalDays: Int,
    val dailyAverage: Float,
    val deficit: Float?
)

data class ValueRange(
    val label: String,
    val count: Float,
    val range: ClosedFloatingPointRange<Float>
)