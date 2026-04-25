package com.markel.flowstate.feature.flow

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.CheckListRepository
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.notifications.ReminderScheduler
import com.markel.flowstate.core.notifications.buildAlarmItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val userPreferencesRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler
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

    init {
        viewModelScope.launch {
            combine(
                taskRepository.getTasks(),
                ideaRepository.getIdeas(),
                checkListRepository.getLists()
            ) { tasks, ideas, lists ->
                FlowUiState.Success(
                    tasks = tasks.filter { !it.isDone },
                    ideas = ideas,
                    checkLists = lists
                )
            }.collect { state ->
                _uiState.value = state
                refreshBanner()
            }
        }
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