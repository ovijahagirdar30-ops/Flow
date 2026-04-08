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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    // ── Optimistic local state ─────────────────────────────────
    private val _uiState = MutableStateFlow<FlowUiState>(FlowUiState.Loading)
    val uiState: StateFlow<FlowUiState> = _uiState

    // ── Unified sectioned UiState ─────────────────────────────────────────────
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
            }.collect {
                _uiState.value = it
            }
        }
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

}