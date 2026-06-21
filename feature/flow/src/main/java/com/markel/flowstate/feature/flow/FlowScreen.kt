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
import com.markel.flowstate.core.designsystem.components.AnimatedUndoFab
import com.markel.flowstate.feature.flow.components.CategoryTabRow
import com.markel.flowstate.feature.flow.components.CreateCategoryDialog
import com.markel.flowstate.feature.flow.components.DynamicHeader
import com.markel.flowstate.feature.flow.components.ExpandableFabMenu
import com.markel.flowstate.feature.flow.components.ReorderCategoriesSheet
import com.markel.flowstate.feature.flow.tasks.TaskViewModel
import com.markel.flowstate.feature.flow.tasks.components.TaskCreationSheetContent
import com.markel.flowstate.feature.flow.tasks.util.HandleSystemBars
import com.markel.flowstate.feature.flow.components.SectionedFlowView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowScreen(
    flowViewModel: FlowViewModel,
    taskViewModel: TaskViewModel = hiltViewModel(),
    // Nvigation Callbacks to detail screens (edition)
    onNavigateToTaskEditor: (taskId: Int) -> Unit,
    onNavigateToIdeaEditor: (ideaId: Int) -> Unit,
    onNavigateToNewIdea: (categoryId: Int?) -> Unit,
    onNavigateToCheckListEditor: (checkListId: Int?, categoryId: Int?) -> Unit
) {
    val flowUiState by flowViewModel.uiState.collectAsStateWithLifecycle()
    val showPermissionBanner by flowViewModel.showReminderBanner.collectAsStateWithLifecycle()
    val showUndoButton by flowViewModel.showUndoButton.collectAsStateWithLifecycle()
    val taskDeleteVersions by flowViewModel.taskDeleteVersions.collectAsStateWithLifecycle()
    var isFabExpanded by remember { mutableStateOf(false) }
    var showCreationSheet by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showReorderCategoriesSheet by remember { mutableStateOf(false) }

    // Only source of truth for the header
    var isHeaderMinimized by rememberSaveable { mutableStateOf(false) }

    val draft by taskViewModel.draft.collectAsStateWithLifecycle()  // State with all the info for the new task

    // Extract category info from state
    val categoriesEnabled = (flowUiState as? FlowUiState.Success)?.categoriesEnabled == true
    val categories = (flowUiState as? FlowUiState.Success)?.categories ?: emptyList()
    val selectedCategoryId = (flowUiState as? FlowUiState.Success)?.selectedCategoryId
    val reorderableCategories = remember(categories) {
        categories.filter { !it.name.equals("General", ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),  // To avoid big gaps of surface at the top & bottom
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                DynamicHeader(
                    isMinimized = isHeaderMinimized
                )
                // ── Category tabs (only when enabled) ───────────────
                if (categoriesEnabled && categories.isNotEmpty()) {
                    CategoryTabRow(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = { flowViewModel.selectCategory(it) },
                        onAddCategoryClick = { showCreateCategoryDialog = true },
                        onCategoryLongPress = { showReorderCategoriesSheet = true }
                    )
                }

                SectionedFlowView(
                    uiState = flowUiState,
                    onScrolled = { isHeaderMinimized = true },
                    onTaskClick = { onNavigateToTaskEditor(it.id) },
                    onTaskDelete = { task -> flowViewModel.onTaskSwiped(task) },
                    onTaskToggle = { taskViewModel.toggleTaskDone(it) },
                    onTaskReorder = { from, to -> flowViewModel.onTaskReorder(from, to) },
                    onIdeaClick = { onNavigateToIdeaEditor(it.id) },
                    onIdeaReorder = { from, to -> flowViewModel.onIdeaReorder(from, to) },
                    onCheckListClick = { onNavigateToCheckListEditor(it.id, it.categoryId) },
                    onCheckListReorder = { from, to -> flowViewModel.onCheckListReorder(from, to) },
                    showPermissionBanner = showPermissionBanner,
                    taskDeleteVersions = taskDeleteVersions,
                    categoriesEnabled = categoriesEnabled
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
                        taskViewModel.submitDraft(categoryId = if (categoriesEnabled) selectedCategoryId else null)
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
                onIdeaClick = { isFabExpanded = false; onNavigateToNewIdea(selectedCategoryId) },
                onCheckListClick = { isFabExpanded = false; onNavigateToCheckListEditor(null, selectedCategoryId) }
            )
        }
        AnimatedUndoFab(
            visible = showUndoButton,
            onUndoClick = { flowViewModel.undoPendingDeletions() },
            modifier = Modifier
                .align(Alignment.BottomStart)
        )

        // ── Create category dialog (opened from the trailing "+ New category" tab) ──
        if (showCreateCategoryDialog) {
            CreateCategoryDialog(
                onDismiss = { showCreateCategoryDialog = false },
                onConfirm = { name ->
                    flowViewModel.createCategory(name)
                    showCreateCategoryDialog = false
                }
            )
        }

        // ── Reorder / switch category sheet (opened by long-pressing a tab) ──
        if (showReorderCategoriesSheet) {
            ReorderCategoriesSheet(
                categories = reorderableCategories,
                onReorder = { flowViewModel.reorderCategories(it) },
                onCategorySelected = { id ->
                    flowViewModel.selectCategory(id)
                    showReorderCategoriesSheet = false
                },
                onDismiss = { showReorderCategoriesSheet = false }
            )
        }
    }
}