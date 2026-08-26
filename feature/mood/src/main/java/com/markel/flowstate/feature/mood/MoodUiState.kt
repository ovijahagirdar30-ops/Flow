package com.markel.flowstate.feature.mood

import com.markel.flowstate.core.domain.MoodEntry

sealed interface MoodUiState {
    data object Loading : MoodUiState
    data class Success(val entries: List<MoodEntry>) : MoodUiState
}