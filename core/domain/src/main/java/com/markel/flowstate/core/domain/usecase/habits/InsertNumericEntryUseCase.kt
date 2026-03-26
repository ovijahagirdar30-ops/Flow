package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.HabitRepository
import java.time.LocalDate
import javax.inject.Inject

class InsertNumericEntryUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: Int, value: Float, date: LocalDate) = repository.logNumericEntry(habitId, date, value)
}