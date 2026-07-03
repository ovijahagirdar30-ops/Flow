package com.markel.flowstate.core.designsystem.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * Returns a Boolean state that controls the visibility of a FAB.
 * It hides when scrolling down and shows when scrolling up,
 * ALWAYS remaining visible at the top or bottom of the list.
 */
@Composable
fun rememberFabVisibilityState(
    lazyListState: LazyListState,
    forceVisible: Boolean = false,
    scrollThreshold: Int = 60
): State<Boolean> {
    val fabVisible = rememberSaveable { mutableStateOf(true) }

    if (forceVisible) {
        fabVisible.value = true
    }

    LaunchedEffect(lazyListState) {
        var previousIndex = 0
        var previousOffset = 0
        var accumulatedScroll = 0

        snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }
            // Avoid processing if the state hasn't changed at all
            .distinctUntilChanged()
            .collect { (currentIndex, currentOffset) ->
                // At the top of the list: Always force visibility regardless of the threshold
                if (currentIndex == 0 && currentOffset == 0) {
                    fabVisible.value = true
                    accumulatedScroll = 0
                } else {
                    // Accurate calculation of the real delta
                    val delta = if (currentIndex == previousIndex) {
                        currentOffset - previousOffset
                    } else {
                        // Item changed: assign the threshold value to trigger an immediate reaction
                        if (currentIndex > previousIndex) scrollThreshold else -scrollThreshold
                    }

                    // Reset accumulator if the scroll direction changes
                    if ((delta > 0 && accumulatedScroll < 0) || (delta < 0 && accumulatedScroll > 0)) {
                        accumulatedScroll = 0
                    }

                    accumulatedScroll += delta

                    // Check if it exceeded the required threshold
                    if (abs(accumulatedScroll) >= scrollThreshold) {
                        // If accumulated scroll is negative, we are scrolling up -> Show FAB
                        fabVisible.value = accumulatedScroll < 0
                        // Reset after applying the change
                        accumulatedScroll = 0
                    }
                }

                previousIndex = currentIndex
                previousOffset = currentOffset
            }
    }

    return fabVisible
}