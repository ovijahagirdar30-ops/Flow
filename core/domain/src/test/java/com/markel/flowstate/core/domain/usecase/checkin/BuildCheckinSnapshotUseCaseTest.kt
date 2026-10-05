package com.markel.flowstate.core.domain.usecase.checkin

import com.markel.flowstate.core.domain.CheckinRepository
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitEntryFlat
import com.markel.flowstate.core.domain.HabitMoodLog
import com.markel.flowstate.core.domain.HabitType
import com.markel.flowstate.core.domain.HabitWithStatus
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.habits.GetAllBooleanEntriesUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetHabitsWithStatusUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The AI context's mood-consent rules: the per-habit `moodLoggingEnabled` flag
 * is what the user agreed to, so it decides what leaves the app — only opted-in
 * habits contribute ratings, ratings older than the 7-day window drop out, and
 * days with no rating (skipped prompt) never appear.
 */
class BuildCheckinSnapshotUseCaseTest {

    private val checkinRepository: CheckinRepository = mockk()
    private val taskRepository: TaskRepository = mockk()
    private val getHabitsWithStatus: GetHabitsWithStatusUseCase = mockk()
    private val getAllBooleanEntries: GetAllBooleanEntriesUseCase = mockk()

    private val useCase = BuildCheckinSnapshotUseCase(
        checkinRepository = checkinRepository,
        taskRepository = taskRepository,
        getHabitsWithStatus = getHabitsWithStatus,
        getAllBooleanEntries = getAllBooleanEntries
    )

    // A pinned day so the window maths can't drift with the wall clock.
    private val date = LocalDate.parse("2026-09-29")

    private fun habit(id: Int, moodLoggingEnabled: Boolean) = Habit(
        id = id,
        name = "habit $id",
        habitType = HabitType.BOOLEAN,
        moodLoggingEnabled = moodLoggingEnabled
    )

    private fun rated(habitId: Int, daysAgo: Long, mood: Int?) = HabitEntryFlat(
        habitId = habitId,
        epochDay = date.toEpochDay() - daysAgo,
        mood = mood
    )

    private suspend fun stubCommon(
        habits: List<HabitWithStatus>,
        entries: List<HabitEntryFlat>
    ) {
        coEvery { checkinRepository.getCheckinByDate(date.toString()) } returns null
        every { taskRepository.getTasks() } returns flowOf(emptyList())
        every { getHabitsWithStatus(date) } returns flowOf(habits)
        every { getAllBooleanEntries() } returns flowOf(entries)
    }

    @Test
    fun invoke_onlyOptedInHabitsAndOnlyInsideSevenDayWindow() = runTest {
        stubCommon(
            habits = listOf(
                HabitWithStatus(habit(1, moodLoggingEnabled = true), isCompletedToday = false),
                HabitWithStatus(habit(2, moodLoggingEnabled = false), isCompletedToday = false)
            ),
            entries = listOf(
                rated(1, daysAgo = 0, mood = 4),   // today
                rated(1, daysAgo = 6, mood = 2),   // oldest day still inside the window
                rated(1, daysAgo = 7, mood = 1),   // one day too old -> dropped
                rated(1, daysAgo = 1, mood = null), // prompt skipped -> no rating
                rated(2, daysAgo = 0, mood = 5)    // logging OFF -> never sent
            )
        )

        val snapshot = useCase(date)

        assertEquals(setOf(1), snapshot.habitMoods.keys)
        // Oldest first, only the two ratings the user opted into.
        assertEquals(
            listOf(
                HabitMoodLog(date.minusDays(6).toString(), 2),
                HabitMoodLog(date.toString(), 4)
            ),
            snapshot.habitMoods[1]
        )
    }

    @Test
    fun invoke_whenNothingOptedIn_sendsNoMoodDataAtAll() = runTest {
        stubCommon(
            habits = listOf(
                HabitWithStatus(habit(1, moodLoggingEnabled = false), isCompletedToday = false)
            ),
            entries = listOf(rated(1, daysAgo = 0, mood = 5))
        )

        val snapshot = useCase(date)

        assertTrue(snapshot.habitMoods.isEmpty())
    }
}
