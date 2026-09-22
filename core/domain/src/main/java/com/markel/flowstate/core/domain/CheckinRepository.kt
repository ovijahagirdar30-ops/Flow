package com.markel.flowstate.core.domain

import com.markel.flowstate.core.domain.checkin.Checkin
import com.markel.flowstate.core.domain.checkin.CheckinMoodState
import com.markel.flowstate.core.domain.checkin.UnexpectedPlan
import kotlinx.coroutines.flow.Flow

/**
 * Defines check-in persistence. Lives in :core:domain, knows nothing about
 * Room — same contract pattern as TaskRepository.
 */
interface CheckinRepository {
    /** Saves (or overwrites) today's check-in. */
    suspend fun saveTodayCheckin(mood: CheckinMoodState, unexpectedPlans: List<UnexpectedPlan>)

    /** Reads a specific day's check-in, if one was saved. */
    suspend fun getCheckinByDate(date: String): Checkin?

    /** All saved check-ins, most recent first — not used yet, but this is what Insights / AI Memory will read from later. */
    fun getAllCheckins(): Flow<List<Checkin>>
}