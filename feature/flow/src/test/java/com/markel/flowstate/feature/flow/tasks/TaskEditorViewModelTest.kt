package com.markel.flowstate.feature.flow.tasks

import app.cash.turbine.test
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.tasks.DeleteTaskUseCase
import com.markel.flowstate.core.domain.usecase.tasks.ToggleTaskUseCase
import com.markel.flowstate.core.notifications.ReminderScheduler
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TaskEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: TaskRepository = mockk(relaxed = true)
    private val toggleTaskUseCase: ToggleTaskUseCase = mockk(relaxed = true)
    private val deleteTaskUseCase: DeleteTaskUseCase = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private lateinit var viewModel: TaskEditorViewModel

    @Test
    fun initialState_isEmpty() = runTest {
        // GIVEN / WHEN - A fresh ViewModel with no loadTask call yet
        viewModel = TaskEditorViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        // THEN - editor state should be empty
        viewModel.editor.test {
            val state = awaitItem()
            assertNull(state.task)
            assertEquals(Priority.NOTHING, state.priority)
            assertNull(state.dueDate)
        }
    }

    @Test
    fun loadTask_whenTaskExists_populatesEditorState() = runTest {
        // GIVEN - A task in the repository
        val task = Task(id = 42, title = "Buy milk", isDone = false, priority = Priority.HIGH, dueDate = 1000L)
        every { repository.getTasks() } returns flowOf(listOf(task))

        viewModel = TaskEditorViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        // WHEN
        viewModel.loadTask(taskId = 42)

        // THEN - Editor state should reflect the task
        viewModel.editor.test {
            val state = awaitItem()
            assertEquals(task, state.task)
            assertEquals(Priority.HIGH, state.priority)
            assertEquals(1000L, state.dueDate)
            assertEquals(false, state.isDone)
        }
    }

    @Test
    fun loadTask_whenTaskDoesNotExist_keepsEmptyState() = runTest {
        // GIVEN - Repository returns a list that does NOT contain the requested ID
        every { repository.getTasks() } returns flowOf(emptyList())

        viewModel = TaskEditorViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        // WHEN
        viewModel.loadTask(taskId = 99)

        // THEN - State should remain null (no crash, no phantom data)
        viewModel.editor.test {
            val state = awaitItem()
            assertNull(state.task)
        }
    }

    @Test
    fun updatePriority_updatesStateCorrectly() = runTest {
        // GIVEN
        viewModel = TaskEditorViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        // WHEN
        viewModel.updatePriority(Priority.MEDIUM)

        // THEN
        viewModel.editor.test {
            assertEquals(Priority.MEDIUM, awaitItem().priority)
        }
    }

    @Test
    fun updateDueDate_updatesStateCorrectly() = runTest {
        // GIVEN
        viewModel = TaskEditorViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        // WHEN
        viewModel.updateDueDate(9999L)

        // THEN
        viewModel.editor.test {
            assertEquals(9999L, awaitItem().dueDate)
        }
    }

    @Test
    fun updateTask_callsRepositoryWithUpdatedData() = runTest {
        // GIVEN - A task loaded in the editor
        val original = Task(id = 5, title = "Old title", isDone = false, priority = Priority.NOTHING)
        every { repository.getTasks() } returns flowOf(listOf(original))
        viewModel = TaskEditorViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)
        viewModel.loadTask(5)

        // WHEN - User edits the task
        viewModel.updateTask(
            originalTask = original,
            newTitle = "New title",
            newDescription = "New desc",
            newPriority = Priority.HIGH,
            newDueDate = 5000L,
            newReminderTime = null,
            newSubTasks = emptyList()
        )

        // THEN - Repository should receive the updated copy
        coVerify {
            repository.upsertTask(match { task ->
                task.id == 5 &&
                        task.title == "New title" &&
                        task.description == "New desc" &&
                        task.priority == Priority.HIGH &&
                        task.dueDate == 5000L
            })
        }
    }

    @Test
    fun updateTask_withBlankTitle_doesNotCallRepository() = runTest {
        // GIVEN
        val original = Task(id = 1, title = "Original", isDone = false)
        every { repository.getTasks() } returns flowOf(listOf(original))
        viewModel = TaskEditorViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        // WHEN - Trying to save with a blank title
        viewModel.updateTask(original, "   ", "", Priority.NOTHING, null, null, emptyList())

        // THEN - Repository must NOT be called
        coVerify(exactly = 0) { repository.upsertTask(any()) }
    }

    @Test
    fun toggleDone_callsToggleUseCaseAndUpdatesState() = runTest {
        // GIVEN
        val task = Task(id = 1, title = "Task", isDone = false)
        every { repository.getTasks() } returns flowOf(listOf(task))
        viewModel = TaskEditorViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)
        viewModel.loadTask(1)

        // WHEN
        viewModel.toggleDone()

        // THEN - State updates optimistically
        viewModel.editor.test {
            assertEquals(true, awaitItem().isDone)
        }
        // AND - Use case is called
        coVerify { toggleTaskUseCase(task) }
    }

    @Test
    fun deleteTask_cancelsAlarmsAndCallsDeleteUseCase() = runTest {
        // GIVEN
        val task = Task(id = 3, title = "Task to delete", isDone = false)
        every { repository.getTasks() } returns flowOf(listOf(task))
        viewModel = TaskEditorViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)
        viewModel.loadTask(3)

        // WHEN
        viewModel.deleteTask(task)

        // THEN
        coVerify { reminderScheduler.cancel(3) }
        coVerify { deleteTaskUseCase(task) }
    }
}