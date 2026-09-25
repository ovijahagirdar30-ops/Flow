package com.markel.flowstate.core.domain

/**
 * Produces the evening plan from a [CheckinSnapshot]. This interface is the
 * swappable seam for the whole AI-brain decision:
 *
 *  - GeminiEveningPlanner (core:data) — posts the snapshot over REST and
 *    parses the response into [EveningPlan]; appends [PlanFeedback] on
 *    regenerate so the model revises rather than re-rolls.
 *  - [LocalEveningPlanner] — deterministic, offline, zero dependencies;
 *    doubles as the fallback when Gemini is unreachable or unkeyed, and
 *    ignores feedback by design.
 *  - A Python/Ollama service later — same interface, one new class.
 *
 * App and feature code depend ONLY on this type; swapping backends is a
 * Hilt binding change, nothing more.
 */
interface EveningPlanner {
    /**
     * Generates an evening plan for [snapshot]'s date. [feedback] is null on
     * the first generation; Regenerate passes the user's comment plus the
     * plan being rejected.
     */
    suspend fun generatePlan(snapshot: CheckinSnapshot, feedback: PlanFeedback? = null): EveningPlan
}
