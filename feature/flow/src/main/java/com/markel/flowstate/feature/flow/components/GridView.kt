package com.markel.flowstate.feature.flow.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.feature.flow.FlowUiState
import com.markel.flowstate.feature.flow.GridItem
import com.markel.flowstate.feature.flow.tasks.components.EmptyStateView
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState

@Composable
fun GridView(
    uiState: FlowUiState,
    onScrolled: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onIdeaClick: (Idea) -> Unit,
    onCheckListClick: (CheckList) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragEnd: () -> Unit
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
                val reorderState = rememberReorderableLazyStaggeredGridState(gridState) { from, to ->
                    onReorder(from.index, to.index)
                }
                LaunchedEffect(reorderState.isAnyItemDragging) {
                    // When is false, the user has dropped the item, and now we can update de db
                    if (!reorderState.isAnyItemDragging) onDragEnd()
                }
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 9.dp
                ) {
                    items(
                        items = uiState.items,
                        key = { item ->
                            when (item) {
                                is GridItem.TaskItem -> "task_${item.task.id}"
                                is GridItem.IdeaItem -> "idea_${item.idea.id}"
                                is GridItem.CheckListItem -> "cl_${item.checkList.id}"
                            }
                        }
                    ) { item ->
                        val itemKey = when (item) {
                            is GridItem.TaskItem -> "task_${item.task.id}"
                            is GridItem.IdeaItem -> "idea_${item.idea.id}"
                            is GridItem.CheckListItem -> "cl_${item.checkList.id}"
                        }
                        ReorderableItem(reorderState, key = itemKey) { isDragging ->
                            val scale by animateFloatAsState(
                                targetValue = if (isDragging) 1.05f else 1.0f,
                                label = "drag_scale"
                            )
                            Box(Modifier
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = if (isDragging) 0.9f else 1.0f
                                }
                                .zIndex(if (isDragging) 1f else 0f)
                            ) {
                                when (item) {
                                    is GridItem.TaskItem ->
                                        TaskGridCard(
                                            task = item.task,
                                            onClick = { onTaskClick(item.task) },
                                            modifier = Modifier.longPressDraggableHandle()
                                        )
                                    is GridItem.IdeaItem ->
                                        IdeaGridCard(
                                            idea = item.idea,
                                            onClick = { onIdeaClick(item.idea) },
                                            modifier = Modifier.longPressDraggableHandle()
                                        )
                                    is GridItem.CheckListItem ->
                                        CheckListGridCard(
                                            checkList = item.checkList,
                                            onClick = { onCheckListClick(item.checkList) },
                                            modifier = Modifier.longPressDraggableHandle()
                                        )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}