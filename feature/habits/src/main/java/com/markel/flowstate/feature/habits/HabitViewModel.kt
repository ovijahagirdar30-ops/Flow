package com.markel.flowstate.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitSchedule
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
import com.markel.flowstate.core.domain.usecase.habits.UpdateHabitsPriorityOrderUseCase
import com.markel.flowstate.core.notifications.HabitReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val updateHabitsOrder: UpdateHabitsOrderUseCase,
    private val updateHabitsPriorityOrder: UpdateHabitsPriorityOrderUseCase,
    private val habitReminderScheduler: HabitReminderScheduler
) : ViewModel() {

    private val _showAddDialog = MutableStateFlow(false)
    private val _pendingMoodPrompt = MutableStateFlow<PendingMoodPrompt?>(null)
    private val _uiState = MutableStateFlow<HabitUiState>(HabitUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // Kept separate from the main combine() pipeline above on purpose — it's
    // purely a UI-mode toggle, not persisted data, so it doesn't need to flow
    // through the same computation as habits/entries/dialogs.
    private val _isPriorityReorderMode = MutableStateFlow(false)
    val isPriorityReorderMode = _isPriorityReorderMode.asStateFlow()

    init{
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
                    completedToday = habits.count { it.isDueToday && it.isCompletedToday },
                    totalHabits = habits.count { it.isDueToday },
                    motivationalMessageIndex = LocalDate.now().dayOfYear % 7,
                    pendingMoodPrompt = pendingMoodPrompt
                )
            }.collect {newState ->
                _uiState.value = newState
            }

        }
    }

    // ==================================
    // OPERATIONS FOR BOOLEAN HABITS
    // ==================================

    /**
     * Marks the habit completed / incomplete for a specific date.
     * If this is a mark-COMPLETE action (not un-marking), it also queues up
     * the mood-prompt sheet — but only for that case, since asking "how did
     * that feel?" when you're un-completing something doesn't make sense.
     */
    fun toggleBooleanHabitOnDate(habitId: Int, date: LocalDate) {
        val currentState = _uiState.value as? HabitUiState.Success ?: return
        val wasAlreadyCompleted = date.toEpochDay() in (currentState.weekEntriesByHabit[habitId] ?: emptySet())

        viewModelScope.launch {
            toggleEntry(habitId, date)
            // Completing (or un-completing) can cross the weekly target, so
            // today's reminder slot must be re-evaluated: booked out when the
            // habit is done for the day/week, restored when it isn't.
            habitReminderScheduler.reschedule(habitId)
            // Mood logging is per-habit and OFF by default: only ask
            // "how did it feel?" when the habit opted in. Un-ticking never
            // prompts (asking on the way out makes no sense), and a habit
            // switched off keeps its already-logged ratings.
            if (!wasAlreadyCompleted) {
                val habit = currentState.habits
                    .firstOrNull { it.habit.id == habitId }?.habit
                if (habit?.moodLoggingEnabled == true) {
                    _pendingMoodPrompt.value = PendingMoodPrompt(habitId, date, habit.name)
                }
            }
        }
    }

    /**
     * Called when the user taps a mood option on the prompt sheet.
     */
    fun submitMood(mood: Int) {
        val prompt = _pendingMoodPrompt.value ?: return
        viewModelScope.launch {
            setHabitMood(prompt.habitId, prompt.date, mood)
            _pendingMoodPrompt.value = null
        }
    }

    /**
     * Called when the user dismisses the prompt without picking a mood —
     * always allowed, since this is meant to reduce friction, not add it.
     */
    fun dismissMoodPrompt() {
        _pendingMoodPrompt.value = null
    }

    // ==================================
    // OPERATIONS FOR NUMERIC HABITS
    // ==================================

    /**
     * Increment numeric habit value on specific date
     */
    fun incrementNumericHabit(habitId: Int, date: LocalDate, currentValue: Float?, step: Float) {
        viewModelScope.launch {
            incrementNumericValue(habitId, date, currentValue, step)
            habitReminderScheduler.reschedule(habitId)
        }
    }

    /**
     * Decrement numeric habit value on specific date
     */
    fun decrementNumericHabit(habitId: Int, date: LocalDate, currentValue: Float?, step: Float) {
        viewModelScope.launch {
            decrementNumericValue(habitId, date, currentValue, step)
            habitReminderScheduler.reschedule(habitId)
        }
    }

    /**
     * Set habit value for specific date
     */
    fun setNumericValue(habitId: Int, date: LocalDate, value: Float) {
        viewModelScope.launch {
            logNumericEntry(habitId, date, value)
            habitReminderScheduler.reschedule(habitId)
        }
    }

    /**
     * Deletes a numeric entry for a specific date
     */
    fun deleteNumericEntry(habitId: Int, date: LocalDate) {
        viewModelScope.launch {
            deleteNumericEntry.invoke(habitId, date)
            habitReminderScheduler.reschedule(habitId)
        }
    }

    // ===================================
    // COMMON OPERATIONS (HABIT CRUD)
    // ===================================

    /**
     * Creates a new habit
     */
    fun addHabit(
        name: String, iconName: String,
        colorArgb: Int,
        habitType: HabitType = HabitType.BOOLEAN,
        unit: String? = null, targetValue: Float? = null,
        step: Float = 1f,
        priorityRank: Int = 5,
        rolloverIfMissed: Boolean = false,
        moodLoggingEnabled: Boolean = false,
        schedule: HabitSchedule = HabitSchedule.DAILY,
        reminderEnabled: Boolean = false,
        reminderMinuteOfDay: Int? = null)
    {
        if (name.isBlank()) return
        viewModelScope.launch {
            // Make room at the chosen position first: bump everyone already
            // sitting there or below it, so stored ranks stay ordered and the
            // slider's number always matches the drag list.
            val targetRank = makeRoomForNewHabit(priorityRank)
            insertHabit(
                Habit(
                    name = name,
                    iconName = iconName,
                    colorArgb = colorArgb,
                    habitType = habitType,
                    unit = unit,
                    targetValue = targetValue,
                    step = step,
                    priorityRank = targetRank,
                    rolloverIfMissed = rolloverIfMissed,
                    moodLoggingEnabled = moodLoggingEnabled,
                    schedule = schedule,
                    reminderEnabled = reminderEnabled,
                    reminderMinuteOfDay = reminderMinuteOfDay
                )
            )
            // The insert happens before the id exists, so re-book from the
            // full list — includes this brand-new habit's first alarm.
            habitReminderScheduler.rescheduleAll()
            _showAddDialog.value = false
        }
    }

    /**
     * Edits an existing habit
     */
    fun editHabit(
        habit: Habit,
        newName: String,
        newIcon: String,
        newColorArgb: Int,
        newUnit: String? = null,
        newTargetValue: Float? = null,
        newStep: Float? = null,
        newPriorityRank: Int? = null,
        newRolloverIfMissed: Boolean? = null,
        newMoodLoggingEnabled: Boolean? = null,
        newSchedule: HabitSchedule? = null,
        newReminderEnabled: Boolean? = null,
        newReminderMinuteOfDay: Int? = null
    ) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val reminderEnabled = newReminderEnabled ?: habit.reminderEnabled
            updateHabit(
                habit.copy(
                    name = newName,
                    iconName = newIcon,
                    colorArgb = newColorArgb,
                    unit = newUnit,
                    targetValue = newTargetValue,
                    step = newStep ?: habit.step,
                    priorityRank = newPriorityRank ?: habit.priorityRank,
                    rolloverIfMissed = newRolloverIfMissed ?: habit.rolloverIfMissed,
                    moodLoggingEnabled = newMoodLoggingEnabled ?: habit.moodLoggingEnabled,
                    schedule = newSchedule ?: habit.schedule,
                    reminderEnabled = reminderEnabled,
                    // Keep the configured time even while the switch is off —
                    // flipping it back on restores the last chosen hour.
                    reminderMinuteOfDay = newReminderMinuteOfDay ?: habit.reminderMinuteOfDay
                )
            )
            // A schedule/time/switch change (or a target that this edit
            // crosses) rewrites the single pending alarm for this habit.
            habitReminderScheduler.reschedule(habit.id)
            // The sheet's slider picks a POSITION in the priority list, so
            // write it back through setPriorityRank: that renumbers every
            // habit to a unique 1..N instead of letting two habits end up
            // sharing the same number.
            if (newPriorityRank != null && newPriorityRank != habit.priorityRank) {
                setPriorityRank(habit.id, newPriorityRank)
            }
        }
    }

    /**
     * Deletes a habit (works for both boolean and numeric types)
     */
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            deleteHabit.invoke(habit)
            habitReminderScheduler.cancel(habit.id)
        }
    }

    // ============================================
    // DIALOG CONTROL
    // ============================================

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }

    fun onReorder(fromIndex: Int, toIndex: Int) {  // optimistic update (same pattern as the other reorderings in the app)
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

        // save in the database (update in the background)
        viewModelScope.launch {
            val positionUpdates = updatedHabits.map { it.habit.id to it.habit.position }
            updateHabitsOrder(positionUpdates)
        }
    }

    // ============================================
    // PRIORITY REORDER MODE (1 = highest priority, top of the list)
    // ============================================

    fun togglePriorityReorderMode() {
        _isPriorityReorderMode.value = !_isPriorityReorderMode.value
    }

    /**
     * Shared move logic for both dragging and "jump to position" — both are
     * really the same operation (take the priority-ordered list, remove one
     * item, reinsert it elsewhere, renumber everyone 1..N), just with the
     * target index arriving a different way.
     */
    private fun movePriorityItem(fromIndex: Int, toIndex: Int) {
        val currentState = _uiState.value as? HabitUiState.Success ?: return
        val priorityOrdered = currentState.habits.sortedBy { it.habit.priorityRank }.toMutableList()
        if (fromIndex !in priorityOrdered.indices) return
        val clampedTo = toIndex.coerceIn(0, priorityOrdered.lastIndex)

        val item = priorityOrdered.removeAt(fromIndex)
        priorityOrdered.add(clampedTo, item)

        val updatedHabits = priorityOrdered.mapIndexed { index, habitWithStatus ->
            habitWithStatus.copy(habit = habitWithStatus.habit.copy(priorityRank = index + 1))
        }

        // Merge the new ranks back into the full list, which may be sorted
        // differently (by `position`, for normal display) — this way,
        // reordering priority never disturbs the regular list's own order.
        val mergedHabits = currentState.habits.map { original ->
            updatedHabits.firstOrNull { it.habit.id == original.habit.id } ?: original
        }

        _uiState.value = currentState.copy(habits = mergedHabits)

        viewModelScope.launch {
            updateHabitsPriorityOrder(updatedHabits.map { it.habit.id to it.habit.priorityRank })
        }
    }

    /** Called by the drag gesture while in priority-reorder mode. */
    fun onPriorityReorder(fromIndex: Int, toIndex: Int) = movePriorityItem(fromIndex, toIndex)

    /**
     * Called by the slider and the "type a number" dialog. `newRank` IS the
     * stored priorityRank: a 1-based position where 1 = top of the priority
     * list. Convert to the 0-indexed list position right here.
     */
    fun setPriorityRank(habitId: Int, newRank: Int) {
        val currentState = _uiState.value as? HabitUiState.Success ?: return
        val priorityOrdered = currentState.habits.sortedBy { it.habit.priorityRank }
        val currentIndex = priorityOrdered.indexOfFirst { it.habit.id == habitId }
        if (currentIndex == -1) return
        movePriorityItem(currentIndex, newRank - 1)
    }

    /**
     * Bumps every habit at or below [requestedRank] up one slot so a new habit
     * can take that exact position, returning the clamped position to insert
     * at. With no loaded state there is nothing to shift — the requested rank
     * goes through untouched.
     */
    private suspend fun makeRoomForNewHabit(requestedRank: Int): Int {
        val currentState = _uiState.value as? HabitUiState.Success ?: return requestedRank
        if (currentState.habits.isEmpty()) return requestedRank
        val targetRank = requestedRank.coerceIn(1, currentState.habits.size + 1)
        val shifted = currentState.habits.map { withStatus ->
            val habit = withStatus.habit
            withStatus.copy(
                habit = habit.copy(
                    priorityRank = if (habit.priorityRank >= targetRank) habit.priorityRank + 1 else habit.priorityRank
                )
            )
        }
        _uiState.value = currentState.copy(habits = shifted)
        updateHabitsPriorityOrder(shifted.map { it.habit.id to it.habit.priorityRank })
        return targetRank
    }
}