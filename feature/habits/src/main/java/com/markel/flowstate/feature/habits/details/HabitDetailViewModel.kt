package com.markel.flowstate.feature.habits.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitNumericEntry
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitType
import com.markel.flowstate.core.domain.usecase.habits.GetHabitByIdUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetNumericEntriesUseCase
import com.markel.flowstate.core.domain.usecase.habits.ToggleHabitEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHabitById: GetHabitByIdUseCase,
    private val habitRepository: HabitRepository,
    private val getNumericDetails: GetNumericEntriesUseCase,
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {

    private val habitId: Int = checkNotNull(savedStateHandle.get<String>("habitId")).toInt()
    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.calendarViewMode.collect { raw ->
                val mode = raw?.let { runCatching { CalendarViewMode.valueOf(it) }.getOrNull() }
                    ?: CalendarViewMode.ONE_MONTH
                _uiState.update { it.copy(viewMode = mode) }
            }
        }

        viewModelScope.launch {
            val habit = getHabitById(habitId) ?: return@launch
            _uiState.update { it.copy(habit = habit) }

            if (habit.habitType == HabitType.BOOLEAN) {
                loadBooleanHabitData()
            } else {
                loadNumericHabitData()
            }
        }
    }

    private suspend fun loadBooleanHabitData() {
        habitRepository.getEntriesForHabit(habitId).collect { entries ->
            val epochDays = entries.map { it.toEpochDay() }.toSet()
            _uiState.update { state ->
                state.copy(
                    allEntries = epochDays,
                    currentStreak = calculateCurrentStreak(epochDays),
                    bestStreak = calculateBestStreak(epochDays),
                    weeklyCompletions = calculateWeeklyCompletions(epochDays),
                    dayOfWeekCompletions = calculateDayOfWeekCompletions(entries)
                )
            }
        }
    }

    private suspend fun loadNumericHabitData() {
        getNumericDetails(habitId).collect { entries ->
            val entriesMap = entries.associate { it.date to it.value }

            _uiState.update { state ->
                state.copy(
                    numericEntries = entriesMap,
                    dailyValues = calculateDailyValues(entries),
                    monthlyProgress = calculateMonthlyProgress(entries, state.habit),
                    heatmapData = calculateHeatmapData(entries),
                    dayOfWeekAverages = calculateDayOfWeekAverages(entries),
                    currentStreak = calculateNumericStreak(entries, state.habit?.targetValue),
                    bestStreak = calculateNumericBestStreak(entries, state.habit?.targetValue)
                )
            }
        }
    }


    fun cycleViewMode() {
        _uiState.update { state ->
            val next = when (state.viewMode) {
                CalendarViewMode.ONE_MONTH -> CalendarViewMode.THREE_MONTHS
                CalendarViewMode.THREE_MONTHS -> CalendarViewMode.ONE_YEAR
                CalendarViewMode.ONE_YEAR -> CalendarViewMode.ONE_MONTH
            }
            state.copy(viewMode = next)
        }
        viewModelScope.launch {
            userPreferences.saveCalendarViewMode(_uiState.value.viewMode.name)
        }
    }

    fun navigatePrevious() {
        _uiState.update { state ->
            when (state.viewMode) {
                CalendarViewMode.ONE_MONTH -> {
                    val newMonth = state.displayMonth - 1
                    if (newMonth < 0) state.copy(displayMonth = 11, displayYear = state.displayYear - 1)
                    else state.copy(displayMonth = newMonth)
                }
                CalendarViewMode.THREE_MONTHS -> {
                    val newMonth = state.displayMonth - 3
                    if (newMonth < 0) state.copy(
                        displayMonth = newMonth + 12,
                        displayYear = state.displayYear - 1
                    ) else state.copy(displayMonth = newMonth)
                }
                CalendarViewMode.ONE_YEAR ->
                    state.copy(displayYear = state.displayYear - 1)
            }
        }
    }

    fun navigateNext() {
        val now = LocalDate.now()
        _uiState.update { state ->
            when (state.viewMode) {
                CalendarViewMode.ONE_MONTH -> {
                    val isAtNow = state.displayYear == now.year &&
                            state.displayMonth == now.monthValue - 1
                    if (isAtNow) state
                    else {
                        val newMonth = state.displayMonth + 1
                        if (newMonth > 11) state.copy(displayMonth = 0, displayYear = state.displayYear + 1)
                        else state.copy(displayMonth = newMonth)
                    }
                }
                CalendarViewMode.THREE_MONTHS -> {
                    val isAtNow = state.displayYear == now.year &&
                            state.displayMonth == now.monthValue - 1
                    if (isAtNow) state
                    else {
                        val newMonth = state.displayMonth + 3
                        if (newMonth > 11) state.copy(
                            displayMonth = newMonth - 12,
                            displayYear = state.displayYear + 1
                        ) else state.copy(displayMonth = newMonth)
                    }
                }
                CalendarViewMode.ONE_YEAR -> {
                    if (state.displayYear >= now.year) state
                    else state.copy(displayYear = state.displayYear + 1)
                }
            }
        }
    }

    fun setWeeklyBarsMode(mode: WeeklyBarsMode) {
        _uiState.update { it.copy(weeklyBarsMode = mode, selectedBarIndex = null) }
    }

    fun selectBar(index: Int) {
        _uiState.update { it.copy(selectedBarIndex = index) }
    }

    // ── Calculations for Boolean Habits ─────────────────────────────────────────────────────

    private fun calculateCurrentStreak(epochDays: Set<Long>): Int {
        if (epochDays.isEmpty()) return 0
        var streak = 0
        var expected = LocalDate.now().toEpochDay()
        if (expected !in epochDays) expected--
        while (expected in epochDays) { streak++; expected-- }
        return streak
    }

    private fun calculateBestStreak(epochDays: Set<Long>): Int {
        if (epochDays.isEmpty()) return 0
        val sorted = epochDays.sorted()
        var best = 1; var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1) {
                current++
                if (current > best) best = current
            } else current = 1
        }
        return best
    }

    private fun calculateWeeklyCompletions(epochDays: Set<Long>): List<Pair<LocalDate, Int>> {
        val today = LocalDate.now()
        val weeks = 16
        return (weeks - 1 downTo 0).map { weeksAgo ->
            val weekStart = today.with(DayOfWeek.MONDAY).minusWeeks(weeksAgo.toLong())
            val count = (0..6).count { day ->
                weekStart.plusDays(day.toLong()).toEpochDay() in epochDays
            }
            Pair(weekStart, count)
        }
    }

    private fun calculateDayOfWeekCompletions(entries: List<LocalDate>): Map<Int, Int> {
        val counts = mutableMapOf<Int, Int>()
        entries.forEach { date ->
            val dow = date.dayOfWeek.value  // 1=Mon, 7=Sun
            counts[dow] = (counts[dow] ?: 0) + 1
        }
        return counts
    }


    // ── Calculations for Numeric Habits ─────────────────────────────────────

    private fun calculateDailyValues(entries: List<HabitNumericEntry>): List<Pair<LocalDate, Float>> {
        val today = LocalDate.now()
        val days = 10
        val entriesMap = entries.associateBy { it.date }

        return (days - 1 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val value = entriesMap[date]?.value ?: 0f
            Pair(date, value)
        }
    }

    private fun calculateMonthlyProgress(
        entries: List<HabitNumericEntry>,
        habit: Habit?
    ): MonthlyProgress? {
        habit ?: return null
        val now = LocalDate.now()
        val yearMonth = YearMonth.from(now)
        val monthStart = yearMonth.atDay(1)

        val monthEntriesByDate = entries
            .filter { it.date.year == now.year && it.date.monthValue == now.monthValue }
            .groupBy { it.date }

        val dailyValues = monthEntriesByDate.values
            .mapNotNull { dayEntries ->
                dayEntries.maxByOrNull { it.value }?.value
            }

        val currentValue = dailyValues.sum()
        val daysWithData = monthEntriesByDate.size
        val daysCompleted = monthEntriesByDate.count { (_, dayEntries) ->
            val dailyTotal = dayEntries.sumOf { it.value.toDouble() }.toFloat()
            dailyTotal >= (habit.targetValue ?: 0f)
        }
        val totalDays = yearMonth.lengthOfMonth()
        val dailyAverage = if (daysWithData > 0) currentValue / daysWithData else 0f

        val monthName = now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            .replaceFirstChar { it.uppercase() }

        val monthTarget = habit.targetValue?.let { it * totalDays }

        val deficit = monthTarget?.let { target ->
            val remaining = target - currentValue
            if (remaining > 0) remaining else null
        }

        return MonthlyProgress(
            month = monthName,
            currentValue = currentValue,
            targetValue = monthTarget,
            daysCompleted = daysCompleted,
            totalDays = totalDays,
            dailyAverage = dailyAverage,
            deficit = deficit
        )
    }

    private fun calculateDayOfWeekAverages(entries: List<HabitNumericEntry>): List<ValueRange> {
        if (entries.isEmpty()) return emptyList()
        val daysOfWeek = DayOfWeek.entries.toTypedArray()

        return daysOfWeek.map { dow ->
            val entriesForDay = entries.filter { it.date.dayOfWeek == dow }
            println(entriesForDay)
            val average = if (entriesForDay.isNotEmpty()) {
                entriesForDay.map { it.value }.average().toFloat()
            } else 0f

            val label = dow.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                .replaceFirstChar { it.uppercase() }

            ValueRange(
                label = label,
                count = average,
                range = average..average
            )
        }
    }

    private fun calculateHeatmapData(entries: List<HabitNumericEntry>): Map<LocalDate, Float> {
        val today = LocalDate.now()
        val weeksAgo = 17
        val startDate = today.with(DayOfWeek.MONDAY).minusWeeks(weeksAgo.toLong())

        return entries
            .filter { !it.date.isBefore(startDate) && !it.date.isAfter(today) }
            .associate { it.date to it.value }
    }

    private fun calculateNumericStreak(
        entries: List<HabitNumericEntry>,
        targetValue: Float?
    ): Int {
        if (entries.isEmpty()) return 0
        val validDays = entries
            .filter { targetValue == null || it.value >= targetValue }
            .map { it.date.toEpochDay() }
            .toSortedSet(reverseOrder())

        var streak = 0
        var expected = LocalDate.now().toEpochDay()
        if (expected !in validDays) expected--
        while (expected in validDays) {
            streak++
            expected--
        }
        return streak
    }

    private fun calculateNumericBestStreak(
        entries: List<HabitNumericEntry>,
        targetValue: Float?
    ): Int {
        if (entries.isEmpty()) return 0
        val validDays = entries
            .filter { targetValue == null || it.value >= targetValue }
            .map { it.date.toEpochDay() }
            .sorted()

        if (validDays.isEmpty()) return 0

        var best = 1
        var current = 1
        for (i in 1 until validDays.size) {
            if (validDays[i] == validDays[i - 1] + 1) {
                current++
                if (current > best) best = current
            } else current = 1
        }
        return best
    }

}