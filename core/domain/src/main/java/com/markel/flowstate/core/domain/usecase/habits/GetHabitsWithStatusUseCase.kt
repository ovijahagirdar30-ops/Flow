package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitWithStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class GetHabitsWithStatusUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    operator fun invoke(date: LocalDate = LocalDate.now()): Flow<List<HabitWithStatus>> {
        return combine(
            repository.getHabits(),
            repository.getAllEntries()
        ) { habits, allEntries ->
            val today = date.toEpochDay()
            val entriesByHabit = allEntries.groupBy { it.habitId }

            habits.map { habit ->
                val entries = entriesByHabit[habit.id] ?: emptyList()
                val isCompletedToday = entries.any { it.epochDay == today }
                val streak = calculateStreak(entries.map { it.epochDay }, date)
                HabitWithStatus(habit, isCompletedToday, streak)
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
}