package com.markel.flowstate.feature.checkin

import com.markel.flowstate.core.domain.CheckinItem

sealed interface CheckinUiState {
    data object Loading : CheckinUiState
    data class Success(val items: List<CheckinItem>) : CheckinUiState
}