package com.markel.flowstate.feature.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markel.flowstate.core.domain.DayReview
import com.markel.flowstate.core.domain.EncouragementGenerator
import com.markel.flowstate.core.domain.usecase.checkin.GetDayReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NightReviewUiState(
    val isLoading: Boolean = true,
    /** Today's bucketed tasks, or null until the first load lands. */
    val review: DayReview? = null,
    /** The closing line; null while the generator is still working. */
    val message: String? = null,
)

/**
 * Backs the 9PM night check-in page: buckets today's tasks via
 * [GetDayReviewUseCase], then asks the [EncouragementGenerator] seam
 * (Gemini with a local fallback) for the closing line. The message load is
 * deliberately non-blocking — the lists render first, the line lands after.
 */
@HiltViewModel
class NightReviewViewModel @Inject constructor(
    private val getDayReview: GetDayReviewUseCase,
    private val encouragementGenerator: EncouragementGenerator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NightReviewUiState())
    val uiState: StateFlow<NightReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val review = getDayReview()
            _uiState.value = NightReviewUiState(isLoading = false, review = review)

            // Belt and braces: the seam contract says implementations degrade
            // instead of throwing, but a blank/failed line must never crash
            // the page — the UI simply keeps the loading placeholder out.
            val message = runCatching { encouragementGenerator.generate(review.stats()) }
                .getOrDefault("")
            _uiState.value = _uiState.value.copy(message = message.ifBlank { null })
        }
    }
}
