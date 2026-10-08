package com.markel.flowstate.feature.checkin

import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.EveningPlanRepository
import com.markel.flowstate.core.domain.PlanBlock
import com.markel.flowstate.core.domain.PlanBlockKind
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.tasks.ToggleTaskUseCase
import com.markel.flowstate.core.notifications.ReminderScheduler
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pins the Plan tab's two guard rails:
 *
 *  1. Overlap rejection — moving a block onto an occupied time (or adding a
 *     block into one) returns false so the dialog stays open, leaving the
 *     persisted plan and ticks untouched. A time-UNCHANGED edit is always
 *     accepted, even when the generated plan already overlaps.
 *
 *  2. Due-gated ticking — a checkbox only accepts a tick once the block's
 *     scheduled start + duration has elapsed; tapping early surfaces the
 *     transient overlay message instead of ticking, and unticking is always
 *     allowed.
 *
 * The ViewModel owns an infinite one-minute expiry ticker, so every test
 * cancels [viewModelScope] as its last statement — otherwise runTest's final
 * scheduler drain would chase the ticker forever.
 */
class PlanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val planRepository: EveningPlanRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val toggleTaskUseCase: ToggleTaskUseCase = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val userPreferences: UserPreferencesRepository = mockk(relaxed = true)

    private lateinit var viewModel: PlanViewModel

    private val today: String = LocalDate.now().toString()

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun block(
        time: String,
        duration: Int = 30,
        title: String = "Block at $time",
        kind: PlanBlockKind = PlanBlockKind.OTHER,
    ) = PlanBlock(
        startTime = time,
        durationMinutes = duration,
        title = title,
        reason = "",
        kind = kind,
    )

    private fun plan(vararg blocks: PlanBlock) = EveningPlan(
        date = today,
        generatedAtMillis = 0L,
        headline = "Headline",
        blocks = blocks.toList(),
    )

    /** Epoch millis for a device-local wall time, same rule as PlanExpiryTest. */
    private fun millisAt(date: String, hour: Int, minute: Int): Long =
        LocalDateTime.parse("${date}T${LocalTime.of(hour, minute)}")
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun buildViewModel(plan: EveningPlan?, ticks: Set<Int> = emptySet()) {
        coEvery { planRepository.latestAgreedPlan() } returns plan
        coEvery { planRepository.checkedIndexes(any()) } returns ticks.toList()
        every { userPreferences.endOfDayMinutes } returns flowOf(0)
        viewModel = PlanViewModel(
            planRepository,
            taskRepository,
            toggleTaskUseCase,
            reminderScheduler,
            userPreferences,
        )
    }

    /** Ends the ViewModel's infinite expiry ticker (see class KDoc). */
    private fun done() {
        viewModel.viewModelScope.cancel()
    }

    // ── Overlap rejection ───────────────────────────────────────────────

    @Test
    fun editBlock_rejectsMoveOntoOccupiedTime_andLeavesPlanUntouched() = runTest {
        buildViewModel(plan(block("19:00"), block("20:00")))

        // 20:15–20:45 collides with the 20:00–20:30 block.
        assertFalse(viewModel.editBlock(0, block("20:15")))

        val blocks = viewModel.uiState.value.plan?.blocks
        assertEquals(listOf("19:00", "20:00"), blocks?.map { it.startTime })
        coVerify(exactly = 0) { planRepository.saveAgreedPlan(any()) }
        coVerify(exactly = 0) { planRepository.recordFeedback(any(), any()) }
        done()
    }

    @Test
    fun editBlock_appliesMoveToFreeTime_andPersists() = runTest {
        buildViewModel(plan(block("19:00"), block("20:00")))

        assertTrue(viewModel.editBlock(0, block("21:00")))

        // Re-sorted: the moved block lands last.
        assertEquals(
            listOf("20:00", "21:00"),
            viewModel.uiState.value.plan?.blocks?.map { it.startTime },
        )
        coVerify { planRepository.saveAgreedPlan(any()) }
        coVerify { planRepository.setCheckedIndexes(today, emptyList()) }
        coVerify { planRepository.recordFeedback(today, match { it.contains("Changed") }) }
        done()
    }

    @Test
    fun editBlock_timelessChangeIsAcceptedEvenWhenPlanAlreadyOverlaps() = runTest {
        // The AI itself produced an overlapping plan (19:00–20:00 vs 19:30–20:00).
        buildViewModel(plan(block("19:00", duration = 60), block("19:30")))

        // Renaming without touching the time must not be treated as a new conflict.
        assertTrue(viewModel.editBlock(0, block("19:00", duration = 60, title = "Renamed")))

        assertEquals("Renamed", viewModel.uiState.value.plan?.blocks?.get(0)?.title)
        coVerify { planRepository.saveAgreedPlan(any()) }
        done()
    }

    @Test
    fun addBlock_rejectsOccupiedTime() = runTest {
        buildViewModel(plan(block("19:00")))

        assertFalse(viewModel.addBlock(block("19:15")))

        assertEquals(1, viewModel.uiState.value.plan?.blocks?.size)
        coVerify(exactly = 0) { planRepository.saveAgreedPlan(any()) }
        done()
    }

    @Test
    fun addBlock_insertsIntoFreeTime() = runTest {
        buildViewModel(plan(block("19:00")))

        assertTrue(viewModel.addBlock(block("21:00")))

        assertEquals(
            listOf("19:00", "21:00"),
            viewModel.uiState.value.plan?.blocks?.map { it.startTime },
        )
        coVerify { planRepository.saveAgreedPlan(any()) }
        coVerify { planRepository.recordFeedback(today, match { it.contains("Added") }) }
        done()
    }

    // ── Due-gated ticking ───────────────────────────────────────────────

    @Test
    fun toggleBlock_beforeDue_showsMessageInsteadOfTicking() = runTest {
        // 23:59 + 60 min only comes due after midnight — never during today.
        buildViewModel(plan(block("23:59", duration = 60)))

        viewModel.toggleBlock(0, millisAt(today, 12, 0))

        val state = viewModel.uiState.value
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("Not due yet"))
        assertTrue(state.checkedIndexes.isEmpty())
        coVerify(exactly = 0) { planRepository.setCheckedIndexes(any(), any()) }
        done()
    }

    @Test
    fun toggleBlock_onceDue_ticksItAndPersists() = runTest {
        buildViewModel(plan(block("19:00", duration = 30)))

        // 19:30 = exactly when the 19:00–19:30 block's duration elapses.
        viewModel.toggleBlock(0, millisAt(today, 19, 30))

        val state = viewModel.uiState.value
        assertEquals(setOf(0), state.checkedIndexes)
        assertNull(state.message)
        coVerify { planRepository.setCheckedIndexes(today, listOf(0)) }
        done()
    }

    @Test
    fun toggleBlock_unticksEvenBeforeTheBlockIsDue() = runTest {
        buildViewModel(plan(block("23:59", duration = 60)), ticks = setOf(0))

        viewModel.toggleBlock(0, millisAt(today, 12, 0))

        val state = viewModel.uiState.value
        assertTrue(state.checkedIndexes.isEmpty())
        assertNull(state.message) // unticking never warns
        coVerify { planRepository.setCheckedIndexes(today, emptyList()) }
        done()
    }

    @Test
    fun toggleMessage_clearsItselfAfterItsWindow() = runTest {
        buildViewModel(plan(block("23:59", duration = 60)))

        viewModel.toggleBlock(0, millisAt(today, 12, 0))
        assertNotNull(viewModel.uiState.value.message)

        // Past the 2.5s auto-clear (bounded advance — the expiry ticker at
        // 60s must not be reached, so no advanceUntilIdle here).
        advanceTimeBy(2_600)
        assertNull(viewModel.uiState.value.message)
        done()
    }

    // ── Drag-reorder (plan step's edit mode) ──────────────────────────

    @Test
    fun moveBlock_swapsSlots_persistsAndSkipsTheMemoryNote() = runTest {
        buildViewModel(plan(block("19:00"), block("19:30")), ticks = setOf(1))

        assertTrue(viewModel.moveBlock(0, 1))

        // Slot times stay put; B (was ticked at 1) moves first, still ticked.
        val state = viewModel.uiState.value
        assertEquals(listOf("19:00", "19:30"), state.plan?.blocks?.map { it.startTime })
        assertEquals(
            listOf("Block at 19:30", "Block at 19:00"),
            state.plan?.blocks?.map { it.title },
        )
        assertEquals(setOf(0), state.checkedIndexes)

        coVerify { planRepository.saveAgreedPlan(any()) }
        coVerify { planRepository.setCheckedIndexes(today, listOf(0)) }
        // A drag records no AI-memory note — one gesture would write one per
        // crossing and drown the real corrections.
        coVerify(exactly = 0) { planRepository.recordFeedback(any(), any()) }
        done()
    }

    @Test
    fun moveBlock_rejectsDoubleBooking_andShowsTheOverlayMessage() = runTest {
        // A runs 60 min; moving it into B's 30-min slot would swallow C.
        buildViewModel(
            plan(
                block("19:00", duration = 60),
                block("20:00", duration = 30),
                block("20:30", duration = 30),
            )
        )

        assertFalse(viewModel.moveBlock(0, 1))

        val state = viewModel.uiState.value
        assertEquals(listOf("19:00", "20:00", "20:30"), state.plan?.blocks?.map { it.startTime })
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("scheduled during that time"))
        coVerify(exactly = 0) { planRepository.saveAgreedPlan(any()) }
        done()
    }

    @Test
    fun moveBlock_isIgnoredOnceThePlanExpires() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toString()
        buildViewModel(
            EveningPlan(
                date = yesterday,
                generatedAtMillis = 0L,
                headline = "Headline",
                blocks = listOf(block("19:00"), block("19:30")),
            )
        )

        assertFalse(viewModel.moveBlock(0, 1))
        assertEquals(
            listOf("19:00", "19:30"),
            viewModel.uiState.value.plan?.blocks?.map { it.startTime },
        )
        coVerify(exactly = 0) { planRepository.saveAgreedPlan(any()) }
        done()
    }
}
