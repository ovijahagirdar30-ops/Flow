package com.markel.flowstate.feature.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.data.UserPreferencesRepository
import com.markel.flowstate.core.domain.CheckinRepository
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.EveningPlanRepository
import com.markel.flowstate.core.domain.EveningPlanner
import com.markel.flowstate.core.domain.PlanFeedback
import com.markel.flowstate.core.domain.PlanBlock
import com.markel.flowstate.core.domain.TaskRepository
import com.markel.flowstate.core.domain.checkin.CheckinMoodState
import com.markel.flowstate.core.domain.checkin.UnexpectedPlan
import com.markel.flowstate.core.domain.usecase.checkin.BuildCheckinSnapshotUseCase
import com.markel.flowstate.core.domain.usecase.checkin.GetCheckinItemsUseCase
import com.markel.flowstate.core.domain.usecase.tasks.AddTaskUseCase
import com.markel.flowstate.core.domain.usecase.tasks.DeleteTaskUseCase
import com.markel.flowstate.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckinViewModel @Inject constructor(
    private val getCheckinItems: GetCheckinItemsUseCase,
    private val checkinRepository: CheckinRepository,
    private val addTaskUseCase: AddTaskUseCase,
    private val taskRepository: TaskRepository,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val buildCheckinSnapshot: BuildCheckinSnapshotUseCase,
    private val eveningPlanner: EveningPlanner,
    private val eveningPlanRepository: EveningPlanRepository,
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {

    private val _step = MutableStateFlow(CheckinStep.GREET)
    private val _mood = MutableStateFlow(CheckinMoodState())
    private val _unexpectedPlans = MutableStateFlow<List<UnexpectedPlan>>(emptyList())
    private val _plan = MutableStateFlow<EveningPlan?>(null)
    private val _isPlanning = MutableStateFlow(false)

    /**
     * Ticks of the plan currently on screen. Empty for a fresh draft (the
     * check-in flow has no checkboxes); populated when the flow was opened by
     * the Plan tab's "Edit plan" — edits remap them and Agree restores them,
     * because [EveningPlanRepository.saveAgreedPlan] resets the tick column.
     */
    private val _planTicks = MutableStateFlow<Set<Int>>(emptySet())

    /** True only when an AGREED plan was loaded for editing (see [_planTicks]). */
    private var editingAgreedPlan = false

    val uiState: StateFlow<CheckinUiState> = combine(
        _step, _mood, _unexpectedPlans, getCheckinItems(),
        combine(_plan, _isPlanning) { plan, isPlanning -> plan to isPlanning }
    ) { step, mood, plans, items, planAndIsPlanning ->
        CheckinUiState.InProgress(
            step = step,
            mood = mood,
            unexpectedPlans = plans,
            items = items,
            plan = planAndIsPlanning.first,
            isPlanning = planAndIsPlanning.second
        ) as CheckinUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CheckinUiState.Loading
    )

    /**
     * End-of-day cutoff (minute-of-day, 0 = midnight) — the Plan tab blanks
     * out once the clock passes it. Read here for the edit-plan expiry guard;
     * the picker UI was dropped from the check-in (a dedicated settings screen
     * is planned), so [setEndOfDayMinutes] currently has no caller in-flow.
     * Eager so nothing ever reads a stale default.
     */
    val endOfDayMinutes: StateFlow<Int> = userPreferences.endOfDayMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** Persists the end-of-day time picked on the mood step. */
    fun setEndOfDayMinutes(minutes: Int) {
        viewModelScope.launch { userPreferences.saveEndOfDayMinutes(minutes) }
    }

    /**
     * Plan tab's "Edit plan": loads the AGREED plan (and its tick state) into
     * the final PLAN step so it can be edited and re-agreed without re-running
     * the mood/tasks steps. Falls back silently to the normal mood start when
     * no plan has ever been agreed — the full flow is the create path.
     * Suspended: the screen holds its first frame until this returns, so the
     * greeting never flashes before the jump to PLAN.
     */
    suspend fun loadAgreedPlanForEditing() {
        val plan = eveningPlanRepository.latestAgreedPlan() ?: return
        // Mirror the Plan tab's own visibility rule — never surface an
        // aged-out plan for editing; the flow then starts at mood as usual.
        if (isPlanExpired(plan.date, endOfDayMinutes.value)) return
        _plan.value = plan
        _planTicks.value = eveningPlanRepository.checkedIndexes(plan.date).toSet()
        editingAgreedPlan = true
        _step.value = CheckinStep.PLAN
    }

    // Question flow — mood values (one question per step)
    fun updateEnergy(value: Int) = _mood.update { it.copy(energy = value) }
    fun updateSleepiness(value: Int) = _mood.update { it.copy(sleepiness = value) }
    fun updateStress(value: Int) = _mood.update { it.copy(stress = value) }
    fun updateHeadache(value: Int) = _mood.update { it.copy(headache = value) }
    fun updateMotivation(value: Int) = _mood.update { it.copy(motivation = value) }

    // Question flow — optional note under each question
    fun updateEnergyComment(value: String) = _mood.update { it.copy(energyComment = value) }
    fun updateSleepinessComment(value: String) = _mood.update { it.copy(sleepinessComment = value) }
    fun updateStressComment(value: String) = _mood.update { it.copy(stressComment = value) }
    fun updateHeadacheComment(value: String) = _mood.update { it.copy(headacheComment = value) }
    fun updateMotivationComment(value: String) = _mood.update { it.copy(motivationComment = value) }

    /** Routes a question step's slider value to the right mood field. */
    fun updateMoodValue(step: CheckinStep, value: Int) {
        when (step) {
            CheckinStep.ENERGY -> updateEnergy(value)
            CheckinStep.SLEEP -> updateSleepiness(value)
            CheckinStep.STRESS -> updateStress(value)
            CheckinStep.BODY -> updateHeadache(value)
            CheckinStep.MOTIVATION -> updateMotivation(value)
            else -> Unit
        }
    }

    /** Routes a question step's note text to the right mood comment field. */
    fun updateMoodComment(step: CheckinStep, comment: String) {
        when (step) {
            CheckinStep.ENERGY -> updateEnergyComment(comment)
            CheckinStep.SLEEP -> updateSleepinessComment(comment)
            CheckinStep.STRESS -> updateStressComment(comment)
            CheckinStep.BODY -> updateHeadacheComment(comment)
            CheckinStep.MOTIVATION -> updateMotivationComment(comment)
            else -> Unit
        }
    }

    // Unexpected-plans step (legacy, unchanged)
    fun addUnexpectedPlan(plan: UnexpectedPlan) = _unexpectedPlans.update { it + plan }
    fun removeUnexpectedPlan(plan: UnexpectedPlan) = _unexpectedPlans.update { it - plan }

    // Navigation
    fun goToNextStep() {
        val generating = _step.value == CheckinStep.RECAP
        _step.update { current ->
            when (current) {
                CheckinStep.GREET -> CheckinStep.ENERGY
                CheckinStep.ENERGY -> CheckinStep.SLEEP
                CheckinStep.SLEEP -> CheckinStep.STRESS
                CheckinStep.STRESS -> CheckinStep.BODY
                CheckinStep.BODY -> CheckinStep.MOTIVATION
                CheckinStep.MOTIVATION -> CheckinStep.RECAP
                // "Generate my evening": the plan starts generating while the
                // new "Tasks for tonight" step is open — it owns its own chrome
                // (heading + add-row + sections + "Show my plan" pill) and is
                // NOT part of the designed header/footer run.
                CheckinStep.RECAP -> CheckinStep.TASKS
                // "Show my plan": hand off to the plan stage, which opens on
                // the planning screen if the planner is still running.
                CheckinStep.TASKS -> CheckinStep.PLAN
                CheckinStep.PLAN -> CheckinStep.PLAN
            }
        }
        if (generating) generatePlan()
    }

    /** Header back arrow on the designed question flow. */
    fun goToPreviousStep() {
        _step.update { current ->
            when (current) {
                CheckinStep.ENERGY -> CheckinStep.GREET
                CheckinStep.SLEEP -> CheckinStep.ENERGY
                CheckinStep.STRESS -> CheckinStep.SLEEP
                CheckinStep.BODY -> CheckinStep.STRESS
                CheckinStep.MOTIVATION -> CheckinStep.BODY
                CheckinStep.RECAP -> CheckinStep.MOTIVATION
                CheckinStep.TASKS -> CheckinStep.RECAP
                else -> current
            }
        }
    }

    /** Recap row tap: jump back into one of the five questions to amend it. */
    fun goToQuestion(step: CheckinStep) {
        if (step in designedQuestionSteps) _step.value = step
    }

    // Tasks step — creates a task through the same shared use case the AI brain
    // will use later; lands in the same tasks table the main FlowState Tasks
    // screen reads from, so it shows up there alongside manually-added tasks
    fun addTask(title: String) {
        viewModelScope.launch { addTaskUseCase(title) }
    }

    /**
     * Tasks-for-tonight step: drops a task from tonight's list — alarms
     * first (the task's own plus every subtask's, same order as the Tasks
     * screen), then the row itself. No-op for a stale id (already deleted).
     */
    fun removeTask(taskId: Int) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            reminderScheduler.cancel(task.id)
            task.subTasks.forEach { reminderScheduler.cancelSubTask(it.id) }
            deleteTaskUseCase(task)
        }
    }

    /**
     * Recap → tasks step: persists today's check-in FIRST (so the
     * snapshot includes it), then builds the snapshot and runs the planner —
     * all while the "Tasks for tonight" step is open, so the plan is ready
     * (or still loading there) when "Show my plan" lands on the PLAN step,
     * which opens on the planning screen on its first frame (no empty-list
     * flash); instant with LocalEveningPlanner, seconds once Gemini backs it.
     */
    fun generatePlan() {
        if (_isPlanning.value) return
        _isPlanning.value = true
        _plan.value = null
        _planTicks.value = emptySet()
        viewModelScope.launch {
            checkinRepository.saveTodayCheckin(_mood.value, _unexpectedPlans.value)
            val snapshot = buildCheckinSnapshot()
            _plan.value = eveningPlanner.generatePlan(snapshot, feedback = null)
            _isPlanning.value = false
        }
    }

    /**
     * Regenerate: feeds the typed comment plus the plan being rejected back
     * into the planner, so Gemini revises instead of re-rolling. Stays on the
     * PLAN step; isPlanning drives the step's planning state.
     *
     * The comment is also RECORDED durably before generating — that record is
     * what later evenings read back as long-term memory ("skincare is only
     * 5 minutes"), so a correction outlives the session it was typed in.
     */
    fun regeneratePlan(comment: String) {
        if (_isPlanning.value) return
        val current = _plan.value ?: return
        viewModelScope.launch {
            _isPlanning.value = true
            val snapshot = buildCheckinSnapshot()
            val trimmed = comment.trim()
            if (trimmed.isNotBlank()) {
                eveningPlanRepository.recordFeedback(snapshot.date, trimmed)
            }
            _plan.value = eveningPlanner.generatePlan(
                snapshot,
                PlanFeedback(comment = trimmed, previousPlan = current)
            )
            // A freshly generated plan shares no structure with the old one —
            // its ticks can't be carried over.
            _planTicks.value = emptySet()
            _isPlanning.value = false
        }
    }

    /**
     * Agree: the only place an evening plan is ever persisted (first write
     * to evening_plans). The screen dismisses the popup once this returns.
     * When the plan was loaded for editing, the tick set is written back
     * AFTER the plan itself — saveAgreedPlan resets the tick column.
     */
    suspend fun agreeToPlan() {
        val plan = _plan.value ?: return
        eveningPlanRepository.saveAgreedPlan(plan)
        if (editingAgreedPlan) {
            eveningPlanRepository.setCheckedIndexes(plan.date, _planTicks.value.toList().sorted())
        }
    }

    // ── Manual plan editing (pre-Agree, ephemeral) ────────────────────────
    //
    // These mutate the in-memory plan only — Agree persists whatever shape
    // it has, discard loses the edits. Tick remapping follows [_planTicks]:
    // empty for a fresh draft (the check-in step has no checkboxes), the
    // loaded set when editing an agreed plan from the Plan tab. Each edit
    // still records a durable memory note so later evenings learn from it
    // (same as a typed regenerate comment).

    /**
     * Replaces the block at [index] (time/details), re-sorted by time.
     *
     * Returns false when moving the block onto an occupied time would
     * double-book it — the dialog stays open and shows
     * [PlanBlockEdits.OVERLAP_MESSAGE]. An edit that keeps the block's own
     * time is always accepted, even if the generated plan already overlaps.
     */
    fun editBlock(index: Int, newBlock: PlanBlock): Boolean {
        val current = _plan.value ?: return true
        if (_isPlanning.value) return true
        val old = current.blocks.getOrNull(index)
        val timeChanged = old != null &&
            (old.startTime != newBlock.startTime || old.durationMinutes != newBlock.durationMinutes)
        if (timeChanged &&
            PlanBlockEdits.findOverlap(newBlock, current.blocks, excludeIndex = index) != null
        ) {
            return false
        }
        val result = PlanBlockEdits.replace(current.blocks, _planTicks.value, index, newBlock)
        if (result.blocks == current.blocks && result.checkedIndexes == _planTicks.value) return true
        _plan.value = current.copy(blocks = result.blocks)
        _planTicks.value = result.checkedIndexes
        recordEditNote(PlanBlockEdits.editNote(newBlock))
        return true
    }

    /** Removes the block at [index] from the plan being reviewed. */
    fun removeBlock(index: Int) {
        val current = _plan.value ?: return
        if (_isPlanning.value) return
        if (index !in current.blocks.indices) return
        val result = PlanBlockEdits.remove(current.blocks, _planTicks.value, index)
        _plan.value = current.copy(blocks = result.blocks)
        _planTicks.value = result.checkedIndexes
        recordEditNote(PlanBlockEdits.removeNote(current.blocks[index]))
    }

    /**
     * Inserts [block] in time order into the plan being reviewed.
     * Returns false (dialog stays open, [PlanBlockEdits.OVERLAP_MESSAGE]) when
     * the new block would land on an occupied time.
     */
    fun addBlock(block: PlanBlock): Boolean {
        val current = _plan.value ?: return true
        if (_isPlanning.value) return true
        if (PlanBlockEdits.findOverlap(block, current.blocks) != null) return false
        val result = PlanBlockEdits.insert(current.blocks, _planTicks.value, block)
        _plan.value = current.copy(blocks = result.blocks)
        _planTicks.value = result.checkedIndexes
        recordEditNote(PlanBlockEdits.addNote(block))
        return true
    }

    /** Durable AI memory — fire-and-forget; a DB hiccup never blocks editing. */
    private fun recordEditNote(note: String) {
        val date = _plan.value?.date ?: return
        viewModelScope.launch {
            runCatching { eveningPlanRepository.recordFeedback(date, note) }
        }
    }
}