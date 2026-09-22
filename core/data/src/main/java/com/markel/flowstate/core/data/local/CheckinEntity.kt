package com.markel.flowstate.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per calendar day. Primary key is the ISO date string (matching
 * CheckinDebounce's date format), so re-saving the same day naturally
 * overwrites via @Upsert with no lookup-first needed.
 */
@Entity(tableName = "checkins")
data class CheckinEntity(
    @PrimaryKey
    val date: String,
    val energy: Int,
    val sleepiness: Int,
    val stress: Int,
    val headache: Int,
    val motivation: Int,
    val unexpectedPlansEncoded: String,
    // Per-slider comments for the future AI brain — nullable with no SQL
    // default on purpose, so the v24→v25 migration is a bare ADD COLUMN
    // (zero default-mismatch risk); old rows read back as "" in the repo.
    val energyComment: String? = null,
    val sleepinessComment: String? = null,
    val stressComment: String? = null,
    val headacheComment: String? = null,
    val motivationComment: String? = null
)