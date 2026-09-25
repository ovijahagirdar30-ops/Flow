package com.markel.flowstate.core.domain

/**
 * What the user said about the plan that was just shown, carried into the
 * next generation so the planner REVISES instead of re-rolling.
 *
 * [previousPlan] rides along deliberately: a note like "start later" is only
 * actionable if the model can see what it produced last time.
 * LocalEveningPlanner ignores this entirely (deterministic by design);
 * GeminiEveningPlanner appends it to the prompt.
 */
data class PlanFeedback(
    /** Free-text note from the plan step's comment box; may be blank. */
    val comment: String,
    /** The plan being rejected — what this feedback is about. */
    val previousPlan: EveningPlan
)

/**
 * A regenerate note that has been PERSISTED — [date] is the ISO day it was
 * typed. Unlike [PlanFeedback] (one request, dies with the session, carries
 * the rejected plan), these are the durable memory entries
 * [EveningPlanRepository.recentFeedback] reads back and GeminiEveningPlanner
 * injects into every future generation.
 */
data class PlanFeedbackNote(
    val date: String,
    val comment: String
)
