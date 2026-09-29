package com.markel.flowstate.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Storage round-trips and the due-day questions [HabitSchedule] answers —
 * the encoding lives in the existing habits.frequency column, so legacy
 * values must decode without loss or exceptions.
 */
class HabitScheduleTest {

    // ── encode → decode round-trips ─────────────────────────────────────

    @Test
    fun daily_encodesToTheLegacyLiteral() {
        assertEquals("DAILY", HabitSchedule.DAILY.encode())
    }

    @Test
    fun roundTrip_preservesEveryShape() {
        val cases = listOf(
            HabitSchedule.DAILY,
            HabitSchedule(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            HabitSchedule(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), 3),
            HabitSchedule(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), 2),
        )
        cases.forEach { schedule -> assertEquals(schedule, HabitSchedule.decode(schedule.encode())) }
    }

    @Test
    fun encode_ordersDaysMondayFirst() {
        val schedule = HabitSchedule(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        assertEquals("MO,FR,SU", schedule.encode())
    }

    // ── lenient decode of legacy / hostile values ───────────────────────

    @Test
    fun decode_unknownOrBlank_fallsBackToDaily() {
        assertEquals(HabitSchedule.DAILY, HabitSchedule.decode("WEEKLY"))
        assertEquals(HabitSchedule.DAILY, HabitSchedule.decode(""))
        assertEquals(HabitSchedule.DAILY, HabitSchedule.decode(null))
        assertEquals(HabitSchedule.DAILY, HabitSchedule.decode("NONSENSE,XYZ"))
    }

    @Test
    fun decode_targetThatFitsNoDays_isClampedAway() {
        // 5×/week on a single scheduled day is unsatisfiable — decode must
        // not throw (the data class invariant still protects direct builds):
        // it clamps to what the days allow, here 1 of 1.
        val schedule = HabitSchedule.decode("MO:5")
        assertEquals(setOf(DayOfWeek.MONDAY), schedule.days)
        assertEquals(1, schedule.weeklyTarget)
    }

    @Test
    fun decode_isCaseInsensitiveOnDayCodes() {
        assertEquals(
            HabitSchedule(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
            HabitSchedule.decode("mo,fr")
        )
    }

    // ── scheduled-day checks ────────────────────────────────────────────

    @Test
    fun isScheduledOn_onlyMatchesChosenDays() {
        val monWedFri = HabitSchedule(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        // 2026-09-28 is a Monday, 29 a Tuesday, 30 a Wednesday.
        assertTrue(monWedFri.isScheduledOn(LocalDate.parse("2026-09-28")))
        assertFalse(monWedFri.isScheduledOn(LocalDate.parse("2026-09-29")))
        assertTrue(monWedFri.isScheduledOn(LocalDate.parse("2026-09-30")))
    }

    @Test
    fun isDaily_trueOnlyForAllDaysWithoutTarget() {
        assertTrue(HabitSchedule.DAILY.isDaily)
        assertFalse(HabitSchedule(setOf(DayOfWeek.MONDAY), weeklyTarget = null).isDaily)
        assertFalse(HabitSchedule(HabitSchedule.DAILY.days, weeklyTarget = 3).isDaily)
    }
}
