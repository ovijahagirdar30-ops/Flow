package com.markel.flowstate.feature.checkin

import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.CheckinRepository
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.EveningPlanRepository
import com.markel.flowstate.core.domain.EveningPlanner
import com.markel.flowstate.core.domain.PlanBlock
import com.markel.flowstate.core.domain.PlanBlockKind
import com.markel.flowstate.core.domain.SubTask
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.checkin.BuildCheckinSnapshotUseCase
import com.markel.flowstate.core.domain.usecase.checkin.GetCheckinItemsUseCase
import com.markel.flowstate.core.domain.usecase.tasks.AddTaskUseCase
import com.markel.flowstate.core.domain.usecase.tasks.DeleteTaskUseCase
import com.markel.flowstate.core.notifications.ReminderScheduler
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Pins the check-in step's plan editing (the path behind the Plan tab's FAB
 * "Edit plan"): moving a block onto an occupied time is rejected — the dialog
 * stays open, nothing is recorded, and Agree still persists the ORIGINAL
 * shape — while free moves and inserts go through. Overlap detection itself
 * lives in [PlanBlockEdits] (covered by PlanBlockEditsTest); these tests pin
 * the ViewModel wiring around it.
 */
class CheckinViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCheckinItems: GetCheckinItemsUseCase = mockk(relaxed = true)
    private val checkinRepository: CheckinRepository = mockk(relaxed = true)
    private val addTaskUseCase: AddTaskUseCase = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val deleteTaskUseCase: DeleteTaskUseCase = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val buildCheckinSnapshot: BuildCheckinSnapshotUseCase = mockk(relaxed = true)
    private val eveningPlanner: EveningPlanner = mockk(relaxed = true)
    private val eveningPlanRepository: EveningPlanRepository = mockk(relaxed = true)
    private val userPreferences: UserPreferencesRepository = mockk(relaxed = true)

    private val today: String = LocalDate.now().toString()

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun block(
        time: String,
        duration: Int = 30,
        title: String = "Block at $time",
    ) = PlanBlock(
        startTime = time,
        durationMinutes = duration,
        title = title,
        reason = "",
        kind = PlanBlockKind.OTHER,
    )

    private fun plan(vararg blocks: PlanBlock) = EveningPlan(
        date = today,
        generatedAtMillis = 0L,
        headline = "Headline",
        blocks = blocks.toList(),
    )

    private fun buildViewModel(): CheckinViewModel {
        every { userPreferences.endOfDayMinutes } returns flowOf(0)
        every { getCheckinItems() } returns flowOf(emptyList())
        return CheckinViewModel(
            getCheckinItems,
            checkinRepository,
            addTaskUseCase,
            taskRepository,
            deleteTaskUseCase,
            reminderScheduler,
            buildCheckinSnapshot,
            eveningPlanner,
            eveningPlanRepository,
            userPreferences,
        )
    }

    /** Loads the agreed plan into the PLAN step exactly like the FAB does. */
    private suspend fun loadForEditing(vm: CheckinViewModel, p: EveningPlan) {
        coEvery { eveningPlanRepository.latestAgreedPlan() } returns p
        coEvery { eveningPlanRepository.checkedIndexes(any()) } returns listOf(0)
        vm.loadAgreedPlanForEditing()
    }

    // ── Overlap rejection on the edit path ──────────────────────────────

    @Test
    fun editBlock_rejectsOverlap_andAgreePersistsTheOriginalShape() = runTest {
        val vm = buildViewModel()
        loadForEditing(vm, plan(block("19:00"), block("20:00")))

        // 20:15–20:45 collides with the 20:00–20:30 block.
        assertFalse(vm.editBlock(0, block("20:15")))

        // No memory note for a rejected edit…
        coVerify(exactly = 0) { eveningPlanRepository.recordFeedback(any(), any()) }

        // …and Agree saves the plan exactly as it was loaded.
        vm.agreeToPlan()
        coVerify {
            eveningPlanRepository.saveAgreedPlan(
                match { p -> p.blocks.map { it.startTime } == listOf("19:00", "20:00") }
            )
        }
        vm.viewModelScope.cancel()
    }

    @Test
    fun editBlock_appliesFreeMove_andRecordsTheNote() = runTest {
        val vm = buildViewModel()
        loadForEditing(vm, plan(block("19:00"), block("20:00")))

        assertTrue(vm.editBlock(0, block("21:00")))
        coVerify { eveningPlanRepository.recordFeedback(today, match { it.contains("Changed") }) }

        vm.agreeToPlan()
        coVerify {
            eveningPlanRepository.saveAgreedPlan(
                match { p -> p.blocks.map { it.startTime } == listOf("20:00", "21:00") }
            )
        }
        vm.viewModelScope.cancel()
    }

    @Test
    fun addBlock_rejectsOverlap_andAcceptsFreeTime() = runTest {
        val vm = buildViewModel()
        loadForEditing(vm, plan(block("19:00")))

        assertFalse(vm.addBlock(block("19:15"))) // collides with 19:00–19:30
        assertTrue(vm.addBlock(block("21:00")))  // free
        coVerify { eveningPlanRepository.recordFeedback(today, match { it.contains("Added") }) }

        vm.agreeToPlan()
        coVerify {
            eveningPlanRepository.saveAgreedPlan(
                match { p -> p.blocks.map { it.startTime } == listOf("19:00", "21:00") }
            )
        }
        vm.viewModelScope.cancel()
    }

    // ── Tasks-for-tonight step navigation ─────────────────────────────

    @Test
    fun recapHandsOffToTasksStep_andThePlannerStartsThere() = runTest {
        val vm = buildViewModel()
        val steps = mutableListOf<CheckinStep>()
        val collect = launch {
            vm.uiState.collect { s -> (s as? CheckinUiState.InProgress)?.let { steps.add(it.step) } }
        }
        advanceUntilIdle()
        assertEquals(CheckinStep.GREET, steps.lastOrNull())

        repeat(6) { vm.goToNextStep() } // GREET → … → RECAP
        advanceUntilIdle()
        assertEquals(CheckinStep.RECAP, steps.lastOrNull())

        // "Generate my evening": hands to TASKS and kicks off the planner,
        // which runs while the user picks tonight's tasks.
        vm.goToNextStep()
        advanceUntilIdle()
        assertEquals(CheckinStep.TASKS, steps.lastOrNull())
        coVerify { eveningPlanner.generatePlan(any(), any()) }

        // "Show my plan": hands to the plan stage.
        vm.goToNextStep()
        advanceUntilIdle()
        assertEquals(CheckinStep.PLAN, steps.lastOrNull())

        // Back is disabled at PLAN — the step holds until Agree/discard.
        vm.goToPreviousStep()
        advanceUntilIdle()
        assertEquals(CheckinStep.PLAN, steps.lastOrNull())

        collect.cancel()
        vm.viewModelScope.cancel()
    }

    @Test
    fun tasksStep_backReturnsToTheRecap() = runTest {
        val vm = buildViewModel()
        val steps = mutableListOf<CheckinStep>()
        val collect = launch {
            vm.uiState.collect { s -> (s as? CheckinUiState.InProgress)?.let { steps.add(it.step) } }
        }
        repeat(7) { vm.goToNextStep() } // → TASKS
        advanceUntilIdle()
        assertEquals(CheckinStep.TASKS, steps.lastOrNull())

        vm.goToPreviousStep() // system back on the tasks step
        advanceUntilIdle()
        assertEquals(CheckinStep.RECAP, steps.lastOrNull())

        collect.cancel()
        vm.viewModelScope.cancel()
    }

    // ── Tasks step: add / remove ───────────────────────────────────────

    @Test
    fun addTask_delegatesToTheSharedUseCase() = runTest {
        val vm = buildViewModel()

        vm.addTask("Water the plants")
        advanceUntilIdle()

        coVerify { addTaskUseCase("Water the plants") }
        vm.viewModelScope.cancel()
    }

    @Test
    fun removeTask_cancelsEveryAlarm_thenDeletesTheTask() = runTest {
        val vm = buildViewModel()
        val task = Task(
            id = 7,
            title = "Maths tutorial",
            isDone = false,
            subTasks = listOf(SubTask(id = "s1", title = "One"), SubTask(id = "s2", title = "Two")),
        )
        coEvery { taskRepository.getTaskById(7) } returns task

        vm.removeTask(7)
        advanceUntilIdle()

        coVerify { reminderScheduler.cancel(7) }
        coVerify { reminderScheduler.cancelSubTask("s1") }
        coVerify { reminderScheduler.cancelSubTask("s2") }
        coVerify { deleteTaskUseCase(task) }
        vm.viewModelScope.cancel()
    }

    @Test
    fun removeTask_withAStaleIdDoesNothing() = runTest {
        val vm = buildViewModel()
        coEvery { taskRepository.getTaskById(404) } returns null

        vm.removeTask(404)
        advanceUntilIdle()

        coVerify(exactly = 0) { reminderScheduler.cancel(any()) }
        coVerify(exactly = 0) { deleteTaskUseCase(any()) }
        vm.viewModelScope.cancel()
    }
}
