package com.markel.flowstate.feature.habits

import app.cash.turbine.test
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitEntryFlat
import com.markel.flowstate.core.domain.HabitWithStatus
import com.markel.flowstate.core.domain.HabitType
import com.markel.flowstate.core.domain.usecase.habits.DecrementNumericValueUseCase
import com.markel.flowstate.core.domain.usecase.habits.DeleteHabitUseCase
import com.markel.flowstate.core.domain.usecase.habits.DeleteNumericEntryUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetAllBooleanEntriesUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetAllNumericEntriesUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetHabitsWithStatusUseCase
import com.markel.flowstate.core.domain.usecase.habits.IncrementNumericValueUseCase
import com.markel.flowstate.core.domain.usecase.habits.InsertHabitUseCase
import com.markel.flowstate.core.domain.usecase.habits.LogNumericEntryUseCase
import com.markel.flowstate.core.domain.usecase.habits.SetHabitMoodUseCase
import com.markel.flowstate.core.domain.usecase.habits.ToggleHabitEntryUseCase
import com.markel.flowstate.core.domain.usecase.habits.UpdateHabitUseCase
import com.markel.flowstate.core.domain.usecase.habits.UpdateHabitsOrderUseCase
import com.markel.flowstate.core.domain.usecase.habits.UpdateHabitsPriorityOrderUseCase
import com.markel.flowstate.core.notifications.HabitReminderScheduler
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class HabitViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mocks for all UseCases used in the ViewModel
    private val getHabitsWithStatus: GetHabitsWithStatusUseCase = mockk(relaxed = true)
    private val getAllBooleanEntries: GetAllBooleanEntriesUseCase = mockk(relaxed = true)
    private val getAllNumericEntries: GetAllNumericEntriesUseCase = mockk(relaxed = true)
    private val insertHabit: InsertHabitUseCase = mockk(relaxed = true)
    private val updateHabit: UpdateHabitUseCase = mockk(relaxed = true)
    private val deleteHabit: DeleteHabitUseCase = mockk(relaxed = true)
    private val toggleEntry: ToggleHabitEntryUseCase = mockk(relaxed = true)
    private val logNumericEntry: LogNumericEntryUseCase = mockk(relaxed = true)
    private val incrementNumericValue: IncrementNumericValueUseCase = mockk(relaxed = true)
    private val decrementNumericValue: DecrementNumericValueUseCase = mockk(relaxed = true)
    private val deleteNumericEntry: DeleteNumericEntryUseCase = mockk(relaxed = true)
    private val updateHabitsOrder: UpdateHabitsOrderUseCase = mockk(relaxed = true)
    private val setHabitMood: SetHabitMoodUseCase = mockk(relaxed = true)
    private val updateHabitsPriorityOrder: UpdateHabitsPriorityOrderUseCase = mockk(relaxed = true)
    private val habitReminderScheduler: HabitReminderScheduler = mockk(relaxed = true)

    private lateinit var viewModel: HabitViewModel

    @Before
    fun setUp() {
        coEvery { getAllNumericEntries() } returns flowOf(emptyList())  // Avoid blocking tests waiting for flows. Tests that would need new numeric entries would overwrite this
    }
    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun habit(id: Int = 1, name: String = "Test habit") = Habit(
        id = id,
        name = name,
        iconName = "icon",
        colorArgb = 0xFF123456.toInt(),
        habitType = HabitType.BOOLEAN,
        createdAt = LocalDate.now().minusDays(7)
    )

    private fun habitWithStatus(habit: Habit = habit(), completedToday: Boolean = false) =
        HabitWithStatus(habit = habit, isCompletedToday = completedToday)

    private fun entry(habitId: Int, epochDay: Long) =
        HabitEntryFlat(habitId = habitId, epochDay = epochDay)

    private fun buildViewModel() = HabitViewModel(
        getHabitsWithStatus = getHabitsWithStatus,
        getAllBooleanEntries = getAllBooleanEntries,
        getAllNumericEntries = getAllNumericEntries,
        insertHabit = insertHabit,
        updateHabit = updateHabit,
        deleteHabit = deleteHabit,
        toggleEntry = toggleEntry,
        logNumericEntry = logNumericEntry,
        incrementNumericValue = incrementNumericValue,
        decrementNumericValue = decrementNumericValue,
        deleteNumericEntry = deleteNumericEntry,
        updateHabitsOrder = updateHabitsOrder,
        setHabitMood = setHabitMood,
        updateHabitsPriorityOrder = updateHabitsPriorityOrder,
        habitReminderScheduler = habitReminderScheduler
    )

    // ── uiState ───────────────────────────────────────────────────────────────

    @Test
    fun uiState_initialValue_isLoading() = runTest {
        // GIVEN - Flows that never emit (simulate slow loading)
        coEvery { getHabitsWithStatus() } returns flowOf()
        coEvery { getAllBooleanEntries() } returns flowOf()

        // WHEN
        viewModel = buildViewModel()

        // THEN - The first emitted value must be Loading
        viewModel.uiState.test {
            assertTrue(awaitItem() is HabitUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_whenRepositoryEmitsData_transitionsToSuccess() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(listOf(habitWithStatus(habit(id = 1))))
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())

        // WHEN
        viewModel = buildViewModel()

        // THEN
        viewModel.uiState.test {
            val state = awaitItem()
            val successState = if (state is HabitUiState.Loading) awaitItem() else state
            assertTrue(successState is HabitUiState.Success)
            assertEquals(1, (successState as HabitUiState.Success).totalHabits)
        }
    }

    @Test
    fun uiState_withNoEntries_completedTodayIsZero() = runTest {
        // GIVEN - Two habits but no completed entries
        coEvery { getHabitsWithStatus() } returns flowOf(
            listOf(habitWithStatus(habit(id = 1)), habitWithStatus(habit(id = 2)))
        )
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())

        // WHEN
        viewModel = buildViewModel()

        // THEN
        viewModel.uiState.test {
            val successState = awaitItem().let {
                if (it is HabitUiState.Loading) awaitItem() else it
            } as HabitUiState.Success

            assertEquals(0, successState.completedToday)
        }
    }

    @Test
    fun uiState_withTodayEntry_completedTodayCountsCorrectly() = runTest {
        // GIVEN - Habit 1 completed today, habit 2 not
        val today = LocalDate.now().toEpochDay()
        coEvery { getHabitsWithStatus() } returns flowOf(
            listOf(habitWithStatus(habit(id = 1), completedToday = true), habitWithStatus(habit(id = 2)))
        )
        coEvery { getAllBooleanEntries() } returns flowOf(
            listOf(entry(habitId = 1, epochDay = today))
        )

        // WHEN
        viewModel = buildViewModel()

        // THEN
        viewModel.uiState.test {
            val successState = awaitItem().let {
                if (it is HabitUiState.Loading) awaitItem() else it
            } as HabitUiState.Success

            assertEquals(1, successState.completedToday)
        }
    }

    @Test
    fun uiState_entriesGroupedByHabitId_inWeekEntriesByHabit() = runTest {
        // GIVEN - Two entries for habit 1, one for habit 2
        val today = LocalDate.now().toEpochDay()
        val yesterday = today - 1
        coEvery { getHabitsWithStatus() } returns flowOf(
            listOf(habitWithStatus(habit(id = 1)), habitWithStatus(habit(id = 2)))
        )
        coEvery { getAllBooleanEntries() } returns flowOf(
            listOf(
                entry(habitId = 1, epochDay = today),
                entry(habitId = 1, epochDay = yesterday),
                entry(habitId = 2, epochDay = today)
            )
        )

        // WHEN
        viewModel = buildViewModel()

        // THEN
        viewModel.uiState.test {
            val successState = awaitItem().let {
                if (it is HabitUiState.Loading) awaitItem() else it
            } as HabitUiState.Success

            assertEquals(setOf(today, yesterday), successState.weekEntriesByHabit[1])
            assertEquals(setOf(today), successState.weekEntriesByHabit[2])
        }
    }

    // ── showAddDialog / hideAddDialog ─────────────────────────────────────────

    @Test
    fun showAddDialog_setsShowAddDialogToTrue() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()

        // WHEN
        viewModel.showAddDialog()

        // THEN
        viewModel.uiState.test {
            val successState = awaitItem().let {
                if (it is HabitUiState.Loading) awaitItem() else it
            } as HabitUiState.Success

            assertTrue(successState.showAddDialog)
        }
    }

    @Test
    fun hideAddDialog_setsShowAddDialogToFalse() = runTest {
        // GIVEN - Dialog already open
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        viewModel.showAddDialog()

        // WHEN
        viewModel.hideAddDialog()

        // THEN
        viewModel.uiState.test {
            val successState = awaitItem().let {
                if (it is HabitUiState.Loading) awaitItem() else it
            } as HabitUiState.Success

            assertFalse(successState.showAddDialog)
        }
    }

    // ── addHabit ──────────────────────────────────────────────────────────────

    @Test
    fun addHabit_withValidName_callsInsertHabitUseCase() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()

        // WHEN
        viewModel.addHabit("Read", "book_icon", 0xFF0000FF.toInt())

        // THEN
        coVerify {
            insertHabit(match { habit ->
                habit.name == "Read" &&
                        habit.iconName == "book_icon" &&
                        habit.colorArgb == 0xFF0000FF.toInt() &&
                        habit.habitType == HabitType.BOOLEAN
            })
        }
    }

    @Test
    fun addHabit_withBlankName_doesNotCallRepository() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()

        // WHEN
        viewModel.addHabit("   ", "icon", 0)

        // THEN - A blank name must be silently ignored
        coVerify(exactly = 0) { insertHabit(any()) }
    }

    @Test
    fun addHabit_withValidName_closesAddDialog() = runTest {
        // GIVEN - Dialog is open before saving
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        viewModel.showAddDialog()

        // WHEN
        viewModel.addHabit("Meditate", "lotus_icon", 0)

        // THEN - Dialog must be dismissed after a successful add
        viewModel.uiState.test {
            val successState = awaitItem().let {
                if (it is HabitUiState.Loading) awaitItem() else it
            } as HabitUiState.Success

            assertFalse(successState.showAddDialog)
        }
    }

    // ── editHabit ─────────────────────────────────────────────────────────────

    @Test
    fun editHabit_withValidName_callsUpdateHabitUseCase() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        val oldHabit = habit(id = 1, name = "Old Name")

        // WHEN
        viewModel.editHabit(
            habit = oldHabit,
            newName = "New Name",
            newIcon = "new_icon",
            newColorArgb = 0xFF00FF00.toInt(),
            newUnit = "Pages",
            newTargetValue = 20f,
            newStep = 5f
        )

        // THEN
        coVerify {
            updateHabit(match { habit ->
                habit.id == 1 &&
                        habit.name == "New Name" &&
                        habit.iconName == "new_icon" &&
                        habit.colorArgb == 0xFF00FF00.toInt() &&
                        habit.unit == "Pages" &&
                        habit.targetValue == 20f &&
                        habit.step == 5f
            })
        }
    }

    @Test
    fun editHabit_withBlankName_doesNotCallUpdateHabitUseCase() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        val oldHabit = habit(id = 1, name = "Old Name")

        // WHEN
        viewModel.editHabit(
            habit = oldHabit,
            newName = "   ",
            newIcon = "new_icon",
            newColorArgb = 0xFF00FF00.toInt()
        )

        // THEN - A blank name must be silently ignored
        coVerify(exactly = 0) { updateHabit(any()) }
    }

    // ── deleteHabit ───────────────────────────────────────────────────────────

    @Test
    fun deleteHabit_callsDeleteUseCaseWithCorrectHabit() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        val habitToDelete = habit(id = 5, name = "Exercise")

        // WHEN
        viewModel.deleteHabit(habitToDelete)

        // THEN
        coVerify { deleteHabit(habitToDelete) }
    }

    // ── toggleBooleanHabitOnDate ──────────────────────────────────────────────

    @Test
    fun toggleBooleanHabitOnDate_callsToggleEntryUseCase_withCorrectArguments() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        val date = LocalDate.now().minusDays(2)

        // WHEN
        viewModel.toggleBooleanHabitOnDate(habitId = 3, date = date)

        // THEN
        coVerify { toggleEntry(3, date) }
    }

    @Test
    fun toggleBooleanHabitOnDate_withMoodLoggingOff_doesNotQueueMoodPrompt() = runTest {
        // GIVEN - mood logging is OFF by default, so this habit never opted in
        coEvery { getHabitsWithStatus() } returns flowOf(listOf(habitWithStatus(habit(id = 3))))
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()

        // WHEN - completing it
        viewModel.toggleBooleanHabitOnDate(habitId = 3, date = LocalDate.now())

        // THEN - no "how did it feel?" sheet is queued
        viewModel.uiState.test {
            val state = awaitItem().let {
                if (it is HabitUiState.Loading) awaitItem() else it
            } as HabitUiState.Success
            assertNull(state.pendingMoodPrompt)
        }
    }

    @Test
    fun toggleBooleanHabitOnDate_withMoodLoggingOn_queuesMoodPromptForThatHabit() = runTest {
        // GIVEN - the same habit opted in via the add/edit sheet switch
        coEvery { getHabitsWithStatus() } returns flowOf(
            listOf(habitWithStatus(habit(id = 3).copy(moodLoggingEnabled = true)))
        )
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()

        // WHEN - completing it
        viewModel.toggleBooleanHabitOnDate(habitId = 3, date = LocalDate.now())

        // THEN - the prompt carries the habit, so the rating lands on it
        viewModel.uiState.test {
            val state = awaitItem().let {
                if (it is HabitUiState.Loading) awaitItem() else it
            } as HabitUiState.Success
            assertEquals(3, state.pendingMoodPrompt?.habitId)
        }
    }

    // ── Numeric Habits Operations ─────────────────────────────────────────────

    @Test
    fun incrementNumericHabit_callsIncrementNumericValueUseCase() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        val date = LocalDate.now()

        // WHEN
        viewModel.incrementNumericHabit(habitId = 1, date = date, currentValue = 5f, step = 1f)

        // THEN
        coVerify { incrementNumericValue(1, date, 5f, 1f) }
    }

    @Test
    fun decrementNumericHabit_callsDecrementNumericValueUseCase() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        val date = LocalDate.now()

        // WHEN
        viewModel.decrementNumericHabit(habitId = 2, date = date, currentValue = 10f, step = 2f)

        // THEN
        coVerify { decrementNumericValue(2, date, 10f, 2f) }
    }

    @Test
    fun setNumericValue_callsLogNumericEntryUseCase() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        val date = LocalDate.now()

        // WHEN
        viewModel.setNumericValue(habitId = 3, date = date, value = 15.5f)

        // THEN
        coVerify { logNumericEntry(3, date, 15.5f) }
    }

    @Test
    fun deleteNumericEntry_callsDeleteNumericEntryUseCase() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        val date = LocalDate.now()

        // WHEN
        viewModel.deleteNumericEntry(habitId = 4, date = date)

        // THEN
        coVerify { deleteNumericEntry(4, date) }
    }

    // ── priority reorder ──────────────────────────────────────────────────

    private fun priorityHabits() = listOf(
        habitWithStatus(habit(id = 1).copy(priorityRank = 1)),
        habitWithStatus(habit(id = 2).copy(priorityRank = 2)),
        habitWithStatus(habit(id = 3).copy(priorityRank = 3))
    )

    private fun successState() = viewModel.uiState.value as HabitUiState.Success

    private fun priorityOrder(state: HabitUiState.Success) =
        state.habits.sortedBy { it.habit.priorityRank }.map { it.habit.id to it.habit.priorityRank }

    @Test
    fun onPriorityReorder_renumbersRanks_oneBasedFromTheTop() = runTest {
        // GIVEN - A priority list of 1, 2, 3 (1 = top of the list)
        coEvery { getHabitsWithStatus() } returns flowOf(priorityHabits())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()

        // WHEN - Dragging the bottom habit to the top
        viewModel.onPriorityReorder(fromIndex = 2, toIndex = 0)

        // THEN - Stored ranks are 1-based positions following the new order,
        // so they match what the edit sheet's slider shows for each habit
        assertEquals(listOf(3 to 1, 1 to 2, 2 to 3), priorityOrder(successState()))
        coVerify { updateHabitsPriorityOrder(listOf(3 to 1, 1 to 2, 2 to 3)) }
    }

    @Test
    fun setPriorityRank_movesHabitToTypedPosition_andRenumbers() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(priorityHabits())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()

        // WHEN - Typing "1" for the habit currently sitting at position 3
        viewModel.setPriorityRank(habitId = 3, newRank = 1)

        // THEN
        assertEquals(listOf(3 to 1, 1 to 2, 2 to 3), priorityOrder(successState()))
        coVerify { updateHabitsPriorityOrder(listOf(3 to 1, 1 to 2, 2 to 3)) }
    }

    @Test
    fun editHabit_withChangedPriorityRank_renumbersWholeList() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(priorityHabits())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        val edited = habit(id = 1).copy(priorityRank = 1)

        // WHEN - The sheet's slider moves habit 1 from position 1 to position 3
        viewModel.editHabit(
            habit = edited,
            newName = "Renamed",
            newIcon = "icon",
            newColorArgb = 0xFF123456.toInt(),
            newPriorityRank = 3
        )

        // THEN - The habit lands at 3 and everyone else keeps a unique slot
        assertEquals(listOf(2 to 1, 3 to 2, 1 to 3), priorityOrder(successState()))
        coVerify { updateHabit(match { it.priorityRank == 3 }) }
        coVerify { updateHabitsPriorityOrder(listOf(2 to 1, 3 to 2, 1 to 3)) }
    }

    @Test
    fun addHabit_makesRoomAtChosenPosition_shiftsHabitsAtOrBelow() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(priorityHabits())
        coEvery { getAllBooleanEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()

        // WHEN - Adding straight into position 2 of 3 existing habits
        viewModel.addHabit("New", "icon", 0, priorityRank = 2)

        // THEN - Habits at/after position 2 are bumped so the slot stays unique
        coVerify { updateHabitsPriorityOrder(listOf(1 to 1, 2 to 3, 3 to 4)) }
        coVerify { insertHabit(match { it.priorityRank == 2 }) }
    }
}