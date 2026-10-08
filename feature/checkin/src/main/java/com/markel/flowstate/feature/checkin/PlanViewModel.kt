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
import kotlinx.coroutines.Job
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
    /**
     * Short transient notice for the page overlay (e.g. tapping a checkbox
     * before its block is due). Cleared by the ViewModel after
     * [MESSAGE_MILLIS] — the Plan screen just renders whatever is here.
     */
    val message: String? = null,
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

    /** In-flight auto-clear for [showMessage]; a new notice cancels the old. */
    private var messageClearJob: Job? = null

    /** In-flight persist from [applyEdit]; chained so writes stay ordered. */
    private var persistJob: Job? = null

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

    /** Wall-clock entry point — see [toggleBlock] with an injectable clock. */
    fun toggleBlock(index: Int) = toggleBlock(index, System.currentTimeMillis())

    /**
     * Flips block [index]: updates the UI state immediately, persists the new
     * tick set for the plan's date, and — when the block maps to a task —
     * toggles the task itself (only if its done-state actually differs, so a
     * stale tick can never flip a task the user changed elsewhere).
     * Ignored once the plan has expired for the day.
     *
     * Ticking is DUE-GATED: a block may only be ticked after its scheduled
     * start + duration has elapsed ([PlanBlockEdits.isDue]) — tapping early
     * shows a disappearing page-overlay notice instead of ticking. Unticking
     * an already-ticked block is always allowed, due or not.
     */
    fun toggleBlock(index: Int, nowMillis: Long) {
        val current = _uiState.value
        val plan = current.plan ?: return
        if (current.isExpired) return
        if (index !in plan.blocks.indices) return

        val block = plan.blocks[index]
        val isTicking = index !in current.checkedIndexes
        if (isTicking && !PlanBlockEdits.isDue(plan.date, block, nowMillis)) {
            val endLabel = PlanBlockEdits.endTimeHhMm(block)?.let { formatPlanTime(it) }
            showMessage(
                if (endLabel != null) {
                    "Not due yet — you can tick it after $endLabel"
                } else {
                    NOT_DUE_MESSAGE
                }
            )
            return
        }

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
     *
     * Returns false when the edit was REJECTED because moving the block onto
     * an occupied time would double-book it — the caller keeps the dialog
     * open and shows [PlanBlockEdits.OVERLAP_MESSAGE] inside it. An edit that
     * keeps the block's own start/duration is always accepted, even if the
     * AI's plan already contains an overlap: only a time change the user
     * made can be the conflict.
     */
    fun editBlock(index: Int, newBlock: PlanBlock): Boolean {
        val current = _uiState.value
        val plan = current.plan ?: return true
        if (current.isExpired) return true
        val old = plan.blocks.getOrNull(index)
        val timeChanged = old != null &&
            (old.startTime != newBlock.startTime || old.durationMinutes != newBlock.durationMinutes)
        if (timeChanged &&
            PlanBlockEdits.findOverlap(newBlock, plan.blocks, excludeIndex = index) != null
        ) {
            return false
        }
        val result = PlanBlockEdits.replace(plan.blocks, current.checkedIndexes, index, newBlock)
        if (result.blocks == plan.blocks && result.checkedIndexes == current.checkedIndexes) return true
        applyEdit(current, result, PlanBlockEdits.editNote(newBlock))
        return true
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

    /**
     * Inserts [block] in time order; the new block starts unticked.
     * Returns false (dialog stays open, [PlanBlockEdits.OVERLAP_MESSAGE]) when
     * the new block would land on an occupied time.
     */
    fun addBlock(block: PlanBlock): Boolean {
        val current = _uiState.value
        val plan = current.plan ?: return true
        if (current.isExpired) return true
        if (PlanBlockEdits.findOverlap(block, plan.blocks) != null) return false
        val result = PlanBlockEdits.insert(plan.blocks, current.checkedIndexes, block)
        applyEdit(current, result, PlanBlockEdits.addNote(block))
        return true
    }

    /**
     * Drag-reorder (plan step's edit mode): moves block [from] to slot [to] —
     * times stay with their slots, ticks follow their block
     * ([PlanBlockEdits.move]). Returns false when the shuffle would
     * double-book a time: the page overlay shows
     * [PlanBlockEdits.OVERLAP_MESSAGE] and the list stays put.
     *
     * Deliberately records NO AI-memory note: a drag only reshuffles the
     * slots the planner itself produced, and one gesture would otherwise
     * write a note per crossing.
     */
    fun moveBlock(from: Int, to: Int): Boolean {
        val current = _uiState.value
        val plan = current.plan ?: return false
        if (current.isExpired) return false
        if (from == to) return true
        if (from !in plan.blocks.indices || to !in plan.blocks.indices) return false
        val result = PlanBlockEdits.move(plan.blocks, current.checkedIndexes, from, to)
        if (result.blocks === plan.blocks) {
            showMessage(PlanBlockEdits.OVERLAP_MESSAGE)
            return false
        }
        applyEdit(current, result, note = null)
        return true
    }

    /**
     * Applies an edit: optimistic UI update, then persist in the only order
     * that survives — saveAgreedPlan RESETS checkedIndexesJson to null, so
     * the remapped tick set must be written AFTER the plan itself. The note
     * lands in durable AI memory (same channel as typed regenerate comments);
     * null skips it (drag-reorder — see [moveBlock]).
     *
     * Writes are chained: a drag-reorder commits once per crossing, and
     * without serializing, a slow saveAgreedPlan could land after a newer
     * one and persist a stale plan.
     */
    private fun applyEdit(current: PlanUiState, result: PlanEditResult, note: String?) {
        val plan = current.plan ?: return
        val updated = plan.copy(blocks = result.blocks)
        _uiState.value = current.copy(plan = updated, checkedIndexes = result.checkedIndexes)

        val previousPersist = persistJob
        persistJob = viewModelScope.launch {
            previousPersist?.join()
            planRepository.saveAgreedPlan(updated)
            planRepository.setCheckedIndexes(updated.date, result.checkedIndexes.toList().sorted())
            if (note != null) planRepository.recordFeedback(updated.date, note)
        }
    }

    /** Shows a page-overlay notice, replacing any in-flight one, and auto-clears it. */
    private fun showMessage(text: String) {
        messageClearJob?.cancel()
        _uiState.update { it.copy(message = text) }
        messageClearJob = viewModelScope.launch {
            delay(MESSAGE_MILLIS)
            _uiState.update { it.copy(message = null) }
        }
    }

    private companion object {
        const val EXPIRY_TICK_MILLIS = 60_000L
        const val MESSAGE_MILLIS = 2_500L
        const val NOT_DUE_MESSAGE = "Not due yet — its time hasn't come"
    }
}
