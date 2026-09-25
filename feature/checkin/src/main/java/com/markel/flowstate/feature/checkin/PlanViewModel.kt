package com.markel.flowstate.feature.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.EveningPlanRepository
import com.markel.flowstate.core.domain.PlanBlockKind
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.usecase.tasks.ToggleTaskUseCase
import com.markel.flowstate.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanUiState(
    /** True until the agreed plan (and its ticks) have been read from Room. */
    val isLoading: Boolean = true,
    /** The most recently agreed plan, or null if the user never agreed to one. */
    val plan: EveningPlan? = null,
    /** Persisted ticked block indexes for [plan]'s date. */
    val checkedIndexes: Set<Int> = emptySet(),
    /** User's end-of-day cutoff, minute-of-day (0 = midnight). */
    val endOfDayMinutes: Int = 0,
    /**
     * True once [plan] has aged out — day rolled over or the cutoff passed —
     * which the Plan screen renders exactly like a missing plan.
     */
    val isExpired: Boolean = false,
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
 *
 * Expiry: the plan blanks out once [isPlanExpired] says so — the user's
 * end-of-day cutoff from DataStore, or the calendar-date rollover (midnight
 * default). Re-evaluated when the preference changes and on a one-minute
 * ticker so a left-open app blanks exactly on time; the plan itself stays in
 * state so moving the cutoff later the same day brings it back.
 */
@HiltViewModel
class PlanViewModel @Inject constructor(
    private val planRepository: EveningPlanRepository,
    private val taskRepository: TaskRepository,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val userPreferences: UserPreferencesRepository,
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
            _uiState.update {
                it.copy(
                    isLoading = false,
                    plan = plan,
                    checkedIndexes = checked,
                    isExpired = isPlanExpired(plan?.date, it.endOfDayMinutes),
                )
            }
        }

        // Cutoff preference: re-evaluate on every change (check-in saves it).
        viewModelScope.launch {
            userPreferences.endOfDayMinutes.collect { minutes ->
                _uiState.update {
                    it.copy(
                        endOfDayMinutes = minutes,
                        isExpired = isPlanExpired(it.plan?.date, minutes),
                    )
                }
            }
        }

        // Ticker: catches the wall clock crossing the cutoff / midnight while
        // the app stays open. Cheap — a single boolean re-computation per minute.
        viewModelScope.launch {
            while (true) {
                delay(EXPIRY_TICK_MILLIS)
                _uiState.update {
                    it.copy(isExpired = isPlanExpired(it.plan?.date, it.endOfDayMinutes))
                }
            }
        }
    }

    /**
     * Flips block [index]: updates the UI state immediately, persists the new
     * tick set for the plan's date, and — when the block maps to a task —
     * toggles the task itself (only if its done-state actually differs, so a
     * stale tick can never flip a task the user changed elsewhere).
     * Ignored once the plan has expired for the day.
     */
    fun toggleBlock(index: Int) {
        val current = _uiState.value
        val plan = current.plan ?: return
        if (current.isExpired) return
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

    private companion object {
        const val EXPIRY_TICK_MILLIS = 60_000L
    }
}
