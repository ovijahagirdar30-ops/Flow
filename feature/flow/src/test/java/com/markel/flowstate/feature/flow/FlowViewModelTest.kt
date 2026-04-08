package com.markel.flowstate.feature.flow

import app.cash.turbine.test
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.CheckListRepository
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FlowViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val ideaRepository: IdeaRepository = mockk(relaxed = true)
    private val checkListRepository: CheckListRepository = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)

    private lateinit var viewModel: FlowViewModel

    // ── Initialization & Combine logic ────────────────────────────────────────

    @Test
    fun uiState_combinesAllRepositories_andFiltersCompletedTasks() = runTest {
        // GIVEN
        val mockTasks = listOf(
            Task(id = 1, title = "Pending Task", isDone = false, position = 0),
            Task(id = 2, title = "Done Task", isDone = true, position = 1)
        )
        val mockIdeas = listOf(Idea(id = 1, title = "Idea 1", position = 0, content = "Content", color = 0L))
        val mockLists = listOf(CheckList(id = 1, title = "List 1", position = 0, color = 0L))

        coEvery { taskRepository.getTasks() } returns flowOf(mockTasks)
        coEvery { ideaRepository.getIdeas() } returns flowOf(mockIdeas)
        coEvery { checkListRepository.getLists() } returns flowOf(mockLists)

        // WHEN
        viewModel = FlowViewModel(
            taskRepository, ideaRepository, checkListRepository, userPreferencesRepository
        )

        // THEN
        viewModel.uiState.test {
            // Skip initial Loading state if it appears
            val state = awaitItem()
            val successState = if (state is FlowUiState.Loading) awaitItem() as FlowUiState.Success else state as FlowUiState.Success

            // Verify tasks are filtered
            assertEquals(1, successState.tasks.size)
            assertEquals("Pending Task", successState.tasks[0].title)

            // Verify ideas and lists are received correctly
            assertEquals(1, successState.ideas.size)
            assertEquals(1, successState.checkLists.size)
        }
    }

    // ── Reorder Logic ─────────────────────────────────────────────────────────

    @Test
    fun onTaskReorder_updatesStateOptimistically_andCallsRepository() = runTest {
        // GIVEN
        val t1 = Task(id = 1, title = "T1", isDone = false, position = 0)
        val t2 = Task(id = 2, title = "T2", isDone = false, position = 1)
        val t3 = Task(id = 3, title = "T3", isDone = false, position = 2)

        coEvery { taskRepository.getTasks() } returns flowOf(listOf(t1, t2, t3))
        coEvery { ideaRepository.getIdeas() } returns flowOf(emptyList())
        coEvery { checkListRepository.getLists() } returns flowOf(emptyList())

        viewModel = FlowViewModel(taskRepository, ideaRepository, checkListRepository, userPreferencesRepository)

        viewModel.uiState.test {
            val initialState = awaitItem()
            val success = if (initialState is FlowUiState.Loading) awaitItem() as FlowUiState.Success else initialState as FlowUiState.Success
            assertEquals(3, success.tasks.size)

            // WHEN - Move the first task (index 0) to the end (index 2) -> Expected: T2, T3, T1
            viewModel.onTaskReorder(fromIndex = 0, toIndex = 2)

            // THEN - Local state is updated
            val updatedState = awaitItem() as FlowUiState.Success
            assertEquals("T2", updatedState.tasks[0].title)
            assertEquals("T3", updatedState.tasks[1].title)
            assertEquals("T1", updatedState.tasks[2].title)

            // Positions were recalculated correctly (0, 1, 2)
            assertEquals(0, updatedState.tasks[0].position)
            assertEquals(1, updatedState.tasks[1].position)
            assertEquals(2, updatedState.tasks[2].position)
        }

        // AND - Repository is called
        coVerify {
            taskRepository.updateTasksOrder(match { list ->
                list[0].id == 2 && list[0].position == 0 &&
                        list[2].id == 1 && list[2].position == 2
            })
        }
    }

    @Test
    fun onIdeaReorder_updatesStateOptimistically_andCallsRepository() = runTest {
        // GIVEN
        val i1 = Idea(id = 1, title = "I1", position = 0, content = "Content", color = 0L)
        val i2 = Idea(id = 2, title = "I2", position = 1, content = "Content", color = 0L)

        coEvery { taskRepository.getTasks() } returns flowOf(emptyList())
        coEvery { ideaRepository.getIdeas() } returns flowOf(listOf(i1, i2))
        coEvery { checkListRepository.getLists() } returns flowOf(emptyList())

        viewModel = FlowViewModel(taskRepository, ideaRepository, checkListRepository, userPreferencesRepository)

        viewModel.uiState.test {
            val initialState = awaitItem()
            if (initialState is FlowUiState.Loading) awaitItem()

            // WHEN - Swap I1 and I2
            viewModel.onIdeaReorder(fromIndex = 0, toIndex = 1)

            // THEN
            val updatedState = awaitItem() as FlowUiState.Success
            assertEquals("I2", updatedState.ideas[0].title)
            assertEquals("I1", updatedState.ideas[1].title)
        }

        coVerify {
            ideaRepository.updateIdeasOrder(match { list ->
                list[0].id == 2 && list[1].id == 1
            })
        }
    }

    @Test
    fun onCheckListReorder_updatesStateOptimistically_andCallsRepository() = runTest {
        // GIVEN
        val c1 = CheckList(id = 1, title = "C1", position = 0, color = 0L)
        val c2 = CheckList(id = 2, title = "C2", position = 1, color = 0L)

        coEvery { taskRepository.getTasks() } returns flowOf(emptyList())
        coEvery { ideaRepository.getIdeas() } returns flowOf(emptyList())
        coEvery { checkListRepository.getLists() } returns flowOf(listOf(c1, c2))

        viewModel = FlowViewModel(taskRepository, ideaRepository, checkListRepository, userPreferencesRepository)

        viewModel.uiState.test {
            val initialState = awaitItem()
            if (initialState is FlowUiState.Loading) awaitItem()

            // WHEN
            viewModel.onCheckListReorder(fromIndex = 1, toIndex = 0)

            // THEN
            val updatedState = awaitItem() as FlowUiState.Success
            assertEquals("C2", updatedState.checkLists[0].title)
            assertEquals("C1", updatedState.checkLists[1].title)
        }

        coVerify {
            checkListRepository.updateCheckListsOrder(match { list ->
                list[0].id == 2 && list[1].id == 1
            })
        }
    }
}