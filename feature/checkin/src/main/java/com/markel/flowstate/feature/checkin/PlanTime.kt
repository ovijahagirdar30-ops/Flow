package com.markel.flowstate.feature.checkin

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Renders a stored plan time — zero-padded 24-hour "HH:mm", the format the
 * planners emit, persist, and (for Gemini) sort on — as a 12-hour clock
 * string like "6:30 PM". Display only: storage and the Gemini wire format
 * stay 24-hour, so no migration or prompt/schema change is involved.
 * Malformed input falls back to the raw string instead of crashing the row.
 */
internal fun formatPlanTime(hhMm: String): String = runCatching {
    LocalTime.parse(hhMm.trim()).format(TWELVE_HOUR)
}.getOrDefault(hhMm)

// Locale.getDefault keeps the AM/PM marker localized; the h/a pattern itself
// guarantees a 12-hour clock regardless of the device's 24-hour preference.
private val TWELVE_HOUR: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
