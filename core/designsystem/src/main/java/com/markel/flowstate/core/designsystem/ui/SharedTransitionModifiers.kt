package com.markel.flowstate.core.designsystem.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Expansion modifier of type "Container Transform".
 *
 * @param key  Unique key identifying the source-destination pair. Must be
 * identical at both ends of the transition.
 * @param shape Shape of the source container for clipping during the animation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedCardBounds(key: Any, shape: RoundedCornerShape = RoundedCornerShape(12.dp)): Modifier {
    val sharedTransition = LocalSharedTransitionScope.current ?: return this
    val animatedVisibility = LocalAnimatedVisibilityScope.current ?: return this

    return with(sharedTransition) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibility,
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
            clipInOverlayDuringTransition = OverlayClip(shape),
            enter = fadeIn(tween(durationMillis = 160)),
            exit  = fadeOut(tween(durationMillis = 80)),
            boundsTransform = { _, _ ->
                tween(durationMillis = 280, easing = FastOutSlowInEasing)
            }
        )
    }
}

/**
 * Expansion modifier of type "Container Transform" for the DESTINATION (detail screen).
 * The container is resized in every frame → the background color expands.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedDetailBounds(key: Any): Modifier {
    val sharedTransition = LocalSharedTransitionScope.current ?: return this
    val animatedVisibility = LocalAnimatedVisibilityScope.current ?: return this

    return with(sharedTransition) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibility,
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(0.dp)),
            enter = fadeIn(tween(durationMillis = 220, delayMillis = 50, easing = FastOutSlowInEasing)),
            exit  = fadeOut(tween(durationMillis = 100)),
            boundsTransform = { _, _ ->
                tween(durationMillis = 250, easing = FastOutSlowInEasing)
            }
        )
    }
}