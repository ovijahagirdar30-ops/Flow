package com.markel.flowstate.feature.habits

import com.markel.flowstate.core.domain.HabitWithStatus

sealed interface HabitUiState {
    data object Loading : HabitUiState
    data class Success(
        val habits: List<HabitWithStatus>,
        val weekEntriesByHabit: Map<Int, Set<Long>>,
        val showAddDialog: Boolean = false
    ) : HabitUiState
}