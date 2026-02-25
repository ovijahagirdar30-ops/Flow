package com.markel.flowstate.feature.flow.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.SubTask
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.DeleteTaskUseCase
import com.markel.flowstate.core.domain.usecase.ToggleTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Define possible screen states
sealed interface TasksUiState {
    data object Loading : TasksUiState
    data class Success(val tasks: List<Task>) : TasksUiState
}

// Draft state for task creation
data class TaskDraftState(
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.NOTHING,
    val dueDate: Long? = null
)

/**
 * ViewModel for the Tasks screen.
 * Contains business logic and exposes state to the UI.
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TasksUiState>(TasksUiState.Loading)

    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val _draft = MutableStateFlow(TaskDraftState())
    val draft: StateFlow<TaskDraftState> = _draft.asStateFlow()

    // The init block acts as a "subscriber" to the repository
    init {
        viewModelScope.launch {
            repository.getTasks().collect { list ->
                val filteredList = list.filter { !it.isDone }
                _uiState.value = TasksUiState.Success(filteredList)
            }
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────
    fun addTask(title: String, description: String, priority: Priority, dueDate: Long?, subTasks: List<SubTask>) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val currentTasks = (uiState.value as? TasksUiState.Success)?.tasks ?: emptyList()
            val minPosition = currentTasks.minOfOrNull { it.position } ?: 0

            val newTask = Task(
                title = title,
                description = description,
                isDone = false,
                position = minPosition - 1,
                priority = priority,
                dueDate = dueDate,
                subTasks = subTasks
            )
            repository.upsertTask(newTask)
        }
    }

    // Function to delete a task
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            deleteTaskUseCase(task)
        }
    }

    // Function to toggle task completion status
    fun toggleTaskDone(task: Task) {
        viewModelScope.launch { toggleTaskUseCase(task) }
    }

    fun onReorder(fromIndex: Int, toIndex: Int) {
        val currentList = (uiState.value as? TasksUiState.Success)?.tasks?.toMutableList() ?: return

        // 1. Apply movement in the temporary list
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)

        // 2. Recalculate positions (indices 0, 1, 2...)
        val updatedList = currentList.mapIndexed { index, task ->
            task.copy(position = index)
        }

        // 3. Update the interface NOW
        _uiState.value = TasksUiState.Success(updatedList)

        // 4. Save to database in the background
        viewModelScope.launch {
            repository.updateTasksOrder(updatedList)
        }
    }

    // TASK CREATION
    fun updateDraftTitle(value: String) { _draft.update { it.copy(title = value) } }
    fun updateDraftDescription(value: String) { _draft.update { it.copy(description = value) } }
    fun updateDraftPriority(value: Priority) { _draft.update { it.copy(priority = value) } }
    fun updateDraftDueDate(value: Long?) { _draft.update { it.copy(dueDate = value) } }

    fun submitDraft() {
        val d = _draft.value
        addTask(d.title, d.description, d.priority, d.dueDate, emptyList())
        _draft.value = TaskDraftState() // reset
    }
}