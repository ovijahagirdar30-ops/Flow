package com.markel.flowstate.feature.flow

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.Category
import com.markel.flowstate.core.domain.CategoryRepository
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.CheckListRepository
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.tasks.DeleteTaskUseCase
import com.markel.flowstate.core.notifications.ReminderScheduler
import com.markel.flowstate.core.notifications.buildAlarmItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FlowScreen coordinator.
 *
 * Responsibilities:
 *  - Control cross-screen UI state (isGridView)
 *  - Combine Tasks, Ideas and CheckLists into a sectioned UiState
 *  - Optimistic reordering per section with DB persistence on drag end
 *
 * Future extensibility note:
 *  Cross-section reordering (e.g. Task → Idea conversion) can be added by
 *  introducing a shared reorder method that mutates multiple local state flows
 *  and calls the corresponding repository operations in a single transaction.
*/
@HiltViewModel
class FlowViewModel @Inject constructor(
    // Repositories required for the unified grid
    private val taskRepository: TaskRepository,
    private val ideaRepository: IdeaRepository,
    private val checkListRepository: CheckListRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val applicationScope: CoroutineScope

) : ViewModel(), DefaultLifecycleObserver {

    // ── Optimistic local state ─────────────────────────────────
    private val _uiState = MutableStateFlow<FlowUiState>(FlowUiState.Loading)
    val uiState: StateFlow<FlowUiState> = _uiState

    // ── Banner ────────────────────────────────────────────────────────────────

    // Backed by a MutableStateFlow so we can force a re-evaluation on resume without waiting for a DB emission
    private val _bannerVisible = MutableStateFlow(false)
    val showReminderBanner: StateFlow<Boolean> = _bannerVisible

    // Tracks whether the permission was missing the last time we checked
    // When it flips from false → true (user just granted it), we reschedule
    private var wasPermissionMissing = false

    // ── Deferred deletion / Undo ──────────────────────────────────────────────

    /** Tasks that have been swiped-to-dismiss but not yet permanently deleted. */
    private val _pendingUndoTasks = MutableStateFlow<Map<Int, Task>>(emptyMap())

    /** Whether the undo FAB should be visible – true while any deletion is pending. */
    val showUndoButton: StateFlow<Boolean> = _pendingUndoTasks
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Per-task monotonic counter incremented each time the user swipes-to-delete.
     * Used as part of the `LazyColumn` item key so that, after an undo, Compose
     * creates a **fresh** composable with a fresh `SwipeToDismissBoxState` instead
     * of restoring the stale `EndToStart` saved-state that would immediately
     * trigger `onDelete()` again.
     */
    private val _taskDeleteVersions = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val taskDeleteVersions: StateFlow<Map<Int, Int>> = _taskDeleteVersions

    /**
     * One [Job] per pending task, representing the 3.5 s countdown to permanent
     * deletion.  When the job completes the task is removed from the DB; when the
     * user taps undo the job is canceled.
     */
    private val deleteJobs = mutableMapOf<Int, Job>()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    // ── Init: combine repos + pending-filter ──────────────────────────────────
    init {
        viewModelScope.launch {
            val coreDataFlow = combine(
                taskRepository.getTasks(),
                ideaRepository.getIdeas(),
                checkListRepository.getLists(),
                categoryRepository.getCategories()
            ) { tasks, ideas, lists, categories ->
                // Combine these into a temporary data class
                CoreData(tasks, ideas, lists, categories)
            }
            // Combine like this to not lose type-safe navigation
            combine(
                coreDataFlow,
                userPreferencesRepository.categoriesEnabled,
                _pendingUndoTasks,
                _selectedCategoryId
            ) { coreData, categoriesEnabled, pendingMap, selectedCategoryId ->

                val pendingIds = pendingMap.keys

                // Filter tasks: exclude done + pending deletions, then filter by category if enabled
                val filteredTasks = coreData.tasks
                    .filter { !it.isDone && it.id !in pendingIds }
                    .let { filtered ->
                        if (categoriesEnabled && selectedCategoryId != null) {
                            filtered.filter { it.categoryId == selectedCategoryId }
                        } else filtered
                    }

                // Filter ideas by category if enabled
                val filteredIdeas = coreData.ideas.let { list ->
                    if (categoriesEnabled && selectedCategoryId != null) {
                        list.filter { it.categoryId == selectedCategoryId }
                    } else list
                }

                // Filter checklists by category if enabled
                val filteredLists = coreData.lists.let { list ->
                    if (categoriesEnabled && selectedCategoryId != null) {
                        list.filter { it.categoryId == selectedCategoryId }
                    } else list
                }

                // When categories are enabled but no category is selected, select null (= General/unassigned)
                val effectiveSelectedId = if (categoriesEnabled) selectedCategoryId else null

                FlowUiState.Success(
                    tasks = filteredTasks,
                    ideas = filteredIdeas,
                    checkLists = filteredLists,
                    categories = coreData.categories,
                    selectedCategoryId = effectiveSelectedId,
                    categoriesEnabled = categoriesEnabled
                )
            }.collect { state ->
                _uiState.value = state
                refreshBanner()
            }
        }
    }

    // ── Category actions ──────────────────────────────────────────────────────

    /**
     * Creates a new user category from the FlowScreen "+ New category" tab.
     *
     * Mirrors the logic in [com.markel.flowstate.feature.settings.CategoriesViewModel.createCategory]:
     * the new category is appended after the existing ones (position = max + 1)
     * and is NOT selected automatically — the user can tap it once it appears.
     *
     * "General" is a reserved virtual tab (null id) and is rejected here.
     */
    fun createCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || trimmed.equals("General", ignoreCase = true)) return

        viewModelScope.launch {
            val currentList = (_uiState.value as? FlowUiState.Success)?.categories
                ?: categoryRepository.getCategories().first()
            val maxPosition = currentList.maxOfOrNull { it.position } ?: -1
            categoryRepository.upsertCategory(
                Category(name = trimmed, position = maxPosition + 1)
            )
        }
    }

    fun selectCategory(id: Int?) {
        _selectedCategoryId.value = id
    }

    /**
     * Persists a new order for the user categories. Mirrors the logic in
     * settings:
     * the categories list is taken as the source of truth and each item is
     * re-positioned by index.
     *
     * Called from the FlowScreen "Reorder categories" sheet.
     */
    fun reorderCategories(categories: List<Category>) {
        viewModelScope.launch {
            val reordered = categories.mapIndexed { index, category ->
                category.copy(position = index)
            }
            categoryRepository.updateCategoriesOrder(reordered)
        }
    }

    // ── Deferred deletion API ─────────────────────────────────────────────────

    /**
     * Called when the user swipes a task to dismiss it.
     *
     * The task is **not** deleted from the DB immediately. Instead, it is added
     * to [_pendingUndoTasks] which causes the `combine` above to filter it out
     * of the UI state optimistically.  A per-task countdown of 3500ms
     * starts; if it expires the task is permanently deleted.  The user may cancel
     * the countdown by pressing the undo FAB ([undoPendingDeletions]).
     */
    fun onTaskSwiped(task: Task) {
        // Add to the pending map – the combine will filter it out of uiState
        _pendingUndoTasks.update { it + (task.id to task) }

        // Bump the delete version so the LazyColumn key changes.
        // This prevents rememberSaveable from restoring the old
        // SwipeToDismissBoxState (EndToStart) when the task is
        // un-filtered after an undo.
        _taskDeleteVersions.update { it + (task.id to (it[task.id] ?: 0) + 1) }

        // Cancel any previous countdown for this same task (defensive)
        deleteJobs[task.id]?.cancel()

        // Start the countdown to permanent deletion
        deleteJobs[task.id] = applicationScope.launch {
            delay(3500L)
            // Time's up – permanently delete from DB
            reminderScheduler.cancel(task.id)
            deleteTaskUseCase(task)
            // Clean up pending map; task is already gone from DB at this point
            _pendingUndoTasks.update { it - task.id }
            deleteJobs.remove(task.id)
        }
    }

    /**
     * Called when the user taps the undo FAB.
     *
     * Cancels **all** pending deletion countdowns and restores every task
     * that was waiting to be deleted.  The tasks are still in the DB, so
     * simply removing them from [_pendingUndoTasks] makes them reappear in
     * the UI state automatically.
     */
    fun undoPendingDeletions() {
        // Cancel every pending countdown
        deleteJobs.values.forEach { it.cancel() }
        deleteJobs.clear()
        // Clear the pending map – tasks reappear in uiState via the combine
        _pendingUndoTasks.update { emptyMap() }
    }

    // ── Lifecycle: called every time the app comes to the foreground ──────────

    override fun onResume(owner: LifecycleOwner) {
        val permissionNowGranted = reminderScheduler.canScheduleExactAlarms()

        if (wasPermissionMissing && permissionNowGranted) {
            // Permission was just granted — reschedule everything that was pending
            viewModelScope.launch {
                val items = buildAlarmItems(taskRepository)
                reminderScheduler.rescheduleAll(items)

            }
        }
        refreshBanner()
    }

    // ── Reorder ────────────────────────────────────────────────────────

    fun onTaskReorder(fromIndex: Int, toIndex: Int) {
        val current = (_uiState.value as? FlowUiState.Success)?.tasks?.toMutableList() ?: return

        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)

        val updated = current.mapIndexed { i, t -> t.copy(position = i) }

        _uiState.value = (_uiState.value as FlowUiState.Success).copy(tasks = updated)

        viewModelScope.launch {
            taskRepository.updateTasksOrder(updated)
        }
    }

    fun onIdeaReorder(fromIndex: Int, toIndex: Int) {
        val currentState = _uiState.value as? FlowUiState.Success ?: return
        val list = currentState.ideas.toMutableList()

        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)

        val updated = list.mapIndexed { i, idea -> idea.copy(position = i) }

        _uiState.value = currentState.copy(ideas = updated)

        viewModelScope.launch {
            ideaRepository.updateIdeasOrder(updated)
        }
    }

    fun onCheckListReorder(fromIndex: Int, toIndex: Int) {
        val currentState = _uiState.value as? FlowUiState.Success ?: return
        val list = currentState.checkLists.toMutableList()

        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)

        val updated = list.mapIndexed { i, cl -> cl.copy(position = i) }

        _uiState.value = currentState.copy(checkLists = updated)

        viewModelScope.launch {
            checkListRepository.updateCheckListsOrder(updated)
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun refreshBanner() {
        val canSchedule = reminderScheduler.canScheduleExactAlarms()
        wasPermissionMissing = !canSchedule

        viewModelScope.launch {
            val tasks = (uiState.value as? FlowUiState.Success)?.tasks
                ?: return@launch  // still loading — init block will call refreshBanner again via first DB emit

            val now = System.currentTimeMillis()
            val hasAnyFutureReminder = tasks.any { task ->
                (task.reminderTime?.let { it > now } == true) ||
                        task.subTasks.any { sub -> sub.reminderTime?.let { it > now } == true }
            }
            _bannerVisible.value = hasAnyFutureReminder && !canSchedule
        }
    }
}

data class CoreData(
    val tasks: List<Task>,
    val ideas: List<Idea>,
    val lists: List<CheckList>,
    val categories: List<Category>
)