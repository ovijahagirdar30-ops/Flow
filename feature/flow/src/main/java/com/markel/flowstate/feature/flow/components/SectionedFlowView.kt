package com.markel.flowstate.feature.flow.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.feature.flow.FlowUiState
import com.markel.flowstate.feature.flow.tasks.components.AnimatableTaskItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyRowState

/**
 * Main sectioned view for the Flow screen.
 *
 * Layout:
 *  - Tasks    → vertical reorderable list using [AnimatableTaskItem]
 *  - Lists    → horizontal reorderable carousel using [CheckListGridCard]
 *  - Ideas    → horizontal reorderable carousel using [IdeaGridCard] (taller cards)
 *
 * Cards are the same components used in the old GridView so shared-element
 * transitions to editor screens continue to work out of the box.
 *
 * Future cross-section reordering:
 *  Replace per-section reorderable states with a unified drag state that tracks
 *  the source section and exposes a combined drop target across LazyList instances.
 */
@Composable
fun SectionedFlowView(
    uiState: FlowUiState,
    onScrolled: () -> Unit,
    // Task callbacks
    onTaskClick: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit,
    onTaskToggle: (Task) -> Unit,
    onTaskReorder: (from: Int, to: Int) -> Unit,
    onTaskDragEnd: () -> Unit,
    // Idea callbacks
    onIdeaClick: (Idea) -> Unit,
    onIdeaReorder: (from: Int, to: Int) -> Unit,
    onIdeaDragEnd: () -> Unit,
    // CheckList callbacks
    onCheckListClick: (CheckList) -> Unit,
    onCheckListReorder: (from: Int, to: Int) -> Unit,
    onCheckListDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState !is FlowUiState.Success) return

    val outerListState = rememberLazyListState()

    LaunchedEffect(outerListState) {
        snapshotFlow {
            outerListState.firstVisibleItemIndex > 0 || outerListState.firstVisibleItemScrollOffset > 90
        }.collect { scrolled -> if (scrolled) onScrolled() }
    }

    LazyColumn(
        state = outerListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // ── Tasks ──────────────────────────────────────────────────────────
        if (uiState.tasks.isNotEmpty()) {
            item(key = "tasks_header") {
                SectionHeader(
                    title = "TASKS",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
            item(key = "tasks_list") {
                TasksSection(
                    tasks = uiState.tasks,
                    onTaskClick = onTaskClick,
                    onTaskDelete = onTaskDelete,
                    onTaskToggle = onTaskToggle,
                    onReorder = onTaskReorder,
                    onDragEnd = onTaskDragEnd
                )
            }
        }

        // ── CheckLists ─────────────────────────────────────────────────────
        if (uiState.checkLists.isNotEmpty()) {
            item(key = "checklists_header") {
                SectionHeader(
                    title = "LISTS",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
                )
            }
            item(key = "checklists_carousel") {
                ReorderableCarousel(
                    items = uiState.checkLists,
                    key = { it.id },
                    onReorder = onCheckListReorder,
                    onDragEnd = onCheckListDragEnd
                ) { checkList ->
                    CheckListGridCard(
                        checkList = checkList,
                        onClick = { onCheckListClick(checkList) },
                        modifier = Modifier.width(160.dp)
                    )
                }
            }
        }

        // ── Ideas ──────────────────────────────────────────────────────────
        if (uiState.ideas.isNotEmpty()) {
            item(key = "ideas_header") {
                SectionHeader(
                    title = "IDEAS & NOTES",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
                )
            }
            item(key = "ideas_carousel") {
                ReorderableCarousel(
                    items = uiState.ideas,
                    key = { it.id },
                    onReorder = onIdeaReorder,
                    onDragEnd = onIdeaDragEnd
                ) { idea ->
                    // Taller modifier to visually differentiate from checklist cards
                    IdeaGridCard(
                        idea = idea,
                        onClick = { onIdeaClick(idea) },
                        modifier = Modifier
                            .width(200.dp)
                            .height(160.dp)
                    )
                }
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

// ── Tasks — vertical reorderable list ────────────────────────────────────────
//
// Uses a non-scrollable inner LazyColumn with a fixed height derived from item
// count. The outer LazyColumn (in SectionedFlowView) handles all scrolling,
// avoiding nested-scroll conflicts.

@Composable
private fun TasksSection(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit,
    onTaskToggle: (Task) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onDragEnd: () -> Unit
) {
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        onReorder(from.index, to.index)
    }

    // Estimated height per task row. Adjust if your AnimatableTaskItem is taller.
    val itemHeightDp = 72.dp
    val listHeight = itemHeightDp * tasks.size

    LazyColumn(
        state = listState,
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(listHeight)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            ReorderableItem(reorderableState, key = task.id) { isDragging ->
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.03f else 1f,
                    label = "task_drag_scale"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .longPressDraggableHandle(
                            onDragStopped = { onDragEnd() },
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                            alpha = if (isDragging) 0.92f else 1f
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                ) {
                    AnimatableTaskItem(
                        task = task,
                        onDelete = { onTaskDelete(task) },
                        onComplete = { onTaskToggle(task) },
                        onContentClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }
}