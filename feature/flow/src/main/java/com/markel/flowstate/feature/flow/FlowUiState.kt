package com.markel.flowstate.feature.flow

import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.Task

sealed interface FlowUiState {
    data object Loading : FlowUiState
    data class Success(
        val tasks: List<Task>,
        val ideas: List<Idea>,
        val checkLists: List<CheckList>
    ) : FlowUiState
}