package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.HabitEntryFlat
import com.markel.flowstate.core.domain.HabitRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllBooleanEntriesUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    operator fun invoke(): Flow<List<HabitEntryFlat>> {
        return repository.getAllEntries()
    }
}
