package com.markel.flowstate.core.domain.usecase.tasks

import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import javax.inject.Inject

class ToggleTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        val newIsDone = !task.isDone
        val newCompletedAt = if (newIsDone) System.currentTimeMillis() else null
        repository.upsertTask(task.copy(isDone = newIsDone, completedAt = newCompletedAt))
    }
}