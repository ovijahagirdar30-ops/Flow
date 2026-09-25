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
