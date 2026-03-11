package com.markel.flowstate.core.domain.usecase.tasks

import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) = repository.deleteTask(task)
}