package com.markel.flowstate.feature.flow

import android.R.attr.data
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markel.flowstate.feature.flow.components.DynamicHeader
import com.markel.flowstate.feature.flow.components.ExpandableFabMenu
import com.markel.flowstate.feature.flow.tasks.TaskViewModel
import com.markel.flowstate.feature.flow.tasks.components.TaskCreationSheetContent
import com.markel.flowstate.feature.flow.tasks.util.HandleSystemBars
import com.markel.flowstate.feature.flow.components.SectionedFlowView
import com.markel.flowstate.feature.flow.components.AnimatedUndoFab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowScreen(
    flowViewModel: FlowViewModel,
    taskViewModel: TaskViewModel = hiltViewModel(),
    // Nvigation Callbacks to detail screens (edition)
    onNavigateToTaskEditor: (taskId: Int) -> Unit,
    onNavigateToIdeaEditor: (ideaId: Int) -> Unit,
    onNavigateToNewIdea: () -> Unit,
    onNavigateToCheckListEditor: (checkListId: Int?) -> Unit
) {
    val flowUiState by flowViewModel.uiState.collectAsStateWithLifecycle()
    val showPermissionBanner by flowViewModel.showReminderBanner.collectAsStateWithLifecycle()
    val showUndoButton by flowViewModel.showUndoButton.collectAsStateWithLifecycle()
    val taskDeleteVersions by flowViewModel.taskDeleteVersions.collectAsStateWithLifecycle()
    var isFabExpanded by remember { mutableStateOf(false) }
    var showCreationSheet by remember { mutableStateOf(false) }

    // Only source of truth for the header
    var isHeaderMinimized by rememberSaveable { mutableStateOf(false) }

    val draft by taskViewModel.draft.collectAsStateWithLifecycle()  // State with all the info for the new task

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),  // To avoid big gaps of surface at the top & bottom
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                DynamicHeader(
                    isMinimized = isHeaderMinimized
                )

                SectionedFlowView(
                    uiState = flowUiState,
                    onScrolled = { isHeaderMinimized = true },
                    onTaskClick = { onNavigateToTaskEditor(it.id) },
                    onTaskDelete = { task -> flowViewModel.onTaskSwiped(task) },
                    onTaskToggle = { taskViewModel.toggleTaskDone(it) },
                    onTaskReorder = { from, to -> flowViewModel.onTaskReorder(from, to) },
                    onIdeaClick = { onNavigateToIdeaEditor(it.id) },
                    onIdeaReorder = { from, to -> flowViewModel.onIdeaReorder(from, to) },
                    onCheckListClick = { onNavigateToCheckListEditor(it.id) },
                    onCheckListReorder = { from, to -> flowViewModel.onCheckListReorder(from, to) },
                    showPermissionBanner = showPermissionBanner,
                    taskDeleteVersions = taskDeleteVersions
                )
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
                    reminderTime = draft.reminderTime,
                    onReminderTimeChange = { taskViewModel.updateDraftReminderTime(it) },
                    onSave = { _, _, _, _, _ ->
                        taskViewModel.submitDraft()
                        showCreationSheet = false
                    }
                )
            }
        }
        AnimatedVisibility(
            visible = !showCreationSheet,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 2.dp, bottom = 4.dp)
                .zIndex(1f)
        ) {
            ExpandableFabMenu(
                expanded = isFabExpanded,
                onToggle = { isFabExpanded = !isFabExpanded },
                onTaskClick = { isFabExpanded = false; showCreationSheet = true },
                onIdeaClick = { isFabExpanded = false; onNavigateToNewIdea() },
                onCheckListClick = { isFabExpanded = false; onNavigateToCheckListEditor(null) }
            )
        }
        AnimatedUndoFab(
            visible = showUndoButton,
            onUndoClick = { flowViewModel.undoPendingDeletions() },
            modifier = Modifier
                .align(Alignment.BottomStart)
        )
    }
}