package com.markel.flowstate.feature.checkin

import com.markel.flowstate.core.domain.CheckinItem
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.checkin.CheckinMoodState
import com.markel.flowstate.core.domain.checkin.UnexpectedPlan

enum class CheckinStep { MOOD, UNEXPECTED_PLANS, TASKS, PLAN }

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