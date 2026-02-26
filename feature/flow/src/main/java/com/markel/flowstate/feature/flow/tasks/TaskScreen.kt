package com.markel.flowstate.feature.flow.tasks

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.feature.flow.tasks.components.AnimatableTaskItem
import com.markel.flowstate.feature.flow.components.DynamicHeader
import com.markel.flowstate.feature.flow.tasks.components.EmptyStateView
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    onTaskClick: (Task) -> Unit,
    onScrolled: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Detect scroll only to activate the flag
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 90 }
            .collect { scrolled ->
                if (scrolled) onScrolled()
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        when (val state = uiState) {
            is TasksUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Empty because it loads too fast to show a loading spinner or skeleton list
                }
            }
            is TasksUiState.Success -> {
                if (state.tasks.isEmpty()) {
                    EmptyStateView()
                }
                else {
                    val reorderableState =
                        rememberReorderableLazyListState(listState) { from, to ->
                            viewModel.onReorder(from.index, to.index)
                        }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
                    ) {
                        items(state.tasks, key = { it.id }) { task ->
                            ReorderableItem(reorderableState, key = task.id) { isDragging ->
                                val scale by animateFloatAsState(
                                    targetValue = if (isDragging) 1.05f else 1.0f,
                                    label = "drag_scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .longPressDraggableHandle(
                                            interactionSource = remember { MutableInteractionSource() }
                                        )
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            alpha = if (isDragging) 0.9f else 1.0f
                                        }
                                        .zIndex(if (isDragging) 1f else 0f)
                                ) {
                                    AnimatableTaskItem(
                                        task = task,
                                        onDelete = { viewModel.deleteTask(task) },
                                        onComplete = { viewModel.toggleTaskDone(task) },
                                        onContentClick = {
                                            onTaskClick(task) // EDIT Mode
                                        }
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
