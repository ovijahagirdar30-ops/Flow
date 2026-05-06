package com.markel.flowstate.feature.flow.tasks

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
    val reminderTime: Long? = null,
    val isDone: Boolean = false
)
@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val reminderScheduler: ReminderScheduler
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
                    reminderTime = task.reminderTime,
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
        newReminderTime: Long?,
        newSubTasks: List<SubTask>
    ) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val updatedTask = originalTask.copy(
                title = newTitle,
                description = newDescription,
                priority = newPriority,
                dueDate = newDueDate,
                reminderTime = newReminderTime,
                subTasks = newSubTasks
            )
            repository.upsertTask(updatedTask)
            reconcileSubTaskAlarms(original = originalTask, updated = updatedTask)
        }
    }

    fun updatePriority(value: Priority) = _editor.update { it.copy(priority = value) }
    fun updateDueDate(value: Long?) = _editor.update { it.copy(dueDate = value) }

    fun updateReminderTime(value: Long?) {
        val task = _editor.value.task ?: return
        _editor.update { it.copy(reminderTime = value) }

        viewModelScope.launch {
            // Cancel the old alarm regardless of whether we're setting a new one.
            reminderScheduler.cancel(task.id)

            val updated = task.copy(reminderTime = value)
            repository.upsertTask(updated)

            if (value != null && value > System.currentTimeMillis()) {
                reminderScheduler.schedule(task.id, task.title, task.description, value)
            }
        }
    }

    fun toggleDone() {
        val current = _editor.value.task ?: return
        val newIsDone = !_editor.value.isDone
        _editor.update { it.copy(isDone = newIsDone) }
        viewModelScope.launch {
            toggleTaskUseCase(current)
            if (newIsDone) {
                reminderScheduler.cancel(current.id)
                current.subTasks.filter { it.reminderTime != null }.forEach { subTask ->
                    reminderScheduler.cancelSubTask(subTask.id)
                }
            }
        }
    }
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            reminderScheduler.cancel(task.id)
            task.subTasks.forEach { reminderScheduler.cancelSubTask(it.id) }  // Cancel all subtask alarms
            deleteTaskUseCase(task)
        }
    }


    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Diffs old vs new subtask list to cancel removed alarms and schedule new ones.
     * Only touches alarms for subtasks whose reminderTime actually changed.
     */
    private fun reconcileSubTaskAlarms(original: Task, updated: Task) {
        val now = System.currentTimeMillis()
        val oldMap = original.subTasks.associateBy { it.id }
        val newMap = updated.subTasks.associateBy { it.id }

        // Canceled or removed subtasks
        oldMap.forEach { (id, old) ->
            if (old.reminderTime != null && newMap[id]?.reminderTime != old.reminderTime) {
                reminderScheduler.cancelSubTask(id)
            }
        }

        // New or updated reminders
        newMap.forEach { (id, new) ->
            val oldReminder = oldMap[id]?.reminderTime
            val newReminder = new.reminderTime
            if (newReminder != null &&
                newReminder != oldReminder &&
                newReminder > now
            ) {
                reminderScheduler.scheduleSubTask(id, new.title, newReminder)
            }
        }
    }

}