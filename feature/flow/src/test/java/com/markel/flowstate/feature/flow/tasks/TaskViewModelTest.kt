package com.markel.flowstate.feature.flow.tasks

import app.cash.turbine.test
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.SubTask
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.tasks.DeleteTaskUseCase
import com.markel.flowstate.core.domain.usecase.tasks.ToggleTaskUseCase
import com.markel.flowstate.core.notifications.ReminderScheduler
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TaskViewModelTest {

    // 1. Apply the rule for coroutines
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // 2. Create a mock of the repository.
    // relaxed = true means that if we call something undefined, it won't fail (returns null/void).
    private val repository: TaskRepository = mockk(relaxed = true)
    private val toggleTaskUseCase: ToggleTaskUseCase = mockk(relaxed = true)
    private val deleteTaskUseCase: DeleteTaskUseCase = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)

    private lateinit var viewModel: TaskViewModel

    @Test
    fun uiState_initiallyLoading() = runTest {
        // GIVEN (Given that the repo doesn't return anything yet or takes time)
        // Simulate an empty flow so it doesn't crash during init
        coEvery { repository.getTasks() } returns flowOf(emptyList())

        // WHEN (When we initialize the ViewModel)
        viewModel = TaskViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        // THEN (Then the first state should be Loading or a quick empty Success)
        // Note: Since we use UnconfinedTestDispatcher, init runs very quickly.
        // Verify the data flow:
        viewModel.uiState.test {
            val firstState = awaitItem()
            // Depending on the speed, we might catch Loading or directly Success.
            if (firstState is TasksUiState.Loading) {
                val secondState = awaitItem()
                assertTrue(secondState is TasksUiState.Success)
            } else {
                assertTrue(firstState is TasksUiState.Success)
            }
        }
    }

    @Test
    fun uiState_whenRepositoryEmitsTasks_updatesToSuccess() = runTest {
        // GIVEN - Prepare fake data
        val mockTasks = listOf(
            Task(id = 1, title = "Test Task 1", isDone = false),
            Task(id = 2, title = "Test Task 2", isDone = true) // This one is done
        )
        // Simulate that the repo returns that data
        coEvery { repository.getTasks() } returns flowOf(mockTasks)

        // WHEN - Initialize
        viewModel = TaskViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        // THEN - Verify that it filters out completed tasks (according to the logic in init, it filters tasks to only work with incomplete ones)
        viewModel.uiState.test {
            // Skip the initial state if it's Loading
            val state = awaitItem()
            val successState = if (state is TasksUiState.Loading) awaitItem() else state

            assertTrue(successState is TasksUiState.Success)
            val tasks = (successState as TasksUiState.Success).tasks

            assertEquals(1, tasks.size)
            assertEquals("Test Task 1", tasks[0].title)
        }
    }

    @Test
    fun addTask_callsRepositoryUpsertWithCorrectData() = runTest {
        // GIVEN
        coEvery { repository.getTasks() } returns flowOf(emptyList())
        viewModel = TaskViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        val title = "New Task"
        val desc = "Description"
        val priority = Priority.HIGH

        // WHEN
        viewModel.addTask(title, desc, priority, null, null, emptyList())

        // THEN
        // Verify that upsertTask was called in the repository
        // "coVerify" is for suspend functions
        coVerify {
            repository.upsertTask(match { task ->
                task.title == title &&
                        task.description == desc &&
                        task.priority == Priority.HIGH
            })
        }
    }

    @Test
    fun toggleTaskDone_callsToggleUseCase_withCorrectTask() = runTest {
        coEvery { repository.getTasks() } returns flowOf(emptyList())
        viewModel = TaskViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        val task = Task(id = 1, title = "Demo", isDone = false)

        viewModel.toggleTaskDone(task)

        coVerify { toggleTaskUseCase(task) }
    }

    @Test
    fun deleteTask_callsDeleteUseCase_withCorrectTask() = runTest {
        coEvery { repository.getTasks() } returns flowOf(emptyList())
        viewModel = TaskViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        val task = Task(id = 1, title = "Demo", isDone = false)

        viewModel.deleteTask(task)

        coVerify { deleteTaskUseCase(task) }
    }


    // ── Reminder scheduling tests ─────────────────────────────────────────────

    @Test
    fun addTask_with_future_reminderTime_schedules_the_alarm() = runTest {
        coEvery { repository.getTasks() } returns flowOf(emptyList())
        coEvery { repository.upsertTask(any()) } returns 42L
        viewModel = TaskViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        val futureTime = System.currentTimeMillis() + 60_000L

        viewModel.addTask("Remind me", "Desc", Priority.HIGH, null, futureTime, emptyList())

        coVerify { reminderScheduler.schedule(42, "Remind me", "Desc", futureTime) }
    }

    @Test
    fun addTask_with_past_reminderTime_does_not_schedule_the_alarm() = runTest {
        coEvery { repository.getTasks() } returns flowOf(emptyList())
        coEvery { repository.upsertTask(any()) } returns 1L
        viewModel = TaskViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        val pastTime = System.currentTimeMillis() - 60_000L

        viewModel.addTask("Old reminder", "Desc", Priority.NOTHING, null, pastTime, emptyList())

        coVerify(exactly = 0) { reminderScheduler.schedule(any(), any(), any(), any()) }
    }

    @Test
    fun addTask_with_null_reminderTime_does_not_schedule_the_alarm() = runTest {
        coEvery { repository.getTasks() } returns flowOf(emptyList())
        viewModel = TaskViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        viewModel.addTask("No reminder", "Desc", Priority.NOTHING, null, null, emptyList())

        coVerify(exactly = 0) { reminderScheduler.schedule(any(), any(), any(), any()) }
    }

    @Test
    fun toggleTaskDone_completing_cancels_task_alarm_and_all_subtask_alarms() = runTest {
        coEvery { repository.getTasks() } returns flowOf(emptyList())
        viewModel = TaskViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        val sub1 = SubTask(id = "s1", title = "Sub1")
        val sub2 = SubTask(id = "s2", title = "Sub2")
        val task = Task(id = 10, title = "With subs", isDone = false, subTasks = listOf(sub1, sub2))

        viewModel.toggleTaskDone(task)

        coVerify { reminderScheduler.cancel(10) }
        coVerify { reminderScheduler.cancelSubTask("s1") }
        coVerify { reminderScheduler.cancelSubTask("s2") }
    }

    @Test
    fun toggleTaskDone_uncompleting_does_not_cancel_alarms() = runTest {
        coEvery { repository.getTasks() } returns flowOf(emptyList())
        viewModel = TaskViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        val task = Task(id = 10, title = "Done task", isDone = true)

        viewModel.toggleTaskDone(task)

        // When uncompleting, !task.isDone = false → completing = false → no cancel
        coVerify(exactly = 0) { reminderScheduler.cancel(any()) }
        coVerify(exactly = 0) { reminderScheduler.cancelSubTask(any()) }
    }

    @Test
    fun deleteTask_cancels_the_task_alarm() = runTest {
        coEvery { repository.getTasks() } returns flowOf(emptyList())
        viewModel = TaskViewModel(repository, toggleTaskUseCase, deleteTaskUseCase, reminderScheduler)

        val task = Task(id = 7, title = "Delete me", isDone = false)

        viewModel.deleteTask(task)

        coVerify { reminderScheduler.cancel(7) }
        coVerify { deleteTaskUseCase(task) }
    }
}