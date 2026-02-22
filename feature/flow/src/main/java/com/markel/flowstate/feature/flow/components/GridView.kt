package com.markel.flowstate.feature.flow.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.markel.flowstate.feature.flow.FlowUiState
import com.markel.flowstate.feature.flow.WorkspaceItem
import com.markel.flowstate.feature.flow.tasks.components.EmptyStateView

@Composable
fun GridView(
    uiState: FlowUiState,
    onScrolled: () -> Unit,
    onTaskClick: (com.markel.flowstate.core.domain.Task) -> Unit,
    onDeleteIdea: (com.markel.flowstate.core.domain.Idea) -> Unit
) {
    val gridState = rememberLazyStaggeredGridState()
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex > 0 }
            .collect { if (it) onScrolled() }
    }

    when (uiState) {
        is FlowUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {}
        is FlowUiState.Success -> {
            if (uiState.items.isEmpty()) {
                EmptyStateView()
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(
                        items = uiState.items,
                        key = { item ->
                            when (item) {
                                is WorkspaceItem.TaskItem      -> "task_${item.task.id}"
                                is WorkspaceItem.IdeaItem      -> "idea_${item.idea.id}"
                                is WorkspaceItem.CheckListItem -> "cl_${item.checkList.id}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is WorkspaceItem.TaskItem ->
                                TaskGridCard(
                                    task = item.task,
                                    onClick = { onTaskClick(item.task) }
                                )
                            is WorkspaceItem.IdeaItem ->
                                IdeaGridCard(
                                    idea = item.idea,
                                    onDelete = { onDeleteIdea(item.idea) }
                                )
                            is WorkspaceItem.CheckListItem ->
                                CheckListGridCard(checkList = item.checkList)
                        }
                    }
                }
            }
        }
    }
}