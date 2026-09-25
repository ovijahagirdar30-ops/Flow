package com.markel.flowstate.feature.checkin

import java.time.Instant
import java.time.ZoneId

/**
 * The Plan tab's visibility rule for the agreed evening plan: it shows only
 * while its own calendar day is still the current day AND the clock is before
 * the user's chosen end-of-day time.
 *
 * [endOfDayMinutes] is a minute-of-day (0..1439) persisted in DataStore.
 * 0 means midnight (the default) — a literal 00:00 cutoff would otherwise be
 * "now >= 0" at every moment and hide the plan all day, so midnight is left
 * entirely to the date comparison, which already flips the plan out at the
 * day rollover.
 *
 * Pure and clock-injectable ([nowMillis]) so tests pin the time instead of
 * depending on when they happen to run.
 */
internal fun isPlanExpired(
    planDate: String?,
    endOfDayMinutes: Int,
    nowMillis: Long = System.currentTimeMillis(),
): Boolean {
    planDate ?: return false
    val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
    // Yesterday's plan never resurfaces, regardless of the cutoff time.
    if (planDate != now.toLocalDate().toString()) return true
    if (endOfDayMinutes <= 0) return false // midnight: the date gate covers it
    return now.hour * 60 + now.minute >= endOfDayMinutes
}
