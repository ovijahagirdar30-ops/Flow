package com.markel.flowstate.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlanFeedbackDao {

    /** Appends a note — id autoincrements, so every correction in a session survives. */
    @Insert
    suspend fun insert(feedback: PlanFeedbackEntity)

    /**
     * The newest [limit] notes, newest first (`id` breaks same-millisecond
     * ties); the planner reverses the list for chronological reading.
     */
    @Query("SELECT * FROM plan_feedback ORDER BY createdAtMillis DESC, id DESC LIMIT :limit")
    suspend fun recentFeedback(limit: Int): List<PlanFeedbackEntity>
}
