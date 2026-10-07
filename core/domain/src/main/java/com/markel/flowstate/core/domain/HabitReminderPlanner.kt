package com.markel.flowstate.core.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * When a habit's reminder should fire — the pure time math behind habit
 * notifications, deliberately mirroring the due-ness gate in
 * `GetHabitsWithStatusUseCase.isDueToday` so the notification and the list can
 * never disagree about whether today counts:
 *
 *  - the weekday must be in the schedule's [HabitSchedule.days];
 *  - an already-completed day never notifies;
 *  - a times-per-week habit (weeklyTarget) keeps notifying **every scheduled
 *    day of the week until the week's target is met**, then rests until the
 *    next ISO week (Mon–Sun) — exactly what "only chose a frequency" means:
 *    `"DAILY:3"` still schedules all 7 weekdays, so all 7 remind until 3
 *    completions land.
 *
 * Stateless and Android-free so it can be unit-tested on the JVM.
 */
object HabitReminderPlanner {

    /** Should the reminder fire on [date]? See the class doc for the rules. */
    fun shouldNotifyOn(
        schedule: HabitSchedule,
        completedDays: Set<Long>,
        date: LocalDate,
    ): Boolean {
        if (!schedule.isScheduledOn(date)) return false
        if (date.toEpochDay() in completedDays) return false
        val target = schedule.weeklyTarget ?: return true
        // Target met for this ISO week → the habit rests until next Monday.
        return HabitStreaks.completionsInWeek(
            schedule = schedule,
            completedDays = completedDays,
            weekStart = date.with(DayOfWeek.MONDAY),
            until = date,
        ) < target
    }

    /**
     * First date on or after [from] whose reminder may fire, or null when the
     * schedule can never fire again (impossible for valid schedules — a weekly
     * target resets every ISO week — but the lookahead is bounded anyway).
     * 21 days covers a met target in the worst case (late in a week) plus a
     * full spare week.
     */
    fun nextFireDate(
        schedule: HabitSchedule,
        completedDays: Set<Long>,
        from: LocalDate,
    ): LocalDate? {
        var day = from
        repeat(MAX_LOOKAHEAD_DAYS) {
            if (shouldNotifyOn(schedule, completedDays, day)) return day
            day = day.plusDays(1)
        }
        return null
    }

    /**
     * Epoch millis of the next reminder strictly after [now] — today's slot is
     * skipped once it has passed, which is what re-arms the chain after an
     * alarm fires. Null = nothing left to fire for this habit.
     */
    fun nextTriggerMillis(
        schedule: HabitSchedule,
        completedDays: Set<Long>,
        minuteOfDay: Int,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        val minute = minuteOfDay.coerceIn(0, 23 * 60 + 59)
        fun triggerOn(day: LocalDate): Long =
            day.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()

        var day = nextFireDate(schedule, completedDays, LocalDate.now(zone)) ?: return null
        var millis = triggerOn(day)
        while (millis <= now) {
            day = nextFireDate(schedule, completedDays, day.plusDays(1)) ?: return null
            millis = triggerOn(day)
        }
        return millis
    }

    private const val MAX_LOOKAHEAD_DAYS = 21
}
