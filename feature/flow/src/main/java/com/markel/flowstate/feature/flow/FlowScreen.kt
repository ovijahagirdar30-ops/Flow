package com.markel.flowstate.feature.flow

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.feature.flow.components.CheckListGridCard
import com.markel.flowstate.feature.flow.components.DynamicHeader
import com.markel.flowstate.feature.flow.components.ExpandableFabMenu
import com.markel.flowstate.feature.flow.components.IdeaGridCard
import com.markel.flowstate.feature.flow.components.TaskGridCard
import com.markel.flowstate.feature.flow.tasks.TaskScreen
import com.markel.flowstate.feature.flow.tasks.TaskViewModel
import com.markel.flowstate.feature.flow.tasks.components.EmptyStateView
import com.markel.flowstate.feature.flow.tasks.components.TaskCreationSheetContent
import com.markel.flowstate.feature.flow.tasks.components.TaskEditorOverlay
import com.markel.flowstate.feature.flow.tasks.util.HandleSystemBars
import com.markel.flowstate.feature.flow.components.GridView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowScreen(
    flowViewModel: FlowViewModel = hiltViewModel(),
    taskViewModel: TaskViewModel = hiltViewModel()
) {
    val isGridView by flowViewModel.isGridView.collectAsStateWithLifecycle()
    val flowUiState by flowViewModel.flowUiState.collectAsStateWithLifecycle()

    var isFabExpanded by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showCreationSheet by remember { mutableStateOf(false) }

    // Only source of truth for the header
    var isHeaderMinimized by rememberSaveable { mutableStateOf(false) }

    val draft by taskViewModel.draft.collectAsStateWithLifecycle()

    // For task edition
    var editorPriority by remember(taskToEdit) { mutableStateOf(taskToEdit?.priority ?: Priority.NOTHING) }
    var editorDueDate by remember(taskToEdit) { mutableStateOf(taskToEdit?.dueDate) }

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),  // To avoid big gaps of surface at the top & bottom
            floatingActionButton = {
                AnimatedVisibility(visible = taskToEdit == null && !showCreationSheet) {
                    ExpandableFabMenu(
                        expanded = isFabExpanded,
                        onToggle = { isFabExpanded = !isFabExpanded },
                        onTaskClick = { isFabExpanded = false; taskToEdit = null; showCreationSheet = true },
                        onIdeaClick = { isFabExpanded = false  /* TODO: Implement something to show while creating an idea */}
                    )
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
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
                        // TaskScreen manages all of the internal states still (FAB, sheets, editor)
                        TaskScreen(
                            viewModel = taskViewModel,
                            onScrolled = { isHeaderMinimized = true },
                            onTaskClick = { clickedTask ->
                                taskToEdit = clickedTask
                            }
                        )
                    }
                }
            }
        }
        if (showCreationSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCreationSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = null,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                HandleSystemBars(isLandscape)

                TaskCreationSheetContent(
                    title = draft.title,
                    onTitleChange = { taskViewModel.updateDraftTitle(it) },
                    description = draft.description,
                    onDescriptionChange = { taskViewModel.updateDraftDescription(it) },
                    priority = draft.priority,
                    onPriorityChange = { taskViewModel.updateDraftPriority(it) },
                    dueDate = draft.dueDate,
                    onDueDateChange = { taskViewModel.updateDraftDueDate(it) },
                    onSave = { _, _, _, _ ->
                        taskViewModel.submitDraft()
                        showCreationSheet = false
                    }
                )
            }
        }
        TaskEditorOverlay(
            task = taskToEdit,
            onDismiss = { taskToEdit = null },
            priority = editorPriority,
            onPriorityChange = { editorPriority = it },
            dueDate = editorDueDate,
            onDueDateChange = { editorDueDate = it },
            onUpdate = { task, title, desc, prio, date, subs ->
                taskViewModel.updateTask(task, title, desc, prio, date, subs)
            }
        )
    }
}