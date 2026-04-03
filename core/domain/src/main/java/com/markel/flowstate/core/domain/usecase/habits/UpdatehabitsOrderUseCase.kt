package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.HabitRepository
import javax.inject.Inject

class UpdateHabitsOrderUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(positions: List<Pair<Int, Int>>) {
        repository.updatePositions(positions)
    }
}