package com.markel.flowstate.feature.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.EveningPlanRepository
import com.markel.flowstate.core.domain.PlanBlockKind
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.tasks.ToggleTaskUseCase
import com.markel.flowstate.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanUiState(
    /** True until the agreed plan (and its ticks) have been read from Room. */
    val isLoading: Boolean = true,
    /** The most recently agreed plan, or null if the user never agreed to one. */
    val plan: EveningPlan? = null,
    /** Persisted ticked block indexes for [plan]'s date. */
    val checkedIndexes: Set<Int> = emptySet(),
)

/**
 * Backs the Plan checklist tab: loads the most recently AGREED evening plan
 * plus its persisted ticks, and owns every write of the tick state
 * (`evening_plans.checkedIndexesJson` — the column landed with the table, so
 * this needed no migration).
 *
 * Ticking a TASK block mirrors onto the real task it maps to
 * ([PlanBlock.referenceId] → [ToggleTaskUseCase]), including the reminder
 * cancellation the Calendar/Flow screens do on completion. HABIT and free
 * blocks (meals, rest, …) are visual-only ticks: their referenceId never
 * reaches a task write.
 */
@HiltViewModel
class PlanViewModel @Inject constructor(
    private val planRepository: EveningPlanRepository,
    private val taskRepository: TaskRepository,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val plan = planRepository.latestAgreedPlan()
            val checked = plan
                ?.let { planRepository.checkedIndexes(it.date) }
                .orEmpty()
                .toSet()
            _uiState.value = PlanUiState(
                isLoading = false,
                plan = plan,
                checkedIndexes = checked,
            )
        }
    }

    /**
     * Flips block [index]: updates the UI state immediately, persists the new
     * tick set for the plan's date, and — when the block maps to a task —
     * toggles the task itself (only if its done-state actually differs, so a
     * stale tick can never flip a task the user changed elsewhere).
     */
    fun toggleBlock(index: Int) {
        val current = _uiState.value
        val plan = current.plan ?: return
        if (index !in plan.blocks.indices) return

        val checked = if (index in current.checkedIndexes) {
            current.checkedIndexes - index
        } else {
            current.checkedIndexes + index
        }
        _uiState.value = current.copy(checkedIndexes = checked)

        viewModelScope.launch {
            planRepository.setCheckedIndexes(plan.date, checked.toList().sorted())

            val block = plan.blocks[index]
            val referenceId = block.referenceId
            if (block.kind == PlanBlockKind.TASK && referenceId != null) {
                val task = taskRepository.getTaskById(referenceId) ?: return@launch
                val shouldBeDone = index in checked
                if (task.isDone != shouldBeDone) {
                    toggleTaskUseCase(task)
                    if (shouldBeDone) {
                        reminderScheduler.cancel(task.id)
                        task.subTasks.forEach { reminderScheduler.cancelSubTask(it.id) }
                    }
                }
            }
        }
    }
}
