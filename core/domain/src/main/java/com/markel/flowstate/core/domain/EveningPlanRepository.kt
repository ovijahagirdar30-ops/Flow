package com.markel.flowstate.core.domain

/**
 * Persistence for the evening plan the user explicitly AGREED to — the
 * bridge between the check-in popup's ephemeral plan step and the Plan
 * checklist tab. Only agreed plans are ever written here; generation itself
 * stays transient behind [EveningPlanner].
 */
interface EveningPlanRepository {

    /** Persists [plan] as the agreed plan for its date; re-agreeing the same date replaces it. */
    suspend fun saveAgreedPlan(plan: EveningPlan)

    /** The most recently agreed plan by date, or null if the user has never agreed to one. */
    suspend fun latestAgreedPlan(): EveningPlan?
}
