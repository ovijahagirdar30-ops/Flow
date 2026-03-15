package com.markel.flowstate.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.usecase.habits.DeleteHabitUseCase
import com.markel.flowstate.core.domain.usecase.habits.GetHabitsWithStatusUseCase
import com.markel.flowstate.core.domain.usecase.habits.InsertHabitUseCase
import com.markel.flowstate.core.domain.usecase.habits.ToggleHabitEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val getHabitsWithStatus: GetHabitsWithStatusUseCase,
    private val insertHabit: InsertHabitUseCase,
    private val deleteHabit: DeleteHabitUseCase,
    private val toggleEntry: ToggleHabitEntryUseCase
) : ViewModel() {

    private val _showAddDialog = MutableStateFlow(false)

    val uiState = combine(
        getHabitsWithStatus(),
        habitRepository.getAllEntries(),
        _showAddDialog
    ) { habits, allEntries, showDialog ->
        val today = LocalDate.now()
        val weekStart = today.with(java.time.DayOfWeek.MONDAY)
        val weekDays = (0..6).map { weekStart.plusDays(it.toLong()).toEpochDay() }.toSet()

        val weekEntriesByHabit = allEntries
            .filter { it.epochDay in weekDays }
            .groupBy({ it.habitId }, { it.epochDay })
            .mapValues { it.value.toSet() }

        HabitUiState.Success(
            habits = habits,
            weekEntriesByHabit = weekEntriesByHabit,
            showAddDialog = showDialog,
            completedToday = habits.count { hw ->
                LocalDate.now().toEpochDay() in (weekEntriesByHabit[hw.habit.id] ?: emptySet())
            },
            totalHabits = habits.size,
            motivationalMessageIndex = LocalDate.now().dayOfYear % 7
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitUiState.Loading
    )

    fun toggleHabitOnDate(habitId: Int, date: LocalDate) {
        viewModelScope.launch { toggleEntry(habitId, date) }
    }

    fun addHabit(name: String, iconName: String, colorArgb: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            insertHabit(Habit(name = name, iconName = iconName, colorArgb = colorArgb))
            _showAddDialog.value = false
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { deleteHabit.invoke(habit) }
    }

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }
}