package com.markel.flowstate.feature.flow

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.feature.flow.components.CheckListGridCard
import com.markel.flowstate.feature.flow.components.DynamicHeader
import com.markel.flowstate.feature.flow.components.IdeaGridCard
import com.markel.flowstate.feature.flow.components.TaskGridCard
import com.markel.flowstate.feature.flow.tasks.TaskScreen
import com.markel.flowstate.feature.flow.tasks.TaskViewModel
import com.markel.flowstate.feature.flow.tasks.components.EmptyStateView

@Composable
fun FlowScreen(
    flowViewModel: FlowViewModel,
    taskViewModel: TaskViewModel
) {
    val isGridView by flowViewModel.isGridView.collectAsStateWithLifecycle()
    val flowUiState by flowViewModel.flowUiState.collectAsStateWithLifecycle()

    // Única fuente de la verdad para el header general
    var isHeaderMinimized by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        DynamicHeader(
            isMinimized = isHeaderMinimized,
            isGridView = isGridView,
            onToggleView = { flowViewModel.toggleView() }
        )

        AnimatedContent(
            targetState = isGridView,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(200)) },
            label = "flow_view_transition"
        ) { showGrid ->
            if (showGrid) {
                GridView(
                    uiState = flowUiState,
                    onScrolled = { isHeaderMinimized = true },
                    onToggleTask = { flowViewModel.toggleTaskDone(it) },
                    onDeleteTask = { flowViewModel.deleteTask(it) },
                    onDeleteIdea = { flowViewModel.deleteIdea(it) }
                )
            } else {
                // TaskScreen manages all is internal states still (FAB, sheets, editor)
                TaskScreen(
                    viewModel = taskViewModel,
                    onScrolled = { isHeaderMinimized = true }
                )
            }
        }
    }
}

@Composable
private fun GridView(
    uiState: FlowUiState,
    onScrolled: () -> Unit,
    onToggleTask: (com.markel.flowstate.core.domain.Task) -> Unit,
    onDeleteTask: (com.markel.flowstate.core.domain.Task) -> Unit,
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
                val gridState = rememberLazyStaggeredGridState()

                LaunchedEffect(gridState) {
                    snapshotFlow { gridState.firstVisibleItemIndex > 0 }
                        .collect { if (it) onScrolled() }
                }

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
                                    onComplete = { onToggleTask(item.task) },
                                    onDelete = { onDeleteTask(item.task) }
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