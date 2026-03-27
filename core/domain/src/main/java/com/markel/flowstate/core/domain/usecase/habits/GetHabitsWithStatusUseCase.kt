package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitEntryFlat
import com.markel.flowstate.core.domain.HabitNumericEntry
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitType
import com.markel.flowstate.core.domain.HabitWithStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

class GetHabitsWithStatusUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(date: LocalDate = LocalDate.now()): Flow<List<HabitWithStatus>> {
        // First we get the habits, then we combine them (boolean + numeric)
        return repository.getHabits()
            .flatMapLatest { habits ->
                if (habits.isEmpty()) return@flatMapLatest flowOf(emptyList())

                // Boolean habits flow
                val boolFlow = repository.getAllEntries()

                // A flow for every numeric habit, combined within one only flow
                // Map<habitId, List<HabitNumericEntry>>
                val numericFlows = habits
                    .filter { it.habitType == HabitType.NUMERIC }
                    .map { habit -> repository.getNumericEntries(habit.id) }

                if (numericFlows.isEmpty()) {
                    boolFlow.combine(flowOf(emptyMap<Int, List<HabitNumericEntry>>())) { entries, numeric ->
                        buildStatus(habits, entries.groupBy { it.habitId }, numeric, date)
                    }
                } else {
                    val numericHabitIds = habits
                        .filter { it.habitType == HabitType.NUMERIC }
                        .map { it.id }

                    val combinedNumeric: Flow<Map<Int, List<HabitNumericEntry>>> =
                        numericFlows.reduce { acc, flow ->
                            acc.combine(flow) { a, b -> a + b }
                        }.combine(flowOf(numericHabitIds)) { allEntries, ids ->
                            allEntries.groupBy { it.habitId }
                                .filterKeys { it in ids }
                        }

                    boolFlow.combine(combinedNumeric) { boolEntries, numericByHabit ->
                        buildStatus(
                            habits = habits,
                            boolEntriesByHabit = boolEntries.groupBy { it.habitId },
                            numericByHabit = numericByHabit,
                            date = date
                        )
                    }
                }
            }
    }

    private fun buildStatus(
        habits: List<Habit>,
        boolEntriesByHabit: Map<Int, List<HabitEntryFlat>>,
        numericByHabit: Map<Int, List<HabitNumericEntry>>,
        date: LocalDate
    ): List<HabitWithStatus> {
        val today = date.toEpochDay()
        val weekStart = date.with(DayOfWeek.MONDAY)

        return habits
            .sortedBy { it.position }
            .map { habit ->
                when (habit.habitType) {
                    HabitType.BOOLEAN -> {
                        val entries = boolEntriesByHabit[habit.id] ?: emptyList()
                        val isCompletedToday = entries.any { it.epochDay == today }
                        val streak = calculateStreak(entries.map { it.epochDay }, date)
                        HabitWithStatus(
                            habit = habit,
                            isCompletedToday = isCompletedToday,
                            streak = streak
                        )
                    }
                    HabitType.NUMERIC -> {
                        val entries = numericByHabit[habit.id] ?: emptyList()
                        val entriesByDay = entries.associateBy { it.date.toEpochDay() }
                        val todayValue = entriesByDay[today]?.value

                        val weekValues = (0L..6L).map { offset ->
                            val day = weekStart.plusDays(offset).toEpochDay()
                            entriesByDay[day]?.value
                        }

                        // Completed if there is a value today and is bigger than the goal (or simply have a value without goal)
                        val isCompletedToday = todayValue != null &&
                                (habit.targetValue == null || todayValue >= habit.targetValue)

                        val streak = calculateNumericStreak(entries, habit.targetValue, date)

                        HabitWithStatus(
                            habit = habit,
                            isCompletedToday = isCompletedToday,
                            streak = streak,
                            todayValue = todayValue,
                            weekValues = weekValues
                        )
                    }
                }
            }
    }

    private fun calculateStreak(epochDays: List<Long>, from: LocalDate): Int {
        if (epochDays.isEmpty()) return 0
        val sorted = epochDays.toSortedSet(reverseOrder())
        var streak = 0
        var expected = from.toEpochDay()

        // If today is not completed, we start counting from yesterday
        if (expected !in sorted) expected--

        while (expected in sorted) {
            streak++
            expected--
        }
        return streak
    }

    private fun calculateNumericStreak(
        entries: List<HabitNumericEntry>,
        targetValue: Float?,
        from: LocalDate
    ): Int {
        if (entries.isEmpty()) return 0
        val validDays = entries
            .filter { targetValue == null || it.value >= targetValue }
            .map { it.date.toEpochDay() }
            .toSortedSet(reverseOrder())

        var streak = 0
        var expected = from.toEpochDay()
        if (expected !in validDays) expected--
        while (expected in validDays) {
            streak++
            expected--
        }
        return streak
    }
}