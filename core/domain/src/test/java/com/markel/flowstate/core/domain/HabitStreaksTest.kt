package com.markel.flowstate.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** Best-run/best-week edge cases the status use case tests don't reach. */
class HabitStreaksTest {

    private val monday = LocalDate.parse("2026-09-28") // ISO week start
    private val mwf = HabitSchedule(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
    private val daily = HabitSchedule.DAILY

    private fun days(vararg dates: LocalDate): Set<Long> = dates.map { it.toEpochDay() }.toSet()

    @Test
    fun bestRun_consecutiveScheduledDays_formOneRun() {
        // Mon + Wed + Fri of one week: no scheduled day missed between them.
        val completed = days(monday, monday.plusDays(2), monday.plusDays(4))
        assertEquals(3, HabitStreaks.bestRun(mwf, completed))
    }

    @Test
    fun bestRun_missedScheduledDay_splitsTheRun() {
        // Mon done, Wed missed, Fri done → two runs of 1, not one run of 2.
        val completed = days(monday, monday.plusDays(4))
        assertEquals(1, HabitStreaks.bestRun(mwf, completed))
    }

    @Test
    fun bestRun_offDayCompletion_countsForNothing() {
        // A Tuesday bonus on a Mon/Wed/Fri habit is never part of a run.
        assertEquals(0, HabitStreaks.bestRun(mwf, days(monday.plusDays(1))))
    }

    @Test
    fun bestRun_daily_isTheLegacyConsecutiveDaysRun() {
        // Fri..Sun done, Thu missed → best run is 3.
        val completed = days(monday.plusDays(4), monday.plusDays(5), monday.plusDays(6))
        assertEquals(3, HabitStreaks.bestRun(daily, completed))
    }

    @Test
    fun bestWeeks_countsConsecutiveMetWeeks_andBreaksOnAMissedWeek() {
        val three = HabitSchedule(daily.days, weeklyTarget = 3)
        val completed = days(
            monday.minusDays(28), monday.minusDays(27), monday.minusDays(26), // week -4 ✓ (then a miss)
            monday.minusDays(21), monday.minusDays(20),                       // week -3 ✗ (2 of 3)
            monday.minusDays(14), monday.minusDays(13), monday.minusDays(12), // week -2 ✓
            monday.minusDays(7), monday.minusDays(6), monday.minusDays(5),    // week -1 ✓
        )
        assertEquals(2, HabitStreaks.bestWeeks(three, completed))
    }

    @Test
    fun currentRun_daily_matchesTheLegacyCounting() {
        // Yesterday and the day before done, today pending → streak counts 2.
        val today = monday.plusDays(2)
        assertEquals(2, HabitStreaks.currentRun(daily, days(monday, monday.plusDays(1)), today))
    }
}
