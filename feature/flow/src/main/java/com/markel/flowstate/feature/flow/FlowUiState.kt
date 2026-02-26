package com.markel.flowstate.feature.flow

import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.Task


sealed class WorkspaceItem {
    data class TaskItem(val task: Task) : WorkspaceItem()
    data class IdeaItem(val idea: Idea) : WorkspaceItem()
    data class CheckListItem(val checkList: CheckList) : WorkspaceItem()
}

sealed interface FlowUiState {
    data object Loading : FlowUiState
    data class Success(val items: List<WorkspaceItem>) : FlowUiState
}