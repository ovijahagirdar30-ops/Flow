package com.markel.flowstate.core.domain.usecase.checkin

import com.markel.flowstate.core.domain.CheckinItem
import com.markel.flowstate.core.domain.CheckinItemType
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.habits.GetHabitsWithStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Combines today's incomplete Tasks and Habits into one unified list — this
 * is what the check-in screen (and later, the AI plan generator) reads from,
 * so "what's relevant tonight" only needs figuring out in one place.
 */
class GetCheckinItemsUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val getHabitsWithStatus: GetHabitsWithStatusUseCase
) {
    operator fun invoke(): Flow<List<CheckinItem>> =
        combine(taskRepository.getTasks(), getHabitsWithStatus()) { tasks, habitsWithStatus ->
            val taskItems = tasks
                .filter { !it.isDone }
                .map { CheckinItem(CheckinItemType.TASK, it.id, it.title, isCompleted = false) }

            val habitItems = habitsWithStatus
                .filter { !it.isCompletedToday }
                .map { CheckinItem(CheckinItemType.HABIT, it.habit.id, it.habit.name, isCompleted = false) }

            taskItems + habitItems
        }
}