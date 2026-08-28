package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.HabitRepository
import javax.inject.Inject

/**
 * Persists a new priority ordering across habits — 0 is highest priority.
 * This is what the future AI scheduler will read to decide what gets
 * cut first when a day doesn't have room for everything.
 */
class UpdateHabitsPriorityOrderUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(ranks: List<Pair<Int, Int>>) {
        repository.updatePriorityRanks(ranks)
    }
}

