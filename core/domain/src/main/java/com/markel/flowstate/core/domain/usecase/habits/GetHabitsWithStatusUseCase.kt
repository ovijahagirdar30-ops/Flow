package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitEntryFlat
import com.markel.flowstate.core.domain.HabitNumericEntry
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitSchedule
import com.markel.flowstate.core.domain.HabitStreaks
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

/**
 * Builds today's per-habit status: completion, due-ness and streak — all
 * driven by the habit's [HabitSchedule]:
 *
 *  - **Due today** = the weekday is scheduled and, for a times-per-week
 *    habit, the weekly target isn't met yet (once it is, the habit rests
 *    until next week — see the check-in list and header progress).
 *  - **Streaks** come from the shared [HabitStreaks] math: consecutive
 *    scheduled days without a target (off-days skip, never break), Consecutive ISO weeks with one.
 */
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

        return habits
            .sortedBy { it.position }
            .map { habit ->
                when (habit.habitType) {
                    HabitType.BOOLEAN -> {
                        val entries = boolEntriesByHabit[habit.id] ?: emptyList()
                        val completedDays = entries.map { it.epochDay }.toSet()
                        val isCompletedToday = today in completedDays
                        HabitWithStatus(
                            habit = habit,
                            isCompletedToday = isCompletedToday,
                            streak = HabitStreaks.current(habit.schedule, completedDays, date),
                            isDueToday = isDueToday(habit.schedule, isCompletedToday, completedDays, date)
                        )
                    }
                    HabitType.NUMERIC -> {
                        val entries = numericByHabit[habit.id] ?: emptyList()
                        val entriesByDay = entries.associateBy { it.date.toEpochDay() }
                        val todayValue = entriesByDay[today]?.value

                        // Completed if there is a value today and is bigger than the goal (or simply have a value without goal)
                        val isCompletedToday = when {
                            todayValue == null -> false
                            habit.targetValue != null -> todayValue >= habit.targetValue
                            else -> todayValue > 0f
                        }

                        // Days that count as completed for streak/due — the
                        // same validity rule the old numeric streak used.
                        val completedDays = entries
                            .filter { habit.targetValue == null || it.value >= habit.targetValue }
                            .map { it.date.toEpochDay() }
                            .toSet()

                        HabitWithStatus(
                            habit = habit,
                            isCompletedToday = isCompletedToday,
                            streak = HabitStreaks.current(habit.schedule, completedDays, date),
                            isDueToday = isDueToday(habit.schedule, isCompletedToday, completedDays, date),
                            todayValue = todayValue
                        )
                    }
                }
            }
    }

    /**
     * Scheduled AND (no weekly target OR the target still has room this
     * week). Completing today always counts as due so the header's done/total
     * keeps showing it; once a target habit is met and today's completion
     * isn't there, it disappears until Monday.
     */
    private fun isDueToday(
        schedule: HabitSchedule,
        isCompletedToday: Boolean,
        completedDays: Set<Long>,
        date: LocalDate,
    ): Boolean {
        if (!schedule.isScheduledOn(date)) return false
        if (isCompletedToday) return true
        val target = schedule.weeklyTarget ?: return true
        return HabitStreaks.completionsInWeek(schedule, completedDays, date.with(DayOfWeek.MONDAY), until = date) < target
    }
}
