package com.markel.flowstate.feature.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.usecase.checkin.GetCheckinItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckinViewModel @Inject constructor(
    private val getCheckinItems: GetCheckinItemsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckinUiState>(CheckinUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCheckinItems().collect { items ->
                _uiState.value = CheckinUiState.Success(items)
            }
        }
    }
}