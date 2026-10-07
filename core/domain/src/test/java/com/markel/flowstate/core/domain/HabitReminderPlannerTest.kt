package com.markel.flowstate.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The rules habit notifications live by — see [HabitReminderPlanner].
 * Fixed dates where possible: 2026-10-05 is a Monday.
 */
class HabitReminderPlannerTest {

    private val monday: LocalDate = LocalDate.of(2026, 10, 5)
    private val zone = ZoneOffset.UTC

    private fun day(offset: Long): LocalDate = monday.plusDays(offset)

    // ── shouldNotifyOn ───────────────────────────────────────────────────────

    @Test
    fun dailyHabit_notifiesEveryDay() {
        val schedule = HabitSchedule.DAILY
        for (offset in 0L..6L) {
            assertTrue("day +$offset should notify", HabitReminderPlanner.shouldNotifyOn(schedule, emptySet(), day(offset)))
        }
    }

    @Test
    fun specificDays_notifiesOnlyOnScheduledWeekdays() {
        val schedule = HabitSchedule(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

        assertTrue(HabitReminderPlanner.shouldNotifyOn(schedule, emptySet(), monday))            // Mon
        assertFalse(HabitReminderPlanner.shouldNotifyOn(schedule, emptySet(), day(1)))           // Tue
        assertTrue(HabitReminderPlanner.shouldNotifyOn(schedule, emptySet(), day(2)))            // Wed
        assertFalse(HabitReminderPlanner.shouldNotifyOn(schedule, emptySet(), day(3)))           // Thu
        assertTrue(HabitReminderPlanner.shouldNotifyOn(schedule, emptySet(), day(4)))            // Fri
        assertFalse(HabitReminderPlanner.shouldNotifyOn(schedule, emptySet(), day(5)))           // Sat
        assertFalse(HabitReminderPlanner.shouldNotifyOn(schedule, emptySet(), day(6)))           // Sun
    }

    @Test
    fun alreadyCompletedDay_isSilent() {
        val schedule = HabitSchedule.DAILY
        val completed = setOf(monday.toEpochDay())

        assertFalse(HabitReminderPlanner.shouldNotifyOn(schedule, completed, monday))
        assertTrue(HabitReminderPlanner.shouldNotifyOn(schedule, completed, day(1)))
    }

    /**
     * The "only chose a frequency" case: `"DAILY:3"` has no specific days, so
     * every weekday still reminds — until 3 completions land this week.
     */
    @Test
    fun weeklyTarget_remindsEveryDayOfTheWeek_untilTargetMet() {
        val schedule = HabitSchedule.decode("DAILY:3")

        // Mon+Tue done = 2 of 3 → Wed through Sun all still remind.
        val twoDone = setOf(monday.toEpochDay(), day(1).toEpochDay())
        for (offset in 2L..6L) {
            assertTrue("Wed+ offset $offset", HabitReminderPlanner.shouldNotifyOn(schedule, twoDone, day(offset)))
        }

        // Third completion lands Wednesday → Thu..Sun go quiet.
        val threeDone = twoDone + day(2).toEpochDay()
        for (offset in 3L..6L) {
            assertFalse("Thu+ offset $offset", HabitReminderPlanner.shouldNotifyOn(schedule, threeDone, day(offset)))
        }

        // Next ISO week starts fresh: Monday reminds again.
        assertTrue(HabitReminderPlanner.shouldNotifyOn(schedule, threeDone, monday.plusWeeks(1)))
    }

    @Test
    fun weeklyTarget_targetStillHasRoomOnANonScheduledDay_staysSilent() {
        // MO,WE,FR with a target of 2: Saturday is never a remind day.
        val schedule = HabitSchedule(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), weeklyTarget = 2)

        assertFalse(HabitReminderPlanner.shouldNotifyOn(schedule, emptySet(), day(5))) // Sat
        assertTrue(HabitReminderPlanner.shouldNotifyOn(schedule, emptySet(), day(2)))   // Wed, 0 of 2
    }

    // ── nextFireDate ─────────────────────────────────────────────────────────

    @Test
    fun nextFireDate_returnsFirstDueDay() {
        val schedule = HabitSchedule(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))

