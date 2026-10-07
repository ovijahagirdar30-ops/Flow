package com.markel.flowstate.core.notifications

import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitEntryFlat
import com.markel.flowstate.core.domain.HabitNumericEntry
import com.markel.flowstate.core.domain.HabitReminderPlanner
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitType
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Books habit reminders on top of the task-focused [ReminderScheduler].
 *
 * One alarm per habit at a time: the next date the schedule says the habit is
 * still wanted (mirroring the list's isDueToday — scheduled weekday, weekly
 * target not yet met, not already done today). When it fires, [consumeAlarm]
 * shows it if it is still wanted and books the following one, which is what
 * makes "every scheduled day until the weekly target is met, then rest until
 * next week" re-evaluate against fresh completions every single day.
 *
 * Call sites: HabitViewModel (add/edit/complete/delete), BootReceiver and
 * FlowViewModel's resume — the same self-healing the task reminders get.
 */
@Singleton
class HabitReminderScheduler @Inject constructor(
    private val reminderScheduler: ReminderScheduler,
    private val habitRepository: HabitRepository,
) {

    /** What a fired habit alarm wants shown — null means stay silent. */
    data class Notice(val notificationId: Int, val title: String)

    /** Re-books (or cancels) the one pending alarm for a single habit. */
    suspend fun reschedule(habitId: Int, now: Long = System.currentTimeMillis()) {
        val habit = habitRepository.getHabitById(habitId) ?: return reminderScheduler.cancelHabit(habitId)
        book(habit, completedDays(habit), now)
    }

    /** Re-books every habit from scratch — also drops disabled/finished ones. */
    suspend fun rescheduleAll(now: Long = System.currentTimeMillis()) {
        val habits = habitRepository.getHabits().first()
        if (habits.isEmpty()) return
        val boolEntries = habitRepository.getAllEntries().first()
        val numericEntries = habitRepository.getAllNumericEntries().first()
        habits.forEach { habit ->
            book(habit, completedDays(habit, boolEntries, numericEntries), now)
        }
    }

    /** Dropped when a habit is deleted or its reminder is switched off. */
    fun cancel(habitId: Int) {
        reminderScheduler.cancelHabit(habitId)
    }

    /**
     * The alarm fired: returns the notification payload if the habit is still
     * due right now (weekly target rule re-checked against the DB), then
     * re-arms the next occurrence either way — today's slot is spent.
     */
    suspend fun consumeAlarm(habitId: Int, now: Long = System.currentTimeMillis()): Notice? {
        val habit = habitRepository.getHabitById(habitId)
        if (habit == null || !habit.reminderEnabled || habit.reminderMinuteOfDay == null) {
            reminderScheduler.cancelHabit(habitId)
            return null
        }
        val completed = completedDays(habit)
        val notice = if (HabitReminderPlanner.shouldNotifyOn(habit.schedule, completed, LocalDate.now())) {
            Notice(ReminderScheduler.habitRequestCode(habitId), habit.name)
        } else {
            null
        }
        book(habit, completed, now)
        return notice
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun book(habit: Habit, completedDays: Set<Long>, now: Long) {
        val minuteOfDay = habit.reminderMinuteOfDay
        if (!habit.reminderEnabled || minuteOfDay == null) {
            reminderScheduler.cancelHabit(habit.id)
            return
        }
        val trigger = HabitReminderPlanner.nextTriggerMillis(
            schedule = habit.schedule,
            completedDays = completedDays,
            minuteOfDay = minuteOfDay,
            now = now,
        )
        if (trigger == null) {
            reminderScheduler.cancelHabit(habit.id)
        } else {
            reminderScheduler.scheduleHabit(habit.id, habit.name, trigger)
        }
    }

    private suspend fun completedDays(habit: Habit): Set<Long> = completedDays(
        habit = habit,
        boolEntries = habitRepository.getAllEntries().first(),
        numericEntries = habitRepository.getAllNumericEntries().first()
    )

    /**
     * Days that count as completed — the exact validity rule
     * `GetHabitsWithStatusUseCase` uses, so a reminder never fires for a day
     * the list already considers done.
     */
    private fun completedDays(
        habit: Habit,
        boolEntries: List<HabitEntryFlat>,
        numericEntries: List<HabitNumericEntry>,
    ): Set<Long> = when (habit.habitType) {
        HabitType.BOOLEAN -> boolEntries
            .filter { it.habitId == habit.id }
            .map { it.epochDay }
            .toSet()

        HabitType.NUMERIC -> numericEntries
            .filter { it.habitId == habit.id }
            // Same validity rule the list uses: a value only counts once it
            // reaches the target (or any value when there is no target).
            .filter { entry -> habit.targetValue?.let { entry.value >= it } ?: true }
            .map { it.date.toEpochDay() }
            .toSet()
    }
}
