package com.markel.flowstate.core.domain

/**
 * The generated evening plan, rendered by the plan display screen. Produced
 * behind the [EveningPlanner] seam — LocalEveningPlanner fills it with a
 * deterministic offline plan today, the Gemini-backed implementation fills
 * the same shape tomorrow, and the screen can't tell the difference.
 *
 * Deliberately plain (no serialization annotations): the Gemini impl maps
 * its JSON response into this model at the boundary, so provider response
 * quirks never leak inward.
 */
data class EveningPlan(
    /** ISO date (yyyy-MM-dd) the plan is for. */
    val date: String,
    /** When this plan was generated — adaptive re-planning later will generate several an evening and show the latest. */
    val generatedAtMillis: Long,
    /** One-line mood-aware summary shown as the screen header. */
    val headline: String,
    /** Time-ordered blocks making up the evening. */
    val blocks: List<PlanBlock>,
    /**
     * True when the plan came from the offline heuristic (LocalEveningPlanner)
     * rather than the AI. Every network failure falls back silently — without
     * this flag the screen shows an offline plan as if it honored the user's
     * corrections, which is exactly how "dinner at 8pm" kept vanishing.
     * Domain-only: not persisted (the check-in step warns while it matters).
     */
    val offline: Boolean = false
)

/**
 * One scheduled chunk of the evening.
 *
 * [startTime] is a zero-padded 24h local "HH:mm" string rather than epoch
 * millis on purpose: Gemini emits and understands clock strings natively and
 * zero-padded 24h sorts cleanly. Storage stays 24h; the UI re-renders it on a
 * 12-hour clock (formatPlanTime in feature:checkin). Parsing back to a
 * timestamp (for future edit/approve) is a trivial "date + HH:mm" conversion
 * when needed.
 */
data class PlanBlock(
    /** 24h local start time, e.g. "18:30". */
    val startTime: String,
    val durationMinutes: Int,
    val title: String,
    /** Why this block is here — the mood/energy-aware justification shown to the user. */
    val reason: String,
    val kind: PlanBlockKind,
    /** Task/habit id when the block maps to one, so approve/edit can sync back later; null for free blocks (meals, rest). */
    val referenceId: Int? = null,
    /**
     * Set when the block covers a SINGLE subtask of the task in [referenceId]
     * rather than the whole task: ticking it flips only that subtask, never
     * the parent. Subtask ids are String UUIDs, hence a separate field —
     * [referenceId] always stays the parent task's Int id so the existing
     * task mapping keeps working. Null for whole-task and free blocks.
     */
    val subtaskId: String? = null
)

enum class PlanBlockKind { TASK, HABIT, MEAL, REST, REFLECTION, OTHER }
