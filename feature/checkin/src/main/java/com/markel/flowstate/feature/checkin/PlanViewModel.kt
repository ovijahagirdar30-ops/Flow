package com.markel.flowstate.feature.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.EveningPlanRepository
import com.markel.flowstate.core.domain.PlanBlock
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
 * cancellation the Calendar/Flow screens do on completion. A block with a
 * [PlanBlock.subtaskId] maps to ONE subtask of that task instead — ticking it
 * flips only the subtask (and cancels its reminder), never the parent.
 * HABIT and free blocks (meals, rest, …) are visual-only ticks: their
 * referenceId never reaches a task write.
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
        reload()

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
     * Re-reads the agreed plan + ticks from Room. Called at init and on every
     * tab resume — the Plan tab's FAB "Edit plan" persists through the
     * check-in and hands back here, so the snapshot taken at process start
     * must never win over what the database now holds.
     */
    fun reload() {
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
                val shouldBeDone = index in checked
                val task = taskRepository.getTaskById(referenceId) ?: return@launch

                val subtaskId = block.subtaskId
                if (subtaskId != null) {
                    // Subtask block: flip ONLY that subtask. Allowing the
                    // whole-task toggle here would mark a task done because
                    // one slice of it was finished. The subtask may have been
                    // deleted since planning — then the tick stays visual.
                    val subTask = task.subTasks.firstOrNull { it.id == subtaskId } ?: return@launch
                    if (subTask.isDone != shouldBeDone) {
                        val updatedSubTask = subTask.copy(
                            isDone = shouldBeDone,
                            completedAt = if (shouldBeDone) System.currentTimeMillis() else null,
                            reminderTime = if (shouldBeDone) null else subTask.reminderTime
                        )
                        taskRepository.upsertTask(
                            task.copy(subTasks = task.subTasks.map {
                                if (it.id == subtaskId) updatedSubTask else it
                            })
                        )
                        if (shouldBeDone) reminderScheduler.cancelSubTask(subtaskId)
                    }
                } else if (task.isDone != shouldBeDone) {
                    toggleTaskUseCase(task)
                    if (shouldBeDone) {
                        reminderScheduler.cancel(task.id)
                        task.subTasks.forEach { reminderScheduler.cancelSubTask(it.id) }
                    }
                }
            }
        }
    }

    /**
     * Replaces the block at [index] (time/details), re-sorting by time and
     * carrying every tick to its block's new position ([PlanBlockEdits]).
     * Saving with no actual change is a no-op — no DB write, no memory note.
     */
    fun editBlock(index: Int, newBlock: PlanBlock) {
        val current = _uiState.value
        val plan = current.plan ?: return
        if (current.isExpired) return
        val result = PlanBlockEdits.replace(plan.blocks, current.checkedIndexes, index, newBlock)
        if (result.blocks == plan.blocks && result.checkedIndexes == current.checkedIndexes) return
        applyEdit(current, result, PlanBlockEdits.editNote(newBlock))
    }

    /** Removes the block at [index]; its tick disappears with it. */
    fun removeBlock(index: Int) {
        val current = _uiState.value
        val plan = current.plan ?: return
        if (current.isExpired) return
        if (index !in plan.blocks.indices) return
        val result = PlanBlockEdits.remove(plan.blocks, current.checkedIndexes, index)
        applyEdit(current, result, PlanBlockEdits.removeNote(plan.blocks[index]))
    }

    /** Inserts [block] in time order; the new block starts unticked. */
    fun addBlock(block: PlanBlock) {
        val current = _uiState.value
        val plan = current.plan ?: return
        if (current.isExpired) return
        val result = PlanBlockEdits.insert(plan.blocks, current.checkedIndexes, block)
        applyEdit(current, result, PlanBlockEdits.addNote(block))
    }

    /**
     * Applies an edit: optimistic UI update, then persist in the only order
     * that survives — saveAgreedPlan RESETS checkedIndexesJson to null, so
     * the remapped tick set must be written AFTER the plan itself. The note
     * lands in durable AI memory (same channel as typed regenerate comments).
     */
    private fun applyEdit(current: PlanUiState, result: PlanEditResult, note: String) {
        val plan = current.plan ?: return
        val updated = plan.copy(blocks = result.blocks)
        _uiState.value = current.copy(plan = updated, checkedIndexes = result.checkedIndexes)

        viewModelScope.launch {
            planRepository.saveAgreedPlan(updated)
            planRepository.setCheckedIndexes(updated.date, result.checkedIndexes.toList().sorted())
            planRepository.recordFeedback(updated.date, note)
        }
    }

    private companion object {
        const val EXPIRY_TICK_MILLIS = 60_000L
    }
}
