package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.MoodEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Feeds the Mood tab — every habit completion that had a mood recorded,
 * most recent first.
 */
class GetMoodHistoryUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    operator fun invoke(): Flow<List<MoodEntry>> = repository.getMoodHistory()
}