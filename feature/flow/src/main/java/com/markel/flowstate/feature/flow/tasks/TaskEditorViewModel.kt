package com.markel.flowstate.feature.flow.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.SubTask
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.ToggleTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel exclusive for TaskEditorScreen.
 *
 * Separating it from TaskViewModel (which lives in FlowScreen/Tasks) prevents crashes:
 *
 * Possible crash because TaskViewModel was obtained with hiltViewModel() in both FlowScreen
 * and TaskEditorScreen. By navigating quickly, the editor's backstack entry was destroyed
 * while FlowScreen was still trying to access the same ViewModel, causing conflicts.
 *
 * With a dedicated ViewModel per screen, each lives and dies with its NavBackStackEntry and solves that problem.
 */
data class TaskEditorState(
    val task: Task? = null,
    val priority: Priority = Priority.NOTHING,
    val dueDate: Long? = null,
    val isDone: Boolean = false
)
@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val toggleTaskUseCase: ToggleTaskUseCase,
) : ViewModel() {

    private val _editor = MutableStateFlow(TaskEditorState())
    val editor: StateFlow<TaskEditorState> = _editor.asStateFlow()

    /**
     * Loads the task from the repository by ID.
     * Uses .first() to get the current Flow value without staying subscribed.
     */
    fun loadTask(taskId: Int) {
        viewModelScope.launch {
            val task = repository.getTasks()
                .first()
                .firstOrNull { it.id == taskId }

            if (task != null) {
                _editor.value = TaskEditorState(
                    task = task,
                    priority = task.priority,
                    dueDate = task.dueDate,
                    isDone = task.isDone
                )
            }
        }
    }

    fun updateTask(
        originalTask: Task,
        newTitle: String,
        newDescription: String,
        newPriority: Priority,
        newDueDate: Long?,
        newSubTasks: List<SubTask>
    ) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.upsertTask(
                originalTask.copy(
                    title = newTitle,
                    description = newDescription,
                    priority = newPriority,
                    dueDate = newDueDate,
                    subTasks = newSubTasks
                )
            )
        }
    }

    fun updatePriority(value: Priority) = _editor.update { it.copy(priority = value) }
    fun updateDueDate(value: Long?) = _editor.update { it.copy(dueDate = value) }
    fun toggleDone() {
        val current = _editor.value.task ?: return
        val newIsDone = !_editor.value.isDone
        _editor.update { it.copy(isDone = newIsDone) }
        viewModelScope.launch {
            toggleTaskUseCase(current)
        }
    }
}