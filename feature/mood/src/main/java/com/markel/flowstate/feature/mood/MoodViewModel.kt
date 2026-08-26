package com.markel.flowstate.feature.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.usecase.habits.GetMoodHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodViewModel @Inject constructor(
    private val getMoodHistory: GetMoodHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoodUiState>(MoodUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMoodHistory().collect { entries ->
                _uiState.value = MoodUiState.Success(entries)
            }
        }
    }
}