        assertEquals(day(1), HabitReminderPlanner.nextFireDate(schedule, emptySet(), monday))    // Mon → Tue
        assertEquals(day(1), HabitReminderPlanner.nextFireDate(schedule, emptySet(), day(1)))    // Tue is due itself
        assertEquals(day(3), HabitReminderPlanner.nextFireDate(schedule, emptySet(), day(2)))    // Wed → Thu
        assertEquals(day(8), HabitReminderPlanner.nextFireDate(schedule, emptySet(), day(4)))    // Fri → next Tue
    }

    @Test
    fun nextFireDate_skipsCompletedDays() {
        val schedule = HabitSchedule.DAILY
        val completed = setOf(day(1).toEpochDay())

        // The day `from` lands on is still fair game — only a completed one is skipped.
        assertEquals(monday, HabitReminderPlanner.nextFireDate(schedule, completed, monday))
        assertEquals(day(2), HabitReminderPlanner.nextFireDate(schedule, completed, day(1)))
    }

    @Test
    fun nextFireDate_targetMetJumpsToNextWeek() {
        val schedule = HabitSchedule.decode("DAILY:3")
        val threeDone = setOf(monday.toEpochDay(), day(1).toEpochDay(), day(2).toEpochDay())

        // Wed was the third completion, so Thu..Sun of this week are out.
        assertEquals(monday.plusWeeks(1), HabitReminderPlanner.nextFireDate(schedule, threeDone, day(3)))
    }

    // ── nextTriggerMillis ────────────────────────────────────────────────────

    @Test
    fun nextTriggerMillis_keepsTodaysSlotWhenItHasNotPassed() {
        val today = LocalDate.now(zone)
        val now = today.atTime(6, 0).toInstant(zone).toEpochMilli()      // 06:00 today
        val expected = today.atTime(8, 0).toInstant(zone).toEpochMilli() // slot at 08:00

        assertEquals(
            expected,
            HabitReminderPlanner.nextTriggerMillis(HabitSchedule.DAILY, emptySet(), 8 * 60, now, zone)
        )
    }

    @Test
    fun nextTriggerMillis_skipsTodaysSlotOnceItHasPassed() {
        val today = LocalDate.now(zone)
        val now = today.atTime(9, 0).toInstant(zone).toEpochMilli()      // 09:00 today, slot was 08:00
        val expected = today.plusDays(1).atTime(8, 0).toInstant(zone).toEpochMilli()

        assertEquals(
            expected,
            HabitReminderPlanner.nextTriggerMillis(HabitSchedule.DAILY, emptySet(), 8 * 60, now, zone)
        )
    }

    @Test
    fun nextTriggerMillis_completedDayPushesToNextDueDay() {
        val today = LocalDate.now(zone)
        val now = today.atTime(6, 0).toInstant(zone).toEpochMilli()
        val completed = setOf(today.toEpochDay())
        val expected = today.plusDays(1).atTime(8, 0).toInstant(zone).toEpochMilli()

        assertEquals(
            expected,
            HabitReminderPlanner.nextTriggerMillis(HabitSchedule.DAILY, completed, 8 * 60, now, zone)
        )
    }

    @Test
    fun nextTriggerMillis_metWeeklyTargetWaitsForNextWeek() {
        val schedule = HabitSchedule.decode("DAILY:3")
        val today = LocalDate.now(zone)
        val now = today.atTime(6, 0).toInstant(zone).toEpochMilli()

        // This week already has 3 completions — today plus the two days
        // before it (every day is scheduled for DAILY:3).
        val completed = setOf(today.toEpochDay(), today.minusDays(1).toEpochDay(), today.minusDays(2).toEpochDay())
        val trigger = HabitReminderPlanner.nextTriggerMillis(schedule, completed, 8 * 60, now, zone)

        assertTrue("trigger should exist", trigger != null && trigger > now)
        val triggerDate = java.time.Instant.ofEpochMilli(trigger!!).atZone(zone).toLocalDate()
        // Nothing left this week → the next slot lands after today.
        assertTrue(triggerDate > today)
    }

    @Test
    fun nextTriggerMillis_neverSchedulesBackwards() {
        val today = LocalDate.now(zone)
        val endOfDay = today.atTime(23, 59).toInstant(zone).toEpochMilli()

        val trigger = HabitReminderPlanner.nextTriggerMillis(HabitSchedule.DAILY, emptySet(), 8 * 60, endOfDay, zone)

        assertTrue(trigger != null && trigger > endOfDay)
    }

    @Test
    fun nextTriggerMillis_nullWhenScheduleCanNeverFire() {
        // Not a real schedule (the UI never removes the last day), but the
        // lookahead must still terminate instead of looping forever: every
        // day in the window is already completed.
        val impossible = HabitSchedule(setOf(DayOfWeek.MONDAY), weeklyTarget = 1)
        val today = LocalDate.now(zone)
        val completed = (0L..40L).map { today.plusDays(it).toEpochDay() }.toSet()

        assertNull(HabitReminderPlanner.nextTriggerMillis(impossible, completed, 8 * 60, 0L, zone))
    }
}
