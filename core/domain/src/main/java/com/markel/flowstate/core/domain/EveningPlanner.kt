package com.markel.flowstate.core.domain

/**
 * Produces the evening plan from a [CheckinSnapshot]. This interface is the
 * swappable seam for the whole AI-brain decision:
 *
 *  - [LocalEveningPlanner] — deterministic, offline, zero dependencies;
 *    powers the display screen until the real backend lands.
 *  - Gemini-backed implementation (coming next) — posts the snapshot,
 *    parses the response into [EveningPlan].
 *  - A Python/Ollama service later — same interface, one new class.
 *
 * App and feature code depend ONLY on this type; swapping backends is a
 * Hilt binding change, nothing more.
 */
interface EveningPlanner {
    /** Generates an evening plan for [snapshot]'s date. */
    suspend fun generatePlan(snapshot: CheckinSnapshot): EveningPlan
}
