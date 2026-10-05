package com.markel.flowstate.core.domain.usecase.checkin

import com.markel.flowstate.core.domain.CheckinRepository
import com.markel.flowstate.core.domain.CheckinSnapshot
import com.markel.flowstate.core.domain.HabitMoodLog
import com.markel.flowstate.core.domain.HabitWithStatus
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.habits.GetAllBooleanEntriesUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetHabitsWithStatusUseCase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Builds the point-in-time [CheckinSnapshot] the AI brain will consume:
 * today's saved check-in (mood, comments, unexpected plans) + incomplete
 * tasks + habit status, read from the same repositories the rest of the app
 * uses — one consistent picture assembled in one place.
 *
 * Point-in-time by design (suspend + Flow.first()): the AI wants the state
 * at the moment it's invoked, not a live-updating Flow. Intended to be called
 * right after the check-in is saved; if today's check-in isn't saved yet,
 * snapshot.checkin comes back null rather than failing.
 */
class BuildCheckinSnapshotUseCase @Inject constructor(
    private val checkinRepository: CheckinRepository,
    private val taskRepository: TaskRepository,
    private val getHabitsWithStatus: GetHabitsWithStatusUseCase,
    private val getAllBooleanEntries: GetAllBooleanEntriesUseCase
) {
    suspend operator fun invoke(date: LocalDate = LocalDate.now()): CheckinSnapshot {
        val isoDate = date.toString()
        val habits = getHabitsWithStatus(date).first().filter { it.isDueToday }
        return CheckinSnapshot(
            date = isoDate,
            checkin = checkinRepository.getCheckinByDate(isoDate),
            tasks = taskRepository.getTasks().first().filter { !it.isDone },
            // Off-day and weekly-target-met habits never reach either
            // planner — "due today" is the single choke point for planning.
            habits = habits,
            habitMoods = habitMoods(date, habits)
        )
    }

    /**
     * The last [MOOD_CONTEXT_DAYS] days of ratings for the habits that opted
     * into mood logging — that flag is the user's consent, so it gates what
     * leaves the device here as well: a habit switched off today contributes
     * nothing even if older ratings still sit in habit_entries (the Mood tab
     * keeps its history; the AI's context doesn't).
     */
    private suspend fun habitMoods(
        date: LocalDate,
        habits: List<HabitWithStatus>
    ): Map<Int, List<HabitMoodLog>> {
        val enabledIds = habits
            .filter { it.habit.moodLoggingEnabled }
            .map { it.habit.id }
            .toSet()
        if (enabledIds.isEmpty()) return emptyMap()

        val sinceEpochDay = date.minusDays(MOOD_CONTEXT_DAYS - 1L).toEpochDay()
        return getAllBooleanEntries().first()
            .filter { it.habitId in enabledIds && it.mood != null && it.epochDay >= sinceEpochDay }
            .groupBy { it.habitId }
            .mapValues { (_, entries) ->
                entries.sortedBy { it.epochDay }.map { entry ->
                    HabitMoodLog(
                        date = LocalDate.ofEpochDay(entry.epochDay).toString(),
                        mood = entry.mood!!
                    )
                }
            }
    }

    private companion object {
        /** How many days of per-habit mood history ride along in the prompt. */
        const val MOOD_CONTEXT_DAYS = 7
    }
}
