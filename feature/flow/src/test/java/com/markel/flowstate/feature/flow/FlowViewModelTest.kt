package com.markel.flowstate.feature.flow

import app.cash.turbine.test
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.CheckListRepository
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.core.domain.SubTask
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.tasks.DeleteTaskUseCase
import com.markel.flowstate.core.notifications.ReminderScheduler
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FlowViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val ideaRepository: IdeaRepository = mockk(relaxed = true)
    private val checkListRepository: CheckListRepository = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val deleteTaskUseCase: DeleteTaskUseCase = mockk(relaxed = true)

    private lateinit var viewModel: FlowViewModel

    private fun createViewModel(
        tasks: List<Task> = emptyList(),
        ideas: List<Idea> = emptyList(),
        checkLists: List<CheckList> = emptyList()
    ): FlowViewModel {
        coEvery { taskRepository.getTasks() } returns flowOf(tasks)
        coEvery { ideaRepository.getIdeas() } returns flowOf(ideas)
        coEvery { checkListRepository.getLists() } returns flowOf(checkLists)
        return FlowViewModel(
            taskRepository, ideaRepository, checkListRepository,
            userPreferencesRepository, reminderScheduler, deleteTaskUseCase
        )
    }

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

        // WHEN
        viewModel = createViewModel(tasks = mockTasks, ideas = mockIdeas, checkLists = mockLists)

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

    // ── Deferred Deletion / Undo ──────────────────────────────────────────────

    @Test
    fun onTaskSwiped_filtersTaskFromUiState_andShowsUndoButton() = runTest {
        // GIVEN
        val t1 = Task(id = 1, title = "T1", isDone = false, position = 0)
        val t2 = Task(id = 2, title = "T2", isDone = false, position = 1)
        viewModel = createViewModel(tasks = listOf(t1, t2))

        // Wait for initial state
        viewModel.uiState.test {
            val initial = awaitItem()
            val success = if (initial is FlowUiState.Loading) awaitItem() as FlowUiState.Success else initial as FlowUiState.Success
            assertEquals(2, success.tasks.size)

            // WHEN
            viewModel.onTaskSwiped(t1)

            // THEN – task 1 is filtered out optimistically
            val afterSwipe = awaitItem() as FlowUiState.Success
            assertEquals(1, afterSwipe.tasks.size)
            assertEquals("T2", afterSwipe.tasks[0].title)
        }

        // AND undo button is visible
        viewModel.showUndoButton.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun onTaskSwiped_bumpsDeleteVersion() = runTest {
        // GIVEN
        val task = Task(id = 5, title = "Task", isDone = false, position = 0)
        viewModel = createViewModel(tasks = listOf(task))

        // Initially no versions
        assertEquals(emptyMap<Int, Int>(), viewModel.taskDeleteVersions.value)

        // WHEN
        viewModel.onTaskSwiped(task)

        // THEN
        assertEquals(mapOf(5 to 1), viewModel.taskDeleteVersions.value)

        // Swipe the same task again (if it reappears after undo)
        viewModel.undoPendingDeletions()
        viewModel.onTaskSwiped(task)

        assertEquals(mapOf(5 to 2), viewModel.taskDeleteVersions.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onTaskSwiped_deletesFromDb_afterUndoTimeout() = runTest {
        // GIVEN
        val task = Task(id = 1, title = "T", isDone = false, position = 0)
        viewModel = createViewModel(tasks = listOf(task))

        // WHEN
        viewModel.onTaskSwiped(task)

        // Advance time past the undo timeout (3.5 s)
        advanceTimeBy(4_000L)

        // THEN – task should be deleted from DB
        coVerify { reminderScheduler.cancel(1) }
        coVerify { deleteTaskUseCase(task) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun undoPendingDeletions_cancelsDeleteAndRestoresTasks() = runTest {
        // GIVEN
        val t1 = Task(id = 1, title = "T1", isDone = false, position = 0)
        val t2 = Task(id = 2, title = "T2", isDone = false, position = 1)
        viewModel = createViewModel(tasks = listOf(t1, t2))

        viewModel.uiState.test {
            // Get initial state
            val initial = awaitItem()
            val startState = if (initial is FlowUiState.Loading) awaitItem() as FlowUiState.Success else initial as FlowUiState.Success
            assertEquals(2, startState.tasks.size)

            // Swipe both tasks
            viewModel.onTaskSwiped(t1)
            awaitItem() // intermediate state after t1 swiped

            viewModel.onTaskSwiped(t2)
            val afterSwipes = awaitItem() as FlowUiState.Success
            assertTrue(afterSwipes.tasks.isEmpty())
        }

        // WHEN – undo
        viewModel.undoPendingDeletions()

        // THEN – tasks reappear in uiState
        viewModel.uiState.test {
            val state = awaitItem()
            val success = if (state is FlowUiState.Loading) awaitItem() as FlowUiState.Success else state as FlowUiState.Success
            assertEquals(2, success.tasks.size)
        }

        // AND undo button is hidden
        viewModel.showUndoButton.test {
            assertFalse(awaitItem())
        }

        // AND advancing time does NOT delete (jobs were canceled)
        advanceTimeBy(4_000L)
        coVerify(exactly = 0) { deleteTaskUseCase(any()) }
    }

    @Test
    fun onTaskSwiped_supportsConsecutiveDeletions() = runTest {
        // GIVEN
        val t1 = Task(id = 1, title = "T1", isDone = false, position = 0)
        val t2 = Task(id = 2, title = "T2", isDone = false, position = 1)
        val t3 = Task(id = 3, title = "T3", isDone = false, position = 2)
        viewModel = createViewModel(tasks = listOf(t1, t2, t3))

        viewModel.uiState.test {
            val initial = awaitItem()
            val success = if (initial is FlowUiState.Loading) awaitItem() as FlowUiState.Success else initial as FlowUiState.Success
            assertEquals(3, success.tasks.size)

            // WHEN – swipe 3 tasks in quick succession
            viewModel.onTaskSwiped(t1)
            val after1 = awaitItem() as FlowUiState.Success
            assertEquals(2, after1.tasks.size)

            viewModel.onTaskSwiped(t2)
            val after2 = awaitItem() as FlowUiState.Success
            assertEquals(1, after2.tasks.size)

            viewModel.onTaskSwiped(t3)
            val afterAll = awaitItem() as FlowUiState.Success

            // THEN – all tasks filtered out
            assertTrue(afterAll.tasks.isEmpty())
        }

        // Undo button visible
        viewModel.showUndoButton.test {
            assertTrue(awaitItem())
        }

        // Undo all
        viewModel.undoPendingDeletions()

        // All 3 tasks come back
        viewModel.uiState.test {
            val state = awaitItem()
            val success = if (state is FlowUiState.Loading) awaitItem() as FlowUiState.Success else state as FlowUiState.Success
            assertEquals(3, success.tasks.size)
        }
    }

    // ── Reorder Logic ─────────────────────────────────────────────────────────

    @Test
    fun onTaskReorder_updatesStateOptimistically_andCallsRepository() = runTest {
        // GIVEN
        val t1 = Task(id = 1, title = "T1", isDone = false, position = 0)
        val t2 = Task(id = 2, title = "T2", isDone = false, position = 1)
        val t3 = Task(id = 3, title = "T3", isDone = false, position = 2)

        viewModel = createViewModel(tasks = listOf(t1, t2, t3))

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

        viewModel = createViewModel(ideas = listOf(i1, i2))

        viewModel.uiState.test {
            val initialState = awaitItem()
            val startState = if (initialState is FlowUiState.Loading) awaitItem() as FlowUiState.Success else initialState as FlowUiState.Success

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

        viewModel = createViewModel(checkLists = listOf(c1, c2))

        viewModel.uiState.test {
            val initialState = awaitItem()
            val startState = if (initialState is FlowUiState.Loading) awaitItem() as FlowUiState.Success else initialState as FlowUiState.Success

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


    // ── Banner logic ──────────────────────────────────────────────────────────

    @Test
    fun banner_is_hidden_when_permission_is_granted() = runTest {
        coEvery { reminderScheduler.canScheduleExactAlarms() } returns true
        val futureTime = System.currentTimeMillis() + 60_000L
        val task = Task(id = 1, title = "T", isDone = false, reminderTime = futureTime)

        viewModel = createViewModel(tasks = listOf(task))

        viewModel.showReminderBanner.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun banner_is_shown_when_permission_missing_and_tasks_have_future_reminders() = runTest {
        coEvery { reminderScheduler.canScheduleExactAlarms() } returns false
        val futureTime = System.currentTimeMillis() + 60_000L
        val task = Task(id = 1, title = "T", isDone = false, reminderTime = futureTime)

        viewModel = createViewModel(tasks = listOf(task))

        viewModel.showReminderBanner.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun banner_is_hidden_when_permission_missing_but_no_future_reminders() = runTest {
        coEvery { reminderScheduler.canScheduleExactAlarms() } returns false
        val task = Task(id = 1, title = "T", isDone = false, reminderTime = null)

        viewModel = createViewModel(tasks = listOf(task))

        viewModel.showReminderBanner.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun banner_is_shown_when_subtask_has_future_reminder_and_permission_missing() = runTest {
        coEvery { reminderScheduler.canScheduleExactAlarms() } returns false
        val futureTime = System.currentTimeMillis() + 60_000L
        val subTask = SubTask(id = "s1", title = "Sub", reminderTime = futureTime)
        val task = Task(id = 1, title = "T", isDone = false, subTasks = listOf(subTask))

        viewModel = createViewModel(tasks = listOf(task))

        viewModel.showReminderBanner.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun banner_is_hidden_when_only_past_reminders_exist_and_permission_missing() = runTest {
        coEvery { reminderScheduler.canScheduleExactAlarms() } returns false
        val pastTime = System.currentTimeMillis() - 60_000L
        val task = Task(id = 1, title = "T", isDone = false, reminderTime = pastTime)

        viewModel = createViewModel(tasks = listOf(task))

        viewModel.showReminderBanner.test {
            assertFalse(awaitItem())
        }
    }

    // ── onResume: permission grant triggers reschedule ───────────────────────

    @Test
    fun onResume_reschedules_alarms_when_permission_was_just_granted() = runTest {
        // First call: permission missing → sets wasPermissionMissing = true
        // Second call: permission granted → triggers reschedule
        coEvery { reminderScheduler.canScheduleExactAlarms() } returns false andThen true

        val futureTime = System.currentTimeMillis() + 60_000L
        val task = Task(id = 1, title = "T", isDone = false, reminderTime = futureTime)

        viewModel = createViewModel(tasks = listOf(task))

        // First onResume: sets wasPermissionMissing = true (permission still missing)
        viewModel.onResume(mockk())

        // Second onResume: permission now granted → should reschedule
        viewModel.onResume(mockk())

        // Verify rescheduleAll was called
        coVerify { reminderScheduler.rescheduleAll(any()) }
    }

    @Test
    fun onResume_does_not_reschedule_when_permission_was_already_granted() = runTest {
        coEvery { reminderScheduler.canScheduleExactAlarms() } returns true

        viewModel = createViewModel()

        viewModel.onResume(mockk())
        viewModel.onResume(mockk())

        // rescheduleAll should never be called since permission was never missing
        coVerify(exactly = 0) { reminderScheduler.rescheduleAll(any()) }
    }

    @Test
    fun onResume_does_not_reschedule_when_permission_is_still_missing() = runTest {
        coEvery { reminderScheduler.canScheduleExactAlarms() } returns false

        viewModel = createViewModel()

        viewModel.onResume(mockk())
        viewModel.onResume(mockk())

        // Permission never granted, so no reschedule
        coVerify(exactly = 0) { reminderScheduler.rescheduleAll(any()) }
    }
}