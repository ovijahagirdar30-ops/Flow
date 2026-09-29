package com.markel.flowstate.core.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Shared streak math for habit schedules — ONE implementation used by both
 * [GetHabitsWithStatusUseCase] (lists, check-in, header) and
 * `HabitDetailViewModel` (detail screen), so the numbers can never disagree.
 *
 * The unit follows the schedule:
 *  - no weekly target → consecutive **scheduled days** (off-days are skipped,
 *    never counted as misses; a missed scheduled day breaks the run);
 *  - weekly target → consecutive **ISO weeks (Mon–Sun)** that hit the target,
 *    where the in-progress week can't break the streak until it ends.
 */
object HabitStreaks {

    /** Consecutive scheduled days completed, walking back from [from]. */
    fun currentRun(schedule: HabitSchedule, completedDays: Set<Long>, from: LocalDate): Int {
        if (completedDays.isEmpty()) return 0
        var day = from
        if (schedule.isScheduledOn(day) && day.toEpochDay() !in completedDays) {
            day = day.minusDays(1)
        }
        var streak = 0
        while (true) {
            if (!schedule.isScheduledOn(day)) {
                day = day.minusDays(1)
                continue
            }
            if (day.toEpochDay() !in completedDays) break
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    /**
     * Longest run of consecutive scheduled days ever completed. Walks the
     * calendar between the first and last completion so a missed scheduled
     * day ends the run even when it sits between two completions.
     */
    fun bestRun(schedule: HabitSchedule, completedDays: Set<Long>): Int {
        if (completedDays.isEmpty()) return 0
        var best = 0
        var current = 0
        var day = LocalDate.ofEpochDay(completedDays.min())
        val last = LocalDate.ofEpochDay(completedDays.max())
        while (!day.isAfter(last)) {
            when {
                !schedule.isScheduledOn(day) -> Unit // off-day: neither helps nor hurts
                day.toEpochDay() in completedDays -> {
                    current++
                    if (current > best) best = current
                }
                else -> current = 0
            }
            day = day.plusDays(1)
        }
        return best
    }

    /** Consecutive ISO weeks (Mon–Sun) that hit the weekly target. */
    fun currentWeeks(schedule: HabitSchedule, completedDays: Set<Long>, from: LocalDate): Int {
        val target = schedule.weeklyTarget ?: return 0
        val thisWeek = from.with(DayOfWeek.MONDAY)
        val currentWeekMet = completionsInWeek(schedule, completedDays, thisWeek, until = from) >= target
        var weekStart = if (currentWeekMet) thisWeek else thisWeek.minusWeeks(1)
        var streak = 0
        while (completionsInWeek(schedule, completedDays, weekStart, until = null) >= target) {
            streak++
            weekStart = weekStart.minusWeeks(1)
        }
        return streak
    }

    /** Longest run of consecutive ISO weeks that hit the weekly target. */
    fun bestWeeks(schedule: HabitSchedule, completedDays: Set<Long>): Int {
        if (completedDays.isEmpty()) return 0
        val firstWeek = LocalDate.ofEpochDay(completedDays.min()).with(DayOfWeek.MONDAY)
        val lastWeek = LocalDate.ofEpochDay(completedDays.max()).with(DayOfWeek.MONDAY)
        var best = 0
        var current = 0
        var weekStart = firstWeek
        while (!weekStart.isAfter(lastWeek)) {
            if (completionsInWeek(schedule, completedDays, weekStart, until = null) >= (schedule.weeklyTarget ?: 0)) {
                current++
                if (current > best) best = current
            } else {
                current = 0
            }
            weekStart = weekStart.plusWeeks(1)
        }
        return best
    }

    /** Current streak in the schedule's own unit (days, or weeks with a target). */
    fun current(schedule: HabitSchedule, completedDays: Set<Long>, from: LocalDate): Int =
        if (schedule.weeklyTarget != null) currentWeeks(schedule, completedDays, from)
        else currentRun(schedule, completedDays, from)

    /** Best-ever streak in the schedule's own unit. */
    fun best(schedule: HabitSchedule, completedDays: Set<Long>): Int =
        if (schedule.weeklyTarget != null) bestWeeks(schedule, completedDays)
        else bestRun(schedule, completedDays)

    /** Completions on scheduled days of [weekStart]'s Mon–Sun week, up to [until] (null = whole week). */
    fun completionsInWeek(
        schedule: HabitSchedule,
        completedDays: Set<Long>,
        weekStart: LocalDate,
        until: LocalDate?,
    ): Int {
        var count = 0
        var day = weekStart
        val weekEnd = weekStart.plusDays(6)
        while (!day.isAfter(weekEnd)) {
            val beyondCutoff = until != null && day.isAfter(until)
            if (!beyondCutoff && schedule.isScheduledOn(day) && day.toEpochDay() in completedDays) {
                count++
            }
            day = day.plusDays(1)
        }
        return count
    }
}
