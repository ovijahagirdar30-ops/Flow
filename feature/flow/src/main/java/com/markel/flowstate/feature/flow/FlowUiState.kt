package com.markel.flowstate.feature.flow

import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.Task


sealed class GridItem {
    data class TaskItem(val task: Task) : GridItem()
    data class IdeaItem(val idea: Idea) : GridItem()
    data class CheckListItem(val checkList: CheckList) : GridItem()
}

sealed interface FlowUiState {
    data object Loading : FlowUiState
    data class Success(val items: List<GridItem>) : FlowUiState
}