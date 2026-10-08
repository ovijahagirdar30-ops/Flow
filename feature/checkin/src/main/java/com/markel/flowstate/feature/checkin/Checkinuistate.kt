package com.markel.flowstate.feature.checkin

import com.markel.flowstate.core.domain.CheckinItem
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.checkin.CheckinMoodState
import com.markel.flowstate.core.domain.checkin.UnexpectedPlan

/**
 * Steps of the evening check-in. The first seven are the designed question
 * flow (greeting → five mood questions → recap); TASKS is the "Tasks for
 * tonight" hand-off step (add/remove what the plan should fit in) and PLAN
 * is the "Planning to Plan" final stage — the planning loading screen and
 * the settled plan list both live behind it. TASKS and PLAN each own their
 * own chrome, so the designed header/footer stops at RECAP.
 */
enum class CheckinStep {
    GREET,
    ENERGY,
    SLEEP,
    STRESS,
    BODY,
    MOTIVATION,
    RECAP,
    TASKS,
    PLAN
}

/** True for the designed flow (greeting → recap) that owns the check-in chrome. */
val CheckinStep.isDesignedFlow: Boolean
    get() = ordinal <= CheckinStep.RECAP.ordinal

/** The five mood question steps, in flow order. */
val designedQuestionSteps = listOf(
    CheckinStep.ENERGY,
    CheckinStep.SLEEP,
    CheckinStep.STRESS,
    CheckinStep.BODY,
    CheckinStep.MOTIVATION
)

sealed interface CheckinUiState {
    data object Loading : CheckinUiState
    data class InProgress(
        val step: CheckinStep,
        val mood: CheckinMoodState,
        val unexpectedPlans: List<UnexpectedPlan>,
        val items: List<CheckinItem>,
        /** Evening plan shown on the final step; null until generated. */
        val plan: EveningPlan?,
        /** True while the planner runs — instant offline, seconds once Gemini backs it. */
        val isPlanning: Boolean
    ) : CheckinUiState
}