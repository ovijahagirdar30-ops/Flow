package com.markel.flowstate.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EveningPlanDao {

    /** REPLACE keyed by `date` — re-agreeing the same day overwrites the previous plan. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: EveningPlanEntity)

    /** The most recent agreed plan (newest date first), or null if none exists yet. */
    @Query("SELECT * FROM evening_plans ORDER BY date DESC LIMIT 1")
    suspend fun latestPlan(): EveningPlanEntity?
}
