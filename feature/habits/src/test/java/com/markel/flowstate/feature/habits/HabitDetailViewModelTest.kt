package com.markel.flowstate.feature.habits.details

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.usecase.habits.GetHabitByIdUseCase
import com.markel.flowstate.core.domain.usecase.habits.ToggleHabitEntryUseCase
import com.markel.flowstate.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val getHabitById: GetHabitByIdUseCase = mockk(relaxed = true)
    private val toggleEntry: ToggleHabitEntryUseCase = mockk(relaxed = true)

    private lateinit var viewModel: HabitDetailViewModel

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun habit(id: Int = 1) = Habit(
        id = id,
        name = "Test habit",
        iconName = "icon",
        colorArgb = 0xFF123456.toInt(),
        createdAt = LocalDate.now().minusDays(30)
    )

    private fun savedStateHandle(habitId: Int = 1) =
        SavedStateHandle(mapOf("habitId" to habitId.toString()))

    private fun buildViewModel(habitId: Int = 1) = HabitDetailViewModel(
        savedStateHandle = savedStateHandle(habitId),
        getHabitById = getHabitById,
        habitRepository = habitRepository,
        toggleEntry = toggleEntry
    )

    // ── init / loading ────────────────────────────────────────────────────────

    @Test
    fun init_whenHabitNotFound_keepsDefaultState() = runTest {
        // GIVEN - getHabitById returns null (habit was deleted or ID is wrong)
        coEvery { getHabitById(99) } returns null

        // WHEN
        viewModel = buildViewModel(habitId = 99)

        // THEN - State should remain as default without crashing
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.habit)
            assertEquals(0, state.currentStreak)
            assertEquals(0, state.bestStreak)
        }
    }

    @Test
    fun init_whenHabitExists_loadsHabitIntoState() = runTest {
        // GIVEN
        val habit = habit(id = 1)
        coEvery { getHabitById(1) } returns habit
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())

        // WHEN
        viewModel = buildViewModel(habitId = 1)

        // THEN
        viewModel.uiState.test {
            assertEquals(habit, awaitItem().habit)
        }
    }

    @Test
    fun init_withEntries_populatesAllEntries() = runTest {
        // GIVEN - Habit with two completed days
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(listOf(today, yesterday))

        // WHEN
        viewModel = buildViewModel()

        // THEN
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(today.toEpochDay() in state.allEntries)
            assertTrue(yesterday.toEpochDay() in state.allEntries)
        }
    }

    // ── calculateCurrentStreak ────────────────────────────────────────────────

    @Test
    fun currentStreak_withNoEntries_isZero() = runTest {
        // GIVEN
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())

        // WHEN
        viewModel = buildViewModel()

        // THEN
        viewModel.uiState.test {
            assertEquals(0, awaitItem().currentStreak)
        }
    }

    @Test
    fun currentStreak_withConsecutiveDaysIncludingToday_countsCorrectly() = runTest {
        // GIVEN - 3 consecutive days ending today
        val today = LocalDate.now()
        val entries = listOf(today, today.minusDays(1), today.minusDays(2))
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(entries)

        // WHEN
        viewModel = buildViewModel()

        // THEN
        viewModel.uiState.test {
            assertEquals(3, awaitItem().currentStreak)
        }
    }

    @Test
    fun currentStreak_withConsecutiveDaysEndingYesterday_countsCorrectly() = runTest {
        // GIVEN - Streak ended yesterday (today not completed yet)
        val today = LocalDate.now()
        val entries = listOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(entries)

        // WHEN
        viewModel = buildViewModel()

        // THEN - Still counts yesterday's streak
        viewModel.uiState.test {
            assertEquals(3, awaitItem().currentStreak)
        }
    }

    @Test
    fun currentStreak_withGapInEntries_onlyCountsLatestStreak() = runTest {
        // GIVEN - today + yesterday, then a gap, then 5 older days
        val today = LocalDate.now()
        val entries = listOf(
            today,
            today.minusDays(1),
            // gap on minusDays(2)
            today.minusDays(3),
            today.minusDays(4),
            today.minusDays(5),
            today.minusDays(6),
            today.minusDays(7)
        )
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(entries)

        // WHEN
        viewModel = buildViewModel()

        // THEN - Only counts 2, not 7
        viewModel.uiState.test {
            assertEquals(2, awaitItem().currentStreak)
        }
    }

    // ── calculateBestStreak ───────────────────────────────────────────────────

    @Test
    fun bestStreak_withNoEntries_isZero() = runTest {
        // GIVEN
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())

        // WHEN
        viewModel = buildViewModel()

        // THEN
        viewModel.uiState.test {
            assertEquals(0, awaitItem().bestStreak)
        }
    }

    @Test
    fun bestStreak_picksLongestConsecutiveRun() = runTest {
        // GIVEN - A short run of 2, then a gap, then a run of 5
        val today = LocalDate.now()
        val entries = listOf(
            today.minusDays(10),
            today.minusDays(9),
            // gap
            today.minusDays(4),
            today.minusDays(3),
            today.minusDays(2),
            today.minusDays(1),
            today
        )
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(entries)

        // WHEN
        viewModel = buildViewModel()

        // THEN - Best streak is 5, not 2
        viewModel.uiState.test {
            assertEquals(5, awaitItem().bestStreak)
        }
    }

    @Test
    fun bestStreak_withSingleEntry_isOne() = runTest {
        // GIVEN
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(listOf(LocalDate.now()))

        // WHEN
        viewModel = buildViewModel()

        // THEN
        viewModel.uiState.test {
            assertEquals(1, awaitItem().bestStreak)
        }
    }

    // ── calculateWeeklyCompletions ────────────────────────────────────────────

    @Test
    fun weeklyCompletions_alwaysReturns16Weeks() = runTest {
        // GIVEN
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())

        // WHEN
        viewModel = buildViewModel()

        // THEN - Chart always has 16 data points
        viewModel.uiState.test {
            assertEquals(16, awaitItem().weeklyCompletions.size)
        }
    }

    @Test
    fun weeklyCompletions_countsCorrectlyForCurrentWeek() = runTest {
        // GIVEN - 3 completions in the current week (Mon, Tue, Wed)
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        val entries = listOf(monday, monday.plusDays(1), monday.plusDays(2))
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(entries)

        // WHEN
        viewModel = buildViewModel()

        // THEN - The last element corresponds to the current week
        viewModel.uiState.test {
            assertEquals(3, awaitItem().weeklyCompletions.last().second)
        }
    }

    @Test
    fun weeklyCompletions_weeksAreOrderedChronologically() = runTest {
        // GIVEN
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())

        // WHEN
        viewModel = buildViewModel()

        // THEN - Each week's start date is before the next one
        viewModel.uiState.test {
            val weeks = awaitItem().weeklyCompletions
            for (i in 1 until weeks.size) {
                assertTrue(weeks[i - 1].first.isBefore(weeks[i].first))
            }
        }
    }

    // ── calculateDayOfWeekCompletions ─────────────────────────────────────────

    @Test
    fun dayOfWeekCompletions_groupsEntriesByDayOfWeek() = runTest {
        // GIVEN - 2 Mondays and 1 Friday
        val monday1 = LocalDate.now().with(DayOfWeek.MONDAY)
        val monday2 = monday1.minusWeeks(1)
        val friday = LocalDate.now().with(DayOfWeek.FRIDAY)
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(
            listOf(monday1, monday2, friday)
        )

        // WHEN
        viewModel = buildViewModel()

        // THEN - Monday (1) has count 2, Friday (5) has count 1
        viewModel.uiState.test {
            val dowCompletions = awaitItem().dayOfWeekCompletions
            assertEquals(2, dowCompletions[1]) // Monday = 1
            assertEquals(1, dowCompletions[5]) // Friday = 5
        }
    }

    @Test
    fun dayOfWeekCompletions_withNoEntries_isEmpty() = runTest {
        // GIVEN
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())

        // WHEN
        viewModel = buildViewModel()

        // THEN
        viewModel.uiState.test {
            assertTrue(awaitItem().dayOfWeekCompletions.isEmpty())
        }
    }

    // ── cycleViewMode ─────────────────────────────────────────────────────────

    @Test
    fun cycleViewMode_cyclesThroughAllModesInOrder() = runTest {
        // GIVEN - Default state starts at ONE_MONTH
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())
        viewModel = buildViewModel()

        // WHEN / THEN - Full cycle: ONE_MONTH → THREE_MONTHS → ONE_YEAR → ONE_MONTH
        viewModel.uiState.test {
            assertEquals(CalendarViewMode.ONE_MONTH, awaitItem().viewMode)

            viewModel.cycleViewMode()
            assertEquals(CalendarViewMode.THREE_MONTHS, awaitItem().viewMode)

            viewModel.cycleViewMode()
            assertEquals(CalendarViewMode.ONE_YEAR, awaitItem().viewMode)

            viewModel.cycleViewMode()
            assertEquals(CalendarViewMode.ONE_MONTH, awaitItem().viewMode)
        }
    }

    // ── navigatePrevious / navigateNext ───────────────────────────────────────

    @Test
    fun navigatePrevious_inOneMonthMode_decrementsMonth() = runTest {
        // GIVEN
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())
        viewModel = buildViewModel()

        val initialMonth = viewModel.uiState.value.displayMonth

        // WHEN
        viewModel.navigatePrevious()

        // THEN
        viewModel.uiState.test {
            val expectedMonth = if (initialMonth == 0) 11 else initialMonth - 1
            assertEquals(expectedMonth, awaitItem().displayMonth)
        }
    }

    @Test
    fun navigatePrevious_inOneMonthMode_wrapsYearWhenJanuary() = runTest {
        // GIVEN - Navigate back until we reach January
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())
        viewModel = buildViewModel()

        val monthsToGoBack = LocalDate.now().monthValue - 1 // monthValue is 1-based
        repeat(monthsToGoBack) { viewModel.navigatePrevious() }

        val yearBeforeNav = viewModel.uiState.value.displayYear

        // WHEN - Navigate back once more from January
        viewModel.navigatePrevious()

        // THEN - Month wraps to December, year decrements
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(11, state.displayMonth)
            assertEquals(yearBeforeNav - 1, state.displayYear)
        }
    }

    @Test
    fun navigateNext_inOneMonthMode_doesNotGoBeyondCurrentMonth() = runTest {
        // GIVEN - Already at the current month (default state)
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())
        viewModel = buildViewModel()

        val currentDisplayMonth = viewModel.uiState.value.displayMonth

        // WHEN - Try to navigate forward from the current month
        viewModel.navigateNext()

        // THEN - State should not change
        viewModel.uiState.test {
            assertEquals(currentDisplayMonth, awaitItem().displayMonth)
        }
    }

    @Test
    fun navigateNext_inOneYearMode_doesNotGoBeyondCurrentYear() = runTest {
        // GIVEN - ONE_MONTH → THREE_MONTHS → ONE_YEAR
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())
        viewModel = buildViewModel()
        viewModel.cycleViewMode()
        viewModel.cycleViewMode()

        val currentYear = viewModel.uiState.value.displayYear

        // WHEN
        viewModel.navigateNext()

        // THEN
        viewModel.uiState.test {
            assertEquals(currentYear, awaitItem().displayYear)
        }
    }

    @Test
    fun navigatePrevious_inOneYearMode_decrementsYear() = runTest {
        // GIVEN - Switch to ONE_YEAR mode
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())
        viewModel = buildViewModel()
        viewModel.cycleViewMode()
        viewModel.cycleViewMode()

        val yearBefore = viewModel.uiState.value.displayYear

        // WHEN
        viewModel.navigatePrevious()

        // THEN
        viewModel.uiState.test {
            assertEquals(yearBefore - 1, awaitItem().displayYear)
        }
    }

    @Test
    fun navigatePrevious_inThreeMonthsMode_goesBackThreeMonths() = runTest {
        // GIVEN - Switch to THREE_MONTHS mode
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())
        viewModel = buildViewModel()
        viewModel.cycleViewMode()

        val monthBefore = viewModel.uiState.value.displayMonth

        // WHEN
        viewModel.navigatePrevious()

        // THEN
        viewModel.uiState.test {
            val expectedMonth = (monthBefore - 3 + 12) % 12
            assertEquals(expectedMonth, awaitItem().displayMonth)
        }
    }

    // ── setWeeklyBarsMode / selectBar ─────────────────────────────────────────

    @Test
    fun setWeeklyBarsMode_updatesMode_andResetsSelectedBar() = runTest {
        // GIVEN - A bar is already selected
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())
        viewModel = buildViewModel()
        viewModel.selectBar(3)

        // WHEN - Change the mode
        viewModel.setWeeklyBarsMode(WeeklyBarsMode.SIXTEEN)

        // THEN - Mode updated and selection cleared
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(WeeklyBarsMode.SIXTEEN, state.weeklyBarsMode)
            assertNull(state.selectedBarIndex)
        }
    }

    @Test
    fun selectBar_updatesSelectedBarIndex() = runTest {
        // GIVEN
        coEvery { getHabitById(1) } returns habit()
        coEvery { habitRepository.getEntriesForHabit(1) } returns flowOf(emptyList())
        viewModel = buildViewModel()

        // WHEN
        viewModel.selectBar(5)

        // THEN
        viewModel.uiState.test {
            assertEquals(5, awaitItem().selectedBarIndex)
        }
    }
}
