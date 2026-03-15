package com.markel.flowstate.feature.habits.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.usecase.habits.GetHabitByIdUseCase
import com.markel.flowstate.core.domain.usecase.habits.ToggleHabitEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import javax.inject.Inject

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHabitById: GetHabitByIdUseCase,
    private val habitRepository: HabitRepository,
    private val toggleEntry: ToggleHabitEntryUseCase
) : ViewModel() {

    private val habitId: Int = checkNotNull(savedStateHandle.get<String>("habitId")).toInt()
    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val habit = getHabitById(habitId) ?: return@launch
            habitRepository.getEntriesForHabit(habitId).collect { entries ->
                val epochDays = entries.map { it.toEpochDay() }.toSet()
                _uiState.update { state ->
                    state.copy(
                        habit = habit,
                        allEntries = epochDays,
                        currentStreak = calculateCurrentStreak(epochDays),
                        bestStreak = calculateBestStreak(epochDays),
                        weeklyCompletions = calculateWeeklyCompletions(epochDays),
                        dayOfWeekCompletions = calculateDayOfWeekCompletions(entries)
                    )
                }
            }
        }
    }

    fun cycleViewMode() {
        _uiState.update { state ->
            val next = when (state.viewMode) {
                CalendarViewMode.ONE_MONTH    -> CalendarViewMode.THREE_MONTHS
                CalendarViewMode.THREE_MONTHS -> CalendarViewMode.ONE_YEAR
                CalendarViewMode.ONE_YEAR     -> CalendarViewMode.ONE_MONTH
            }
            state.copy(viewMode = next)
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

    // ── Calculations ─────────────────────────────────────────────────────

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
}