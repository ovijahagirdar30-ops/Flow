package com.markel.flowstate.feature.habits.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitNumericEntry
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitSchedule
import com.markel.flowstate.core.domain.HabitStreaks
import com.markel.flowstate.core.domain.HabitType
import com.markel.flowstate.core.domain.usecase.habits.GetHabitByIdUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetNumericEntriesUseCase
import com.markel.flowstate.core.domain.usecase.habits.ToggleHabitEntryUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel(assistedFactory = HabitDetailViewModel.Factory::class)
class HabitDetailViewModel @AssistedInject constructor(
    @Assisted private val habitId: Int,
    private val getHabitById: GetHabitByIdUseCase,
    private val habitRepository: HabitRepository,
    private val getNumericDetails: GetNumericEntriesUseCase,
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(habitId: Int): HabitDetailViewModel
    }
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
        combine(
            habitRepository.getEntriesForHabit(habitId),
            habitRepository.getMoodsForHabit(habitId)
        ) { entries, moods -> entries to moods }
            .collect { (entries, moods) ->
                val epochDays = entries.map { it.toEpochDay() }.toSet()
                _uiState.update { state ->
                    // Every section the numeric page fills gets the equivalent
                    // here (done = 1f, missed = 0f) so both detail pages render
                    // the same section list.
                    val dayOfWeekRates = calculateDayOfWeekCompletions(
                        entries, state.habit?.createdAt, state.habit?.schedule
                    )
                    state.copy(
                        allEntries = epochDays,
                        currentStreak = state.habit?.schedule
                            ?.let { HabitStreaks.current(it, epochDays, LocalDate.now()) } ?: 0,
                        bestStreak = state.habit?.schedule
                            ?.let { HabitStreaks.best(it, epochDays) } ?: 0,
                        weeklyCompletions = calculateWeeklyCompletions(epochDays),
                        dayOfWeekCompletions = dayOfWeekRates,
                        dailyValues = calculateDailyValuesFromCompletions(epochDays),
                        heatmapData = calculateHeatmapFromCompletions(epochDays),
                        monthlyProgress = calculateMonthlyProgressFromCompletions(epochDays, state.habit),
                        dayOfWeekAverages = completionRateDistribution(dayOfWeekRates),
                        // Feeds the detail page's Mood section; the query
                        // already returns newest-first and mood-not-null.
                        moodHistory = moods.mapNotNull { e ->
                            e.mood?.let { MoodLogEntry(LocalDate.ofEpochDay(e.epochDay), it) }
                        }
                    )
                }
            }
    }

    private suspend fun loadNumericHabitData() {
        getNumericDetails(habitId).collect { entries ->
            val entriesMap = entries.associate { it.date to it.value }

            _uiState.update { state ->
                val habit = state.habit
                // "Completed" = target met (or any log when there is no
                // target) — the same rule the streaks use. It feeds the
                // shared calendar, weekly-bars and radar sections.
                val completedDays = numericCompletedDays(entries, habit?.targetValue)
                val completedDates = completedDays.map { LocalDate.ofEpochDay(it) }
                state.copy(
                    numericEntries = entriesMap,
                    allEntries = completedDays,
                    currentStreak = habit?.let {
                        HabitStreaks.current(it.schedule, completedDays, LocalDate.now())
                    } ?: 0,
                    bestStreak = habit?.let {
                        HabitStreaks.best(it.schedule, completedDays)
                    } ?: 0,
                    weeklyCompletions = calculateWeeklyCompletions(completedDays),
                    dayOfWeekCompletions = calculateDayOfWeekCompletions(
                        completedDates, habit?.createdAt, habit?.schedule
                    ),
                    dailyValues = calculateDailyValues(entries),
                    monthlyProgress = calculateMonthlyProgress(entries, habit),
                    heatmapData = calculateHeatmapData(entries),
                    dayOfWeekAverages = calculateDayOfWeekAverages(entries, habit?.createdAt, habit?.schedule)
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

    // ── Sections shared by both habit types ────────────────────────────────────────────────

    /** Last 10 days as 1f (done) / 0f (missed) — Evolution for boolean habits. */
    private fun calculateDailyValuesFromCompletions(epochDays: Set<Long>): List<Pair<LocalDate, Float>> {
        val today = LocalDate.now()
        return (9 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            date to if (date.toEpochDay() in epochDays) 1f else 0f
        }
    }

    /** Last 18 weeks of completions — Heatmap for boolean habits (missed days stay absent). */
    private fun calculateHeatmapFromCompletions(epochDays: Set<Long>): Map<LocalDate, Float> {
        val today = LocalDate.now()
        val startDate = today.with(DayOfWeek.MONDAY).minusWeeks(17)
        return (0..ChronoUnit.DAYS.between(startDate, today))
            .map { startDate.plusDays(it) }
            .filter { it.toEpochDay() in epochDays }
            .associateWith { 1f }
    }

    /** Monthly goal for boolean habits: days completed vs scheduled days this month. */
    private fun calculateMonthlyProgressFromCompletions(epochDays: Set<Long>, habit: Habit?): MonthlyProgress? {
        habit ?: return null
        val now = LocalDate.now()
        val yearMonth = YearMonth.from(now)
        val monthStart = yearMonth.atDay(1)
        val elapsedDays = (ChronoUnit.DAYS.between(monthStart, now) + 1).toInt()

        val totalDays = (0 until yearMonth.lengthOfMonth()).count {
            habit.schedule.isScheduledOn(monthStart.plusDays(it.toLong()))
        }
        val daysCompleted = (0 until elapsedDays).count {
            monthStart.plusDays(it.toLong()).toEpochDay() in epochDays
        }
        val elapsedScheduled = (0 until elapsedDays).count {
            habit.schedule.isScheduledOn(monthStart.plusDays(it.toLong()))
        }

        val monthName = now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            .replaceFirstChar { it.uppercase() }

        return MonthlyProgress(
            month = monthName,
            currentValue = daysCompleted.toFloat(),
            targetValue = totalDays.toFloat(),
            daysCompleted = daysCompleted,
            totalDays = totalDays,
            dailyAverage = if (elapsedScheduled > 0) daysCompleted.toFloat() / elapsedScheduled else 0f,
            deficit = (totalDays - daysCompleted).toFloat().takeIf { it > 0 }
        )
    }

    /** Weekday breakdown for the Average section from a 0..1 completion rate. */
    private fun completionRateDistribution(rates: Map<Int, Float>): List<ValueRange> =
        rates.map { (dow, rate) ->
            val label = DayOfWeek.of(dow)
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .replaceFirstChar { it.uppercase() }
            ValueRange(label = label, count = rate, range = rate..rate)
        }

    // ── Calculations for Boolean Habits ─────────────────────────────────────────────────────

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

    private fun calculateDayOfWeekCompletions(entries: List<LocalDate>, createdAt: LocalDate?, schedule: HabitSchedule?): Map<Int, Float> {
        if (createdAt == null) return emptyMap()
        val today = LocalDate.now()
        val start = if (createdAt.isAfter(today)) today else createdAt

        // Count completions per day of week (only entries within the habit's lifetime)
        val completionsByDow = mutableMapOf<Int, Int>()
        entries.forEach { date ->
            if (!date.isBefore(start) && !date.isAfter(today)) {
                val dow = date.dayOfWeek.value  // 1=Mon, 7=Sun
                completionsByDow[dow] = (completionsByDow[dow] ?: 0) + 1
            }
        }

        // Opportunities per day of week — only for SCHEDULED weekdays: an
        // off-day has no expectation, so it never drags a rate down.
        val opportunitiesByDow = countDaysOfWeekBetween(start, today)
            .filterKeys { dow -> schedule?.days?.contains(DayOfWeek.of(dow)) != false }

        // Calculate completion rate per day of week
        return (1..7)
            .filter { dow -> schedule?.days?.contains(DayOfWeek.of(dow)) != false }
            .associateWith { dow ->
            val opportunities = opportunitiesByDow[dow] ?: 0
            if (opportunities > 0) {
                (completionsByDow[dow] ?: 0).toFloat() / opportunities
            } else 0f
        }
    }

    private fun countDaysOfWeekBetween(start: LocalDate, end: LocalDate): Map<Int, Int> {
        if (start.isAfter(end)) return emptyMap()
        val totalDays = ChronoUnit.DAYS.between(start, end) + 1  // inclusive
        val startDow = start.dayOfWeek.value  // 1=Mon, 7=Sun

        val counts = mutableMapOf<Int, Int>()
        for (dow in 1..7) {
            val diff = (dow - startDow + 7) % 7  // days until first occurrence of 'dow'
            counts[dow] = if (diff < totalDays) {
                1 + ((totalDays - 1 - diff) / 7).toInt()
            } else 0
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

        val monthEntriesByDate = entries
            .filter { it.date.year == now.year && it.date.monthValue == now.monthValue && !it.date.isAfter(now) }
            .groupBy { it.date }

        val dailyMaxValues = monthEntriesByDate.mapValues { (_, dayEntries) ->
            dayEntries.maxByOrNull { it.value }?.value ?: 0f
        }

        val currentValue = dailyMaxValues.values.sum()
        val daysWithData = monthEntriesByDate.size
        val daysCompleted = dailyMaxValues.count { (_, maxValue) ->
            maxValue >= (habit.targetValue ?: 0f)
        }
        val totalDays = (1..yearMonth.lengthOfMonth()).count { day ->
            habit.schedule.isScheduledOn(yearMonth.atDay(day))
        }
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

    private fun calculateDayOfWeekAverages(
        entries: List<HabitNumericEntry>,
        createdAt: LocalDate? = null,
        schedule: HabitSchedule? = null
    ): List<ValueRange> {
        if (entries.isEmpty()) return emptyList()

        val today = LocalDate.now()
        val start = createdAt?.let { if (it.isAfter(today)) today else it } ?: today
        val daysOfWeek = DayOfWeek.entries.toTypedArray()

        // Count how many opportunities (days) exist per day of week since habit creation
        val opportunitiesByDow = countDaysOfWeekBetween(start, today)

        // Sum values per day of week (only for days within the habit's lifetime)
        val sumByDow = mutableMapOf<Int, Float>()
        entries.forEach { entry ->
            if (!entry.date.isBefore(start) && !entry.date.isAfter(today)) {
                val dow = entry.date.dayOfWeek.value
                sumByDow[dow] = (sumByDow[dow] ?: 0f) + entry.value
            }
        }

        return daysOfWeek
            .filter { dow -> schedule?.days?.contains(dow) != false }
            .map { dow ->
            val dowValue = dow.value
            val opportunities = opportunitiesByDow[dowValue] ?: 0
            // Average over ALL opportunities: days without data count as 0
            val average = if (opportunities > 0) {
                (sumByDow[dowValue] ?: 0f) / opportunities
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

    /** Days whose value met the target — the same rule the status use case uses. */
    private fun numericCompletedDays(entries: List<HabitNumericEntry>, targetValue: Float?): Set<Long> =
        entries
            .filter { targetValue == null || it.value >= targetValue }
            .map { it.date.toEpochDay() }
            .toSet()

}