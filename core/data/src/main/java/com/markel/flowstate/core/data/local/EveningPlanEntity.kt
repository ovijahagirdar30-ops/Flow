package com.markel.flowstate.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One AGREED evening plan, keyed by ISO date (same yyyy-MM-dd convention as
 * CheckinEntity), so re-agreeing the same day replaces rather than stacks.
 *
 * Nothing is ever written here without an explicit Agree tap at the end of
 * the check-in — plan generation itself stays transient behind EveningPlanner.
 *
 * checkedIndexesJson is nullable with NO SQL default (the v25 lesson: bare
 * columns keep migrations trivially exact): NULL means nothing ticked yet.
 * It is reserved for the upcoming Plan-tab checklist — the column exists now
 * so that stage never needs its own migration.
 */
@Entity(tableName = "evening_plans")
data class EveningPlanEntity(
    @PrimaryKey val date: String,
    val headline: String,
    val generatedAtMillis: Long,
    /** PlanBlock list as JSON, mapped at the data boundary (see EveningPlanRepositoryImpl). */
    val blocksJson: String,
    /** JSON array of ticked block indexes, or null if none ticked. */
    val checkedIndexesJson: String?
)
