package com.markel.flowstate.core.domain

import com.markel.flowstate.core.domain.usecase.tasks.ToggleTaskUseCase
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ToggleTaskUseCaseTest {

    private val repository: TaskRepository = mockk(relaxed = true)
    private val useCase = ToggleTaskUseCase(repository)

    @Test
    fun invoke_setsIsDone_completedAt_andClearsReminderTime() = runTest {
        val task = Task(
            id = 1, title = "Demo", isDone = false, completedAt = null,
            reminderTime = 12345L,
            subTasks = listOf(SubTask(id = "s1", title = "Sub", reminderTime = 9999L))
        )

        useCase(task)

        coVerify {
            repository.upsertTask(match { updated ->
                updated.isDone &&
                    updated.completedAt != null &&
                    updated.reminderTime == null &&
                    updated.subTasks.all { it.reminderTime == null }
            })
        }
    }

    // Just to check that marking a task as pending doesn't recover any reminder.
    // This is an intended behavior, after completing a task the reminder is "consumed".
    // The only edge case when you would like to recover the reminder is if you accidentally completed
    // a task, but most people if the "uncomplete" a task won't remember any reminder and would be
    // confusing to recover it.
    @Test
    fun invoke_uncompleting_preservesReminderTime() = runTest {
        val task = Task(
            id = 1, title = "Demo", isDone = true, completedAt = 1000L,
            reminderTime = null,
            subTasks = listOf(SubTask(id = "s1", title = "Sub", reminderTime = null))
        )

        useCase(task)

        coVerify {
            repository.upsertTask(match { updated ->
                !updated.isDone && updated.completedAt == null && updated.reminderTime == null
            })
        }
    }
}