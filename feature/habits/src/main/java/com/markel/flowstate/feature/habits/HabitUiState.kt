package com.markel.flowstate.feature.habits

import com.markel.flowstate.core.domain.HabitNumericEntry
import com.markel.flowstate.core.domain.HabitWithStatus
import java.time.LocalDate

data class PendingMoodPrompt(
    val habitId: Int,
    val date: LocalDate,
    val habitName: String
)

sealed interface HabitUiState {
    data object Loading : HabitUiState
    data class Success(
        val habits: List<HabitWithStatus>,
        val weekEntriesByHabit: Map<Int, Set<Long>>,
        val numericEntriesByHabit: Map<Int, List<HabitNumericEntry>>,
        val showAddDialog: Boolean = false,
        val completedToday: Int = 0,
        val totalHabits: Int = 0,
        val motivationalMessageIndex: Int = 0,
        val pendingMoodPrompt: PendingMoodPrompt? = null
    ) : HabitUiState
}