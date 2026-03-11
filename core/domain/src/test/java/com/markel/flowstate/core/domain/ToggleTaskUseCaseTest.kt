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
    fun invoke_setsIsDoneAndCompletedAt() = runTest {
        val task = Task(id = 1, title = "Demo", isDone = false, completedAt = null)

        useCase(task)

        coVerify {
            repository.upsertTask(match { updated ->
                updated.isDone && updated.completedAt != null
            })
        }
    }
}