package com.markel.flowstate.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.movableContentOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope

// ─────────────────────────────────────────────────────────────────────────────
// FlowStateSceneDecoratorStrategy
//
// Wraps NON-fullscreen scenes (the five tab keys) with a
// Scaffold that hosts the bottom navigation bar. Fullscreen scenes (detail
// screens, editors, settings sub-screens) carry the `FullScreenMeta` flag and
// pass through unwrapped, so they render edge-to-edge over the bar.
//
// The bottom bar is wrapped in `movableContentOf` so its internal state
// (Material 3 ripple, selection animation) survives the transition between the
// outgoing and incoming decorated scenes without recomposition.
//
// `sharedElement` + `cacheSize` keep the bar's placeholder box sized correctly
// in the non-visible scene during the transition, eliminating the flicker that
// a bare `if (showBar)` was causing.
//
// Inspired by the official `ResponsiveNavigationSceneDecoratorStrategy` recipe
// (https://developer.android.com/guide/navigation/navigation-3/recipes/navscenedecorator)
// simplified to bottom-bar-only (no nav rail).
// ─────────────────────────────────────────────────────────────────────────────

class FlowStateSceneDecoratorStrategy(
    private val sharedTransitionScope: SharedTransitionScope,
    private val bottomBarContent: @Composable () -> Unit,
) : SceneDecoratorStrategy<NavKey> {

    override fun SceneDecoratorStrategyScope<NavKey>.decorateScene(
        scene: Scene<NavKey>
    ): Scene<NavKey> {
        // Pass-through: fullscreen entries don't get the bar.
        // `scene.metadata` defaults to the last entry's metadata (see Scene docs).
        if (scene.metadata.get<Boolean>(FullScreenMeta) == true) return scene

        return FlowStateDecoratedScene(
            scene = scene,
            sharedTransitionScope = sharedTransitionScope,
            bottomBarContent = bottomBarContent,
        )
    }
}

@Composable
fun rememberFlowStateSceneDecoratorStrategy(
    sharedTransitionScope: SharedTransitionScope,
    bottomBar: @Composable () -> Unit,
): FlowStateSceneDecoratorStrategy {
    // Wrap once — the same movable instance is reused across every decorated
    // scene, so the bar's state moves with the visible scene instead of being
    // recomposed from scratch.
    val movableBottomBar = remember { movableContentOf { bottomBar() } }
    return remember(sharedTransitionScope) {
        FlowStateSceneDecoratorStrategy(
            sharedTransitionScope = sharedTransitionScope,
            bottomBarContent = movableBottomBar,
        )
    }
}

private class FlowStateDecoratedScene(
    private val scene: Scene<NavKey>,
    private val sharedTransitionScope: SharedTransitionScope,
    private val bottomBarContent: @Composable () -> Unit,
) : Scene<NavKey> by scene {

    override val key: Any = "flowstate_decorated" to scene.key

    override val content: @Composable () -> Unit = @Composable {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        // True only in the scene that is becoming visible. The other scene
        // (outgoing) keeps a sized placeholder via cacheSize so the shared
        // element animation is smooth.
        val isMovableContentCaller =
            animatedContentScope.transition.targetState == EnterExitState.Visible

        with(sharedTransitionScope) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    scene.content()
                }
                Box(
                    modifier = Modifier
                        // While the bar is rendered in the other scene, this
                        // box reuses the cached size to keep the layout stable.
                        .cacheSize(!isMovableContentCaller)
                        .sharedElement(
                            rememberSharedContentState("flowstate-bottom-bar"),
                            animatedContentScope
                        )
                ) {
                    bottomBarContent()  // no if-clause here, the bottom bar is shown even in the transition to a full-screen screen to avoid a flicker with the icons and labels
                }
            }
        }
    }
}
