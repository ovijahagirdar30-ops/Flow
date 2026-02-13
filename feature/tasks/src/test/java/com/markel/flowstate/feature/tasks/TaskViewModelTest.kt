package com.markel.flowstate.feature.tasks

import app.cash.turbine.test
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
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

    private lateinit var viewModel: TaskViewModel

    @Test
    fun uiState_initiallyLoading() = runTest {
        // GIVEN (Given that the repo doesn't return anything yet or takes time)
        // Simulate an empty flow so it doesn't crash during init
        coEvery { repository.getTasks() } returns flowOf(emptyList())

        // WHEN (When we initialize the ViewModel)
        viewModel = TaskViewModel(repository)

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
        viewModel = TaskViewModel(repository)

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
        viewModel = TaskViewModel(repository)

        val title = "New Task"
        val desc = "Description"
        val priority = Priority.HIGH

        // WHEN
        viewModel.addTask(title, desc, priority, null, emptyList())

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
    fun toggleTaskDone_updatesCompletedAtWhenTaskIsDone() = runTest {
        // GIVEN
        coEvery { repository.getTasks() } returns flowOf(emptyList())
        viewModel = TaskViewModel(repository)

        val task = Task(id = 1, title = "Demo", isDone = false)

        // WHEN
        viewModel.toggleTaskDone(task)

        // THEN
        coVerify {
            repository.upsertTask(match { updatedTask ->
                updatedTask.isDone && updatedTask.completedAt != null // Verify that the date was set
            })
        }
    }

    @Test
    fun onReorder_updatesLocalState_and_callsRepositoryUpdate() = runTest {
        // GIVEN - An initial list of unordered tasks or with positions
        val task1 = Task(id = 1, title = "T1", position = 0, isDone = false)
        val task2 = Task(id = 2, title = "T2", position = 1, isDone = false)
        val task3 = Task(id = 3, title = "T3", position = 2, isDone = false)
        val initialList = listOf(task1, task2, task3)

        coEvery { repository.getTasks() } returns flowOf(initialList)
        viewModel = TaskViewModel(repository)

        // Wait for the ViewModel to load the initial state
        viewModel.uiState.test {
            // Skip Loading and the first Success
            val successState = awaitItem() as TasksUiState.Success
            assertEquals(3, successState.tasks.size)

            // WHEN - Move Task 1 (index 0) to the end (index 2)
            // Expected list: T2, T3, T1
            viewModel.onReorder(fromIndex = 0, toIndex = 2)

            // THEN 1 - The UI state should update immediately
            val reorderedState = awaitItem() as TasksUiState.Success
            val reorderedList = reorderedState.tasks

            assertEquals("T2", reorderedList[0].title)
            assertEquals("T3", reorderedList[1].title)
            assertEquals("T1", reorderedList[2].title)

            // Verify that internal positions were updated (0, 1, 2)
            assertEquals(0, reorderedList[0].position)
            assertEquals(1, reorderedList[1].position)
            assertEquals(2, reorderedList[2].position)
        }

        // THEN 2 - The repository should be called to persist the change
        coVerify {
            repository.updateTasksOrder(match { list ->
                list[0].id == 2 && list[0].position == 0 &&
                        list[2].id == 1 && list[2].position == 2
            })
        }
    }
}