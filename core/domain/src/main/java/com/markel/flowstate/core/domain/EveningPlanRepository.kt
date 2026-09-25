package com.markel.flowstate.core.domain

/**
 * Persistence for the evening plan the user explicitly AGREED to — the
 * bridge between the check-in popup's ephemeral plan step and the Plan
 * checklist tab. Only agreed plans are ever written here; generation itself
 * stays transient behind [EveningPlanner].
 *
 * The checklist tick state lives alongside the plan: [checkedIndexes] reads
 * it back and [setCheckedIndexes] writes it (the storage column landed with
 * the table itself, so ticking needed no migration).
 */
interface EveningPlanRepository {

    /** Persists [plan] as the agreed plan for its date; re-agreeing the same date replaces it. */
    suspend fun saveAgreedPlan(plan: EveningPlan)

    /** The most recently agreed plan by date, or null if the user has never agreed to one. */
    suspend fun latestAgreedPlan(): EveningPlan?

    /**
     * The ticked block indexes for [date]'s agreed plan, sorted ascending —
     * empty if nothing is ticked or [date] has no saved plan.
     */
    suspend fun checkedIndexes(date: String): List<Int>

    /**
     * Replaces the ticked block indexes for [date]'s agreed plan.
     * No storage write happens if [date] has no saved plan; an empty [indexes]
     * list clears the column back to null.
     */
    suspend fun setCheckedIndexes(date: String, indexes: List<Int>)

    /**
     * Durably records a regenerate note so LATER evenings inherit it — the
     * planner's long-term memory ("skincare is only 5 minutes, not 20").
     * Blank comments are ignored; every non-blank note is appended, so
     * multiple regenerations in one check-in all survive.
     */
    suspend fun recordFeedback(date: String, comment: String)

    /** The last [limit] recorded notes, newest first, oldest last. */
    suspend fun recentFeedback(limit: Int): List<PlanFeedbackNote>
}
