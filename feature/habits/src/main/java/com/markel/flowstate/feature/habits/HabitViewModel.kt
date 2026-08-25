package com.markel.flowstate.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitType
import com.markel.flowstate.core.domain.usecase.habits.DecrementNumericValueUseCase
import com.markel.flowstate.core.domain.usecase.habits.DeleteHabitUseCase
import com.markel.flowstate.core.domain.usecase.habits.DeleteNumericEntryUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetAllBooleanEntriesUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetAllNumericEntriesUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetHabitsWithStatusUseCase
import com.markel.flowstate.core.domain.usecase.habits.IncrementNumericValueUseCase
import com.markel.flowstate.core.domain.usecase.habits.InsertHabitUseCase
import com.markel.flowstate.core.domain.usecase.habits.LogNumericEntryUseCase
import com.markel.flowstate.core.domain.usecase.habits.SetHabitMoodUseCase
import com.markel.flowstate.core.domain.usecase.habits.ToggleHabitEntryUseCase
import com.markel.flowstate.core.domain.usecase.habits.UpdateHabitUseCase
import com.markel.flowstate.core.domain.usecase.habits.UpdateHabitsOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val getHabitsWithStatus: GetHabitsWithStatusUseCase,
    private val getAllBooleanEntries: GetAllBooleanEntriesUseCase,
    private val getAllNumericEntries: GetAllNumericEntriesUseCase,
    private val insertHabit: InsertHabitUseCase,
    private val updateHabit: UpdateHabitUseCase,
    private val deleteHabit: DeleteHabitUseCase,
    private val toggleEntry: ToggleHabitEntryUseCase,
    private val setHabitMood: SetHabitMoodUseCase,
    private val logNumericEntry: LogNumericEntryUseCase,
    private val incrementNumericValue: IncrementNumericValueUseCase,
    private val decrementNumericValue: DecrementNumericValueUseCase,
    private val deleteNumericEntry: DeleteNumericEntryUseCase,
    private val updateHabitsOrder: UpdateHabitsOrderUseCase
) : ViewModel() {

    private val _showAddDialog = MutableStateFlow(false)
    private val _pendingMoodPrompt = MutableStateFlow<PendingMoodPrompt?>(null)
    private val _uiState = MutableStateFlow<HabitUiState>(HabitUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getHabitsWithStatus(),
                getAllBooleanEntries(),
                getAllNumericEntries(),
                _showAddDialog,
                _pendingMoodPrompt
            ) { habits, allBooleanEntries, allNumericEntries, showDialog, pendingMoodPrompt ->
                val weekEntriesByHabit = allBooleanEntries
                    .groupBy({ it.habitId }, { it.epochDay })
                    .mapValues { it.value.toSet() }

                val numericEntriesByHabit = allNumericEntries.groupBy { it.habitId }

                HabitUiState.Success(
                    habits = habits,
                    weekEntriesByHabit = weekEntriesByHabit,
                    numericEntriesByHabit = numericEntriesByHabit,
                    showAddDialog = showDialog,
                    completedToday = habits.count { it.isCompletedToday },
                    totalHabits = habits.size,
                    motivationalMessageIndex = LocalDate.now().dayOfYear % 7,
                    pendingMoodPrompt = pendingMoodPrompt
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    // ==================================
    // OPERATIONS FOR BOOLEAN HABITS
    // ==================================

    fun toggleBooleanHabitOnDate(habitId: Int, date: LocalDate) {
        val currentState = _uiState.value as? HabitUiState.Success ?: return
        val wasAlreadyCompleted = date.toEpochDay() in (currentState.weekEntriesByHabit[habitId] ?: emptySet())

        viewModelScope.launch {
            toggleEntry(habitId, date)
            if (!wasAlreadyCompleted) {
                val habitName = currentState.habits
                    .firstOrNull { it.habit.id == habitId }?.habit?.name ?: ""
                _pendingMoodPrompt.value = PendingMoodPrompt(habitId, date, habitName)
            }
        }
    }

    fun submitMood(mood: Int) {
        val prompt = _pendingMoodPrompt.value ?: return
        viewModelScope.launch {
            setHabitMood(prompt.habitId, prompt.date, mood)
            _pendingMoodPrompt.value = null
        }
    }

    fun dismissMoodPrompt() {
        _pendingMoodPrompt.value = null
    }

    // ==================================
    // OPERATIONS FOR NUMERIC HABITS
    // ==================================

    fun incrementNumericHabit(habitId: Int, date: LocalDate, currentValue: Float?, step: Float) {
        viewModelScope.launch {
            incrementNumericValue(habitId, date, currentValue, step)
        }
    }

    fun decrementNumericHabit(habitId: Int, date: LocalDate, currentValue: Float?, step: Float) {
        viewModelScope.launch {
            decrementNumericValue(habitId, date, currentValue, step)
        }
    }

    fun setNumericValue(habitId: Int, date: LocalDate, value: Float) {
        viewModelScope.launch {
            logNumericEntry(habitId, date, value)
        }
    }

    fun deleteNumericEntry(habitId: Int, date: LocalDate) {
        viewModelScope.launch {
            deleteNumericEntry.invoke(habitId, date)
        }
    }

    // ===================================
    // COMMON OPERATIONS (HABIT CRUD)
    // ===================================

    fun addHabit(
        name: String,
        iconName: String,
        colorArgb: Int,
        habitType: HabitType = HabitType.BOOLEAN,
        unit: String? = null,
        targetValue: Float? = null,
        step: Float = 1f,
        priorityRank: Int = 5,
        rolloverIfMissed: Boolean = false
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            insertHabit(
                Habit(
                    name = name,
                    iconName = iconName,
                    colorArgb = colorArgb,
                    habitType = habitType,
                    unit = unit,
                    targetValue = targetValue,
                    step = step,
                    priorityRank = priorityRank,
                    rolloverIfMissed = rolloverIfMissed
                )
            )
            _showAddDialog.value = false
        }
    }

    fun editHabit(
        habit: Habit,
        newName: String,
        newIcon: String,
        newColorArgb: Int,
        newUnit: String? = null,
        newTargetValue: Float? = null,
        newStep: Float? = null,
        newPriorityRank: Int? = null,
        newRolloverIfMissed: Boolean? = null
    ) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            updateHabit(
                habit.copy(
                    name = newName,
                    iconName = newIcon,
                    colorArgb = newColorArgb,
                    unit = newUnit,
                    targetValue = newTargetValue,
                    step = newStep ?: habit.step,
                    priorityRank = newPriorityRank ?: habit.priorityRank,
                    rolloverIfMissed = newRolloverIfMissed ?: habit.rolloverIfMissed
                )
            )
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { deleteHabit.invoke(habit) }
    }

    // ============================================
    // DIALOG CONTROL
    // ============================================

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }

    fun onReorder(fromIndex: Int, toIndex: Int) {
        val currentState = _uiState.value as? HabitUiState.Success ?: return
        val currentList = currentState.habits.toMutableList()

        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)

        val updatedHabits = currentList.mapIndexed { index, habitWithStatus ->
            habitWithStatus.copy(
                habit = habitWithStatus.habit.copy(position = index)
            )
        }

        _uiState.value = currentState.copy(habits = updatedHabits)

        viewModelScope.launch {
            val positionUpdates = updatedHabits.map { it.habit.id to it.habit.position }
            updateHabitsOrder(positionUpdates)
        }
    }
}