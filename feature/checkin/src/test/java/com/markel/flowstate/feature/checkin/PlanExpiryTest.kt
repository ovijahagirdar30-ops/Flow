package com.markel.flowstate.feature.checkin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pins [isPlanExpired]: midnight (0) stays visible all day and relies on the
 * date gate, a custom minute-of-day cutoff blanks the tab at that minute, and
 * yesterday's plan is expired no matter what.
 */
class PlanExpiryTest {

    private val today = "2026-09-25"
    private val tomorrow = "2026-09-26"

    /** Epoch millis for a device-local wall time, built with the same zone the rule reads back. */
    private fun millisAt(date: String, hour: Int, minute: Int): Long =
        LocalDateTime.parse("${date}T${LocalTime.of(hour, minute)}")
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    @Test
    fun noPlan_isNeverExpired() {
        assertFalse(isPlanExpired(null, 0, millisAt(today, 23, 59)))
        assertFalse(isPlanExpired(null, 22 * 60, millisAt(today, 23, 59)))
    }

    @Test
    fun midnightCutoff_keepsTodaysPlanVisibleAllDay() {
        assertFalse(isPlanExpired(today, 0, millisAt(today, 0, 0)))
        assertFalse(isPlanExpired(today, 0, millisAt(today, 12, 0)))
        assertFalse(isPlanExpired(today, 0, millisAt(today, 23, 59)))
    }

    @Test
    fun dayRollover_expiresThePlanAtMidnight() {
        // Plan agreed for the 25th; first minute of the 26th it is gone even
        // though the stored cutoff is still 0 (midnight).
        assertTrue(isPlanExpired(today, 0, millisAt(tomorrow, 0, 0)))
    }

    @Test
    fun customCutoff_expiresAtTheChosenMinute() {
        val cutoff = 22 * 60 // 22:00
        assertFalse(isPlanExpired(today, cutoff, millisAt(today, 21, 59)))
        assertTrue(isPlanExpired(today, cutoff, millisAt(today, 22, 0)))
        assertTrue(isPlanExpired(today, cutoff, millisAt(today, 23, 30)))
    }

    @Test
    fun yesterdaysPlan_isExpiredEvenBeforeTodaysCutoff() {
        val yesterday = "2026-09-24"
        assertTrue(isPlanExpired(yesterday, 23 * 60, millisAt(today, 10, 0)))
        assertTrue(isPlanExpired(yesterday, 0, millisAt(today, 10, 0)))
    }
}
