package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.HabitRepository
import java.time.LocalDate
import javax.inject.Inject

class IncrementNumericValueUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: Int, date: LocalDate, currentValue: Float?, step: Float) {
        val newValue = (currentValue ?: 0f) + step
        repository.logNumericEntry(habitId, date, newValue)
    }
}
