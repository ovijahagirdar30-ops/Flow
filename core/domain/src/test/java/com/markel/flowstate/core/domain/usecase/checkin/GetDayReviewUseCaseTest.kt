package com.markel.flowstate.core.domain.usecase.checkin

import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetDayReviewUseCaseTest {

    private val repository: TaskRepository = mockk()
    private val useCase = GetDayReviewUseCase(repository)

    private val zone = ZoneId.systemDefault()
    private val today: LocalDate = LocalDate.of(2026, 9, 25)
    private val now: Long = today.atTime(21, 0).atZone(zone).toInstant().toEpochMilli()
    private val startOfToday: Long = today.atStartOfDay(zone).toInstant().toEpochMilli()
    private val startOfTomorrow: Long = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun task(
        id: Int,
        title: String,
        isDone: Boolean = false,
        completedAt: Long? = null,
        dueDate: Long? = null,
    ) = Task(id = id, title = title, isDone = isDone, completedAt = completedAt, dueDate = dueDate)

    @Test
    fun invoke_splitsTasksIntoTheThreeBuckets() = runTest {
        val tasks = listOf(
            task(1, "done today", isDone = true, completedAt = startOfToday + 3_600_000),
            task(2, "done yesterday", isDone = true, completedAt = startOfToday - 1),
            task(3, "overdue", dueDate = startOfToday - 86_400_000),
            task(4, "due today", dueDate = startOfToday + 3_600_000),
            task(5, "unscheduled"),
            task(6, "due tomorrow", dueDate = startOfTomorrow + 1_000),
            task(7, "due next week", dueDate = startOfTomorrow + 5 * 86_400_000),
        )
        every { repository.getTasks() } returns flowOf(tasks)

        val review = useCase(nowMillis = now)

        assertEquals("2026-09-25", review.date)
        assertEquals(listOf("done today"), review.completedToday.map { it.title })
        // Overdue first, then due-today, unscheduled last.
        assertEquals(listOf("overdue", "due today", "unscheduled"), review.pending.map { it.title })
        assertEquals(listOf("due tomorrow", "due next week"), review.pushedToTomorrow.map { it.title })
        // "done yesterday" belongs to none of today's buckets.
        val all = review.completedToday + review.pending + review.pushedToTomorrow
        assertEquals(emptyList<String>(), all.filter { it.title == "done yesterday" }.map { it.title })
    }

    @Test
    fun invoke_legacyDoneTasksWithoutTimestamp_areExcluded() = runTest {
        every { repository.getTasks() } returns flowOf(
            listOf(task(1, "ancient done", isDone = true, completedAt = null))
        )

        val review = useCase(nowMillis = now)

        assertEquals(emptyList<Task>(), review.completedToday)
        assertEquals(emptyList<Task>(), review.pending)
        assertEquals(emptyList<Task>(), review.pushedToTomorrow)
    }

    @Test
    fun stats_countsAllBucketsAndCapsTitles() = runTest {
        val tasks = (1..7).map { task(it, "t$it", isDone = true, completedAt = startOfToday + it) } +
            task(8, "open", dueDate = startOfToday) +
            task(9, "later", dueDate = startOfTomorrow)
        every { repository.getTasks() } returns flowOf(tasks)

        val stats = useCase(nowMillis = now).stats()

        assertEquals(7, stats.completedCount)
        assertEquals(5, stats.completedTitles.size) // capped for the prompt
        assertEquals(1, stats.pendingCount)
        assertEquals(1, stats.pushedCount)
        assertEquals("2026-09-25", stats.date)
    }
}
