package com.markel.flowstate.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One regenerate note the user typed during a check-in, kept past the
 * session so the planner inherits it on LATER evenings — the durable
 * memory half of "skincare is only 5 minutes, not 20".
 *
 * A plain append-only log keyed by autoincrement id: regenerating twice in
 * one evening records two rows (both are real corrections), and `date` is
 * context for the prompt rather than part of the key. `createdAtMillis`
 * drives the newest-first read in [PlanFeedbackDao.recentFeedback].
 */
@Entity(tableName = "plan_feedback")
data class PlanFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** ISO yyyy-MM-dd day the note was typed (same convention as checkins/evening_plans). */
    val date: String,
    val comment: String,
    val createdAtMillis: Long
)
