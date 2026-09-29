package com.markel.flowstate.core.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * When a habit is actually due: the weekdays it is scheduled on, plus an
 * optional weekly completion target ("3×/week"). The two models combine —
 * [days] says which days the habit *may* happen, [weeklyTarget] says how
 * many of those days must be completed each week (null = every scheduled
 * day counts, the plain "daily" / "specific days" cases).
 *
 * Persists in the existing `habits.frequency` TEXT column, so no Room
 * migration and backups keep working untouched:
 *
 *   "DAILY"        all days, every scheduled day counts (legacy default)
 *   "MO,WE,FR"     those weekdays only
 *   "DAILY:3"      any 3 days of the week
 *   "MO,WE,FR:2"   two of those three days
 *
 * Unknown or blank legacy values (e.g. the never-used "WEEKLY") decode to
 * the daily default instead of throwing, matching the lenient backup parser.
 */
data class HabitSchedule(
    /** Non-empty set of weekdays this habit may be completed on (ISO codes). */
    val days: Set<DayOfWeek>,
    /** Completions required per week among [days]; null = every scheduled day counts. */
    val weeklyTarget: Int? = null,
) {
    init {
        require(days.isNotEmpty()) { "habit schedule needs at least one day" }
        if (weeklyTarget != null) {
            require(weeklyTarget in 1..days.size) { "weekly target must fit the scheduled days" }
        }
    }

    /** True when [date]'s weekday is one of the scheduled days. */
    fun isScheduledOn(date: LocalDate): Boolean = date.dayOfWeek in days

    /** True for the plain everyday habit — nothing to configure. */
    val isDaily: Boolean
        get() = days == ALL_DAYS && weeklyTarget == null

    /** Stable storage form — see the class doc for the grammar. */
    fun encode(): String {
        val daysPart = if (days == ALL_DAYS) DAILY_CODE
        else DAY_CODES.keys.filter { it in days }.joinToString(",") { DAY_CODES.getValue(it) }
        val target = weeklyTarget?.let { ":$it" }.orEmpty()
        return daysPart + target
    }

    companion object {
        /** ISO weekday abbreviations, Monday-first — what [DayOfWeek] names them. */
        private val DAY_CODES = linkedMapOf(
            DayOfWeek.MONDAY to "MO",
            DayOfWeek.TUESDAY to "TU",
            DayOfWeek.WEDNESDAY to "WE",
            DayOfWeek.THURSDAY to "TH",
            DayOfWeek.FRIDAY to "FR",
            DayOfWeek.SATURDAY to "SA",
            DayOfWeek.SUNDAY to "SU",
        )

        private val ALL_DAYS: Set<DayOfWeek> = DAY_CODES.keys.toSet()
        private const val DAILY_CODE = "DAILY"

        /** The everyday default every existing habit already decodes to. */
        val DAILY: HabitSchedule = HabitSchedule(ALL_DAYS)

        /**
         * Lenient decode of the stored `frequency` string. Never throws:
         * blank/unknown day parts fall back to all days, a target that does
         * not fit the scheduled days is clamped down.
         */
        fun decode(raw: String?): HabitSchedule {
            val trimmed = raw?.trim().orEmpty()
            val separator = trimmed.lastIndexOf(':')
            val daysPart = if (separator >= 0) trimmed.substring(0, separator) else trimmed
            val targetPart = if (separator >= 0) trimmed.substring(separator + 1) else null

            val days = parseDays(daysPart)
            val target = targetPart?.toIntOrNull()?.coerceIn(1, days.size)
            return HabitSchedule(days, target)
        }

        private fun parseDays(part: String): Set<DayOfWeek> {
            if (part.isBlank() || part.equals(DAILY_CODE, ignoreCase = true)) return ALL_DAYS
            val codesToDays = DAY_CODES.entries.associate { (day, code) -> code to day }
            return part.split(',')
                .mapNotNull { codesToDays[it.trim().uppercase()] }
                .toSet()
                .ifEmpty { ALL_DAYS }
        }
    }
}
