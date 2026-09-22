package com.markel.flowstate.core.domain.usecase.checkin

import com.markel.flowstate.core.domain.CheckinRepository
import com.markel.flowstate.core.domain.CheckinSnapshot
import com.markel.flowstate.core.domain.TaskRepository
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
    private val getHabitsWithStatus: GetHabitsWithStatusUseCase
) {
    suspend operator fun invoke(date: LocalDate = LocalDate.now()): CheckinSnapshot {
        val isoDate = date.toString()
        return CheckinSnapshot(
            date = isoDate,
            checkin = checkinRepository.getCheckinByDate(isoDate),
            tasks = taskRepository.getTasks().first().filter { !it.isDone },
            habits = getHabitsWithStatus(date).first()
        )
    }
}
