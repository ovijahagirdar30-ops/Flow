package com.markel.flowstate.feature.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.CheckListRepository
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // ── Optimistic local state per section ─────────────────────────────────
    // null means "use the DB-driven value"; non-null means "drag is in progress"
    private val _localTasks = MutableStateFlow<List<Task>?>(null)
    private val _localIdeas = MutableStateFlow<List<Idea>?>(null)
    private val _localCheckLists = MutableStateFlow<List<CheckList>?>(null)

    // ── Unified sectioned UiState ─────────────────────────────────────────────

    private val domainDataFlow = combine(
        taskRepository.getTasks(),
        ideaRepository.getIdeas(),
        checkListRepository.getLists()
    ) { tasks: List<Task>, ideas: List<Idea>, lists: List<CheckList> ->
        Triple(tasks, ideas, lists)
    }

    private val localDataFlow = combine(
        _localTasks,
        _localIdeas,
        _localCheckLists
    ) { lTasks: List<Task>?, lIdeas: List<Idea>?, lLists: List<CheckList>? ->
        Triple(lTasks, lIdeas, lLists)
    }

    val flowUiState: StateFlow<FlowUiState> = combine(
        domainDataFlow,
        localDataFlow
    ) { (tasks, ideas, lists), (lTasks, lIdeas, lLists) ->
        FlowUiState.Success(
            tasks = lTasks ?: tasks.filter { !it.isDone },
            ideas = lIdeas ?: ideas,
            checkLists = lLists ?: lists
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FlowUiState.Loading
    )

    // ── Reorder ────────────────────────────────────────────────────────

    fun onTaskReorder(fromIndex: Int, toIndex: Int) {
        val current = _localTasks.value
            ?: (flowUiState.value as? FlowUiState.Success)?.tasks
            ?: return
        _localTasks.value = current.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    fun onTaskDragEnd() {
        val final = _localTasks.value?.mapIndexed { i, t -> t.copy(position = i) } ?: return
        viewModelScope.launch {
            taskRepository.updateTasksOrder(final)
            _localTasks.value = null
        }
    }

    fun onIdeaReorder(fromIndex: Int, toIndex: Int) {
        val current = _localIdeas.value
            ?: (flowUiState.value as? FlowUiState.Success)?.ideas
            ?: return
        _localIdeas.value = current.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    fun onIdeaDragEnd() {
        val final = _localIdeas.value?.mapIndexed { i, idea -> idea.copy(position = i) } ?: return
        viewModelScope.launch {
            ideaRepository.updateIdeasOrder(final)
            _localIdeas.value = null
        }
    }

    fun onCheckListReorder(fromIndex: Int, toIndex: Int) {
        val current = _localCheckLists.value
            ?: (flowUiState.value as? FlowUiState.Success)?.checkLists
            ?: return
        _localCheckLists.value = current.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    fun onCheckListDragEnd() {
        val final = _localCheckLists.value?.mapIndexed { i, cl -> cl.copy(position = i) } ?: return
        viewModelScope.launch {
            checkListRepository.updateCheckListsOrder(final)
            _localCheckLists.value = null
        }
    }

}