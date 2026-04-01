package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.HabitNumericEntry
import com.markel.flowstate.core.domain.HabitRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllNumericEntriesUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    operator fun invoke(): Flow<List<HabitNumericEntry>> {
        return repository.getAllNumericEntries()
    }
}
