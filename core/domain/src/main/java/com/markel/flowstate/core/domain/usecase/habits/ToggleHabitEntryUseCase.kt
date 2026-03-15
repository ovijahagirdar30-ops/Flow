package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.HabitRepository
import java.time.LocalDate
import javax.inject.Inject

class ToggleHabitEntryUseCase @Inject constructor(private val repository: HabitRepository) {
    suspend operator fun invoke(habitId: Int, date: LocalDate = LocalDate.now()) =
        repository.toggleEntry(habitId, date)
}