package com.markel.flowstate.feature.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.CheckListRepository
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.SubTask
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.DeleteTaskUseCase
import com.markel.flowstate.core.domain.usecase.ToggleTaskUseCase
import com.markel.flowstate.feature.flow.tasks.TaskViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FlowScreen coordinator.
 *
 * Responsibilities:
 *  - Control cross-screen UI state (isGridView)
 *  - Combine Tasks, Ideas, and CheckLists flows for the Grid view
 *  - Delegate task CRUD operations to TaskViewModel
 *  - Directly manage Ideas and CheckLists
 */
@HiltViewModel
class FlowViewModel @Inject constructor(
    // Repositories required for the unified grid
    private val taskRepository: TaskRepository,
    private val ideaRepository: IdeaRepository,
    private val checkListRepository: CheckListRepository,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    // ── View state ───────────────────────────────────────────────────────

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    fun toggleView() {
        _isGridView.value = !_isGridView.value
    }

    // ── Unified Grid state ─────────────────────────────────────────────

    val flowUiState: StateFlow<FlowUiState> = combine(
        taskRepository.getTasks(),
        ideaRepository.getIdeas(),
        checkListRepository.getLists()
    ) { tasks, ideas, checkLists ->
        val items = buildList {
            // Completed tasks do not appear in the grid either
            addAll(tasks.filter { !it.isDone }.map { WorkspaceItem.TaskItem(it) })
            addAll(ideas.map { WorkspaceItem.IdeaItem(it) })
            addAll(checkLists.map { WorkspaceItem.CheckListItem(it) })
        }
        FlowUiState.Success(items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FlowUiState.Loading
    )

    // ── Delegation to TaskRepository (logic lives in TaskViewModel) ───────────
    // FlowViewModel only needs these operations for the grid cards

    fun deleteTask(task: Task) {
        viewModelScope.launch { deleteTaskUseCase(task) }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch { toggleTaskUseCase(task) }
    }

    // ── Ideas ─────────────────────────────────────────────────────────────────

    fun addIdea(title: String, content: String, color: Long) {
        if (title.isBlank()) return
        viewModelScope.launch {
            ideaRepository.upsertIdea(Idea(title = title, content = content, color = color))
        }
    }

    fun deleteIdea(idea: Idea) {
        viewModelScope.launch { ideaRepository.deleteIdea(idea) }
    }

    // ── CheckLists ────────────────────────────────────────────────────────────
    // Add CheckList operations here when you implement its sheet
}