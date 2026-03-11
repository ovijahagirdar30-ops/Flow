package com.markel.flowstate.core.domain

import com.markel.flowstate.core.domain.usecase.tasks.DeleteTaskUseCase
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteTaskUseCaseTest {

    private val repository: TaskRepository = mockk(relaxed = true)
    private val useCase = DeleteTaskUseCase(repository)

    @Test
    fun invoke_callsRepositoryDelete() = runTest {
        val task = Task(id = 1, title = "Demo", isDone = false)

        useCase(task)

        coVerify { repository.deleteTask(task) }
    }
}