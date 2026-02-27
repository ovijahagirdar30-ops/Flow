package com.markel.flowstate.feature.flow

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.feature.flow.components.DynamicHeader
import com.markel.flowstate.feature.flow.components.ExpandableFabMenu
import com.markel.flowstate.feature.flow.tasks.TaskScreen
import com.markel.flowstate.feature.flow.tasks.TaskViewModel
import com.markel.flowstate.feature.flow.tasks.components.TaskCreationSheetContent
import com.markel.flowstate.feature.flow.tasks.util.HandleSystemBars
import com.markel.flowstate.feature.flow.components.GridView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowScreen(
    flowViewModel: FlowViewModel = hiltViewModel(),
    taskViewModel: TaskViewModel = hiltViewModel(),
    // Nvigation Callbacks to detail screens (edition)
    onNavigateToTaskEditor: (taskId: Int) -> Unit,
    onNavigateToIdeaEditor: (ideaId: Int) -> Unit,
    onNavigateToNewIdea: () -> Unit
) {
    val isGridView by flowViewModel.isGridView.collectAsStateWithLifecycle()
    if (isGridView == null) return
    val flowUiState by flowViewModel.flowUiState.collectAsStateWithLifecycle()

    var isFabExpanded by remember { mutableStateOf(false) }
    var showCreationSheet by remember { mutableStateOf(false) }

    // Only source of truth for the header
    var isHeaderMinimized by rememberSaveable { mutableStateOf(false) }

    val draft by taskViewModel.draft.collectAsStateWithLifecycle()  // State with all the info for the new task

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),  // To avoid big gaps of surface at the top & bottom
            floatingActionButton = {
                AnimatedVisibility(!showCreationSheet) {
                    ExpandableFabMenu(
                        expanded = isFabExpanded,
                        onToggle = { isFabExpanded = !isFabExpanded },
                        onTaskClick = { isFabExpanded = false; showCreationSheet = true },
                        onIdeaClick = { isFabExpanded = false; onNavigateToNewIdea() }
                    )
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                DynamicHeader(
                    isMinimized = isHeaderMinimized,
                    isGridView = isGridView!!,
                    onToggleView = { flowViewModel.toggleView() }
                )

                AnimatedContent(
                    targetState = isGridView,
                    transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(200)) },
                    label = "flow_view_transition"
                ) { showGrid ->
                    if (showGrid == true) {
                        GridView(
                            uiState = flowUiState,
                            onScrolled = { isHeaderMinimized = true },
                            onTaskClick = { onNavigateToTaskEditor(it.id) },
                            onIdeaClick = { onNavigateToIdeaEditor(it.id) },
                            onReorder = { from, to -> flowViewModel.onGridReorder(from, to) },
                            onDragEnd = { flowViewModel.onGridDragEnd() }
                        )
                    } else {
                        // TaskScreen manages all of the internal states still (FAB, sheets, editor)
                        TaskScreen(
                            viewModel = taskViewModel,
                            onScrolled = { isHeaderMinimized = true },
                            onTaskClick = { onNavigateToTaskEditor(it.id) }
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
    }
}