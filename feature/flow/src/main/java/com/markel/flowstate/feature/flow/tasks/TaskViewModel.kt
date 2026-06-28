package com.markel.flowstate.feature.flow.tasks

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.SubTask
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.tasks.DeleteTaskUseCase
import com.markel.flowstate.core.domain.usecase.tasks.ToggleTaskUseCase
import com.markel.flowstate.core.notifications.ReminderScheduler
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
    val dueDate: Long? = null,
    val reminderTime: Long? = null  // Epoch millis for the reminder alarm. Independent of dueDate.
)

/**
 * ViewModel for the Tasks screen.
 * Contains business logic and exposes state to the UI.
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val reminderScheduler: ReminderScheduler
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
    fun addTask(title: String, description: String, priority: Priority, dueDate: Long?, reminderTime: Long?, subTasks: List<SubTask>, categoryId: Int? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val currentTasks = (uiState.value as? TasksUiState.Success)?.tasks ?: emptyList()
            val sameCategoryTasks = currentTasks.filter { it.categoryId == categoryId }
            val minPosition = sameCategoryTasks.minOfOrNull { it.position } ?: 0
            val effectiveReminderTime = if (reminderTime != null && reminderTime > System.currentTimeMillis()) reminderTime else null

            val newTask = Task(
                title = title,
                description = description,
                isDone = false,
                position = minPosition - 1,
                priority = priority,
                dueDate = dueDate,
                reminderTime = reminderTime,
                subTasks = subTasks,
                categoryId = categoryId
            )
            val generatedId = repository.upsertTask(newTask)

            // Schedule the alarm after the task is persisted.
            if (effectiveReminderTime != null) {
                reminderScheduler.schedule(generatedId.toInt(), title, description, effectiveReminderTime)
            }
        }
    }

    // Function to toggle task completion status
    fun toggleTaskDone(task: Task) {
        viewModelScope.launch {
            val completing = !task.isDone
            toggleTaskUseCase(task)
            if (completing) {
                reminderScheduler.cancel(task.id)
                task.subTasks.forEach { reminderScheduler.cancelSubTask(it.id) }
            }
        }
    }

    // TASK CREATION
    fun updateDraftTitle(value: String) { _draft.update { it.copy(title = value) } }
    fun updateDraftDescription(value: String) { _draft.update { it.copy(description = value) } }
    fun updateDraftPriority(value: Priority) { _draft.update { it.copy(priority = value) } }
    fun updateDraftDueDate(value: Long?) { _draft.update { it.copy(dueDate = value) } }
    fun updateDraftReminderTime(value: Long?) { _draft.update { it.copy(reminderTime = value) } }

    fun submitDraft(categoryId: Int? = null) {
        val d = _draft.value
        addTask(d.title, d.description, d.priority, d.dueDate, d.reminderTime, emptyList(), categoryId)
        _draft.value = TaskDraftState() // reset
    }
}