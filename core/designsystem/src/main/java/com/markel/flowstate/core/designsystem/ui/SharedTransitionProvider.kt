package com.markel.flowstate.core.designsystem.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocals that expose the SharedTransition scopes to any
 * composable in the tree without the need to pass them as parameters.
 *
 * Usage from any composable:
 *
 * val sharedTransition = LocalSharedTransitionScope.current
 * val animatedVisibility = LocalAnimatedVisibilityScope.current
 *
 * // If either is null, it simply doesn't animate (graceful degradation)
 * if (sharedTransition != null && animatedVisibility != null) {
 * with(sharedTransition) { Modifier.sharedBounds(...) }
 * }
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }