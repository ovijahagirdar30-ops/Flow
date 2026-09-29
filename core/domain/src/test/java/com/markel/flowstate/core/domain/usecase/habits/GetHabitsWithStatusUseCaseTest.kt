package com.markel.flowstate.core.domain.usecase.habits

import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitEntryFlat
import com.markel.flowstate.core.domain.HabitNumericEntry
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitSchedule
import com.markel.flowstate.core.domain.HabitType
import com.markel.flowstate.core.domain.HabitWithStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Schedule-aware due/streak rules — the heart of the frequency feature:
 * off-days are never due and never break a streak, missed scheduled days do,
 * and times-per-week habits count streaks in ISO weeks while hiding once the
 * weekly target is met.
 */
class GetHabitsWithStatusUseCaseTest {

    private val repository: HabitRepository = mockk()
    private val useCase = GetHabitsWithStatusUseCase(repository)

    // Pinned ISO week: Mon 2026-09-28 … Sun 2026-10-04 (Monday start).
    private val monday = LocalDate.parse("2026-09-28")
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val thursday = monday.plusDays(3)

    private val mwf = HabitSchedule(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

    private fun booleanHabit(
        schedule: HabitSchedule = HabitSchedule.DAILY,
        id: Int = 1,
    ) = Habit(id = id, name = "habit", schedule = schedule, habitType = HabitType.BOOLEAN)

    private fun days(vararg dates: LocalDate, habitId: Int = 1): List<HabitEntryFlat> =
        dates.map { HabitEntryFlat(habitId, it.toEpochDay()) }

    private suspend fun status(
        habit: Habit,
        date: LocalDate,
        boolEntries: List<HabitEntryFlat> = emptyList(),
        numericEntries: List<HabitNumericEntry> = emptyList(),
    ): HabitWithStatus {
        every { repository.getHabits() } returns flowOf(listOf(habit))
        every { repository.getAllEntries() } returns flowOf(boolEntries)
        if (habit.habitType == HabitType.NUMERIC) {
            every { repository.getNumericEntries(habit.id) } returns flowOf(numericEntries)
        }
        return useCase(date).first().single()
    }

    // ── Daily habits: behavior identical to the old everyday logic ──────

    @Test
    fun daily_pendingToday_isDue_andCountsFromYesterday() = runTest {
        val result = status(
            booleanHabit(HabitSchedule.DAILY),
            date = wednesday,
            boolEntries = days(monday, tuesday)
        )
        assertTrue(result.isDueToday)
        assertFalse(result.isCompletedToday)
        assertEquals(2, result.streak)
    }

    @Test
    fun daily_completedToday_isDueAndDone() = runTest {
        val result = status(
            booleanHabit(HabitSchedule.DAILY),
            date = wednesday,
            boolEntries = days(monday, tuesday, wednesday)
        )
        assertTrue(result.isDueToday)
        assertTrue(result.isCompletedToday)
        assertEquals(3, result.streak)
    }

    // ── Specific days: off-days are neither due nor misses ──────────────

    @Test
    fun offDay_isNotDue_andDoesNotBreakTheStreak() = runTest {
        // Tuesday isn't scheduled: not due, and Monday's completion survives.
        val tue = status(booleanHabit(mwf), date = tuesday, boolEntries = days(monday))
        assertFalse(tue.isDueToday)
        assertEquals(1, tue.streak)

        // Wednesday is due; yesterday's off-day is skipped in the count.
        val wed = status(booleanHabit(mwf), date = wednesday, boolEntries = days(monday))
        assertTrue(wed.isDueToday)
        assertEquals(1, wed.streak)
    }

    @Test
    fun missedScheduledDay_breaksTheStreak() = runTest {
        // Wednesday was scheduled and missed → 0, even though Monday was done.
        val thuMissed = status(booleanHabit(mwf), date = thursday, boolEntries = days(monday))
        assertEquals(0, thuMissed.streak)

        // Completing Wednesday keeps the run alive: Wed + Mon = 2 (Tue skipped).
        val thuDone = status(booleanHabit(mwf), date = thursday, boolEntries = days(monday, wednesday))
        assertEquals(2, thuDone.streak)
    }

    @Test
    fun offDay_completionDoesNotMakeItDue() = runTest {
        // Completing on an off-day never makes it "due" — the schedule rules.
        val result = status(booleanHabit(mwf), date = tuesday, boolEntries = days(monday, tuesday))
        assertFalse(result.isDueToday)
        assertTrue(result.isCompletedToday)
    }

    // ── Times per week: weekly target gates due-ness, weeks count streaks

    @Test
    fun weeklyTarget_dueUntilTheTargetIsMet() = runTest {
        val three = HabitSchedule(HabitSchedule.DAILY.days, weeklyTarget = 3)
        val result = status(booleanHabit(three), date = wednesday, boolEntries = days(monday, tuesday))
        assertTrue(result.isDueToday) // 2 of 3 done this week — room for one more
        // Previous week had no completions → nothing judgeable → week streak 0.
        assertEquals(0, result.streak)
    }

    @Test
    fun weeklyTarget_metHidesTheHabitUntilNextWeek() = runTest {
        val three = HabitSchedule(HabitSchedule.DAILY.days, weeklyTarget = 3)
        // Third completion lands Wednesday…
        val onCompletion = status(booleanHabit(three), date = wednesday, boolEntries = days(monday, tuesday, wednesday))
        assertTrue(onCompletion.isDueToday) // still visible while done today
        assertTrue(onCompletion.isCompletedToday)

        // …so on Thursday the habit rests: not due, no fake "miss".
        val thursday = status(booleanHabit(three), date = thursday, boolEntries = days(monday, tuesday, wednesday))
        assertFalse(thursday.isDueToday)
        assertFalse(thursday.isCompletedToday)
    }

    @Test
    fun weeklyTarget_offDayCompletionsDoNotCount() = runTest {
        // Mon+Wed scheduled, target 2: Monday done + a Tuesday bonus. If the
        // off-day counted, the week would read 2/2 and hide — it must read
        // 1/2 instead, so Wednesday still asks for the second session.
        val twoOfTwo = HabitSchedule(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), weeklyTarget = 2)
        val result = status(booleanHabit(twoOfTwo), date = wednesday, boolEntries = days(monday, tuesday))
        assertTrue(result.isDueToday)
    }

    @Test
    fun weeklyTarget_streakCountsWeeks_andPendingWeekCannotBreakIt() = runTest {
        val three = HabitSchedule(HabitSchedule.DAILY.days, weeklyTarget = 3)
        // Two fully-met weeks behind us, current week at 1/3 on Thursday.
        val entries = days(
            monday.minusDays(14), monday.minusDays(13), monday.minusDays(12), // week -2 ✓
            monday.minusDays(7), monday.minusDays(6), monday.minusDays(5),    // week -1 ✓
            monday,                                                           // current 1/3
        )
        val result = status(booleanHabit(three), date = thursday, boolEntries = entries)
        assertEquals(2, result.streak) // current week can't break it yet
        assertTrue(result.isDueToday)

        // Filling the current week to 3/3 extends the streak to 3.
        val filled = status(
            booleanHabit(three),
            date = thursday,
            boolEntries = entries + days(tuesday, wednesday)
        )
        assertEquals(3, filled.streak)
        assertFalse(filled.isDueToday) // met → rests until Monday
    }

    // ── Numeric habits share the same schedule rules ────────────────────

    @Test
    fun numeric_offDay_isNotDue() = runTest {
        val habit = booleanHabit(mwf).copy(habitType = HabitType.NUMERIC, targetValue = 10f)
        val result = status(
            habit,
            date = tuesday,
            numericEntries = listOf(HabitNumericEntry(1, monday, 10f))
        )
        assertFalse(result.isDueToday)
        assertFalse(result.isCompletedToday) // no entry today anyway
        assertEquals(1, result.streak)
    }
}
