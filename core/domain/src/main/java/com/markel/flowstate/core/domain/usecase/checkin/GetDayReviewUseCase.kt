package com.markel.flowstate.core.domain.usecase.checkin

import com.markel.flowstate.core.domain.DayReview
import com.markel.flowstate.core.domain.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Buckets every task into the night check-in's three sections — completed
 * today / pending / pushed to tomorrow — using device-local midnight as the
 * day boundary (see [DayReview] for the exact bucket rules).
 *
 * [nowMillis] is injectable so tests pin the clock instead of depending on
 * the wall time they happen to run in.
 */
class GetDayReviewUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    suspend operator fun invoke(nowMillis: Long = System.currentTimeMillis()): DayReview {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val tasks = taskRepository.getTasks().first()

        val completedToday = tasks
            .filter { task ->
                task.isDone && (task.completedAt ?: 0L) in startOfToday until startOfTomorrow
            }
            .sortedByDescending { it.completedAt }

        val open = tasks.filter { !it.isDone }

        // Due-date sort doubles as priority: real dates land overdue-first,
        // then due-today; the unscheduled sentinel sorts them to the end.
        val pending = open
            .filter { it.dueDate == null || it.dueDate < startOfTomorrow }
            .sortedBy { it.dueDate ?: Long.MAX_VALUE }

        val pushedToTomorrow = open
            .filter { it.dueDate != null && it.dueDate >= startOfTomorrow }
            .sortedBy { it.dueDate }

        return DayReview(
            date = today.toString(),
            completedToday = completedToday,
            pending = pending,
            pushedToTomorrow = pushedToTomorrow,
        )
    }
}
