package com.markel.flowstate.core.notifications

import com.markel.flowstate.core.domain.SubTask
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildAlarmItemsTest {

    private val taskRepository: TaskRepository = mockk()

    // ── Tasks with reminders ────────────────────────────────────────────────

    @Test
    fun taskWithFutureReminderIsIncluded() = runTest {
        val futureTime = System.currentTimeMillis() + 60_000L
        val task = Task(id = 1, title = "Future", isDone = false, reminderTime = futureTime)

        coEvery { taskRepository.getTasks() } returns flowOf(listOf(task))

        val result = buildAlarmItems(taskRepository)

        assertEquals(1, result.size)
        assertEquals(1, result[0].requestCode)
        assertEquals("Future", result[0].title)
        assertEquals(futureTime, result[0].triggerMillis)
        assertEquals(false, result[0].isSubtask)
    }

    @Test
    fun taskWithPastReminderIsIncluded() = runTest {
        val pastTime = System.currentTimeMillis() - 60_000L
        val task = Task(id = 2, title = "Past", isDone = false, reminderTime = pastTime)

        coEvery { taskRepository.getTasks() } returns flowOf(listOf(task))

        val result = buildAlarmItems(taskRepository)

        assertEquals(1, result.size)
        assertEquals(pastTime, result[0].triggerMillis)
    }

    @Test
    fun completedTaskIsExcludedEvenWithReminderTime() = runTest {
        val futureTime = System.currentTimeMillis() + 60_000L
        val task = Task(id = 1, title = "Done", isDone = true, reminderTime = futureTime)

        coEvery { taskRepository.getTasks() } returns flowOf(listOf(task))

        val result = buildAlarmItems(taskRepository)

        assertTrue(result.isEmpty())
    }

    @Test
    fun taskWithoutReminderTimeIsExcluded() = runTest {
        val task = Task(id = 1, title = "No reminder", isDone = false, reminderTime = null)

        coEvery { taskRepository.getTasks() } returns flowOf(listOf(task))

        val result = buildAlarmItems(taskRepository)

        assertTrue(result.isEmpty())
    }

    // ── SubTask handling ─────────────────────────────────────────────────────

    @Test
    fun subtaskWithReminderIsIncluded() = runTest {
        val futureTime = System.currentTimeMillis() + 60_000L
        val subTask = SubTask(id = "sub-1", title = "Sub future", reminderTime = futureTime)
        val task = Task(id = 1, title = "Parent", isDone = false, subTasks = listOf(subTask))

        coEvery { taskRepository.getTasks() } returns flowOf(listOf(task))

        val result = buildAlarmItems(taskRepository)

        assertEquals(1, result.size)
        assertTrue(result[0].isSubtask)
        assertEquals("sub-1".hashCode(), result[0].requestCode)
        assertEquals("sub-1", result[0].subTaskId)
    }

    @Test
    fun completedSubtaskIsExcluded() = runTest {
        val futureTime = System.currentTimeMillis() + 60_000L
        val doneSubTask = SubTask(id = "sub-done", title = "Done sub", isDone = true, reminderTime = futureTime)
        val task = Task(id = 1, title = "Parent", isDone = false, subTasks = listOf(doneSubTask))

        coEvery { taskRepository.getTasks() } returns flowOf(listOf(task))

        val result = buildAlarmItems(taskRepository)

        assertTrue(result.isEmpty())
    }

    @Test
    fun subtaskWithoutReminderIsExcluded() = runTest {
        val subTask = SubTask(id = "sub-no-reminder", title = "No reminder sub", reminderTime = null)
        val task = Task(id = 1, title = "Parent", isDone = false, subTasks = listOf(subTask))

        coEvery { taskRepository.getTasks() } returns flowOf(listOf(task))

        val result = buildAlarmItems(taskRepository)

        assertTrue(result.isEmpty())
    }

    // ── Combined task + subtask scenarios ────────────────────────────────────

    @Test
    fun taskAndSubtaskRemindersAreBothIncluded() = runTest {
        val now = System.currentTimeMillis()
        val subFuture = SubTask(id = "s1", title = "Sub future", reminderTime = now + 30_000L)
        val task = Task(
            id = 1, title = "Parent", isDone = false,
            reminderTime = now + 60_000L,
            subTasks = listOf(subFuture)
        )

        coEvery { taskRepository.getTasks() } returns flowOf(listOf(task))

        val result = buildAlarmItems(taskRepository)

        // 1 task reminder + 1 subtask reminder = 2 items
        assertEquals(2, result.size)
    }

    @Test
    fun multipleTasksWithMixedReminders() = runTest {
        val now = System.currentTimeMillis()
        val tasks = listOf(
            Task(id = 1, title = "T1", isDone = false, reminderTime = now + 10_000L),
            Task(id = 2, title = "T2", isDone = false, reminderTime = now - 10_000L),
            Task(id = 3, title = "T3", isDone = false, reminderTime = null),
            Task(id = 4, title = "T4", isDone = true, reminderTime = now + 10_000L),
            Task(
                id = 5, title = "T5", isDone = false,
                subTasks = listOf(
                    SubTask(id = "s5a", title = "S5a", reminderTime = now + 20_000L),
                    SubTask(id = "s5b", title = "S5b", isDone = true, reminderTime = now + 20_000L)
                )
            )
        )

        coEvery { taskRepository.getTasks() } returns flowOf(tasks)

        val result = buildAlarmItems(taskRepository)

        // T1 (future), T2 (past), s5a (future) → 3 items
        // Excluded: T3 (no reminder), T4 (done), s5b (done subtask)
        assertEquals(3, result.size)
    }

    // ── AlarmItem field correctness ──────────────────────────────────────────

    @Test
    fun taskAlarmItemHasCorrectFields() = runTest {
        val futureTime = System.currentTimeMillis() + 60_000L
        val task = Task(
            id = 42, title = "My Task", description = "My Desc",
            isDone = false, reminderTime = futureTime
        )

        coEvery { taskRepository.getTasks() } returns flowOf(listOf(task))

        val result = buildAlarmItems(taskRepository)

        val item = result[0]
        assertEquals(42, item.requestCode)
        assertEquals("My Task", item.title)
        assertEquals("My Desc", item.description)
        assertEquals(futureTime, item.triggerMillis)
        assertEquals(false, item.isSubtask)
        assertEquals(null, item.subTaskId)
    }

    @Test
    fun subtaskAlarmItemHasCorrectFields() = runTest {
        val futureTime = System.currentTimeMillis() + 60_000L
        val subTask = SubTask(id = "uuid-123", title = "My Sub", reminderTime = futureTime)
        val task = Task(id = 1, title = "Parent", isDone = false, subTasks = listOf(subTask))

        coEvery { taskRepository.getTasks() } returns flowOf(listOf(task))

        val result = buildAlarmItems(taskRepository)

        val item = result[0]
        assertEquals("uuid-123".hashCode(), item.requestCode)
        assertEquals("My Sub", item.title)
        assertEquals(null, item.description) // subtasks don't pass description
        assertEquals(futureTime, item.triggerMillis)
        assertEquals(true, item.isSubtask)
        assertEquals("uuid-123", item.subTaskId)
    }

    // ── Empty edge case ──────────────────────────────────────────────────────

    @Test
    fun emptyTaskListReturnsEmptyResult() = runTest {
        coEvery { taskRepository.getTasks() } returns flowOf(emptyList())

        val result = buildAlarmItems(taskRepository)

        assertTrue(result.isEmpty())
    }
}
