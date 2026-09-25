package com.markel.flowstate.core.domain

/**
 * Produces the night check-in's closing message: one short, warm,
 * guilt-free paragraph about how the day actually went.
 *
 * The swappable seam, exactly like [EveningPlanner]:
 *  - GeminiEncouragementGenerator (core:data) — writes it from
 *    [DayReviewStats] over REST, falls back to [LocalEncouragementGenerator]
 *    whenever the key is missing or the call fails.
 *  - [LocalEncouragementGenerator] — deterministic offline templates.
 *
 * App and feature code depend ONLY on this type; swapping backends is a
 * Hilt binding change.
 */
interface EncouragementGenerator {

    /**
     * The closing line for [stats]. Implementations should degrade rather
     * than throw — the page must always end with something kind.
     */
    suspend fun generate(stats: DayReviewStats): String
}
