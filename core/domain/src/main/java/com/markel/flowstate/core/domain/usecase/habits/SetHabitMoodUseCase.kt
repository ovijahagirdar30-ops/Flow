package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.HabitRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Records how a habit completion felt, after the fact — used for the
 * mood-picker prompt shown right after marking a habit done. Kept separate
 * from ToggleHabitEntryUseCase so marking complete stays instant, and the
 * mood is a quick, skippable follow-up rather than something that blocks
 * the completion itself.
 */
class SetHabitMoodUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: Int, date: LocalDate, mood: Int) =
        repository.setEntryMood(habitId, date, mood)
}

