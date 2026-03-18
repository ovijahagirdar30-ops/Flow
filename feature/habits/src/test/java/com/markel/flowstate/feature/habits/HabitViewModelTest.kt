package com.markel.flowstate.feature.habits

import app.cash.turbine.test
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitEntryFlat
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitWithStatus
import com.markel.flowstate.core.domain.usecase.habits.DeleteHabitUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetHabitsWithStatusUseCase
import com.markel.flowstate.core.domain.usecase.habits.InsertHabitUseCase
import com.markel.flowstate.core.domain.usecase.habits.ToggleHabitEntryUseCase
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class HabitViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val getHabitsWithStatus: GetHabitsWithStatusUseCase = mockk(relaxed = true)
    private val insertHabit: InsertHabitUseCase = mockk(relaxed = true)
    private val deleteHabit: DeleteHabitUseCase = mockk(relaxed = true)
    private val toggleEntry: ToggleHabitEntryUseCase = mockk(relaxed = true)

    private lateinit var viewModel: HabitViewModel

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun habit(id: Int = 1, name: String = "Test habit") = Habit(
        id = id,
        name = name,
        iconName = "icon",
        colorArgb = 0xFF123456.toInt(),
        createdAt = LocalDate.now().minusDays(7)
    )

    private fun habitWithStatus(habit: Habit = habit(), completedToday: Boolean = false) = HabitWithStatus(habit = habit, isCompletedToday = completedToday)

    private fun entry(habitId: Int, epochDay: Long) = HabitEntryFlat(habitId = habitId, epochDay = epochDay)

    private fun buildViewModel() = HabitViewModel(
        habitRepository = habitRepository,
        getHabitsWithStatus = getHabitsWithStatus,
        insertHabit = insertHabit,
        deleteHabit = deleteHabit,
        toggleEntry = toggleEntry
    )

    // ── uiState ───────────────────────────────────────────────────────────────

    @Test
    fun uiState_initialValue_isLoading() = runTest {
        // GIVEN - Flows that never emit (simulate slow loading)
        coEvery { getHabitsWithStatus() } returns flowOf()
        coEvery { habitRepository.getAllEntries() } returns flowOf()

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
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())

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
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())

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
            listOf(habitWithStatus(habit(id = 1)), habitWithStatus(habit(id = 2)))
        )
        coEvery { habitRepository.getAllEntries() } returns flowOf(
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
        coEvery { habitRepository.getAllEntries() } returns flowOf(
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

    @Test
    fun uiState_historicalEntries_areIncludedInWeekEntriesByHabit() = runTest {
        // GIVEN - An entry from 3 weeks ago (used to be filtered out, now must be included)
        val threeWeeksAgo = LocalDate.now().minusWeeks(3).toEpochDay()
        coEvery { getHabitsWithStatus() } returns flowOf(listOf(habitWithStatus(habit(id = 1))))
        coEvery { habitRepository.getAllEntries() } returns flowOf(
            listOf(entry(habitId = 1, epochDay = threeWeeksAgo))
        )

        // WHEN
        viewModel = buildViewModel()

        // THEN - Historical entry must be present so the WeekCalendar can render it
        viewModel.uiState.test {
            val successState = awaitItem().let {
                if (it is HabitUiState.Loading) awaitItem() else it
            } as HabitUiState.Success

            assertTrue(threeWeeksAgo in (successState.weekEntriesByHabit[1] ?: emptySet()))
        }
    }

    // ── showAddDialog / hideAddDialog ─────────────────────────────────────────

    @Test
    fun showAddDialog_setsShowAddDialogToTrue() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())
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
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())
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
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()

        // WHEN
        viewModel.addHabit("Read", "book_icon", 0xFF0000FF.toInt())

        // THEN
        coVerify {
            insertHabit(match { habit ->
                habit.name == "Read" &&
                        habit.iconName == "book_icon" &&
                        habit.colorArgb == 0xFF0000FF.toInt()
            })
        }
    }

    @Test
    fun addHabit_withBlankName_doesNotCallRepository() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())
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
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())
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

    // ── deleteHabit ───────────────────────────────────────────────────────────

    @Test
    fun deleteHabit_callsDeleteUseCaseWithCorrectHabit() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        val habitToDelete = habit(id = 5, name = "Exercise")

        // WHEN
        viewModel.deleteHabit(habitToDelete)

        // THEN
        coVerify { deleteHabit(habitToDelete) }
    }

    // ── toggleHabitOnDate ─────────────────────────────────────────────────────

    @Test
    fun toggleHabitOnDate_callsToggleEntryUseCase_withCorrectArguments() = runTest {
        // GIVEN
        coEvery { getHabitsWithStatus() } returns flowOf(emptyList())
        coEvery { habitRepository.getAllEntries() } returns flowOf(emptyList())
        viewModel = buildViewModel()
        val date = LocalDate.now().minusDays(2)

        // WHEN
        viewModel.toggleHabitOnDate(habitId = 3, date = date)

        // THEN
        coVerify { toggleEntry(3, date) }
    }
}