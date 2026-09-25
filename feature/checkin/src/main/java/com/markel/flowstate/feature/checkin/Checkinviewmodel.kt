package com.markel.flowstate.feature.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.CheckinRepository
import com.markel.flowstate.core.domain.EveningPlan
import com.markel.flowstate.core.domain.EveningPlanRepository
import com.markel.flowstate.core.domain.EveningPlanner
import com.markel.flowstate.core.domain.PlanFeedback
import com.markel.flowstate.core.domain.checkin.CheckinMoodState
import com.markel.flowstate.core.domain.checkin.UnexpectedPlan
import com.markel.flowstate.core.domain.usecase.checkin.BuildCheckinSnapshotUseCase
import com.markel.flowstate.core.domain.usecase.checkin.GetCheckinItemsUseCase
import com.markel.flowstate.core.domain.usecase.tasks.AddTaskUseCase
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
    private val buildCheckinSnapshot: BuildCheckinSnapshotUseCase,
    private val eveningPlanner: EveningPlanner,
    private val eveningPlanRepository: EveningPlanRepository
) : ViewModel() {

    private val _step = MutableStateFlow(CheckinStep.MOOD)
    private val _mood = MutableStateFlow(CheckinMoodState())
    private val _unexpectedPlans = MutableStateFlow<List<UnexpectedPlan>>(emptyList())
    private val _plan = MutableStateFlow<EveningPlan?>(null)
    private val _isPlanning = MutableStateFlow(false)

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

    // Step 1 — mood
    fun updateEnergy(value: Int) = _mood.update { it.copy(energy = value) }
    fun updateSleepiness(value: Int) = _mood.update { it.copy(sleepiness = value) }
    fun updateStress(value: Int) = _mood.update { it.copy(stress = value) }
    fun updateHeadache(value: Int) = _mood.update { it.copy(headache = value) }
    fun updateMotivation(value: Int) = _mood.update { it.copy(motivation = value) }

    // Step 1 — mood comments
    fun updateEnergyComment(value: String) = _mood.update { it.copy(energyComment = value) }
    fun updateSleepinessComment(value: String) = _mood.update { it.copy(sleepinessComment = value) }
    fun updateStressComment(value: String) = _mood.update { it.copy(stressComment = value) }
    fun updateHeadacheComment(value: String) = _mood.update { it.copy(headacheComment = value) }
    fun updateMotivationComment(value: String) = _mood.update { it.copy(motivationComment = value) }

    // Step 2 — unexpected plans
    fun addUnexpectedPlan(plan: UnexpectedPlan) = _unexpectedPlans.update { it + plan }
    fun removeUnexpectedPlan(plan: UnexpectedPlan) = _unexpectedPlans.update { it - plan }

    // Navigation
    fun goToNextStep() {
        _step.update { current ->
            when (current) {
                CheckinStep.MOOD -> CheckinStep.UNEXPECTED_PLANS
                CheckinStep.UNEXPECTED_PLANS -> CheckinStep.TASKS
                CheckinStep.TASKS -> CheckinStep.TASKS
                CheckinStep.PLAN -> CheckinStep.PLAN
            }
        }
    }

    // Step 3 — creates a task through the same shared use case the AI brain
    // will use later; lands in the same tasks table the main FlowState Tasks
    // screen reads from, so it shows up there alongside manually-added tasks
    fun addTask(title: String) {
        viewModelScope.launch { addTaskUseCase(title) }
    }

    /**
     * Step 3 → final plan step: persists today's check-in FIRST (so the
     * snapshot includes it), then builds the snapshot and runs the planner.
     * Instant with LocalEveningPlanner; will take a few seconds once Gemini
     * backs it, which is what [CheckinUiState.InProgress.isPlanning] drives.
     */
    fun generatePlan() {
        if (_isPlanning.value) return
        viewModelScope.launch {
            _isPlanning.value = true
            checkinRepository.saveTodayCheckin(_mood.value, _unexpectedPlans.value)
            val snapshot = buildCheckinSnapshot()
            _plan.value = eveningPlanner.generatePlan(snapshot, feedback = null)
            _isPlanning.value = false
            _step.value = CheckinStep.PLAN
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
            _isPlanning.value = false
        }
    }

    /**
     * Agree: the only place an evening plan is ever persisted (first write
     * to evening_plans). The screen dismisses the popup once this returns.
     */
    suspend fun agreeToPlan() {
        _plan.value?.let { eveningPlanRepository.saveAgreedPlan(it) }
    }
}