package com.markel.flowstate.core.domain.usecase.tasks

import com.markel.flowstate.core.domain.Category
import com.markel.flowstate.core.domain.Priority
import com.markel.flowstate.core.domain.Task
import com.markel.flowstate.core.domain.TaskRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Creates a task through the exact same conventions the manual Tasks screen
 * uses: General category, top-of-list position (min position − 1, same as
 * TaskViewModel.addTask), no reminder. Deliberately lives in core:domain so
 * anything can create tasks through one path — the check-in popup today, the
 * AI evening plan later.
 *
 * Note: if a caller passes a future [reminderTime], it must schedule the
 * reminder itself via core:notifications' ReminderScheduler — core:domain
 * can't depend on that module (dependency direction).
 *
 * @return the generated task id, or null if [title] was blank.
 */
class AddTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String = "",
        priority: Priority = Priority.NOTHING,
        dueDate: Long? = null,
        reminderTime: Long? = null,
        categoryId: Int? = Category.GENERAL_ID
    ): Long? {
        if (title.isBlank()) return null

        val minPosition = repository.getTasks().first()
            .filter { it.categoryId == categoryId }
            .minOfOrNull { it.position } ?: 0

        return repository.upsertTask(
            Task(
                title = title.trim(),
                description = description,
                isDone = false,
                position = minPosition - 1,
                priority = priority,
                dueDate = dueDate,
                reminderTime = reminderTime,
                categoryId = categoryId,
                subTasks = emptyList()
            )
        )
    }
}
